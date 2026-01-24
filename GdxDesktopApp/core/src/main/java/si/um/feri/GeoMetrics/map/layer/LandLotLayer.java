package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;

import javax.swing.text.View;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class LandLotLayer extends MapGeoJsonLayer {

    private Map owner;
    private Bbox previousViewBounds;
    private HashSet<Integer> objectIdSet;
    private BitmapFont font;

    private OkHttpClient httpClient;


    public LandLotLayer() {
        super(null);

        previousViewBounds = new Bbox(0, 0, 0, 0);
        objectIdSet = new HashSet<>();
        httpClient = new OkHttpClient();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.borderWidth = 1.f;
        parameter.borderColor = Color.BLACK;
        parameter.color = Color.WHITE;

        font = generator.generateFont(parameter);
        generator.dispose();
    }


    @Override
    public void onAddedToMap(Map map) {
        owner = map;
        mapBatch = map.getBatch();
        map.addCallback(LandLotLayer.this::onViewChanged);
    }


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        if(map.getMapZoomLevelFloat() < 13.5f){
            return; //Minimum zoom for rendering land lots
        }


        GeoPoint topLeft = map.unproject(new Vector2(0.f, 0.f));
        GeoPoint bottomRight = map.unproject(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        Bbox viewBounds = Bbox.fromPoints(topLeft, bottomRight);

        Gdx.gl.glEnable(GL32.GL_BLEND);
        Gdx.gl.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);


        //Set shader constants
        ShaderProgram shaderProgram = MapGeoJsonLayer.getGeoJsonShader();
        shaderProgram.bind();
        shaderProgram.setUniformMatrix("u_projTrans", mapBatch.getProjectionMatrix());
        shaderProgram.setUniformf("u_inColor", new Color(1.f, 0.f, 0.f, 0.4f));


        //Query visible items
        Envelope visibleEnvelope = new Envelope(viewBounds.xMin, viewBounds.xMax, viewBounds.yMin, viewBounds.yMax);
        List<GeoJsonGeometry> visibleGeometry = quadtree.query(visibleEnvelope);


        //Query items near cursor
        GeoPoint mapMousePos = map.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        Envelope hoverEnvelope = new Envelope(
            mapMousePos.longitude - viewBounds.width * 0.025f,
            mapMousePos.longitude + viewBounds.width * 0.025f,
            mapMousePos.latitude - viewBounds.height * 0.025f,
            mapMousePos.latitude + viewBounds.height * 0.025f
        );

        List<GeoJsonGeometry> hoveredGeometry = quadtree.query(hoverEnvelope);

        //System.out.printf("hoveredGeometry size: %d / %d\n", hoveredGeometry.size(), visibleGeometry.size());


        //Render infill only on hovered!
        Vector2 mousePointWorld = GeoPoint.toWorldCoordinates(mapMousePos);
        Point mousePoint = new Point(new Coordinate(mousePointWorld.x, mousePointWorld.y), new PrecisionModel(), 1);

        //Render infill
        for(GeoJsonGeometry geom : hoveredGeometry) {

            if(!geom.infillPolygon.contains(mousePoint)){
                continue;
            }

            geom.infillMesh.render(shaderProgram, GL32.GL_TRIANGLES);
        }


        //Render outlines
        //shaderProgram.setUniformf("u_inColor", new Color(0.5f, 0.313f, 0.f, 1.f));
        shaderProgram.setUniformf("u_inColor", new Color(1.f, 208.f / 255.f, 0, 1.f));

        Gdx.gl.glLineWidth(2.f);
        Gdx.gl.glDisable(GL32.GL_CULL_FACE);
        Gdx.gl.glDisable(GL32.GL_DEPTH_TEST);

        for(GeoJsonGeometry geom : visibleGeometry) {
            geom.outlineMesh.render(shaderProgram, GL32.GL_LINE_STRIP);
        }

        Gdx.gl.glLineWidth(1.f);


        //Render labels
        Viewport mapViewport = map.getViewport();

        Matrix4 uiMatrix = mapViewport.getCamera().combined.cpy();
        uiMatrix.setToOrtho2D(mapViewport.getScreenX(), mapViewport.getScreenY(), mapViewport.getScreenWidth(), mapViewport.getScreenHeight());

        mapBatch.setProjectionMatrix(uiMatrix);
        mapBatch.begin();


        float unitWidth = (float)(Gdx.graphics.getWidth() / (bottomRight.longitude - topLeft.longitude));


        GlyphLayout gl = new GlyphLayout();

        for(GeoJsonGeometry geom : visibleGeometry) {
            String label = geom.getProperties().getString("ST_PARCELE");
            Vector2 worldPos = geom.getCenter();
            Vector2 screenPos = map.project(worldPos.cpy());

            gl.setText(font, label);

            Bbox geomBbox = geom.getBbox();

            if((geomBbox.width * unitWidth * 0.5f) < gl.width || ((geomBbox.height * unitWidth * 0.5f) < gl.height)){
                continue; //Text doesn't fix in bounding box + some padding
            }

            font.draw(mapBatch, label, screenPos.x - gl.width / 2.f, screenPos.y + gl.height / 2.f);
        }

        mapBatch.end();
    }


    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }


    private void onViewChanged(Bbox viewBounds){
        System.out.printf("onViewChanged: %.4f, %.4f, %.4f, %.4f\n", viewBounds.xMin, viewBounds.yMin, viewBounds.xMax, viewBounds.yMax);

        //Only query data if we are zoomed in enough
        if(owner.getMapZoomLevelFloat() < 13.5f){
            return;
        }

        //Query new data
        ArrayList<GeoJsonGeometry> newGeometry = new ArrayList<>();

        if(!queryNewFeatures(viewBounds, previousViewBounds, newGeometry)){
            return;
        }

        //Add data...
        int numInserted = 0;

        for(GeoJsonGeometry geom : newGeometry){
            int geomId = geom.getId();

            //Skip already added
            if(objectIdSet.contains(geomId)){
                continue;
            }

            if(geom.infillMesh == null){
                continue; //Triangulation for some may fail; skip them
            }

            objectIdSet.add(geomId);
            quadtree.insert(geom.getEnvelopeBounds(), geom);
            numInserted++;
        }

        System.out.printf("Queried %d results (inserted: %d)\n", newGeometry.size(), numInserted);

        previousViewBounds = viewBounds;
    }


    private boolean queryNewFeatures(Bbox newBounds, Bbox oldBounds, ArrayList<GeoJsonGeometry> results){
        String url = AppConfig.BACKEND_URL + "/map/query/" + bboxToString(oldBounds) + "," + bboxToString(newBounds);
        Request request = new Request.Builder().url(url).build();

        System.out.println(url);

        try {
            Response response = httpClient.newCall(request).execute();

            if(response.code() == 200){
                String resBody = response.body().string();
                JSONObject resObj = new JSONObject(resBody);
                JSONArray data = resObj.getJSONArray("data");

                for(int i = 0; i < data.length(); i++){
                    JSONObject item = data.getJSONObject(i);
                    int itemId = item.getInt("id"); //Every feature MUST have a unique id

                    GeoJsonGeometry geom = GeoJsonGeometry.fromJson(item, itemId, false);
                    results.add(geom);
                }

                return true;
            }
            else {
                System.out.printf("Error: queryNewFeatures response code %d\n", response.code());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    private String bboxToString(Bbox b){
        return String.format("%.6f,%.6f,%.6f,%.6f", b.xMin, b.yMin, b.xMax, b.yMax);
    }
}
