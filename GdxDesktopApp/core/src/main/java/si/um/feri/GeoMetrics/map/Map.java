package si.um.feri.GeoMetrics.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.layer.MapLayer;
import si.um.feri.GeoMetrics.map.tile.MapTile;
import si.um.feri.GeoMetrics.map.tile.MapTileManager;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;
import si.um.feri.GeoMetrics.map.tile.TilePreloadAction;

import java.util.ArrayList;
import java.util.Iterator;

import static com.badlogic.gdx.math.MathUtils.lerp;



public class Map implements InputProcessor, Disposable {

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer sr;
    private SpriteBatch batch;

    private MapTileManager tileManager;


    private int mapZoom; //Map tile zoom / detail level

    private float targetZoom = 1.f; //Target camera zoom for interpolation
    private boolean lerpingZoom = false;
    private final float minCameraZoom = 0.001f;
    private final float maxCameraZoom = 1f;

    private ArrayList<TilePreloadAction> preloadActions = new ArrayList<>();


    private ArrayList<MapLayer> layers = new ArrayList<>();


    private Vector2 lastTouchPoint = new Vector2();
    private Vector2 lastMousePos = new Vector2();
    private boolean dragged = false;
    private boolean isLeftDown = false;


    public interface MapActionCallback {
        void onViewChanged(Bbox viewBounds);
    }

    ArrayList<MapActionCallback> mapCallbacks = new ArrayList<>();



    public Map(SpriteBatch batch, MapTileManager tileManager){
        camera = new OrthographicCamera();
        viewport = new FillViewport(AppConfig.WORLD_WIDTH, AppConfig.WORLD_HEIGHT, camera);
        sr = new ShapeRenderer();

        this.batch = batch;
        this.tileManager = tileManager;

        init();
    }


    private void init(){

        //Set initial camera position
        Vector2 worldCenter = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.getCenter());
        camera.position.set(worldCenter.x, worldCenter.y, 0.f);

        //Set initial zoom level
        mapZoom = tileManager.getMinZoom();

