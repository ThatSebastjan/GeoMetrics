package si.um.feri.GeoMetrics.navigation;

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
import com.badlogic.gdx.Screen;
import si.um.feri.GeoMetrics.GdxMap;

public class DisasterScreen extends ScreenAdapter {
    private final GdxMap game;
    private Stage stage;
    private Skin skin;

    public DisasterScreen(GdxMap game, Skin skin) {
        this.game = game;
        this.skin = skin;
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
        Gdx.input.setInputProcessor(stage);
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
