package si.um.feri.GeoMetrics.cef;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.cef.CefApp;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.*;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;
import org.lwjgl.opengl.WGL;
import si.um.feri.GeoMetrics.util.AppLogger;

import java.awt.*;


public class GdxCefInstance {

    public static String MSG_ROUTER_QUERY = "cefQuery";
    public static String MSG_ROUTER_CANCEL = "cefQueryCancel";

    private static ShaderProgram shaderProgram;
    private static Mesh mesh;

    private static CefApp cefApp;
    private static CefClient cefClient;
    private static CefBrowserOsrGdx browser;
    private static CefMessageRouter messageRouter;

    private static boolean init_once = true;
    private static boolean init_context = true;

    private static long gl_hdc = -1;
    private static long gl_hglrc = -1;


    private GdxCefInstance(){}


    public static boolean isInitialized(){
        return !init_once;
    }


    //Must be called on start-up before usage!
    public static void init(String startUrl, CefMessageRouterHandler msgHandler, int frameRate){

        AppLogger.info("GdxCefInstance.init() called");
        AppLogger.info("Start URL: " + startUrl);
        AppLogger.info("Frame rate: " + frameRate);

        if(!init_once){
            AppLogger.error("GdxCefInstance::init() already called!");
            throw new RuntimeException("GdxCefInstance::init() already called!");
        }

        init_once = false;

        try {
            //Create screen quad mesh and shaders
            AppLogger.info("Creating mesh...");
            createMesh();
            AppLogger.info("Mesh created successfully");

            AppLogger.info("Loading shaders...");
            shaderProgram = new ShaderProgram(
                Gdx.files.internal("assets/shaders/cef/vertex.glsl").readString(),
                Gdx.files.internal("assets/shaders/cef/fragment.glsl").readString()
            );

            if (!shaderProgram.isCompiled()) {
                AppLogger.error("Shader compilation failed: " + shaderProgram.getLog());
                throw new RuntimeException("Shader compilation failed: " + shaderProgram.getLog());
            }
            AppLogger.info("Shaders loaded successfully");


            String[] args = new String[]{
                "--off-screen-rendering-enabled",
                "--shared-texture-enabled"
            };

            AppLogger.info("Starting CEF with args: " + String.join(", ", args));
            CefApp.startup(args);
            AppLogger.info("CefApp.startup() completed");

            AppLogger.info("Adding CEF app handler...");
            CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
                @Override
                public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                    AppLogger.info("CEF state changed to: " + state);
                    if (state == CefApp.CefAppState.TERMINATED){
                        AppLogger.info("CEF terminated, exiting application");
                        System.exit(0); //Terminate if CEF exists
                    }
                }
            });
            AppLogger.info("CEF app handler added");


            AppLogger.info("Creating CEF settings...");
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = true;
            AppLogger.info("CEF settings configured");

            AppLogger.info("Getting CEF instance...");
            cefApp = CefApp.getInstance(args, settings);
            AppLogger.info("CEF instance obtained");

            AppLogger.info("Creating CEF client...");
            cefClient = cefApp.createClient();
            AppLogger.info("CEF client created");


            //Create and register message router for handling js communication
            AppLogger.info("Creating message router...");
            CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig();
            config.jsQueryFunction = MSG_ROUTER_QUERY;
            config.jsCancelFunction = MSG_ROUTER_CANCEL;

            messageRouter = CefMessageRouter.create(config);
            messageRouter.addHandler(msgHandler, true);

            cefClient.addMessageRouter(messageRouter);
            AppLogger.info("Message router registered");


            //Create browser
            AppLogger.info("Creating browser settings...");
            CefBrowserSettings browserSettings = new CefBrowserSettings();
            browserSettings.shared_texture_enabled = true;
            browserSettings.external_begin_frame_enabled = true;
            browserSettings.windowless_frame_rate = frameRate;
            AppLogger.info("Browser settings configured");


            AppLogger.info("Creating CEF browser...");
            browser = new CefBrowserOsrGdx(cefClient, startUrl, true, CefRequestContext.getGlobalContext(), browserSettings);
            AppLogger.info("Browser instance created");

            AppLogger.info("Calling browser.createImmediately()...");
            browser.createImmediately();
            AppLogger.info("Browser initialized successfully");

            AppLogger.info("GdxCefInstance initialization completed successfully");

        } catch (Exception e) {
            AppLogger.error("Fatal error during GdxCefInstance initialization", e);
            throw e;
        }
    }


    private static void createMesh(){
        float[] verts = new float[30];
        int i = 0;


        verts[i++] = -1.f;
        verts[i++] = 1.f;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i++] = 0f;

        verts[i++] = 1.f;
        verts[i++] = 1.f;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 0f;

        verts[i++] = -1.f;
        verts[i++] = -1.f;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i++] = 1f;

        verts[i++] = 1.f;
        verts[i++] = 1.f;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 0f;

        verts[i++] = 1.f;
        verts[i++] = -1.f;
        verts[i++] = 0;
        verts[i++] = 1f;
        verts[i++] = 1f;

        verts[i++] = -1.f;
        verts[i++] = -1.f;
        verts[i++] = 0;
        verts[i++] = 0f;
        verts[i] = 1f;


        mesh = new Mesh(true, 6, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));

        mesh.setVertices(verts);
    }


    public static CefClient getCefClient(){
        return cefClient;
    }


    public static CefBrowserOsrGdx getBrowser(){
        return browser;
    }


    //Must be called at the beginning of render method, if using CEF UI!
    public static void onRenderBegin(){

        try {
            Gdx.g_window_mutex.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        //Get context on first call
        if(init_context){
            init_context = false;

            gl_hdc = WGL.wglGetCurrentDC();
            gl_hglrc = WGL.wglGetCurrentContext();
            browser.getRenderer().setGLContext(gl_hdc, gl_hglrc);
        }


        //Set context
        if(!WGL.wglMakeCurrent(gl_hdc, gl_hglrc)){
            throw new RuntimeException(String.format("GdxCefInstance::onRenderBegin(): failed to make current context: %d\n", Gdx.gl.glGetError()));
        }
    }


    //Must be called at the end of render method, if using CEF UI!
    public static void onRenderEnd(){

        //Clear context
        if(!WGL.wglMakeCurrent(0, 0)){
            throw new RuntimeException(String.format("GdxCefInstance::onRenderEnd(): Failed to clear current context: %d\n", Gdx.gl.glGetError()));
        }

        Gdx.g_window_mutex.release();
    }


    //Must be called on main render thread!
    //Draws CEF shared texture
    public static void render(){

        //Check for resize (can't resize on resize callback as g_window_mutex is locked there)
        Rectangle r = browser.getViewRect(browser);
        int bWidth = (int)r.getWidth();
        int bHeight = (int)r.getHeight();

        if (bWidth != Gdx.graphics.getWidth() || bHeight != Gdx.graphics.getHeight()) {
            browser.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            //System.out.printf("Resize!\n");
        }


        //Draw texture, if it exists
        CefGdxRenderer renderer = browser.getRenderer();

        if(renderer.getSharedTexture() != -1){

            Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            Gdx.gl20.glEnable(GL20.GL_TEXTURE_2D);
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            Gdx.gl20.glBindTexture(GL32.GL_TEXTURE_2D, renderer.getSharedTexture());


            shaderProgram.begin();
            shaderProgram.setUniformi("u_texture", 0);
            mesh.render(shaderProgram, GL20.GL_TRIANGLES);
            shaderProgram.end();

        }

        //Trigger new frame
        browser.sendExternalBeginFrame();


        //DEBUG TEST RELOAD
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            browser.reload();
        }

    }


    //Must be called on app shutdown, otherwise cef helper instances remain running and the program doesn't exit
    public static void dispose(){
        AppLogger.info("Disposing GdxCefInstance...");
        try {
            if (cefClient != null) {
                AppLogger.info("Disposing CEF client...");
                cefClient.dispose();
            }
            if (cefApp != null) {
                AppLogger.info("Disposing CEF app...");
                cefApp.dispose();
            }
            AppLogger.info("GdxCefInstance disposed successfully");
        } catch (Exception e) {
            AppLogger.error("Error during GdxCefInstance disposal", e);
        } finally {
            AppLogger.close();
            System.exit(0);
        }
    }


}