        //Trigger initial tile preload
        onCameraPan();
    }


    @Override
    public void dispose() {

        for(MapLayer layer : layers){
            layer.dispose();
        }

        sr.dispose();
    }


    //Should be called on resize event
    public void onResize(int width, int height){
        viewport.update(width, height, false);

        onCameraPan(); //Trigger tile load on resize
    }


    public void update(float dt){


        //Update preload requests
        if(!preloadActions.isEmpty()){
            tileManager.preloadUpdate();

            Iterator<TilePreloadAction> it = preloadActions.iterator();

            while(it.hasNext()){
                TilePreloadAction action = it.next();

                if(tileManager.isPreloadActionComplete(action)){
                    mapZoom = action.targetZoomLevel; //Update target zoom
                    it.remove();
                    System.out.printf("Async action complete. New zoom: %d\n", action.targetZoomLevel);
                }
                else {
                    System.out.printf("Async action has %d items left\n", action.pendingTiles.size());
                }
            }
        }


        //Handle zoom updates
        if(Math.abs(targetZoom - camera.zoom) > 0.00001f) {
            lerpingZoom = true;
            camera.zoom = lerp(camera.zoom, targetZoom, 0.08f);

            Vector3 mouseWorldLocOld = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));

            //Translate so world location under mouse remains constant in screen space (zoom on "constant" position)
            camera.update(); //Force update camera

            Vector3 mouseWorldLocNew = viewport.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0.f));
            Vector3 mDelta = mouseWorldLocNew.cpy().sub(mouseWorldLocOld);

            camera.position.sub(mDelta.x, mDelta.y, 0.f);
            clampCamera();


            //Update level of detail
            int newMapZoom = tileManager.getMapZoomLevel(camera.zoom);

            //Load visible tiles on mapZoom change; Only update mapZoom once all are loaded
            if(newMapZoom != mapZoom){
                MapTileRegion region = getVisibleTileRegion(newMapZoom);

                if(queueTilePreload(region, newMapZoom)){
                    System.out.printf("Schedule preload for zoom %d (%d)\n", newMapZoom, preloadActions.size());
                }
            }
        }
        else {

            //Zoom interpolation complete
            if(lerpingZoom) {
                onZoomComplete();
            }

            lerpingZoom = false;
        }


        //Update layers
        for(MapLayer layer : layers){
            layer.update(this, dt);
        }

    }


    //Queue tile region preload. Returns true if new tiles have been scheduled for loading, false otherwise
    private boolean queueTilePreload(MapTileRegion region, int targetZoom){

        //Check if this region is already pending preload and is scheduled at same target zoom level
        if(preloadActions.stream().noneMatch(x -> (x.region.zoomLevel == region.zoomLevel) && x.region.contains(region))) {
            TilePreloadAction loadReq = tileManager.preloadAsync(region, targetZoom);

            if(!tileManager.isPreloadActionComplete(loadReq) || (targetZoom != mapZoom)) { //Already loaded and no zoom switch?
                preloadActions.add(loadReq);
                return true;
            }
        }

        return false;
    }


    //Camera moved, schedule tile preload
    private void onCameraPan(){
        MapTileRegion region = getVisibleTileRegion(mapZoom);

        if(queueTilePreload(region, mapZoom)){
            System.out.printf("Schedule pan preload for zoom %d (%d)\n", mapZoom, preloadActions.size());
        }
    }


    //Zoom action completed
    private void onZoomComplete(){
        MapTileRegion region = getVisibleTileRegion(mapZoom);

        if(queueTilePreload(region, mapZoom)){
            System.out.printf("Schedule zoom complete preload for zoom %d (%d)\n", mapZoom, preloadActions.size());
        }

        onViewChanged();
    }


    //Camera visible field changed (drag end, zoom end)
    private void onViewChanged(){

        Vector3 screenTopLeft = viewport.unproject(new Vector3(0.f, 0.f, 0.f));
        Vector3 screenBottomRight = viewport.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.f));

        Bbox viewBounds = Bbox.fromPoints(
            GeoPoint.fromWorldCoordinates(screenTopLeft.x, screenTopLeft.y),
            GeoPoint.fromWorldCoordinates(screenBottomRight.x, screenBottomRight.y)
        );

        for(MapActionCallback cb : mapCallbacks){
            cb.onViewChanged(viewBounds);
        }
    }


    public void addCallback(MapActionCallback cb){
        mapCallbacks.add(cb);
    }


    public void render(float dt){

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();


        MapTileRegion visibleRegion = getVisibleTileRegion(mapZoom);

        for(int tileY = visibleRegion.startTileY; tileY <= visibleRegion.endTileY; tileY++){
            for(int tileX = visibleRegion.startTileX; tileX <= visibleRegion.endTileX; tileX++){

                GeoPoint tileTopLeft = MapTile.getTileGeoPoint(tileX, tileY, mapZoom);
                GeoPoint tileBottomRight = MapTile.getTileGeoPoint(tileX + 1, tileY - 1, mapZoom);

                Vector2 topLeft = GeoPoint.toWorldCoordinates(tileTopLeft);
                Vector2 bottomRight = GeoPoint.toWorldCoordinates(tileBottomRight);

                float width = bottomRight.x - topLeft.x;
                float height = bottomRight.y - topLeft.y;

                MapTile tile = tileManager.tryGetTile(tileX, tileY, mapZoom);

                if(tile == null){
                    continue; //Still loading
                }


                batch.draw(tile.texture, topLeft.x, topLeft.y - height, width, height);
            }
        }

        batch.end();


        //Render layers
        for(MapLayer layer : layers){
            layer.render(this, dt, visibleRegion);
        }

    }


    private MapTileRegion getVisibleTileRegion(int zoomLevel){

        //Get map bounds
        MapTileRegion mapTileBounds = MapTileManager.getMapTileBounds(zoomLevel);

        //Get visible tile range
        Vector3 screenTopLeft = viewport.unproject(new Vector3(0.f, 0.f, 0.f));
        Vector3 screenBottomRight = viewport.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.f));

        GeoPoint pointTopLeft = GeoPoint.fromWorldCoordinates(screenTopLeft.x, screenTopLeft.y);
        GeoPoint pointBottomRight = GeoPoint.fromWorldCoordinates(screenBottomRight.x, screenBottomRight.y);

        //Convert and limit
        MapTileRegion visibleRegion = MapTileRegion.fromLngLat(pointTopLeft, pointBottomRight, zoomLevel);
        visibleRegion.limitToBounds(mapTileBounds); //Limit to dataset bounds

        return visibleRegion;
    }


    //Zoom speed based on zoom
    private float getZoomDelta(float camZoom){
        float baseDelta = 0.01f;
        return (float)(baseDelta / (1 + Math.log10(1 / camZoom))) * camZoom * 10;
    }


    //Clamp camera position to Map bounds
    private void clampCamera(){
        Vector2 mapMin = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMin, AppConfig.MAP_BOUNDS.yMin);
        Vector2 mapMax = GeoPoint.toWorldCoordinates(AppConfig.MAP_BOUNDS.xMax, AppConfig.MAP_BOUNDS.yMax);

        Vector3 camMin = viewport.unproject(new Vector3(0.f, 0.f, 0.f));
        Vector3 camMax = viewport.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.f));

        float halfViewWidth = (camMax.x - camMin.x) * 0.5f;
        float halfViewHeight = (camMin.y - camMax.y) * 0.5f;

        if((camera.position.x - halfViewWidth) < mapMin.x){
            camera.position.x = mapMin.x + halfViewWidth;
        }
        else if((camera.position.x + halfViewWidth) > mapMax.x){
            camera.position.x = mapMax.x - halfViewWidth;
        }

        if((camera.position.y - halfViewHeight) < mapMin.y){
            camera.position.y = mapMin.y + halfViewHeight;
        }
        else if((camera.position.y + halfViewHeight) > mapMax.y){
            camera.position.y = mapMax.y - halfViewHeight;
        }

    }



    /*
        Exposed utility functions
    */

    public void addLayer(MapLayer layer){

        for(MapLayer l : layers){
            if(l.getUuid() == layer.getUuid()){
                throw new IllegalArgumentException("Layer already present!"); //Sanity check to prevent multiple same layers
            }
        }

        layers.add(layer);
        layer.onAddedToMap(this);
    }


    //GeoPoint to screen coordinates
    public Vector2 project(GeoPoint p){
        Vector2 worldPos = GeoPoint.toWorldCoordinates(p);
        return viewport.project(worldPos);
    }


    //World coordinates to screen coordinates
    public Vector2 project(Vector2 worldPos){
        return viewport.project(worldPos);
    }


    //Screen position to GeoPoint
    public GeoPoint unproject(Vector2 screenPos){
        Vector3 worldPos = viewport.unproject(new Vector3(screenPos.x, screenPos.y, 0.f));
        return GeoPoint.fromWorldCoordinates(worldPos.x, worldPos.y);
    }


    //Screen position to world coordinates
    public Vector3 unprojectToWorld(Vector2 screenPos){
        return viewport.unproject(new Vector3(screenPos.x, screenPos.y, 0.f));
    }


    public Batch getBatch(){
        return batch;
    }


    public Viewport getViewport(){
        return viewport;
    }


    public ShapeRenderer getShapeRenderer(){
        return sr;
    }


    //Get map detail level
    public int getMapZoom(){
        return mapZoom;
    }


    public float getMapZoomLevelFloat(){
        return tileManager.getMapZoomLevelFloat(camera.zoom);
    }


    public MapTileManager getTileManager(){
        return tileManager;
    }


    public OrthographicCamera getCamera(){
        return camera;
    }




    /*
        Input processor methods
    */

    @Override
    public boolean keyDown(int i) {

        for(MapLayer l : layers){
            l.onKeyDown(i);
        }

        return false;
    }

    @Override
    public boolean keyUp(int i) {

        for(MapLayer l : layers){
            l.onKeyUp(i);
        }

        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {

        //Mouse left down
        if(button == Input.Buttons.LEFT){
            lastTouchPoint.set(x, y);
            isLeftDown = true;
            dragged = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {

        if(button == Input.Buttons.LEFT){

            if(dragged){
                isLeftDown = false;
                dragged = false;
                onViewChanged();
            }

        }

        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {

        if(!isLeftDown){
            return false;
        }

        Vector2 dragDelta = new Vector2(x, y).sub(lastTouchPoint);
        lastTouchPoint.add(dragDelta);

        Vector2 basePos = viewport.unproject(new Vector2(0.f, 0.f));
        Vector2 worldDragDelta = viewport.unproject(dragDelta);
        camera.position.sub(worldDragDelta.x - basePos.x, worldDragDelta.y - basePos.y, 0.f);

        //Limit camera to world bounds
        clampCamera();

        camera.update();

        onCameraPan();
        dragged = true;

        return true;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        lastMousePos.set(x, y);
        return true;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        targetZoom += v1 * getZoomDelta(targetZoom);
        targetZoom = Math.max(targetZoom, minCameraZoom); //Limit max zoom
        targetZoom = Math.min(targetZoom, maxCameraZoom);

        return true;
    }
}
