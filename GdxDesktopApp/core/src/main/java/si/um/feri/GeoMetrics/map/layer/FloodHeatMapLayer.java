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



//Test point container for data
class HeatMapPoint {
    public final GeoPoint coordinates;
    public final float score;

    public HeatMapPoint(GeoPoint c, float s){
        coordinates = c;
        score = s;
    }
}


public class FloodHeatMapLayer extends MapHeatMapLayer {

    private OkHttpClient httpClient;


    public FloodHeatMapLayer(){
        super();

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
        super.onAddedToMap(map);

        //Get flood heatmap data
        ArrayList<HeatMapPoint> features = new ArrayList<>();

        if(getFeatures(features)){

            for(HeatMapPoint p : features){
                Vector2 worldPos = GeoPoint.toWorldCoordinates(p.coordinates);
                heatmap.addPoint(worldPos.x, worldPos.y, 0.8f, p.score / 3.f);
            }

            heatmap.updateMesh();

            System.out.printf("Queried and added %d heatmap points!\n", features.size());
        }
        else {
            System.err.print("Failed to query heatmap data!\n");
        }
    }


    private boolean getFeatures(ArrayList<HeatMapPoint> results){
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
                        new HeatMapPoint(
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
