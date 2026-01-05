package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTile;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;


class DebugPoint {
    public final String str;
    public final Vector2 pos;

    public DebugPoint(String s, Vector2 v){
        str = s;
        pos = v;
    }
}


public class MapDebugLayer extends MapLayer {

    private Batch mapBatch;
    private ShapeRenderer mapSr;
    private Viewport mapViewPort;

    private BitmapFont font;


    public MapDebugLayer(){
        super();
    }


    @Override
    public void onAddedToMap(Map map) {
        mapBatch = map.getBatch();
        mapSr = map.getShapeRenderer();
        mapViewPort = map.getViewport();

        //Test freetype font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;

        font = generator.generateFont(parameter);
        generator.dispose();
    }


    @Override
    public void update(Map map, float dt) {}


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        mapSr.setProjectionMatrix(mapBatch.getProjectionMatrix());
        mapSr.begin(ShapeRenderer.ShapeType.Line);

        Array<DebugPoint> points = new Array<>();

        int mapZoom = map.getMapZoom();


        //Draw tile bounds
        mapSr.setColor(1.f, 0.48f, 0.26f, 1.f);

        for(int ty = visibleRegion.startTileY; ty <= visibleRegion.endTileY; ty++){
            for(int tx = visibleRegion.startTileX; tx <= visibleRegion.endTileX; tx++){

                GeoPoint tileTopLeft = MapTile.getTileGeoPoint(tx, ty, mapZoom);
                GeoPoint tileBottomRight = MapTile.getTileGeoPoint(tx + 1, ty - 1, mapZoom);

                Vector2 topLeft = GeoPoint.toWorldCoordinates(tileTopLeft);
                Vector2 bottomRight = GeoPoint.toWorldCoordinates(tileBottomRight);


                float width = bottomRight.x - topLeft.x;
                float height = bottomRight.y - topLeft.y;

                topLeft.y -= height;

                mapSr.line(topLeft.x, topLeft.y, topLeft.x + width, topLeft.y);
                mapSr.line(topLeft.x, topLeft.y + height, topLeft.x + width, topLeft.y + height);
                mapSr.line(topLeft.x, topLeft.y, topLeft.x, topLeft.y + height);
                mapSr.line(topLeft.x + width, topLeft.y, topLeft.x + width, topLeft.y + height);

                Vector2 center = new Vector2(topLeft.x + width / 2.f, topLeft.y + height / 2.f);
                points.add(new DebugPoint(tx + "_" + ty, map.project(center)));
            }
        }


        //Draw map bounds
        mapSr.setColor(Color.WHITE);

        Vector2 topLeft = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMin, AppConfig.MAP_BOUNDS.yMax);
        Vector2 topRight = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMax, AppConfig.MAP_BOUNDS.yMax);
        Vector2 bottomLeft = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMin, AppConfig.MAP_BOUNDS.yMin);
        Vector2 bottomRight = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMax, AppConfig.MAP_BOUNDS.yMin);

        mapSr.line(topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y);
        mapSr.line(topLeft.x, topLeft.y, topRight.x, topRight.y);
        mapSr.line(bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y);
        mapSr.line(topRight.x, topRight.y, bottomRight.x, bottomRight.y);
        
        mapSr.end();



        /*
            Screen space rendering here...
        */

        Matrix4 uiMatrix = mapViewPort.getCamera().combined.cpy();
        uiMatrix.setToOrtho2D(mapViewPort.getScreenX(), mapViewPort.getScreenY(), mapViewPort.getScreenWidth(), mapViewPort.getScreenHeight());
        mapBatch.setProjectionMatrix(uiMatrix);
        mapBatch.begin();

        GlyphLayout gl = new GlyphLayout();

        for(DebugPoint pp : points){
            gl.setText(font, pp.str);
            font.draw(mapBatch, pp.str, pp.pos.x - gl.width / 2.f, pp.pos.y + gl.height / 2.f);
        }


        font.draw(mapBatch, String.format("Rendered tiles: %d", visibleRegion.getNumTiles()), 10.f, 128.f);
        font.draw(mapBatch, String.format("Camera zoom: %.4f", map.getCamera().zoom), 10.f, 96.f);
        font.draw(mapBatch, String.format("Map zoom: %d", mapZoom), 10.f, 64.f);

        mapBatch.end();
    }


    @Override
    public void onKeyDown(int key) {
        if(key == Input.Keys.NUMPAD_0){
            setEnabled(!isEnabled());
        }
    }


    @Override
    public void onKeyUp(int key) {}


    @Override
    public void dispose() {
        font.dispose();
    }

}
