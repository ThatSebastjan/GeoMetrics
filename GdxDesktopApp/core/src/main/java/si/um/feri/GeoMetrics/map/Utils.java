package si.um.feri.GeoMetrics.map;

public class Utils {

    private Utils(){}


    public static double tileToLongitude(int x, int zoomLevel) {
        return x / Math.pow(2.0, zoomLevel) * 360.0 - 180;
    }


    public static double tileToLatitude(int y, int zoomLevel) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, zoomLevel);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }


    public static int longitudeToTile(double lon, int zoom) {
        return (int)Math.floor((lon + 180) / 360 * (1 << zoom));
    }


    public static int latitudeToTile(double lat, int zoom)  {
        return (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom)) ;
    }

}
