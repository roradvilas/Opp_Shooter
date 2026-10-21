package game;

import strategy.MovementType;

import java.awt.event.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class Input implements KeyListener, MouseListener, MouseMotionListener {
    private final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private volatile boolean mouseDown;
    private volatile int mouseX;
    private volatile int mouseY;

    public void reset() { keys.clear(); mouseDown = false; }

    public boolean isKeyDown(int key) { return keys.contains(key); }
    public MovementType movementType() {
        if (isKeyDown(KeyEvent.VK_SPACE)) return MovementType.DASH;
        if (isKeyDown(KeyEvent.VK_CONTROL)) return MovementType.SNEAK;
        if (isKeyDown(KeyEvent.VK_SHIFT)) return MovementType.SPRINT;
        return MovementType.WALK;
    }
    public boolean isMouseDown() { return mouseDown; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }

    @Override public void keyPressed(KeyEvent e) { keys.add(e.getKeyCode()); }
    @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseDown = true;
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseDown = false;
    }
    @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
