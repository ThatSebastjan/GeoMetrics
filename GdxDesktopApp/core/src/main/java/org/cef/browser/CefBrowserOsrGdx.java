package org.cef.browser;


import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.handler.CefAcceleratedPaintInfo;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;



public class CefBrowserOsrGdx extends CefBrowserOsr {

    //For tracking full repaints
    private int lastWidth = 0;
    private int lastHeight = 0;

    private final CefGdxRenderer renderer;

    //For tracking input state
    private int mouseButton = 0;
    private long lastClickTime = 0;
    private int btnMask = 0;
    private int clicks = 0;


    public CefBrowserOsrGdx(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings) {
        super(client, url, transparent, context, settings);

        renderer = new CefGdxRenderer();
    }


    public CefGdxRenderer getRenderer(){
        return renderer;
    }


    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        throw new RuntimeException("TODO: CefBrowserOsrGdx::onPaint()");
    }


    @Override
    public void onAcceleratedPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, CefAcceleratedPaintInfo info) {

        //Nothing to update
        if (dirtyRects.length == 0) {
            return;
        }

        //Refuse 1x1 rectangles
        if (info.width <= 1 || info.height <= 1 ||
            dirtyRects[0].width <= 1 || dirtyRects[0].height <= 1) {
            return;
        }

        int width = info.width;
        int height = info.height;

        if (lastWidth != width || lastHeight != height) {
            Rectangle rect = dirtyRects[0];

            if (rect.width == width && rect.height == height && rect.x == 0 && rect.y == 0) {
                lastWidth = width;
                lastHeight = height;
            } else {
                //Likely an outdated paint-call?
                return;
            }
        }

        if (!popup) {
            renderer.onAcceleratedPaint(info, width, height);
        } else {
            System.err.println("CefBrowserOsrGdx::onAcceleratedPaint(): popups not supported!");
        }

        //super.onAcceleratedPaint(browser, popup, dirtyRects, info);
    }


    public void resize(int width, int height) {
        browser_rect_.setBounds(0, 0, width, height);
        wasResized(width, height);
    }


    /*
        Input event simulation
    */

    public void sendKeyPress(int keyCode, int modifiers) {
        KeyEvent e = new KeyEvent(getUIComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char)keyCode);
        sendKeyEvent(e);
    }

    public void sendKeyRelease(int keyCode, int modifiers) {
        KeyEvent e = new KeyEvent(getUIComponent(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char)keyCode);
        sendKeyEvent(e);
    }

    public void sendKeyTyped(char c, int modifiers) {
        KeyEvent e = new KeyEvent(getUIComponent(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), modifiers, KeyEvent.VK_UNDEFINED, c);
        sendKeyEvent(e);
    }

    public void sendMouseMove(int mouseX, int mouseY) {
        MouseEvent e = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, mouseX, mouseY, clicks, false, mouseButton);
        sendMouseEvent(e);
    }

    public void sendMousePress(int mouseX, int mouseY, int button) {

        if (button == 0) {
            btnMask |= MouseEvent.BUTTON1_MASK;
        } else if (button == 1) {
            btnMask |= MouseEvent.BUTTON2_MASK;
        } else if (button == 2) {
            btnMask |= MouseEvent.BUTTON3_MASK;
        }

        // Double click handling
        long time = System.currentTimeMillis();
        clicks = time - lastClickTime < 500 ? 2 : 1;

        MouseEvent e = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_PRESSED, time, 0, mouseX, mouseY, clicks, false, button);
        sendMouseEvent(e);

        this.lastClickTime = time;
        this.mouseButton = button;
    }

    //TODO: it may be necessary to add modifiers here
    public void sendMouseRelease(int mouseX, int mouseY, int button) {

        if (button == 0 && (btnMask & MouseEvent.BUTTON1_MASK) != 0) {
            btnMask ^= MouseEvent.BUTTON1_MASK;
        } else if (button == 1 && (btnMask & MouseEvent.BUTTON2_MASK) != 0) {
            btnMask ^= MouseEvent.BUTTON2_MASK;
        } else if (button == 2 && (btnMask & MouseEvent.BUTTON3_MASK) != 0) {
            btnMask ^= MouseEvent.BUTTON3_MASK;
        }

        MouseEvent e = new MouseEvent(getUIComponent(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, mouseX, mouseY, clicks, false, button);
        sendMouseEvent(e);
        this.mouseButton = 0;
    }

    public void sendMouseWheel(int mouseX, int mouseY, double amount) {
        MouseWheelEvent e = new MouseWheelEvent(getUIComponent(), MouseWheelEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, mouseX, mouseY, clicks, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, (int)amount, 1);
        sendMouseWheelEvent(e);
    }
}
