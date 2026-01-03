package si.um.feri.GeoMetrics.map;

import com.badlogic.gdx.math.Vector2;
import si.um.feri.GeoMetrics.config.AppConfig;


public class GeoPoint {

    //Used for transformations between world <-> map space coordinates
    private static final double bboxMinX = longitudeToMercatorX(AppConfig.MAP_BOUNDS.xMin);
    private static final double bboxMaxX = longitudeToMercatorX(AppConfig.MAP_BOUNDS.xMax);
    private static final double bboxMinY = latitudeToMercatorY(AppConfig.MAP_BOUNDS.yMax);
    private static final double bboxMaxY = latitudeToMercatorY(AppConfig.MAP_BOUNDS.yMin);


    public double longitude;
    public double latitude;


    public GeoPoint(double lon, double lat){
        longitude = lon;
        latitude = lat;
    }



    public static Vector2 toWorldCoordinates(GeoPoint p){
        return toWorldCoordinates(p.longitude, p.latitude);
    }


    private static double longitudeToMercatorX(double lon) {
        return (lon + 180.0) / 360.0;
    }


    private static double latitudeToMercatorY(double lat) {
        double sinLat = Math.sin(lat * Math.PI / 180.0);
        return 0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI);
    }


    public static Vector2 toWorldCoordinates(double longitude, double latitude) {
        double mercatorX = longitudeToMercatorX(longitude);
        double mercatorY = latitudeToMercatorY(latitude);

        float nx = (float)((mercatorX - bboxMinX) / (bboxMaxX - bboxMinX));
        float ny = 1.f - (float)((mercatorY - bboxMinY) / (bboxMaxY - bboxMinY)); //1 - ... otherwise inverted

        return new Vector2(nx * AppConfig.WORLD_WIDTH, ny * AppConfig.WORLD_HEIGHT);
    }


    public static GeoPoint fromWorldCoordinates(double x, double y) {
        double nx = x / AppConfig.WORLD_WIDTH;
        double ny = y / AppConfig.WORLD_HEIGHT;

        double mercatorX = nx * (bboxMaxX - bboxMinX) + bboxMinX;
        double mercatorY = (1.f - ny) * (bboxMaxY - bboxMinY) + bboxMinY;

        double longitude = mercatorX * 360.0 - 180.0;
        //double latitude = Math.toDegrees(Math.asin(Math.tanh(Math.PI * (0.5 - mercatorY))));

        double res = Math.exp(4 * Math.PI * (0.5 - mercatorY));
        double sinLatInverse = (res - 1) / (res + 1);
        double latitude = (180.0 / Math.PI) * Math.asin(sinLatInverse);

        return new GeoPoint(longitude, latitude);
    }


}
