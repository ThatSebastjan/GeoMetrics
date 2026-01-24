package si.um.feri.GeoMetrics.cef;


import com.badlogic.gdx.InputProcessor;
import org.cef.browser.CefBrowserOsrGdx;



public class GdxCefInput implements InputProcessor {

    private CefBrowserOsrGdx browser;

    private int lastMouseX = 0;
    private int lastMouseY = 0;


    public GdxCefInput(CefBrowserOsrGdx browser){
        this.browser = browser;
    }


    /*
        Input processor methods
    */

    @Override
    public boolean keyDown(int i) {

        if(GdxCefInputMap.getJavaScriptKeyCode(i) != null){
            i = GdxCefInputMap.getJavaScriptKeyCode(i);
            browser.sendKeyPress(i, 0);
            //return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int i) {

        if(GdxCefInputMap.getJavaScriptKeyCode(i) != null){
            i = GdxCefInputMap.getJavaScriptKeyCode(i);
            browser.sendKeyRelease(i, 0);
            //return true;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        browser.sendKeyTyped(c, 0);
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {

        if(button == 0){
            button = 1;
        }
        else if(button == 2){
            button = 0;
        }

        browser.sendMousePress(x, y, button);
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {

        if(button == 0){
            button = 1;
        }
        else if(button == 2){
            button = 0;
        }

        browser.sendMouseRelease(x, y, button);
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
        browser.sendMouseMove(x, y);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        browser.sendMouseWheel(lastMouseX, lastMouseY, -30*amountY);
        return false;
    }
}
