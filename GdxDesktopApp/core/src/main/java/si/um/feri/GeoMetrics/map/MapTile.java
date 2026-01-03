package si.um.feri.GeoMetrics.map;

import com.badlogic.gdx.graphics.Texture;


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


    public static long getHashKey(int x, int y, int zoomLevel){
        return ((long)zoomLevel << 48) | ((long)x << 24) | ((long)y);
    }
}
