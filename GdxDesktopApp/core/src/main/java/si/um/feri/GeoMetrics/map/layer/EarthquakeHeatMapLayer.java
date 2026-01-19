package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.math.Vector2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;

import java.io.IOException;
import java.util.ArrayList;



public class EarthquakeHeatMapLayer extends MapHeatMapLayer {

    private OkHttpClient httpClient;


    public EarthquakeHeatMapLayer(){
        super();

        httpClient = new OkHttpClient();

        heatmap.setColorMap(new float[]{
            0.980f, 0.863f, 0.820f,
            0.965f, 0.725f, 0.635f,
            0.945f, 0.584f, 0.455f,
            0.925f, 0.447f, 0.275f,
            0.910f, 0.310f, 0.090f
        });
    }


    @Override
    public void onAddedToMap(Map map) {
        super.onAddedToMap(map);

        //Get earthquake heatmap data
        ArrayList<HeatMapPoint> features = new ArrayList<>();

        if(getFeatures(features)){

            for(HeatMapPoint p : features){
                Vector2 worldPos = GeoPoint.toWorldCoordinates(p.coordinates);
                heatmap.addPoint(worldPos.x, worldPos.y, 6.f, p.score / 80.f);
            }

            heatmap.updateMesh();

            System.out.printf("Queried and added %d heatmap points!\n", features.size());
        }
        else {
            System.err.print("Failed to query heatmap data!\n");
        }
    }


    private boolean getFeatures(ArrayList<HeatMapPoint> results){
        String url = AppConfig.BACKEND_URL + "/map/earthquakes";
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
                    JSONObject geometry = item.getJSONObject("geometry");
                    JSONArray coords = geometry.getJSONArray("coordinates");

                    float depth = props.getFloat("depth");
                    float magnitude = props.getFloat("magnitude");
                    float weight = magnitude * magnitude * (float)(10.0001 - Math.min(Math.sqrt(depth), 100));

                    results.add(
                        new HeatMapPoint(
                            new GeoPoint(coords.getDouble(0), coords.getDouble(1)),
                            weight
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
