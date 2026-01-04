package si.um.feri.GeoMetrics.map;


public class MapTileRegion {
    public int startTileX;
    public int endTileX;
    public int startTileY;
    public int endTileY;


    public MapTileRegion(int sx, int ex, int sy, int ey){
        startTileX = sx;
        endTileX = ex;
        startTileY = sy;
        endTileY = ey;
    }


    public MapTileRegion limitToBounds(MapTileRegion bounds){
        startTileX = Math.max(startTileX, bounds.startTileX);
        endTileX = Math.min(endTileX, bounds.endTileX);

        startTileY = Math.max(startTileY, bounds.startTileY);
        endTileY = Math.min(endTileY, bounds.endTileY);

        return this;
    }


    public int getNumTiles(){
        return (endTileX - startTileX + 1) * (endTileY - startTileY + 1);
    }


    public static MapTileRegion fromLngLat(GeoPoint topLeft, GeoPoint bottomRight, int zoomLevel){
        int startTx = MapTile.longitudeToTile(topLeft.longitude, zoomLevel);
        int endTx = MapTile.longitudeToTile(bottomRight.longitude, zoomLevel);

        int startTy = MapTile.latitudeToTile(topLeft.latitude, zoomLevel);
        int endTy = MapTile.latitudeToTile(bottomRight.latitude, zoomLevel);

        return new MapTileRegion(startTx, endTx, startTy, endTy);
    }

}
