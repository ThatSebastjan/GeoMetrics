package si.um.feri.GeoMetrics.screen;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.ScreenUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.cef.GdxCefInput;
import si.um.feri.GeoMetrics.cef.GdxCefInstance;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.layer.LandLotLayer;
import si.um.feri.GeoMetrics.map.layer.MapDebugLayer;
import si.um.feri.GeoMetrics.map.layer.MapGeoJsonLayer;
import si.um.feri.GeoMetrics.map.tile.MapTileManager;

import java.io.File;



public class CefUITestScreen extends ScreenAdapter implements CefMessageRouterHandler {

    private final GdxMap mainInstance;
    private final AssetManager assetManager;
    private Map map;


    public CefUITestScreen(GdxMap mainInstance){
        this.mainInstance = mainInstance;
        assetManager = mainInstance.getAssetManager();
    }



    @Override
    public void show(){

        if(!GdxCefInstance.isInitialized()){

            String fileUrl = new File("./core/src/main/html/test.html").getAbsolutePath();
            //int refreshRate = Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1;
            int refreshRate = 60;

            GdxCefInstance.init(fileUrl, this, refreshRate);
        }

        //Input processor to forward events to UI
        GdxCefInput cefInputProcessor = new GdxCefInput(GdxCefInstance.getBrowser());


        //Create map
        MapTileManager tileManager = new MapTileManager(assetManager, AppConfig.TILE_DATA_PATH, 9, 15);
        map = new Map(mainInstance.getBatch(), tileManager);


        //Add map layers
        map.addLayer(new MapGeoJsonLayer(Gdx.files.internal("assets/static/si_border.json").path()));
        map.addLayer(new LandLotLayer());

        map.addLayer(new MapDebugLayer());


        //Set input handlers
        InputMultiplexer inputMultiplexer = new InputMultiplexer(cefInputProcessor, map);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }


    @Override
    public void resize(int width, int height){
        map.onResize(width, height);
    }


    @Override
    public void render(float dt){

        //onRenderBegin must be the first thing that is called on render
        GdxCefInstance.onRenderBegin();


        //Render stuff
        ScreenUtils.clear(1.f, 0.f, 0.f, 1.f);

        //Update and render map
        map.update(dt);
        map.render(dt);

        //Render UI on top
        GdxCefInstance.render();


        //onRenderEnd must be the last thing that is called on render
        GdxCefInstance.onRenderEnd();
    }


    @Override
    public void hide(){
        dispose();
    }


    @Override
    public void dispose() {
        map.dispose();
        GdxCefInstance.dispose();
    }



    /*
        CEF MessageRouter methods below
    */


    //This is used to receive data from CEF
    /*
        Call window.cefQuery to trigger this

        Example:

        window.cefQuery({
            request: "request name",
            persistent: false,
            onSuccess: function(response) {...},
            onFailure: function(error_code, error_message) {...}
        });
    */
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        System.out.printf("onQuery: %d, %s\n", queryId, request);
        callback.success("Resp test!");
        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {}

    @Override
    public void setNativeRef(String identifier, long nativeRef) {}

    @Override
    public long getNativeRef(String identifier) {
        return 0;
    }



}
