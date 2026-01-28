package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;
import si.um.feri.GeoMetrics.util.AppLogger;

import javax.swing.text.View;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class LandLotLayer extends MapGeoJsonLayer {

    private Map owner;
    private Bbox previousViewBounds;
    private HashSet<Integer> objectIdSet;
    private BitmapFont font;

    private OkHttpClient httpClient;

    private PlotClickListener plotClickListener;
    private GeoJsonGeometry lastHoveredGeometry;

    private final ArrayList<GeoJsonGeometry> pendingGeometries;
    private final Object pendingLock = new Object();

    private final ArrayList<GeoJsonGeometry> allGeometries;
    private static final int MAX_TOTAL_FEATURES = 40000;
    private static final double CLEANUP_DISTANCE_MULTIPLIER = 1.3;

    private GeoPoint lastRefreshCenter;
    private double lastRefreshViewSize;


    public LandLotLayer() {
        super(null);

        previousViewBounds = new Bbox(0, 0, 0, 0);
        objectIdSet = new HashSet<>();
        httpClient = new OkHttpClient();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.borderWidth = 1.f;
        parameter.borderColor = Color.BLACK;
        parameter.color = Color.WHITE;

        font = generator.generateFont(parameter);
        generator.dispose();

        pendingGeometries = new ArrayList<>();
        allGeometries = new ArrayList<>();
    }

    public void setPlotClickListener(PlotClickListener listener) {
        this.plotClickListener = listener;
    }

    public interface PlotClickListener {
        void onPlotClicked(JSONObject plotData);
    }


    @Override
    public void onAddedToMap(Map map) {
        owner = map;
        mapBatch = map.getBatch();
        map.addCallback(LandLotLayer.this::onViewChanged);
    }


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        if(map.getMapZoomLevelFloat() < 15.5f){
            return;
        }

        processPendingGeometries();

        if (Gdx.graphics.getFrameId() % 10 == 0 && allGeometries.size() > MAX_TOTAL_FEATURES * 0.8) {
            cleanupOldFeatures();
        }

        GeoPoint topLeft = map.unproject(new Vector2(0.f, 0.f));
        GeoPoint topRight = map.unproject(new Vector2(Gdx.graphics.getWidth(), 0.f));
        GeoPoint bottomLeft = map.unproject(new Vector2(0.f, Gdx.graphics.getHeight()));
        GeoPoint bottomRight = map.unproject(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        double minLon = Math.min(Math.min(topLeft.longitude, topRight.longitude),
                                 Math.min(bottomLeft.longitude, bottomRight.longitude));
        double maxLon = Math.max(Math.max(topLeft.longitude, topRight.longitude),
                                 Math.max(bottomLeft.longitude, bottomRight.longitude));
        double minLat = Math.min(Math.min(topLeft.latitude, topRight.latitude),
                                 Math.min(bottomLeft.latitude, bottomRight.latitude));
        double maxLat = Math.max(Math.max(topLeft.latitude, topRight.latitude),
                                 Math.max(bottomLeft.latitude, bottomRight.latitude));

        Bbox viewBounds = new Bbox(minLon, minLat, maxLon, maxLat);

        Gdx.gl.glEnable(GL32.GL_BLEND);
        Gdx.gl.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram shaderProgram = MapGeoJsonLayer.getGeoJsonShader();

        if(shaderProgram == null || !shaderProgram.isCompiled()){
            System.err.println("LandLotLayer: Shader is not compiled, skipping render");
            return;
        }

        shaderProgram.bind();
        shaderProgram.setUniformMatrix("u_projTrans", mapBatch.getProjectionMatrix());
        shaderProgram.setUniformf("u_inColor", new Color(1.f, 0.f, 0.f, 0.4f));

        Envelope visibleEnvelope = new Envelope(viewBounds.xMin, viewBounds.xMax, viewBounds.yMin, viewBounds.yMax);
        List<GeoJsonGeometry> queriedGeometry = quadtree.query(visibleEnvelope);

        List<GeoJsonGeometry> visibleGeometry = new ArrayList<>();
        for (GeoJsonGeometry geom : queriedGeometry) {
            if (geom != null && geom.infillMesh != null && geom.outlineMesh != null) {
                visibleGeometry.add(geom);
            }
        }

        GeoPoint mapMousePos = map.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        Envelope hoverEnvelope = new Envelope(
            mapMousePos.longitude - viewBounds.width * 0.025f,
            mapMousePos.longitude + viewBounds.width * 0.025f,
            mapMousePos.latitude - viewBounds.height * 0.025f,
            mapMousePos.latitude + viewBounds.height * 0.025f
        );

        List<GeoJsonGeometry> queriedHovered = quadtree.query(hoverEnvelope);

        List<GeoJsonGeometry> hoveredGeometry = new ArrayList<>();
        for (GeoJsonGeometry geom : queriedHovered) {
            if (geom != null && geom.infillMesh != null && geom.infillPolygon != null) {
                hoveredGeometry.add(geom);
            }
        }


        Vector2 mousePointWorld = GeoPoint.toWorldCoordinates(mapMousePos);
        GeometryFactory geometryFactory = new GeometryFactory();
        Point mousePoint = geometryFactory.createPoint(new Coordinate(mousePointWorld.x, mousePointWorld.y));

        lastHoveredGeometry = null;

        for(GeoJsonGeometry geom : hoveredGeometry) {

            if(geom.infillPolygon == null || !geom.infillPolygon.contains(mousePoint)){
                continue;
            }

            lastHoveredGeometry = geom;

            if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && plotClickListener != null) {
                plotClickListener.onPlotClicked(geom.getProperties());
            }

            if(geom.infillMesh != null && geom.infillMesh.getNumVertices() > 0) {
                try {
                    geom.infillMesh.render(shaderProgram, GL32.GL_TRIANGLES);
                } catch (Exception e) {
                    System.err.println("Error rendering infill mesh: " + e.getMessage());
                }
            }
        }


        shaderProgram.setUniformf("u_inColor", new Color(1.f, 208.f / 255.f, 0, 1.f));

        Gdx.gl.glLineWidth(2.f);
        Gdx.gl.glDisable(GL32.GL_CULL_FACE);
        Gdx.gl.glDisable(GL32.GL_DEPTH_TEST);

        for(GeoJsonGeometry geom : visibleGeometry) {
            if(geom.outlineMesh != null && geom.outlineMesh.getNumVertices() > 0) {
                try {
                    geom.outlineMesh.render(shaderProgram, GL32.GL_LINE_STRIP);
                } catch (Exception e) {
                    System.err.println("Error rendering outline mesh: " + e.getMessage());
                }
            }
        }

        Gdx.gl.glLineWidth(1.f);


        Viewport mapViewport = map.getViewport();

        Matrix4 uiMatrix = mapViewport.getCamera().combined.cpy();
        uiMatrix.setToOrtho2D(mapViewport.getScreenX(), mapViewport.getScreenY(), mapViewport.getScreenWidth(), mapViewport.getScreenHeight());

        mapBatch.setProjectionMatrix(uiMatrix);
        mapBatch.begin();


        float unitWidth = (float)(Gdx.graphics.getWidth() / (bottomRight.longitude - topLeft.longitude));


        GlyphLayout gl = new GlyphLayout();

        for(GeoJsonGeometry geom : visibleGeometry) {
            String label = geom.getProperties().getString("ST_PARCELE");
            Vector2 worldPos = geom.getCenter();
            Vector2 screenPos = map.project(worldPos.cpy());

            gl.setText(font, label);

            Bbox geomBbox = geom.getBbox();

            if((geomBbox.width * unitWidth * 0.5f) < gl.width || ((geomBbox.height * unitWidth * 0.5f) < gl.height)){
                continue;
            }

            font.draw(mapBatch, label, screenPos.x - gl.width / 2.f, screenPos.y + gl.height / 2.f);
        }

        mapBatch.end();
    }


    @Override
    public void dispose() {
        synchronized (pendingLock) {
            for (GeoJsonGeometry geom : allGeometries) {
                if (geom != null) {
                    geom.dispose();
                }
            }
            allGeometries.clear();
            pendingGeometries.clear();
            objectIdSet.clear();
        }
        super.dispose();
        font.dispose();
    }


    /**
     * Process pending geometries and add them to the quadtree.
     * Must be called during render when OpenGL context is active.
     * Processes features in priority order (center first) and limits per frame for responsiveness.
     */
    private void processPendingGeometries() {
        synchronized (pendingLock) {
            if (pendingGeometries.isEmpty()) {
                return;
            }

            GeoPoint topLeft = owner.unproject(new Vector2(0.f, 0.f));
            GeoPoint topRight = owner.unproject(new Vector2(Gdx.graphics.getWidth(), 0.f));
            GeoPoint bottomLeft = owner.unproject(new Vector2(0.f, Gdx.graphics.getHeight()));
            GeoPoint bottomRight = owner.unproject(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

            double minLon = Math.min(Math.min(topLeft.longitude, topRight.longitude),
                                     Math.min(bottomLeft.longitude, bottomRight.longitude));
            double maxLon = Math.max(Math.max(topLeft.longitude, topRight.longitude),
                                     Math.max(bottomLeft.longitude, bottomRight.longitude));
            double minLat = Math.min(Math.min(topLeft.latitude, topRight.latitude),
                                     Math.min(bottomLeft.latitude, bottomRight.latitude));
            double maxLat = Math.max(Math.max(topLeft.latitude, topRight.latitude),
                                     Math.max(bottomLeft.latitude, bottomRight.latitude));

            double viewWidth = maxLon - minLon;
            double viewHeight = maxLat - minLat;
            double expandX = viewWidth * 0.1;
            double expandY = viewHeight * 0.1;
            Bbox currentViewBounds = new Bbox(minLon - expandX, minLat - expandY, maxLon + expandX, maxLat + expandY);

            if (allGeometries.size() > MAX_TOTAL_FEATURES * 0.7) {
                cleanupOldFeatures();
            }

            ArrayList<GeoJsonGeometry> visiblePending = new ArrayList<>();
            int skippedOffScreen = 0;
            for (GeoJsonGeometry geom : pendingGeometries) {
                Bbox geomBbox = geom.getBbox();
                if (geomBbox.xMax >= currentViewBounds.xMin && geomBbox.xMin <= currentViewBounds.xMax &&
                    geomBbox.yMax >= currentViewBounds.yMin && geomBbox.yMin <= currentViewBounds.yMax) {
                    visiblePending.add(geom);
                } else {
                    skippedOffScreen++;
                }
            }

            if (skippedOffScreen > 0) {
                System.out.printf("Skipped %d off-screen pending geometries\n", skippedOffScreen);
            }

            GeoPoint screenCenter = owner.unproject(new Vector2(
                Gdx.graphics.getWidth() / 2.0f,
                Gdx.graphics.getHeight() / 2.0f
            ));
            visiblePending.sort((geom1, geom2) -> {
                double dist1 = distanceToCenter(geom1, screenCenter);
                double dist2 = distanceToCenter(geom2, screenCenter);
                return Double.compare(dist1, dist2);
            });

            final int MAX_PROCESS_PER_FRAME = 200;
            int numInserted = 0;
            int processed = 0;
            ArrayList<GeoJsonGeometry> remaining = new ArrayList<>();

            for (GeoJsonGeometry geom : visiblePending) {
                if (processed >= MAX_PROCESS_PER_FRAME) {
                    remaining.add(geom);
                    continue;
                }

                int geomId = geom.getId();

                if (objectIdSet.contains(geomId)) {
                    processed++;
                    continue;
                }

                if (!geom.areMeshesCreated()) {
                    if (!geom.createMeshes()) {
                        System.err.println("Failed to create meshes for geometry " + geomId);
                        continue;
                    }
                }

                if (geom.infillMesh == null || geom.outlineMesh == null) {
                    continue;
                }

                objectIdSet.add(geomId);
                quadtree.insert(geom.getEnvelopeBounds(), geom);
                allGeometries.add(geom);
                numInserted++;
                processed++;
            }

            pendingGeometries.clear();
            pendingGeometries.addAll(remaining);

            if (numInserted > 0) {
                System.out.printf("Processed %d pending geometries (inserted: %d, remaining: %d, total: %d)\n",
                    processed, numInserted, remaining.size(), allGeometries.size());
            }

            if (allGeometries.size() > MAX_TOTAL_FEATURES) {
                cleanupOldFeatures();
            }
        }
    }

    /**
     * Remove features far from current view to free memory.
     * Prioritizes keeping features closest to screen center.
     */
    private void cleanupOldFeatures() {
        if (owner == null || allGeometries.isEmpty()) {
            return;
        }

        GeoPoint topLeft = owner.unproject(new Vector2(0.f, 0.f));
        GeoPoint topRight = owner.unproject(new Vector2(Gdx.graphics.getWidth(), 0.f));
        GeoPoint bottomLeft = owner.unproject(new Vector2(0.f, Gdx.graphics.getHeight()));
        GeoPoint bottomRight = owner.unproject(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        double minLon = Math.min(Math.min(topLeft.longitude, topRight.longitude),
                                 Math.min(bottomLeft.longitude, bottomRight.longitude));
        double maxLon = Math.max(Math.max(topLeft.longitude, topRight.longitude),
                                 Math.max(bottomLeft.longitude, bottomRight.longitude));
        double minLat = Math.min(Math.min(topLeft.latitude, topRight.latitude),
                                 Math.min(bottomLeft.latitude, bottomRight.latitude));
        double maxLat = Math.max(Math.max(topLeft.latitude, topRight.latitude),
                                 Math.max(bottomLeft.latitude, bottomRight.latitude));

        Bbox viewBounds = new Bbox(minLon, minLat, maxLon, maxLat);

        GeoPoint screenCenter = owner.unproject(new Vector2(
            Gdx.graphics.getWidth() / 2.0f,
            Gdx.graphics.getHeight() / 2.0f
        ));

        double viewWidth = viewBounds.width;
        double viewHeight = viewBounds.height;
        double centerX = (viewBounds.xMin + viewBounds.xMax) / 2.0;
        double centerY = (viewBounds.yMin + viewBounds.yMax) / 2.0;

        Bbox keepBounds = new Bbox(
            centerX - viewWidth * CLEANUP_DISTANCE_MULTIPLIER,
            centerY - viewHeight * CLEANUP_DISTANCE_MULTIPLIER,
            centerX + viewWidth * CLEANUP_DISTANCE_MULTIPLIER,
            centerY + viewHeight * CLEANUP_DISTANCE_MULTIPLIER
        );

        ArrayList<GeoJsonGeometry> toKeep = new ArrayList<>();
        ArrayList<GeoJsonGeometry> toRemove = new ArrayList<>();

        for (GeoJsonGeometry geom : allGeometries) {
            Bbox geomBbox = geom.getBbox();
            if (geomBbox.xMax >= keepBounds.xMin && geomBbox.xMin <= keepBounds.xMax &&
                geomBbox.yMax >= keepBounds.yMin && geomBbox.yMin <= keepBounds.yMax) {
                toKeep.add(geom);
            } else {
                toRemove.add(geom);
            }
        }

        if (toKeep.size() > MAX_TOTAL_FEATURES) {
            toKeep.sort((geom1, geom2) -> {
                double dist1 = distanceToCenter(geom1, screenCenter);
                double dist2 = distanceToCenter(geom2, screenCenter);
                return Double.compare(dist2, dist1);
            });

            int toRemoveCount = toKeep.size() - MAX_TOTAL_FEATURES;
            for (int i = 0; i < toRemoveCount; i++) {
                toRemove.add(toKeep.remove(0));
            }
        }

        for (GeoJsonGeometry geom : toRemove) {
            objectIdSet.remove(geom.getId());
            geom.dispose();
        }

        allGeometries.clear();
        allGeometries.addAll(toKeep);

        if (toRemove.size() > 0) {
            System.out.printf("Cleanup: Removed %d farthest features, kept %d closest (total: %d)\n",
                toRemove.size(), toKeep.size(), allGeometries.size());
        }
    }


    private void onViewChanged(Bbox viewBounds){
        System.out.printf("onViewChanged: %.4f, %.4f, %.4f, %.4f\n", viewBounds.xMin, viewBounds.yMin, viewBounds.xMax, viewBounds.yMax);

        if(owner.getMapZoomLevelFloat() < 15.5f){
            return;
        }

        GeoPoint screenCenter = owner.unproject(new Vector2(
            Gdx.graphics.getWidth() / 2.0f,
            Gdx.graphics.getHeight() / 2.0f
        ));
        double currentViewSize = viewBounds.width * viewBounds.height;

        boolean significantChange = false;
        if (lastRefreshCenter == null) {
            significantChange = true;
        } else {
            double sizeRatio = currentViewSize / lastRefreshViewSize;
            if (sizeRatio < 0.5 || sizeRatio > 2.0) {
                significantChange = true;
                System.out.println("Significant zoom change detected, forcing full refresh");
            }

            double centerDist = Math.sqrt(
                Math.pow(screenCenter.longitude - lastRefreshCenter.longitude, 2) +
                Math.pow(screenCenter.latitude - lastRefreshCenter.latitude, 2)
            );
            if (centerDist > viewBounds.width * 0.5) {
                significantChange = true;
                System.out.println("Significant pan detected, forcing full refresh");
            }
        }

        lastRefreshCenter = screenCenter;
        lastRefreshViewSize = currentViewSize;

        if (significantChange) {
            synchronized (pendingLock) {
                pendingGeometries.clear();
                System.out.println("Cleared pending queue for fresh view refresh");
            }
        }

        ArrayList<GeoJsonGeometry> newGeometry = new ArrayList<>();

        Bbox queryOldBounds = significantChange ? new Bbox(0, 0, 0, 0) : previousViewBounds;

        if(!queryNewFeatures(viewBounds, queryOldBounds, newGeometry)){
            return;
        }

        newGeometry.sort((geom1, geom2) -> {
            double dist1 = distanceToCenter(geom1, screenCenter);
            double dist2 = distanceToCenter(geom2, screenCenter);
            return Double.compare(dist1, dist2);
        });

        if (!newGeometry.isEmpty()) {
            GeoJsonGeometry closest = newGeometry.get(0);
            GeoJsonGeometry farthest = newGeometry.get(newGeometry.size() - 1);
            System.out.printf("Priority: Closest feature at distance %.2f, farthest at %.2f (total: %d)\n",
                distanceToCenter(closest, screenCenter),
                distanceToCenter(farthest, screenCenter),
                newGeometry.size());
        }

        synchronized (pendingLock) {
            if (significantChange) {
                ArrayList<GeoJsonGeometry> combined = new ArrayList<>(newGeometry);
                combined.addAll(pendingGeometries);
                pendingGeometries.clear();
                pendingGeometries.addAll(combined);
            } else {
                pendingGeometries.addAll(newGeometry);
            }
            System.out.printf("Queued %d new geometries for processing (total pending: %d)\n",
                newGeometry.size(), pendingGeometries.size());
        }

        previousViewBounds = viewBounds;
    }

    /**
     * Calculate distance from geometry center to screen center using haversine formula.
     */
    private double distanceToCenter(GeoJsonGeometry geom, GeoPoint screenCenter) {
        Bbox geomBbox = geom.getBbox();
        double geomCenterLon = (geomBbox.xMin + geomBbox.xMax) / 2.0;
        double geomCenterLat = (geomBbox.yMin + geomBbox.yMax) / 2.0;

        double dLon = Math.toRadians(geomCenterLon - screenCenter.longitude);
        double dLat = Math.toRadians(geomCenterLat - screenCenter.latitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(screenCenter.latitude)) *
                   Math.cos(Math.toRadians(geomCenterLat)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371000 * c;
    }


    private boolean queryNewFeatures(Bbox newBounds, Bbox oldBounds, ArrayList<GeoJsonGeometry> results){
        String url = AppConfig.BACKEND_URL + "/map/query/" + bboxToString(oldBounds) + "," + bboxToString(newBounds);
        Request request = new Request.Builder().url(url).build();

        AppLogger.info("Querying plot features from: " + url);
        System.out.println("Querying: " + url);

        try {
            Response response = httpClient.newCall(request).execute();
            int statusCode = response.code();

            AppLogger.info("Backend response code: " + statusCode);

            if(response.code() == 200){
                try {
                    String resBody = response.body().string();
                    JSONObject resObj = new JSONObject(resBody);

                    if(!resObj.has("data")) {
                        AppLogger.error("Response missing 'data' field");
                        System.err.println("Error: Response missing 'data' field");
                        return true;
                    }

                    JSONArray data = resObj.getJSONArray("data");
                    AppLogger.info("Received " + data.length() + " features from backend");

                    for(int i = 0; i < data.length(); i++){
                        try {
                            JSONObject item = data.getJSONObject(i);
                            int itemId = item.getInt("id");

                            GeoJsonGeometry geom = GeoJsonGeometry.fromJson(item, itemId, false);
                            if(geom != null) {
                                results.add(geom);
                            }
                        } catch (Exception e) {
                            AppLogger.error("Error parsing geometry item " + i + ": " + e.getMessage());
                            System.err.println("Error parsing geometry item " + i + ": " + e.getMessage());
                        }
                    }

                    return true;
                } catch (Exception e) {
                    AppLogger.error("Error parsing response body", e);
                    System.err.println("Error parsing response body: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            }
            else if(response.code() == 304) {
                AppLogger.info("304 Not Modified (cached data is still valid)");
                System.out.println("queryNewFeatures: 304 Not Modified (cached data is still valid)");
                return true;
            }
            else if(response.code() == 500) {
                AppLogger.error("Server error (500) when querying features from: " + url);
                System.err.println("Error: Server error (500) when querying features");
                try {
                    String errorBody = response.body().string();
                    AppLogger.error("Server response: " + errorBody);
                    System.err.println("Server response: " + errorBody);
                } catch (IOException e) {
                    AppLogger.error("Could not read error response body");
                    System.err.println("Could not read error response body");
                }
                return true;
            }
            else {
                AppLogger.error("Unexpected response code: " + response.code());
                System.err.printf("Error: queryNewFeatures response code %d\n", response.code());
                return true;
            }

        } catch (IOException e) {
            AppLogger.error("HTTP request failed", e);
            System.err.println("Error making HTTP request: " + e.getMessage());
            e.printStackTrace();
            return true;
        } catch (Exception e) {
            AppLogger.error("Unexpected error in queryNewFeatures", e);
            System.err.println("Unexpected error in queryNewFeatures: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }


    private String bboxToString(Bbox b){
        return String.format("%.6f,%.6f,%.6f,%.6f", b.xMin, b.yMin, b.xMax, b.yMax);
    }
}
