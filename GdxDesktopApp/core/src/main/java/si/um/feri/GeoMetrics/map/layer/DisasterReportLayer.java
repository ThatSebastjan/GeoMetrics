package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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



class DisasterReport {
    public final String type;
    public final int severity;
    public final GeoPoint location;

    public DisasterReport(String type, int severity, GeoPoint location){
        this.type = type;
        this.severity = severity;
        this.location = location;
    }
}



public class DisasterReportLayer extends MapLayer {

    private static final float iconSize = 128.f;

    private Map owner;
    private Batch mapBatch;
    private ShapeRenderer mapSr;

    private ArrayList<DisasterReport> reports;
    private OkHttpClient httpClient;

    Texture floodMarker;
    Texture landslideMarker;
    Texture earthquakeMarker;


    public DisasterReportLayer() {
        super();

        httpClient = new OkHttpClient();

        floodMarker = new Texture(Gdx.files.internal("./assets/markers/flood.png"));
        landslideMarker = new Texture(Gdx.files.internal("./assets/markers/landslide.png"));
        earthquakeMarker = new Texture(Gdx.files.internal("./assets/markers/earthquake.png"));
    }


    @Override
    public void onAddedToMap(Map map) {
        owner = map;
        mapBatch = map.getBatch();
        mapSr = map.getShapeRenderer();

        //Get reports
        reports = new ArrayList<>();

        if(!queryReports(reports)){
            System.out.println("Failed to query disaster reports!");
        }
    }


    @Override
    public void update(Map map, float dt) {}


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        /*
        if(Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)){
            Vector2 mPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());

            GeoPoint mapPoint = map.unproject(mPos);

            System.out.printf("%.4f, %.4f\n", mapPoint.longitude, mapPoint.latitude);
        }
        */

        map.getViewport().apply();

        mapSr.setProjectionMatrix(map.getCamera().combined);
        mapSr.begin(ShapeRenderer.ShapeType.Filled);

        for(DisasterReport report : reports){
            Texture tx = getReportTexture(report.type);
            Vector2 worldLoc = GeoPoint.toWorldCoordinates(report.location);

            mapSr.setColor(1.f, 0.f, 0.f, report.severity / 6.f);
            mapSr.circle(worldLoc.x, worldLoc.y, 0.1f, 32);
        }

        mapSr.end();


        mapSr.begin(ShapeRenderer.ShapeType.Line);

        for(DisasterReport report : reports){
            Texture tx = getReportTexture(report.type);
            Vector2 worldLoc = GeoPoint.toWorldCoordinates(report.location);

            mapSr.setColor(1.f, 0.f, 0.f, report.severity / 4.f);
            mapSr.circle(worldLoc.x, worldLoc.y, 0.1f, 32);
        }

        mapSr.end();



        //Render icons in screen space to preserve dimensions regardless of zoom
        Viewport mapViewPort = map.getViewport();
        Matrix4 uiMatrix = mapViewPort.getCamera().combined.cpy();
        uiMatrix.setToOrtho2D(mapViewPort.getScreenX(), mapViewPort.getScreenY(), mapViewPort.getScreenWidth(), mapViewPort.getScreenHeight());
        mapBatch.setProjectionMatrix(uiMatrix);
        mapBatch.begin();

        for(DisasterReport report : reports){
            Texture tx = getReportTexture(report.type);
            Vector2 screenLoc = map.project(report.location);
            mapBatch.draw(tx, screenLoc.x - iconSize * 0.5f, screenLoc.y, iconSize, iconSize);
        }

        mapBatch.end();

    }


    private Texture getReportTexture(String type){
        switch (type){
            case "Flood":
                return floodMarker;

            case "Earthquake":
                return earthquakeMarker;

            case "Landslide":
                return landslideMarker;

            default:
                throw new RuntimeException("Invalid report type: " + type);
        }
    }


    @Override
    public void onKeyDown(int key) {}

    @Override
    public void onKeyUp(int key) {}

    @Override
    public void dispose() {
        floodMarker.dispose();
        landslideMarker.dispose();
        earthquakeMarker.dispose();
    }



    private boolean queryReports(ArrayList<DisasterReport> results){
        String url = AppConfig.BACKEND_URL + "/report";
        Request request = new Request.Builder().url(url).build();

        System.out.println(url);

        try {
            Response response = httpClient.newCall(request).execute();

            if(response.code() == 200){
                String resBody = response.body().string();
                JSONArray resList = new JSONArray(resBody);

                for(int i = 0; i < resList.length(); i++){
                    JSONObject item = resList.getJSONObject(i);

                    JSONObject props = item.getJSONObject("properties");
                    String type = props.getString("type");
                    int severity = props.getInt("severity");

                    JSONObject geometry = item.getJSONObject("geometry");
                    JSONArray coords = geometry.getJSONArray("coordinates");

                    DisasterReport report = new DisasterReport(type, severity, new GeoPoint(coords.getDouble(0), coords.getDouble(1)));
                    results.add(report);
                }

                return true;
            }
            else {
                System.out.printf("Error: queryReports response code %d\n", response.code());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}
