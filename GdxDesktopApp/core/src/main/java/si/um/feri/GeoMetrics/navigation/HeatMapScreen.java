package si.um.feri.GeoMetrics.navigation;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.layer.*;
import si.um.feri.GeoMetrics.map.tile.MapTileManager;


public class HeatMapScreen extends ScreenAdapter {
    public enum DisasterType { LANDSLIDE, FLOOD, EARTHQUAKE, NONE }

    private final GdxMap game;
    private Stage stage;
    private Skin skin;
    private Label centerLabel;
    private DisasterType current = DisasterType.NONE;

    private final AssetManager assetManager;
    private Map map;

    private FloodHeatMapLayer floodHeatmap;
    private LandslideHeatMapLayer landslideHeatmap;
    private EarthquakeHeatMapLayer earthquakeHeatmap;

    // Track buttons for highlighting
    private TextButton bLandslide;
    private TextButton bFlood;
    private TextButton bEarth;
    private TextButton bNone;

    public HeatMapScreen(GdxMap game, Skin skin) {
        this.game = game;
        this.skin = skin;

        assetManager = game.getAssetManager();

        floodHeatmap = new FloodHeatMapLayer();
        landslideHeatmap = new LandslideHeatMapLayer();
        earthquakeHeatmap = new EarthquakeHeatMapLayer();
        // Start with all disabled
        floodHeatmap.setEnabled(false);
        landslideHeatmap.setEnabled(false);
        earthquakeHeatmap.setEnabled(false);

        create();
    }

    private void create() {
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        centerLabel = new Label("HeatMap: " + current.name(), skin);
        centerLabel.setColor(Color.WHITE);
        centerLabel.setVisible(false);

        Table nav = navBar();

        Table bottom = new Table();
        // sensible defaults for bottom buttons
        bottom.defaults().pad(8).width(160).height(44);
        bLandslide = new TextButton("LANDSLIDE", skin);
        bFlood = new TextButton("FLOOD", skin);
        bEarth = new TextButton("EARTHQUAKE", skin);
        bNone = new TextButton("NONE", skin);

        bLandslide.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.LANDSLIDE); }
        });
        bFlood.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.FLOOD); }
        });
        bEarth.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.EARTHQUAKE); }
        });
        bNone.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.NONE); }
        });

        bottom.add(bLandslide).padRight(8);
        bottom.add(bFlood).padRight(8);
        bottom.add(bEarth).padRight(8);
        bottom.add(bNone);

        root.add(centerLabel).expand().center();
        root.add(nav).width(160).center().padRight(20);
        root.row();
        root.add(bottom).colspan(2).center().bottom().padBottom(20);

        // Set initial button state
        updateButtonStates();
    }

    @Override
    public void show() {

        MapTileManager tileManager = new MapTileManager(assetManager, AppConfig.TILE_DATA_PATH, AppConfig.TILE_MAP_MIN_ZOOM, AppConfig.TILE_MAP_MAX_ZOOM);
        map = new Map(game.getBatch(), tileManager);

        //Add map layers
        map.addLayer(new MapGeoJsonLayer(Gdx.files.internal("assets/static/si_border.json").path()));

        map.addLayer(floodHeatmap);
        map.addLayer(landslideHeatmap);
        map.addLayer(earthquakeHeatmap);
        map.addLayer(new MapDebugLayer());


        InputMultiplexer m = new InputMultiplexer(stage, map);
        Gdx.input.setInputProcessor(m);
    }

    private void setType(DisasterType t) {
        current = t;
        //centerLabel.setText("HeatMap: " + current.name());

        switch (t){
            case FLOOD:
                floodHeatmap.setEnabled(true);
                landslideHeatmap.setEnabled(false);
                earthquakeHeatmap.setEnabled(false);
                break;

            case LANDSLIDE:
                floodHeatmap.setEnabled(false);
                landslideHeatmap.setEnabled(true);
                earthquakeHeatmap.setEnabled(false);
                break;

            case EARTHQUAKE:
                floodHeatmap.setEnabled(false);
                landslideHeatmap.setEnabled(false);
                earthquakeHeatmap.setEnabled(true);
                break;

            case NONE:
                floodHeatmap.setEnabled(false);
                landslideHeatmap.setEnabled(false);
                earthquakeHeatmap.setEnabled(false);
                break;
        }

        updateButtonStates();
    }

    private void updateButtonStates() {
        // Reset all buttons to default state
        bLandslide.setChecked(false);
        bFlood.setChecked(false);
        bEarth.setChecked(false);
        bNone.setChecked(false);

        // Highlight the currently selected button
        switch (current) {
            case LANDSLIDE:
                bLandslide.setChecked(true);
                break;
            case FLOOD:
                bFlood.setChecked(true);
                break;
            case EARTHQUAKE:
                bEarth.setChecked(true);
                break;
            case NONE:
                bNone.setChecked(true);
                break;
        }
    }

    private Table navBar() {
        Table nav = new Table();
        // sensible defaults for nav buttons: wider and taller for readability
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
