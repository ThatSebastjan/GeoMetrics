package si.um.feri.GeoMetrics.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.GeoMetrics.config.AppConfig;
import static com.badlogic.gdx.math.MathUtils.lerp;



public class Map implements InputProcessor, Disposable {

    private OrthographicCamera camera;
    private Viewport viewport;

    private ShapeRenderer sr;
    private BitmapFont font;

    private SpriteBatch batch;

    private MapTileManager tileManager;


    private int mapZoom; //Map tile zoom level
    private float targetZoom = 1.f; //Target camera zoom for interpolation
    private final float minCameraZoom = 0.001f;
    private final float maxCameraZoom = 1f;



    private Vector2 lastTouchPoint = new Vector2();
    private Vector2 lastMousePos = new Vector2();



    public Map(SpriteBatch batch, MapTileManager tileManager){
        camera = new OrthographicCamera();
        viewport = new FillViewport(AppConfig.WORLD_WIDTH, AppConfig.WORLD_HEIGHT, camera);
        sr = new ShapeRenderer();

        this.batch = batch;
        this.tileManager = tileManager;

        init();
    }


    private void init(){

        //Test freetype font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;

        font = generator.generateFont(parameter);
        generator.dispose();


        //Set initial camera position
        Vector2 worldCenter = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.getCenter());
        camera.position.set(worldCenter.x, worldCenter.y, 0.f);

        //Set initial zoom level
        mapZoom = tileManager.getMinZoom();
    }


    @Override
    public void dispose() {
        sr.dispose();
        font.dispose();
    }


    //Should be called on resize event
    public void onResize(int width, int height){
        viewport.update(width, height, false);
    }


    public void update(float dt){

        //Handle zoom updates
        if(Math.abs(targetZoom - camera.zoom) > 0.0001f) {

            camera.zoom = lerp(camera.zoom, targetZoom, 0.08f);
            mapZoom = tileManager.getMapZoomLevel(camera.zoom);

            //TODO: load visible tiles on mapZoom change; Only update mapZoom once all are loaded!


            Vector3 mouseWorldLocOld = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));

            //Translate so world location under mouse remains constant in screen space (zoom on "constant" position)
            camera.update(); //Force update camera

            Vector3 mouseWorldLocNew = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));
            Vector3 mDelta = mouseWorldLocNew.cpy().sub(mouseWorldLocOld);

            camera.position.sub(mDelta.x, mDelta.y, 0.f);
        }

    }


    public void render(float dt){

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();


        MapTileRegion visibleRegion = getVisibleTileRegion();

        for(int tileY = visibleRegion.startTileY; tileY <= visibleRegion.endTileY; tileY++){
            for(int tileX = visibleRegion.startTileX; tileX <= visibleRegion.endTileX; tileX++){

                GeoPoint tileTopLeft = MapTile.getTileGeoPoint(tileX, tileY, mapZoom);
                GeoPoint tileBottomRight = MapTile.getTileGeoPoint(tileX + 1, tileY - 1, mapZoom);

                Vector2 topLeft = GeoPoint.toWorldCoordinates(tileTopLeft);
                Vector2 bottomRight = GeoPoint.toWorldCoordinates(tileBottomRight);

                float width = bottomRight.x - topLeft.x;
                float height = bottomRight.y - topLeft.y;

                MapTile tile = tileManager.getTile(tileX, tileY, mapZoom);

                batch.draw(tile.texture, topLeft.x, topLeft.y - height, width, height);
            }
        }

        batch.end();


        //DEBUG RENDER
        debugRender(dt, visibleRegion);
    }


    //DEBUG RENDERING
    void debugRender(float dt, MapTileRegion visibleRegion){

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

        for(int ty = visibleRegion.startTileY; ty <= visibleRegion.endTileY; ty++){
            for(int tx = visibleRegion.startTileX; tx <= visibleRegion.endTileX; tx++){

                GeoPoint tileTopLeft = MapTile.getTileGeoPoint(tx, ty, mapZoom);
                GeoPoint tileBottomRight = MapTile.getTileGeoPoint(tx + 1, ty - 1, mapZoom);

                Vector2 topLeft = GeoPoint.toWorldCoordinates(tileTopLeft);
                Vector2 bottomRight = GeoPoint.toWorldCoordinates(tileBottomRight);


                float width = bottomRight.x - topLeft.x;
                float height = bottomRight.y - topLeft.y;

                topLeft.y -= height;

                sr.line(topLeft.x, topLeft.y, topLeft.x + width, topLeft.y);
                sr.line(topLeft.x, topLeft.y + height, topLeft.x + width, topLeft.y + height);
                sr.line(topLeft.x, topLeft.y, topLeft.x, topLeft.y + height);
                sr.line(topLeft.x + width, topLeft.y, topLeft.x + width, topLeft.y + height);

                Vector2 center = new Vector2(topLeft.x + width / 2.f, topLeft.y + height / 2.f);
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


        font.draw(batch, String.format("Rendered tiles: %d", visibleRegion.getNumTiles()), 10.f, 128.f);
        font.draw(batch, String.format("Camera zoom: %.4f", camera.zoom), 10.f, 96.f);
        font.draw(batch, String.format("Map zoom: %d", mapZoom), 10.f, 64.f);

        batch.end();
    }


    MapTileRegion getVisibleTileRegion(){

        //Get map bounds
        MapTileRegion mapTileBounds = MapTileManager.getMapTileBounds(mapZoom);

        //Get visible tile range
        Vector3 screenTopLeft = viewport.unproject(new Vector3(0.f, 0.f, 0.f));
        Vector3 screenBottomRight = viewport.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.f));

        GeoPoint pointTopLeft = GeoPoint.fromWorldCoordinates(screenTopLeft.x, screenTopLeft.y);
        GeoPoint pointBottomRight = GeoPoint.fromWorldCoordinates(screenBottomRight.x, screenBottomRight.y);

        //Convert and limit
        MapTileRegion visibleRegion = MapTileRegion.fromLngLat(pointTopLeft, pointBottomRight, mapZoom);
        visibleRegion.limitToBounds(mapTileBounds); //Limit to dataset bounds

        return visibleRegion;
    }


    //Zoom speed based on zoom
    float getZoomDelta(float camZoom){
        float baseDelta = 0.01f;
        return (float)(baseDelta / (1 + Math.log10(1 / camZoom)));
    }



    /*
        Input processor methods
    */

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

        return true;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        lastMousePos.set(x, y);
        return true;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        targetZoom += v1 * getZoomDelta(targetZoom);
        targetZoom = Math.max(targetZoom, minCameraZoom); //Limit max zoom
        targetZoom = Math.min(targetZoom, maxCameraZoom);

        return true;
    }
}
