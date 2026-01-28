package si.um.feri.GeoMetrics.screen;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import si.um.feri.GeoMetrics.GdxMap;
import si.um.feri.GeoMetrics.cef.GdxCefInput;
import si.um.feri.GeoMetrics.cef.GdxCefInstance;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.layer.*;
import si.um.feri.GeoMetrics.map.tile.MapTileManager;
import si.um.feri.GeoMetrics.util.AppLogger;

import java.io.File;



public class CefUITestScreen extends ScreenAdapter implements CefMessageRouterHandler {

    private final GdxMap mainInstance;
    private final AssetManager assetManager;
    private Map map;
    private final Gson gson;
    private boolean sidebarOpen = true;
    private String currentRiskType = null;

    private FloodHeatMapLayer floodHeatmap;
    private LandslideHeatMapLayer landslideHeatmap;
    private EarthquakeHeatMapLayer earthquakeHeatmap;

    private LandLotLayer landLotLayer;
    private DisasterReportLayer disasterReportLayer;


    public CefUITestScreen(GdxMap mainInstance){
        this.mainInstance = mainInstance;
        assetManager = mainInstance.getAssetManager();
        this.gson = new Gson();

        floodHeatmap = new FloodHeatMapLayer();
        landslideHeatmap = new LandslideHeatMapLayer();
        earthquakeHeatmap = new EarthquakeHeatMapLayer();

        floodHeatmap.setEnabled(false);
        landslideHeatmap.setEnabled(false);
        earthquakeHeatmap.setEnabled(false);
    }



    @Override
    public void show(){

        if(!GdxCefInstance.isInitialized()){
            String fileUrl = new File("./core/src/main/html/test.html").getAbsolutePath();
            int refreshRate = 60;

            GdxCefInstance.init(fileUrl, this, refreshRate);
        }

        GdxCefInput cefInputProcessor = new GdxCefInput(GdxCefInstance.getBrowser());

        MapTileManager tileManager = new MapTileManager(assetManager, AppConfig.TILE_DATA_PATH, AppConfig.TILE_MAP_MIN_ZOOM, AppConfig.TILE_MAP_MAX_ZOOM);
        map = new Map(mainInstance.getBatch(), tileManager);

        map.addLayer(new MapGeoJsonLayer(Gdx.files.internal("assets/static/si_border.json").path()));

        map.addLayer(floodHeatmap);
        map.addLayer(landslideHeatmap);
        map.addLayer(earthquakeHeatmap);

        landLotLayer = new LandLotLayer();
        landLotLayer.setPlotClickListener(this::onPlotClicked);
        map.addLayer(landLotLayer);

        disasterReportLayer = new DisasterReportLayer();
        disasterReportLayer.setEnabled(false);  // Start with disaster reports disabled
        disasterReportLayer.setDisasterClickListener(this::onDisasterClicked);  // Handle disaster clicks
        map.addLayer(disasterReportLayer);

        map.addLayer(new MapDebugLayer());

        InputMultiplexer inputMultiplexer = new InputMultiplexer(cefInputProcessor, map);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }


    @Override
    public void resize(int width, int height){
        map.onResize(width, height);
    }


    @Override
    public void render(float dt){
        GdxCefInstance.onRenderBegin();

        ScreenUtils.clear(1.f, 0.f, 0.f, 1.f);

        map.update(dt);
        map.render(dt);

        GdxCefInstance.render();

        GdxCefInstance.onRenderEnd();
    }


    @Override
    public void hide(){
        dispose();
    }


