package si.um.feri.GeoMetrics.map.tile;


import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;

public class MapTileRegion {
    public int startTileX;
    public int endTileX;
    public int startTileY;
    public int endTileY;
    public int zoomLevel; //tile x and y are dependant on zoom


    public MapTileRegion(int sx, int ex, int sy, int ey, int zoomLevel){
        startTileX = sx;
        endTileX = ex;
        startTileY = sy;
        endTileY = ey;
        this.zoomLevel = zoomLevel;
    }


    public void limitToBounds(MapTileRegion bounds){
        startTileX = Math.max(startTileX, bounds.startTileX);
        endTileX = Math.min(endTileX, bounds.endTileX);

        startTileY = Math.max(startTileY, bounds.startTileY);
        endTileY = Math.min(endTileY, bounds.endTileY);
    }


    public int getNumTiles(){
        return (endTileX - startTileX + 1) * (endTileY - startTileY + 1);
    }


    //Inclusive contains - contains self -> true
    public boolean contains(MapTileRegion other){

        if(other.zoomLevel != zoomLevel){
            other = atZoomLevel(other, zoomLevel); //Transform to same zoom level, as comparison can only be performed at same zoom
        }

        return (startTileX <= other.startTileX) && (endTileX >= other.endTileX) && (startTileY <= other.startTileY) && (endTileY >= other.endTileY);
    }


    public static MapTileRegion atZoomLevel(MapTileRegion region, int zoomLevel){

        if(region.zoomLevel == zoomLevel){
            return region;
        }

        //Scale
        GeoPoint otherTopLeft = MapTile.getTileGeoPoint(region.startTileX, region.startTileY, region.zoomLevel);
        GeoPoint otherBottomRight = MapTile.getTileGeoPoint(region.endTileX, region.endTileY, region.zoomLevel);

        return fromLngLat(otherTopLeft, otherBottomRight, zoomLevel); //At new zoom level
    }


    public static MapTileRegion fromLngLat(GeoPoint topLeft, GeoPoint bottomRight, int zoomLevel){
        int startTx = MapTile.longitudeToTile(topLeft.longitude, zoomLevel);
        int endTx = MapTile.longitudeToTile(bottomRight.longitude, zoomLevel);

        int startTy = MapTile.latitudeToTile(topLeft.latitude, zoomLevel);
        int endTy = MapTile.latitudeToTile(bottomRight.latitude, zoomLevel);

        return new MapTileRegion(startTx, endTx, startTy, endTy, zoomLevel);
    }

}
