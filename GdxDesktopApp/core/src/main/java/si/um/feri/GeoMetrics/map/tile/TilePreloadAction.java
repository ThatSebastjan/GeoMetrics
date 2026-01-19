package si.um.feri.GeoMetrics.map.tile;

import java.util.ArrayList;


public class TilePreloadAction {

    public final MapTileRegion region;
    public final int targetZoomLevel;
    public ArrayList<MapTile> pendingTiles;


    public TilePreloadAction(MapTileRegion region, int targetZoomLevel){
        pendingTiles = new ArrayList<>();
        this.region = region;
        this.targetZoomLevel = targetZoomLevel;
    }

}
