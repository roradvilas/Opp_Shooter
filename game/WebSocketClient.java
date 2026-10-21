package game;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class WebSocketClient {
    public interface Listener {
        void onText(String text);
        void onClose();
    }

    private final String host;
    private final int port;
    private final Listener listener;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private final Random random = new SecureRandom();

    public WebSocketClient(String host, int port, Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() throws Exception {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);

        in = socket.getInputStream();
        out = socket.getOutputStream();

        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        String key = Base64.getEncoder().encodeToString(nonce);

        String request =
                "GET / HTTP/1.1\r\n" +
                "Host: " + host + ":" + port + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: " + key + "\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "\r\n";

        out.write(request.getBytes(StandardCharsets.US_ASCII));
        out.flush();

        String headers = readHttpHeaders();
        if (headers == null) {
            throw new IOException("WebSocket handshake failed: no response");
        }

        String[] lines = headers.split("\r\n");
        if (lines.length == 0 || !lines[0].contains("101")) {
            throw new IOException("WebSocket handshake failed: " + lines[0]);
        }

        new Thread(this::readLoop, "websocket-reader").start();
    }

    private String readHttpHeaders() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        while (buffer.size() <= 16384) {
            int b = in.read();
            if (b < 0) return null;

            buffer.write(b);

            if ((matched == 0 && b == '\r') ||
                (matched == 1 && b == '\n') ||
                (matched == 2 && b == '\r') ||
                (matched == 3 && b == '\n')) {
                matched++;
                if (matched == 4) {
                    return buffer.toString(StandardCharsets.US_ASCII.name());
                }
            } else {
                matched = (b == '\r') ? 1 : 0;
            }
        }
        throw new IOException("HTTP headers too large");
    }

    public synchronized void sendText(String text) throws IOException {
        if (out == null) throw new IOException("WebSocket is not connected");

        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        writeFrame(0x1, data);
    }

    private synchronized void sendPong(byte[] payload) throws IOException {
        if (payload.length > 125) throw new IOException("Ping payload too large");
        writeFrame(0xA, payload);
    }

    private void writeFrame(int opcode, byte[] data) throws IOException {
        out.write(0x80 | (opcode & 0x0F));

        if (data.length < 126) {
            out.write(0x80 | data.length);
        } else if (data.length <= 65535) {
            out.write(0x80 | 126);
            out.write((data.length >>> 8) & 255);
            out.write(data.length & 255);
        } else {
            out.write(0x80 | 127);
            long n = data.length;
            for (int i = 7; i >= 0; i--) {
                out.write((int)(n >>> (i * 8)) & 255);
            }
        }

        byte[] mask = new byte[4];
        random.nextBytes(mask);
        out.write(mask);

        for (int i = 0; i < data.length; i++) {
            out.write(data[i] ^ mask[i & 3]);
        }
        out.flush();
    }

    private void readLoop() {
        try {
            while (true) {
                int b0 = in.read();
                int b1 = in.read();
                if (b0 < 0 || b1 < 0) break;

                int opcode = b0 & 0x0F;
                boolean fin = (b0 & 0x80) != 0;
                long len = b1 & 0x7F;

                if (len == 126) len = readUnsignedShort();
                else if (len == 127) len = readLong();

                if (len > 2_000_000) throw new IOException("Frame too large");

                // This client currently expects complete server text frames.
                if (!fin) throw new IOException("Fragmented frames are not supported");

                byte[] payload = readFully((int)len);

                if (opcode == 8) break;
                if (opcode == 9) {
                    sendPong(payload);
                    continue;
                }
                if (opcode == 1) {
                    listener.onText(new String(payload, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        } finally {
            listener.onClose();
        }
    }

    private int readUnsignedShort() throws IOException {
        int a = in.read(), b = in.read();
        if ((a | b) < 0) throw new EOFException();
        return (a << 8) | b;
    }

    private long readLong() throws IOException {
        long n = 0;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b < 0) throw new EOFException();
            n = (n << 8) | b;
        }
        return n;
    }

    private byte[] readFully(int n) throws IOException {
        byte[] b = new byte[n];
        int off = 0;
        while (off < n) {
            int got = in.read(b, off, n - off);
            if (got < 0) throw new EOFException();
            off += got;
        }
        return b;
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
