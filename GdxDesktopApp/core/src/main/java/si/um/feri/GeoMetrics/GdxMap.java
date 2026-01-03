package si.um.feri.GeoMetrics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Logger;
import si.um.feri.GeoMetrics.screen.MapScreen;


public class GdxMap extends Game {

    private SpriteBatch batch;
    private AssetManager assetManager;


    @Override
    public void create() {
        batch = new SpriteBatch();

        assetManager = new AssetManager();
        assetManager.getLogger().setLevel(Logger.DEBUG);

        setScreen(new MapScreen(this));
    }


    @Override
    public void dispose() {
        //assetManager.dispose();
        batch.dispose();
    }



    public AssetManager getAssetManager() {
        return assetManager;
    }


    public SpriteBatch getBatch() {
        return batch;
    }

}