    @Override
    public void dispose() {
        map.dispose();
        GdxCefInstance.dispose();
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        System.out.printf("onQuery: %d, %s\n", queryId, request);

        try {
            JsonObject jsonRequest = JsonParser.parseString(request).getAsJsonObject();
            String messageType = jsonRequest.get("type").getAsString();
            JsonObject data = jsonRequest.has("data") ? jsonRequest.getAsJsonObject("data") : null;

            switch (messageType) {
                case "sidebarToggle":
                    handleSidebarToggle(data);
                    callback.success("Sidebar toggle received");
                    break;

                case "riskTypeChange":
                    handleRiskTypeChange(data);
                    callback.success("Risk type change received");
                    break;

                default:
                    callback.success("Unknown message type");
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error parsing CEF message: " + e.getMessage());
            callback.failure(1, "Failed to parse message: " + e.getMessage());
        }

        return true;
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {}

    @Override
    public void setNativeRef(String identifier, long nativeRef) {}

    @Override
    public long getNativeRef(String identifier) {
        return 0;
    }


    private void handleSidebarToggle(JsonObject data) {
        if (data != null && data.has("open")) {
            sidebarOpen = data.get("open").getAsBoolean();
            System.out.println("Sidebar is now: " + (sidebarOpen ? "open" : "closed"));
        }
    }

    public void showSidebar() {
        if (GdxCefInstance.isInitialized()) {
            CefBrowser browser = GdxCefInstance.getBrowser();
            browser.executeJavaScript("showSidebar();", browser.getURL(), 0);
            sidebarOpen = true;
        }
    }

    public void hideSidebar() {
        if (GdxCefInstance.isInitialized()) {
            CefBrowser browser = GdxCefInstance.getBrowser();
            browser.executeJavaScript("hideSidebar();", browser.getURL(), 0);
            sidebarOpen = false;
        }
    }

    public void toggleSidebar() {
        if (sidebarOpen) {
            hideSidebar();
        } else {
            showSidebar();
        }
    }

    public boolean isSidebarOpen() {
        return sidebarOpen;
    }

    private void handleRiskTypeChange(JsonObject data) {
        if (data != null && data.has("riskType")) {
            currentRiskType = data.get("riskType").getAsString();
            System.out.println("Risk type changed to: " + currentRiskType);

            switch (currentRiskType.toLowerCase()) {
                case "flood":
                    floodHeatmap.setEnabled(true);
                    landslideHeatmap.setEnabled(false);
                    earthquakeHeatmap.setEnabled(false);
                    disasterReportLayer.setEnabled(false);
                    landLotLayer.setEnabled(false);
                    break;

                case "landslide":
                    floodHeatmap.setEnabled(false);
                    landslideHeatmap.setEnabled(true);
                    earthquakeHeatmap.setEnabled(false);
                    disasterReportLayer.setEnabled(false);
                    landLotLayer.setEnabled(false);
                    break;

                case "earthquake":
                    floodHeatmap.setEnabled(false);
                    landslideHeatmap.setEnabled(false);
                    earthquakeHeatmap.setEnabled(true);
                    disasterReportLayer.setEnabled(false);
                    landLotLayer.setEnabled(false);
                    break;

                case "disaster-reports":
                    floodHeatmap.setEnabled(false);
                    landslideHeatmap.setEnabled(false);
                    earthquakeHeatmap.setEnabled(false);
                    disasterReportLayer.setEnabled(true);
                    landLotLayer.setEnabled(false);
                    break;

                default:
                    floodHeatmap.setEnabled(false);
                    landslideHeatmap.setEnabled(false);
                    earthquakeHeatmap.setEnabled(false);
                    disasterReportLayer.setEnabled(false);
                    landLotLayer.setEnabled(true);
                    break;
            }
        }
    }

    public String getCurrentRiskType() {
        return currentRiskType;
    }

    private void onPlotClicked(org.json.JSONObject plotData) {
        System.out.println("Plot clicked: " + plotData.toString());

        if (GdxCefInstance.isInitialized()) {
            try {
                CefBrowser browser = GdxCefInstance.getBrowser();

                String plotId = plotData.optString("EID_PARCELA", "N/A");
                String plotNumber = plotData.optString("ST_PARCELE", "N/A");
                String area = plotData.optString("POVRSINA", "N/A");
                String cadastralArea = plotData.optString("KO_ID", "N/A");

                String jsonData = String.format(
                    "{\"id\":\"%s\",\"plotNumber\":\"%s\",\"area\":\"%s\",\"cadastralArea\":\"%s\"}",
                    escapeJson(plotId),
                    escapeJson(plotNumber),
                    escapeJson(area),
                    escapeJson(cadastralArea)
                );

                String jsCode = String.format("updatePlotInfo(%s);", jsonData);
                browser.executeJavaScript(jsCode, browser.getURL(), 0);

                if (!sidebarOpen) {
                    showSidebar();
                }

            } catch (Exception e) {
                System.err.println("Error updating plot info: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void onDisasterClicked(DisasterReport disaster) {
        System.out.println("Disaster clicked: " + disaster.type + " (Severity: " + disaster.severity + ")");

        if (GdxCefInstance.isInitialized()) {
            try {
                CefBrowser browser = GdxCefInstance.getBrowser();

                String jsonData = String.format(
                    "{\"type\":\"%s\",\"severity\":%d,\"isDisaster\":true}",
                    escapeJson(disaster.type),
                    disaster.severity
                );

                String jsCode = String.format("updateDisasterInfo(%s);", jsonData);
                browser.executeJavaScript(jsCode, browser.getURL(), 0);

                if (!sidebarOpen) {
                    showSidebar();
                }

            } catch (Exception e) {
                System.err.println("Error updating disaster info: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

}
