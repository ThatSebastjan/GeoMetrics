package si.um.feri.GeoMetrics.navigation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Screen;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.layer.DisasterReportLayer;
import si.um.feri.GeoMetrics.map.layer.LandLotLayer;
import si.um.feri.GeoMetrics.map.layer.MapDebugLayer;
import si.um.feri.GeoMetrics.map.layer.MapGeoJsonLayer;
import si.um.feri.GeoMetrics.map.tile.MapTileManager;

public class DisasterScreen extends ScreenAdapter {
    private final GdxMap game;
    private Stage stage;
    private Skin skin;

    private final AssetManager assetManager;
    private Map map;


    public DisasterScreen(GdxMap game, Skin skin) {
        this.game = game;
        this.skin = skin;

        assetManager = game.getAssetManager();

        create();
    }

    private void create() {
        stage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label center = new Label("Disaster Screen", skin);
        center.setColor(Color.WHITE);
        center.setVisible(false);

        Table nav = navBar();

        root.add(center).expand().center();
        root.add(nav).width(160).center().padRight(20);
    }

    @Override
    public void show() {
        MapTileManager tileManager = new MapTileManager(assetManager, AppConfig.TILE_DATA_PATH, AppConfig.TILE_MAP_MIN_ZOOM, AppConfig.TILE_MAP_MAX_ZOOM);
        map = new Map(game.getBatch(), tileManager);

        //Add map layers
        map.addLayer(new MapGeoJsonLayer(Gdx.files.internal("assets/static/si_border.json").path()));

        map.addLayer(new DisasterReportLayer());
        //map.addLayer(new MapDebugLayer());


        InputMultiplexer m = new InputMultiplexer(stage, map);
        Gdx.input.setInputProcessor(m);
    }

    private Table navBar() {
        Table nav = new Table();
        nav.defaults().pad(8).fillX().width(160).height(48);
        TextButton toHeat = new TextButton("HeatMap", skin);
        TextButton toDisaster = new TextButton("Disaster", skin);
        TextButton toPlot = new TextButton("Plot", skin);

        toHeat.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                final Screen newScreen = new HeatMapScreen(game, skin);
                Gdx.app.postRunnable(new Runnable() {
                    @Override public void run() {
                        Screen old = game.getScreen();
                        game.setScreen(newScreen);
                        if (old != null) old.dispose();
                    }
                });
            }
        });
        toDisaster.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                final Screen newScreen = new DisasterScreen(game, skin);
                Gdx.app.postRunnable(new Runnable() {
                    @Override public void run() {
                        Screen old = game.getScreen();
                        game.setScreen(newScreen);
                        if (old != null) old.dispose();
                    }
                });
            }
        });
        toPlot.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                final Screen newScreen = new PlotScreen(game, skin);
                Gdx.app.postRunnable(new Runnable() {
                    @Override public void run() {
                        Screen old = game.getScreen();
                        game.setScreen(newScreen);
                        if (old != null) old.dispose();
                    }
                });
            }
        });

        nav.add(toHeat).row();
        nav.add(toDisaster).row();
        nav.add(toPlot).row();
        return nav;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.f, 0.f, 0.f, 1.f);

        //Update map
        map.update(delta);

        //Render map
        map.render(delta);

        //Update and render UI
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        map.onResize(width, height);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        map.dispose();
    }
}
