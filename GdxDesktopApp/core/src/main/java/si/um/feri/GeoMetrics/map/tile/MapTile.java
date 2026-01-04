package si.um.feri.GeoMetrics.map.tile;

import com.badlogic.gdx.graphics.Texture;
import si.um.feri.GeoMetrics.map.GeoPoint;


public class MapTile {

    public final int x;
    public final int y;
    public final int zoomLevel;
    public Texture texture;


    public MapTile(int x, int y, int zoomLevel, Texture tx){
        this.x = x;
        this.y = y;
        this.zoomLevel = zoomLevel;
        texture = tx;
    }


    public long hashKey(){
        return getHashKey(x, y, zoomLevel);
    }


    public static long getHashKey(int x, int y, int zoomLevel){
        return ((long)zoomLevel << 48) | ((long)x << 24) | ((long)y);
    }


    public static double tileToLongitude(int x, int zoomLevel) {
        return x / Math.pow(2.0, zoomLevel) * 360.0 - 180;
    }


    public static double tileToLatitude(int y, int zoomLevel) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, zoomLevel);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }


    public static int longitudeToTile(double lon, int zoomLevel) {
        return (int)Math.floor((lon + 180) / 360 * (1 << zoomLevel));
    }


    public static int latitudeToTile(double lat, int zoomLevel)  {
        return (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoomLevel));
    }


    //Get world coordinates from tile x, y and zoom level
    public static GeoPoint getTileGeoPoint(int tileX, int tileY, int zoomLevel){
        double longitude = tileToLongitude(tileX, zoomLevel);
        double latitude = tileToLatitude(tileY, zoomLevel);

        return new GeoPoint(longitude, latitude);
    }
}
