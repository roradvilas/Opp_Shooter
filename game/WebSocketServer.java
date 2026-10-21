package game;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class WebSocketServer {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public WebSocketServer(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        socket.setTcpNoDelay(true);
    }

    public boolean handshake() throws Exception {
        String headers = readHttpHeaders();
        if (headers == null) return false;

        String[] lines = headers.split("\r\n");
        if (lines.length == 0 || !lines[0].startsWith("GET ")) return false;

        String key = null;
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0 &&
                lines[i].substring(0, colon).trim().equalsIgnoreCase("Sec-WebSocket-Key")) {
                key = lines[i].substring(colon + 1).trim();
                break;
            }
        }
        if (key == null || key.isEmpty()) return false;

        String accept = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(
                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                    .getBytes(StandardCharsets.US_ASCII)
            )
        );

        String response =
            "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: " + accept + "\r\n" +
            "\r\n";

        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return true;
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
            out.write(data.length);
        } else if (data.length <= 65535) {
            out.write(126);
            out.write((data.length >>> 8) & 255);
            out.write(data.length & 255);
        } else {
            out.write(127);
            long n = data.length;
            for (int i = 7; i >= 0; i--) out.write((int)(n >>> (i * 8)) & 255);
        }

        out.write(data);
        out.flush();
    }

    public String readText() throws IOException {
        while (true) {
            int b0 = in.read();
            if (b0 < 0) return null;
            int b1 = in.read();
            if (b1 < 0) return null;

            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;

            if (len == 126) len = readUnsignedShort();
            else if (len == 127) len = readLong();

            if (len > 1_000_000) throw new IOException("Frame too large");
            if (!masked) throw new IOException("Client frame must be masked");

            byte[] mask = readFully(4);
            byte[] payload = readFully((int)len);
            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];

            if (opcode == 8) return null;
            if (opcode == 9) {
                sendPong(payload);
                continue;
            }
            if (opcode != 1) continue;

            return new String(payload, StandardCharsets.UTF_8);
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

    public void close() throws IOException {
        socket.close();
    }
}
