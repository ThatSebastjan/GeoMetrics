package si.um.feri.GeoMetrics.map.layer;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.heatmap.GlHeatmap;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;

import java.io.IOException;
import java.util.ArrayList;


public class MapHeatMapLayer extends MapLayer {

    protected Batch mapBatch;
    //private ShapeRenderer mapSr;

    private GlHeatmap heatmap;
    private OkHttpClient httpClient;

    int prevWidth = 0;
    int prevHeight = 0;


    //Test point container for data
    private class TestPoint {
        public final GeoPoint coordinates;
        public final float score;

        public TestPoint(GeoPoint c, float s){
            coordinates = c;
            score = s;
        }
    }



    public MapHeatMapLayer(){
        super();
        heatmap = new GlHeatmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        httpClient = new OkHttpClient();

        heatmap.setColorMap(new float[]{
            0.392f, 0.635f, 1.000f,
            0.259f, 0.557f, 1.000f,
            0.110f, 0.467f, 1.000f,
            0.204f, 0.243f, 1.000f,
            0.718f, 0.110f, 1.000f
        });
    }


    @Override
    public void onAddedToMap(Map map) {

        mapBatch = map.getBatch();
        //mapSr = map.getShapeRenderer();

        //Test get data
        ArrayList<TestPoint> features = new ArrayList<>();

        if(getFeatures(features)){

            for(TestPoint p : features){
                Vector2 worldPos = GeoPoint.toWorldCoordinates(p.coordinates);
                heatmap.addPoint(worldPos.x, worldPos.y, 1.2f, (float)Math.pow(p.score, 10));
            }

            heatmap.updateMesh();

            System.out.printf("Queried and added %d heatmap points!\n", features.size());
        }
        else {
            System.err.print("Failed to query heatmap data!\n");
        }
    }


    @Override
    public void update(Map map, float dt) {

        //TODO: add map layer on resize event!
        int cWidth = Gdx.graphics.getWidth();
        int cHeight = Gdx.graphics.getHeight();

        if(cWidth != prevWidth || cHeight != prevHeight){
            heatmap.resize(cWidth, cHeight);

            prevWidth = cWidth;
            prevHeight = cHeight;

            System.out.println("Resize!");
        }

    }


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        //Add points if mouse right held
        if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            float x = Gdx.input.getX();
            float y = Gdx.input.getY();

            for (int i = 0; i < 10; i++) {
                float offX = (float)(Math.random() * 2 - 1) * 0.01f; //offset in world space
                float offY = (float)(Math.random() * 2 - 1) * 0.01f;

                Vector3 p = map.unprojectToWorld(new Vector2(x, y));
                heatmap.addPoint(p.x + offX, p.y + offY, 0.1f, 2.f / 300.f);
            }

            heatmap.updateMesh();
        }

        heatmap.render(map.getCamera().combined);
    }


    @Override
    public void onKeyDown(int key) {}


    @Override
    public void onKeyUp(int key) {}


    @Override
    public void dispose() {}



    private boolean getFeatures(ArrayList<TestPoint> results){
        String url = AppConfig.BACKEND_URL + "/flood_point_features.geojson";
        Request request = new Request.Builder().url(url).build();

        System.out.println(url);

        try {
            Response response = httpClient.newCall(request).execute();

            if(response.code() == 200){
                String resBody = response.body().string();
                JSONObject resObj = new JSONObject(resBody);
                JSONArray data = resObj.getJSONArray("features");

                for(int i = 0; i < data.length(); i++){
                    JSONObject item = data.getJSONObject(i);

                    JSONObject props = item.getJSONObject("properties");
                    JSONObject geometry = item.getJSONObject("geometry");
                    JSONArray coords = geometry.getJSONArray("coordinates");

                    float score = props.getFloat("score");

                    results.add(
                        new TestPoint(
                            new GeoPoint(coords.getDouble(0), coords.getDouble(1)),
                            score
                        )
                    );
                }

                return true;
            }
            else {
                System.out.printf("Error: getFeatures response code %d\n", response.code());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}
