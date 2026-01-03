package si.um.feri.GeoMetrics.map;


import com.badlogic.gdx.math.Vector2;

public class Bbox {

    public final double xMin;
    public final double yMin;
    public final double xMax;
    public final double yMax;

    public final double width;
    public final double height;


    public Bbox(double xMin, double yMin, double xMax, double yMax){
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;

        this.width = xMax - xMin;
        this.height = yMax - yMin;
    }


    public GeoPoint getCenter(){
        return new GeoPoint(xMin + width * 0.5, yMin + height * 0.5);
    }


    public boolean contains(GeoPoint p){
        return contains(p.longitude, p.latitude);
    }


    public boolean contains(double longitude, double latitude){
        return (xMin < longitude) && (xMax > longitude) && (yMin < latitude) && (yMax > latitude);
    }

}
