package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;

import java.io.IOException;
import java.util.ArrayList;


public class DisasterReportLayer extends MapLayer {

    private static final float iconSize = 128.f;

    private Map owner;
    private Batch mapBatch;

    private ArrayList<DisasterReport> reports;
    private OkHttpClient httpClient;

    Texture floodMarker;
    Texture landslideMarker;
    Texture earthquakeMarker;

    private DisasterClickListener clickListener;


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

        reports = new ArrayList<>();

        if(!queryReports(reports)){
            System.out.println("Failed to query disaster reports!");
        }
    }


    @Override
    public void update(Map map, float dt) {
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            Vector2 clickPos = new Vector2(mouseX, mouseY);

            for(DisasterReport report : reports){
                Vector2 screenLoc = owner.project(report.location);

                float left = screenLoc.x - iconSize * 0.5f;
                float right = screenLoc.x + iconSize * 0.5f;
                float bottom = screenLoc.y;
                float top = screenLoc.y + iconSize;

                if(clickPos.x >= left && clickPos.x <= right &&
                   clickPos.y >= bottom && clickPos.y <= top){
                    if(clickListener != null){
                        clickListener.onDisasterClicked(report);
                    }
                    break;
                }
            }
        }
    }


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        map.getViewport().apply();

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

    public void setDisasterClickListener(DisasterClickListener listener){
        this.clickListener = listener;
    }

    public interface DisasterClickListener {
        void onDisasterClicked(DisasterReport report);
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
