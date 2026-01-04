package si.um.feri.GeoMetrics.map;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import si.um.feri.GeoMetrics.config.AppConfig;

import java.util.HashMap;
import java.util.Iterator;


public class MapTileManager {

    private final AssetManager assetManager;
    private final String dataPath;
    private final int minZoom;
    private final int maxZoom;

    private final HashMap<Long, MapTile> tileHashMap;


    public MapTileManager(AssetManager assetManager, String dataPath, int minZoom, int maxZoom){
        this.assetManager = assetManager;
        this.dataPath = dataPath;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;

        tileHashMap = new HashMap<>();
    }


    //Get tile if loaded
    public MapTile tryGetTile(int tileX, int tileY, int zoomLevel) {

        if((zoomLevel < minZoom) || (zoomLevel > maxZoom)){
            throw new IllegalArgumentException("Zoom level out of range!"); //Sanity check
        }

        Long hashKey = MapTile.getHashKey(tileX, tileY, zoomLevel);

        if(tileHashMap.containsKey(hashKey)){
            return tileHashMap.get(hashKey);
        }

        return null; //Pending load
    }


    public TilePreloadAction preloadAsync(MapTileRegion region, int targetZoomLevel){

        if((targetZoomLevel < minZoom) || (targetZoomLevel > maxZoom)){
            throw new IllegalArgumentException("Zoom level out of range!"); //Sanity check
        }

        TilePreloadAction action = new TilePreloadAction(region, targetZoomLevel);

        for(int tileY = region.startTileY; tileY <= region.endTileY; tileY++) {
            for (int tileX = region.startTileX; tileX <= region.endTileX; tileX++) {

                MapTile preloadTile = new MapTile(tileX, tileY, targetZoomLevel, null);
                action.pendingTiles.add(preloadTile);
            }
        }

        //Queue loading
        Iterator<MapTile> it = action.pendingTiles.iterator();

        while(it.hasNext()){
            MapTile t = it.next();

            if(tileHashMap.containsKey(t.hashKey())){
                it.remove(); //Remove already loaded
                continue;
            }

            String filePath = getPathForTile(t.x, t.y, t.zoomLevel);
            assetManager.load(filePath, Texture.class);
        }

        return action;
    }


    public boolean isPreloadActionComplete(TilePreloadAction action){
        Iterator<MapTile> it = action.pendingTiles.iterator();

        while(it.hasNext()){
            MapTile t = it.next();
            String filePath = getPathForTile(t.x, t.y, t.zoomLevel);

            if(!assetManager.isLoaded(filePath, Texture.class)){
                return false;
            }

            //Populate texture
            t.texture = assetManager.get(filePath, Texture.class);

            //Add to tile hash map
            tileHashMap.put(t.hashKey(), t);

            it.remove(); //Remove from pending
        }

        return true;
    }


    public boolean preloadUpdate(){
        return assetManager.update();
    }


    private String getPathForTile(int tileX, int tileY, int zoomLevel){
        return dataPath + "/" + zoomLevel + "/" + tileX + "_" + tileY + ".jpg";
    }


    public int getMinZoom() {
        return minZoom;
    }


    public int getMaxZoom() {
        return maxZoom;
    }


    //Get map tileset "zoom" / detail level based on camera zoom
    public int getMapZoomLevel(float camZoom){
        /*
        if(camZoom > 0.5f){
            return 9.f;
        }
        else if(camZoom > 0.25f){
            return 10.f;
        }
        else if(camZoom > 0.125f){
            return 11.f;
        }
        else if(camZoom > 0.0625f){
            return 12.f;
        }
        else if(camZoom > 0.03125f){
            return 13.f;
        }
        else if(camZoom > 0.015625f){
            return 14.f;
        }

        return 15.f;
        */

        //For every zoom halving we increase the detail level by one
        int numHalved = (int)(Math.log(1 / camZoom) / Math.log(2)); //Java has not inbuilt log2...
        return Math.max(minZoom, Math.min(minZoom + numHalved, maxZoom));
    }


    //Get map tileset bounds based on zoom level
    public static MapTileRegion getMapTileBounds(int zoomLevel){
        int topTile = MapTile.latitudeToTile(AppConfig.MAP_BOUNDS.yMin, zoomLevel);
        int leftTile = MapTile.longitudeToTile(AppConfig.MAP_BOUNDS.xMin, zoomLevel);
        int bottomTile = MapTile.latitudeToTile(AppConfig.MAP_BOUNDS.yMax, zoomLevel);
        int rightTile = MapTile.longitudeToTile(AppConfig.MAP_BOUNDS.xMax, zoomLevel);

        return new MapTileRegion(leftTile, rightTile, bottomTile, topTile, zoomLevel);
    }
}
