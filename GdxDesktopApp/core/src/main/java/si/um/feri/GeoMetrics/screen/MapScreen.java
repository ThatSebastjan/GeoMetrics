package si.um.feri.GeoMetrics.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.ScreenUtils;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.*;


public class MapScreen extends ScreenAdapter {

    private final GdxMap mainInstance;
    private final AssetManager assetManager;

    private Map map;
    private MapTileManager tileManager;


    public MapScreen(GdxMap mainInstance){
        this.mainInstance = mainInstance;
        assetManager = mainInstance.getAssetManager();
    }


    @Override
    public void show(){
        tileManager = new MapTileManager(assetManager, AppConfig.TILE_DATA_PATH, 9, 15);
        map = new Map(mainInstance.getBatch(), tileManager);

        //Set map input handling
        Gdx.input.setInputProcessor(map);
    }


    @Override
    public void resize(int width, int height){
        map.onResize(width, height);
    }


    @Override
    public void render(float dt){

        ScreenUtils.clear(0.f, 0.f, 0.f, 1.f);

        //Update map
        map.update(dt);

        //Render map
        map.render(dt);
    }


    @Override
    public void hide(){
        dispose();
    }


    @Override
    public void dispose() {
        map.dispose();
    }
}
