package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;
import java.util.ArrayList;
import java.util.Objects;


public class GeoJsonGeometry {

    public final String type;
    public final ArrayList<GeoPoint> coordinates;
    private ArrayList<Vector2> worldCoordinates;
    private Bbox bounds;

    public Geometry _debug_outlineGeometry = null;
    public Geometry _debug_infillGeometry = null;


    public GeoJsonGeometry(String type, ArrayList<GeoPoint> coordinates){
        this.type = type;
        this.coordinates = coordinates;
        this.worldCoordinates = new ArrayList<>();
        bounds = computeBbox();
    }


    Bbox computeBbox(){
        double xMin = coordinates.get(0).longitude;
        double xMax = xMin;
        double yMin = coordinates.get(0).latitude;
        double yMax = yMin;

        for(GeoPoint p : coordinates){
            xMin = Math.min(xMin, p.longitude);
            xMax = Math.max(xMax, p.longitude);

            yMin = Math.min(yMin, p.latitude);
            yMax = Math.max(yMax, p.latitude);
        }

        return new Bbox(xMin, yMin, xMax, yMax);
    }


    public ArrayList<Vector2> getWorldCoordinates(){
        if(worldCoordinates.size() != coordinates.size()){

            System.out.printf("DEBUG: transforming %d points to world coordinates!\n", coordinates.size());

            worldCoordinates = new ArrayList<>(coordinates.size());

            for(GeoPoint p : coordinates){
                worldCoordinates.add(GeoPoint.toWorldCoordinates(p));
            }
        }

        return worldCoordinates;
    }


    public Bbox getBbox(){
        return bounds;
    }


    //TODO: support for other features?
    public static GeoJsonGeometry fromJson(JSONObject feature){
        JSONObject geometry = feature.getJSONObject("geometry");
        String geometryType = geometry.getString("type");

        if(!Objects.equals(geometryType, "Polygon")){
            throw new IllegalArgumentException("Only polygon geometry supported for now!");
        }

        JSONArray coordinates = geometry.getJSONArray("coordinates");

        if(coordinates.length() != 1){
            throw new IllegalArgumentException("Only polygons with single ring are supported for now!");
        }

        JSONArray ring = coordinates.getJSONArray(0);
        ArrayList<GeoPoint> cList = new ArrayList<>();

        for(int cIdx = 0; cIdx < ring.length(); cIdx++){
            JSONArray cPair = ring.getJSONArray(cIdx);
            cList.add(new GeoPoint(cPair.getDouble(0), cPair.getDouble(1)));
        }

        return new GeoJsonGeometry(geometryType, cList);
    }
}
