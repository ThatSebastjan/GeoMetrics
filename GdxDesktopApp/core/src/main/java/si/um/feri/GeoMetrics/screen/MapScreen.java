package si.um.feri.GeoMetrics.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.MapTile;
import si.um.feri.GeoMetrics.map.Utils;

import java.util.HashMap;

import static com.badlogic.gdx.math.MathUtils.lerp;


/*
    This is a Proof-of-concept implementation
    Requires A LOT of refactoring :)
*/


public class MapScreen extends ScreenAdapter implements InputProcessor {

    private final GdxMap mainInstance;
    private final AssetManager assetManager;

    private OrthographicCamera camera;
    private Viewport viewport;

    private ShapeRenderer sr;
    private BitmapFont font;

    private float mapZoom = 9.0f;
    private float targetZoom = 1.f; //Target camera zoom for interpolation
    private HashMap<Long, MapTile> tileHashMap = new HashMap<>();

    private Vector2 lastTouchPoint = new Vector2();
    private Vector2 lastMousePos = new Vector2();


    //Min / max zoom for out tileset - should probably move somewhere else
    private static final float minMapZoom = 9.f;
    private static final float maxMapZoom = 15.f;




    public MapScreen(GdxMap mainInstance){
        this.mainInstance = mainInstance;
        assetManager = mainInstance.getAssetManager();
    }


