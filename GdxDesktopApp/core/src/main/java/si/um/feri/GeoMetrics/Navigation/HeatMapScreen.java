package si.um.feri.GeoMetrics.Navigation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class HeatMapScreen extends ScreenAdapter {
    public enum DisasterType { LANDSILDE, FLOOD, EARTHQUAKE }

    private final Game game;
    private Stage stage;
    private Skin skin;
    private Label centerLabel;
    private DisasterType current = DisasterType.LANDSILDE;

    public HeatMapScreen(Game game, Skin skin) {
        this.game = game;
        this.skin = skin;
        create();
    }

    private void create() {
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        centerLabel = new Label("HeatMap: " + current.name(), skin);
        centerLabel.setColor(Color.WHITE);

        Table nav = navBar();

        Table bottom = new Table();
        // sensible defaults for bottom buttons
        bottom.defaults().pad(8).width(160).height(44);
        TextButton bLandslide = new TextButton("LANDSILDE", skin);
        TextButton bFlood = new TextButton("FLOOD", skin);
        TextButton bEarth = new TextButton("EARTHQUAKE", skin);

        bLandslide.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.LANDSILDE); }
        });
        bFlood.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.FLOOD); }
        });
        bEarth.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { setType(DisasterType.EARTHQUAKE); }
        });

        bottom.add(bLandslide).padRight(8);
        bottom.add(bFlood).padRight(8);
        bottom.add(bEarth);

        root.add(centerLabel).expand().center();
        root.add(nav).width(160).center().padRight(20);
        root.row();
        root.add(bottom).colspan(2).center().bottom().padBottom(20);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    private void setType(DisasterType t) {
        current = t;
        centerLabel.setText("HeatMap: " + current.name());
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
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
