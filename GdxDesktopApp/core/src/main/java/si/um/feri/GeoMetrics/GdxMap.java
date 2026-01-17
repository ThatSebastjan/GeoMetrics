package si.um.feri.GeoMetrics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Logger;
import si.um.feri.GeoMetrics.navigation.HeatMapScreen;


public class GdxMap extends Game {

    private SpriteBatch batch;
    private AssetManager assetManager;
    private Skin skin;


    @Override
    public void create() {
        batch = new SpriteBatch();

        assetManager = new AssetManager();
        assetManager.getLogger().setLevel(Logger.DEBUG);
        assetManager.load("assets/ui/ui.atlas", TextureAtlas.class);
        assetManager.load("assets/ui/ui.json", Skin.class, new SkinLoader.SkinParameter("assets/ui/ui.atlas"));
        assetManager.finishLoading();

        skin = assetManager.get("assets/ui/ui.json", Skin.class);
        setScreen(new HeatMapScreen(this, skin));
    }


    @Override
    public void dispose() {
        super.dispose();
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