    @Override
    public void show(){
        camera = new OrthographicCamera();
        //viewport = new FitViewport(AppConfig.WORLD_WIDTH, AppConfig.WORLD_HEIGHT, camera);
        viewport = new FillViewport(AppConfig.WORLD_WIDTH, AppConfig.WORLD_HEIGHT, camera);

        sr = new ShapeRenderer();

        //Test freetype font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;

        font = generator.generateFont(parameter);
        generator.dispose();

        //Enable input handling
        Gdx.input.setInputProcessor(this);


        //Set initial camera position
        Vector2 worldCenter = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.getCenter());
        camera.position.set(worldCenter.x, worldCenter.y, 0.f);
    }


    @Override
    public void resize(int width, int height){
        viewport.update(width, height, false);
    }


    @Override
    public void render(float dt){


        //Update asset manager
        if(!assetManager.isFinished()) {
            assetManager.update();
        }


        //Handle zoom updates
        if(Math.abs(targetZoom - camera.zoom) > 0.0001f) {

            camera.zoom = lerp(camera.zoom, targetZoom, 0.08f);
            mapZoom = getMapZoomLevel(camera.zoom);

            //TODO: load visible tiles on mapZoom change; Only update mapZoom once all are loaded!


            Vector3 mouseWorldLocOld = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));

            //Translate so world location under mouse remains constant in screen space (zoom on "constant" position)
            camera.update(); //Force update camera

            Vector3 mouseWorldLocNew = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));
            Vector3 mDelta = mouseWorldLocNew.cpy().sub(mouseWorldLocOld);

            camera.position.sub(mDelta.x, mDelta.y, 0.f);
        }




        int intZoom = (int)mapZoom;

        int topTile = Utils.latitudeToTile(AppConfig.MAP_BOUNDS.yMin, intZoom);
        int leftTile = Utils.longitudeToTile(AppConfig.MAP_BOUNDS.xMin, intZoom);
        int bottomTile = Utils.latitudeToTile(AppConfig.MAP_BOUNDS.yMax, intZoom);
        int rightTile = Utils.longitudeToTile(AppConfig.MAP_BOUNDS.xMax, intZoom);

        //int numTilesX = Math.abs(leftTile - rightTile) + 1;
        //int numTilesY = Math.abs(topTile - bottomTile) + 1;


        //Render
        ScreenUtils.clear(0.5f, 0.5f, 0.f, 1.f);

        SpriteBatch batch = mainInstance.getBatch();


        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();



        //Draw only visible tiles
        Vector3 screenTopLeft = viewport.unproject(new Vector3(0.f, 0.f, 0.f));
        Vector3 screenBottomRight = viewport.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.f));

        GeoPoint pointTopLeft = GeoPoint.fromWorldCoordinates(screenTopLeft.x, screenTopLeft.y);
        GeoPoint pointBottomRight = GeoPoint.fromWorldCoordinates(screenBottomRight.x, screenBottomRight.y);

        int startTx = Utils.longitudeToTile(pointTopLeft.longitude, intZoom);
        int endTx = Utils.longitudeToTile(pointBottomRight.longitude, intZoom);

        int startTy = Utils.latitudeToTile(pointTopLeft.latitude, intZoom);
        int endTy = Utils.latitudeToTile(pointBottomRight.latitude, intZoom);

        //Limit to available tile bounds
        startTx = Math.max(startTx, leftTile);
        endTx = Math.min(endTx, rightTile);

        startTy = Math.max(startTy, bottomTile);
        endTy = Math.min(endTy, topTile);


        for(int ty = startTy; ty <= endTy; ty++){
            for(int tx = startTx; tx <= endTx; tx++){

                double lonStart = Utils.tileToLongitude(tx, intZoom);
                double lonEnd = Utils.tileToLongitude(tx + 1, intZoom);

                double latStart = Utils.tileToLatitude(ty, intZoom);
                double latEnd = Utils.tileToLatitude(ty - 1, intZoom);

                Vector2 p1 = GeoPoint.toWorldCoordinates(lonStart, latStart);
                Vector2 p2 = GeoPoint.toWorldCoordinates(lonEnd, latEnd);

                float width = p2.x - p1.x;
                float height = p2.y - p1.y;

                MapTile tile = getTile(tx, ty, true);

                if(tile == null){
                    continue; //Pending load...
                }

                batch.draw(tile.texture, p1.x, p1.y - height, width, height);
            }
        }

        batch.end();


        //DEBUG RENDERING
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.begin(ShapeRenderer.ShapeType.Line);


        class Tmp {
            public final String str;
            public final Vector2 pos;

            public Tmp(String s, Vector2 v){
                str = s;
                pos = v;
            }
        }

        Array<Tmp> points = new Array<>();


        //Draw tile bounds
        sr.setColor(1.f, 0.48f, 0.26f, 1.f);

        for(int ty = startTy; ty <= endTy; ty++){
            for(int tx = startTx; tx <= endTx; tx++){

                double lonStart = Utils.tileToLongitude(tx, intZoom);
                double lonEnd = Utils.tileToLongitude(tx + 1, intZoom);

                double latStart = Utils.tileToLatitude(ty, intZoom);
                double latEnd = Utils.tileToLatitude(ty - 1, intZoom);

                Vector2 p1 = GeoPoint.toWorldCoordinates(lonStart, latStart);
                Vector2 p2 = GeoPoint.toWorldCoordinates(lonEnd, latEnd);


                float width = p2.x - p1.x;
                float height = p2.y - p1.y;

                p1.y -= height;

                sr.line(p1.x, p1.y, p1.x + width, p1.y);
                sr.line(p1.x, p1.y + height, p1.x + width, p1.y + height);
                sr.line(p1.x, p1.y, p1.x, p1.y + height);
                sr.line(p1.x + width, p1.y, p1.x + width, p1.y + height);

                Vector2 center = new Vector2(p1.x + width / 2.f, p1.y + height / 2.f);
                points.add(new Tmp((tx) + "_" + (ty), viewport.project(center)));
            }
        }


        //Draw map bounds
        sr.setColor(Color.WHITE);

        Vector2 topLeft = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMin, AppConfig.MAP_BOUNDS.yMax);
        Vector2 topRight = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMax, AppConfig.MAP_BOUNDS.yMax);
        Vector2 bottomLeft = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMin, AppConfig.MAP_BOUNDS.yMin);
        Vector2 bottomRight = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMax, AppConfig.MAP_BOUNDS.yMin);

        sr.line(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y);
        sr.line(topLeft.x, topLeft.y, topRight.x, topRight.y);
        sr.line(bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y);
        sr.line(topRight.x, topRight.y, bottomRight.x, bottomRight.y);


        sr.end();



        /*
            Screen space rendering here...
        */

        Matrix4 uiMatrix = viewport.getCamera().combined.cpy();
        uiMatrix.setToOrtho2D(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
        batch.setProjectionMatrix(uiMatrix);
        batch.begin();

        GlyphLayout gl = new GlyphLayout();

        for(Tmp pp : points){
            gl.setText(font, pp.str);
            font.draw(batch, pp.str, pp.pos.x - gl.width / 2.f, pp.pos.y + gl.height / 2.f);
        }


        font.draw(batch, String.format("Rendered tiles: %d", (endTx - startTx + 1) * (endTy - startTy + 1)), 10.f, 128.f);
        font.draw(batch, String.format("Camera zoom: %.4f", camera.zoom), 10.f, 96.f);
        font.draw(batch, String.format("Map zoom: %.2f", mapZoom), 10.f, 64.f);

        batch.end();
    }


    @Override
    public void hide(){
        dispose();
    }


    @Override
    public void dispose() {

    }


    //Get or load and cache tile on demand
    MapTile getTile(int tx, int ty, boolean TEMPORARY_SYNC_LOAD){
        int zoom = (int)mapZoom;

        Long hashKey = MapTile.getHashKey(tx, ty, zoom);

        if(tileHashMap.containsKey(hashKey)){
            return tileHashMap.get(hashKey);
        }

        String filePath = AppConfig.TILE_DATA_PATH + "/" + zoom + "/" + tx + "_" + ty + ".jpg";

        if(assetManager.isLoaded(filePath)){
            Texture texture = assetManager.get(filePath, Texture.class);
            MapTile tile = new MapTile(tx, ty, zoom, texture);
            tileHashMap.put(hashKey, tile);

            return tile;
        }

        assetManager.load(filePath, Texture.class);

        //TODO: TEMPORARY SYNC LOADING! FREEZES APP ON ZOOM
        if(TEMPORARY_SYNC_LOAD){
            assetManager.finishLoading();

            Texture texture = assetManager.get(filePath, Texture.class);
            MapTile tile = new MapTile(tx, ty, zoom, texture);
            tileHashMap.put(hashKey, tile);

            return tile;
        }

        return null; //Pending load
    }


    //Get map tileset "zoom" / detail level based on camera zoom
    float getMapZoomLevel(float camZoom){
        if(camZoom > 0.5f){
            return 9.f;
        }
        else if(camZoom > 0.25f){
            return 10.f;
        }
        else if(camZoom > 0.125f){
            return 11.f;
        }
        else if(camZoom > 0.0625f){
            return 12.f;
        }
        else if(camZoom > 0.03125f){
            return 13.f;
        }
        else if(camZoom > .015625f){
            return 14.f;
        }

        return 15.f;
    }


    //Zoom speed based on zoom
    float getZoomDelta(float camZoom){
        float baseDelta = 0.01f;
        return (float)(baseDelta / (1 + Math.log10(1 / camZoom)));
    }


    @Override
    public boolean keyDown(int i) {
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {

        //Mouse left down
        if(button == Input.Buttons.LEFT){
            lastTouchPoint.set(x, y);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        Vector2 dragDelta = new Vector2(x, y).sub(lastTouchPoint);
        lastTouchPoint.add(dragDelta);

        Vector2 basePos = viewport.unproject(new Vector2(0.f, 0.f));
        Vector2 worldDragDelta = viewport.unproject(dragDelta);
        camera.position.sub(worldDragDelta.x - basePos.x, worldDragDelta.y - basePos.y, 0.f);
        camera.update();

        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        lastMousePos.set(x, y);
        return true;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        targetZoom += v1 * getZoomDelta(targetZoom);
        targetZoom = Math.max(targetZoom, 0.001f); //Limit max zoom
        targetZoom = Math.min(targetZoom, 1.f);

        return true;
    }
}
