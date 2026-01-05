package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.QuadTreeFloat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.triangulate.polygon.PolygonTriangulator;
import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class MapGeoJsonLayer extends MapLayer {

    private Batch mapBatch;
    //private ShapeRenderer mapSr;

    private final String dataPath;
    private final ArrayList<GeoJsonGeometry> geometryList;
    private final Quadtree quadtree;

    //Static test shader
    static ShaderProgram geoJsonShader = null;


    public MapGeoJsonLayer(String dataPath){
        super();

        this.dataPath = dataPath;
        geometryList = new ArrayList<>();
        quadtree = new Quadtree();
    }


    @Override
    public void onAddedToMap(Map map) {

        mapBatch = map.getBatch();
        //mapSr = map.getShapeRenderer();


        //Load geometry from file
        JSONObject data = readData();

        if(data == null){
            return;
        }

        if(!Objects.equals(data.getString("type"), "FeatureCollection")){
            throw new IllegalArgumentException("Invalid GeoJson data!");
        }

        JSONArray features = data.getJSONArray("features");

        for(int i = 0; i < features.length(); i++){
            JSONObject feature = features.getJSONObject(i);

            boolean cutout = false;
            JSONObject properties = feature.getJSONObject("properties");

            if(properties != null){
                if(properties.has("cutout")){
                    cutout = properties.getBoolean("cutout");
                }
            }

            GeoJsonGeometry geom = GeoJsonGeometry.fromJson(feature, cutout);
            geometryList.add(geom);

            quadtree.insert(geom.getEnvelopeBounds(), geom);
        }

    }


    JSONObject readData(){
        try {
            InputStream inStream = Gdx.files.absolute(dataPath).read();

            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }

            return new JSONObject(textBuilder.toString());
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void update(Map map, float dt) {}


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }


        GeoPoint topLeft = map.unproject(new Vector2(0.f, 0.f));
        GeoPoint bottomRight = map.unproject(new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        Bbox viewBounds = Bbox.fromPoints(topLeft, bottomRight);

        Gdx.gl.glEnable(GL32.GL_BLEND);
        Gdx.gl.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);


        //Set shader constants
        ShaderProgram shaderProgram = getGeoJsonShader();
        shaderProgram.bind();
        shaderProgram.setUniformMatrix("u_projTrans", mapBatch.getProjectionMatrix());
        shaderProgram.setUniformf("u_inColor", new Color(0.f, 0.f, 0.f, 0.4f));


        //Query visible items
        Envelope visibleEnvelope = new Envelope(viewBounds.xMin, viewBounds.xMax, viewBounds.yMin, viewBounds.yMax);

        //long startTime = System.nanoTime();
        List<GeoJsonGeometry> visibleGeometry = quadtree.query(visibleEnvelope);

        //long endTime = System.nanoTime();
        //long duration = (endTime - startTime);

        //System.out.printf("Num visible: %d (%d)\n", visibleGeometry.size(), duration);

        //Render infill
        for(GeoJsonGeometry geom : visibleGeometry) {
            geom.infillMesh.render(shaderProgram, GL32.GL_TRIANGLES);
        }


        //Render outlines
        shaderProgram.setUniformf("u_inColor", new Color(0.f, 0.f, 0.f, 0.7f));

        Gdx.gl.glLineWidth(5.f);
        Gdx.gl.glDisable(GL32.GL_CULL_FACE);
        Gdx.gl.glDisable(GL32.GL_DEPTH_TEST);

        for(GeoJsonGeometry geom : visibleGeometry) {
            geom.outlineMesh.render(shaderProgram, GL32.GL_LINE_STRIP);
        }

        Gdx.gl.glLineWidth(1.f);

    }


    @Override
    public void onKeyDown(int key) {}


    @Override
    public void onKeyUp(int key) {}


    @Override
    public void dispose() {}



    static ShaderProgram getGeoJsonShader(){

        //Load on first call
        if(geoJsonShader == null){
            String vertexShader = Gdx.files.internal("assets/shaders/geojson_vertex.glsl").readString();
            String fragmentShader = Gdx.files.internal("assets/shaders/geojson_fragment.glsl").readString();
            geoJsonShader = new ShaderProgram(vertexShader, fragmentShader);
        }

        return geoJsonShader;
    }

}
