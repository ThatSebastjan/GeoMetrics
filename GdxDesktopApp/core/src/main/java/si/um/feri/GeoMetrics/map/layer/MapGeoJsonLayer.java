package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;
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
import java.util.Objects;



public class MapGeoJsonLayer extends MapLayer {

    private Batch mapBatch;
    private ShapeRenderer mapSr;

    private final String dataPath;
    private final ArrayList<GeoJsonGeometry> geometryList;


    public MapGeoJsonLayer(String dataPath){
        super();

        this.dataPath = dataPath;
        geometryList = new ArrayList<>();
    }


    @Override
    public void onAddedToMap(Map map) {

        mapBatch = map.getBatch();
        mapSr = map.getShapeRenderer();


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
            geometryList.add(GeoJsonGeometry.fromJson(feature));
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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        mapSr.setProjectionMatrix(mapBatch.getProjectionMatrix());
        mapSr.begin(ShapeRenderer.ShapeType.Filled);

        mapSr.setColor(Color.RED);

        for(GeoJsonGeometry geom : geometryList){

            if(!viewBounds.overlaps(geom.getBbox())){
                continue; //Basic visibility check
            }


            /*
            ArrayList<Vector2> worldPoints = geom.getWorldCoordinates();
            Vector2 previous = worldPoints.get(0);

            for(int i = 1; i < worldPoints.size(); i++){
                Vector2 point = worldPoints.get(i);
                mapSr.line(previous, point);
                previous = point;
            }
            */


            //TODO: THIS NEEDS TO BE REIMPLEMENTED USING VBOs SO WE DON'T RE-CREATE MILLIONS OF TRIANGLES EACH FRAME!
            if(geom._debug_outlineGeometry == null) {

                ArrayList<Vector2> worldPoints = geom.getWorldCoordinates();

                Coordinate[] coords = new Coordinate[worldPoints.size()];

                for (int i = 0; i < worldPoints.size(); i++) {
                    Vector2 point = worldPoints.get(i);
                    coords[i] = new Coordinate(point.x, point.y);
                }

                GeometryFactory factory = new GeometryFactory();
                LineString outlineString = factory.createLineString(coords);
                Geometry outlineGeometry = outlineString.buffer(0.001f);
                Geometry outlineTriangles = PolygonTriangulator.triangulate(outlineGeometry);

                Polygon infillPolygon = factory.createPolygon(coords);
                Geometry infillTriangles = PolygonTriangulator.triangulate(infillPolygon);

                geom._debug_outlineGeometry = outlineTriangles;
                geom._debug_infillGeometry = infillTriangles;
            }

            mapSr.setColor(1.f, 0.f, 0.f, 0.3f);
            renderTriangles(geom._debug_infillGeometry);

            mapSr.setColor(Color.RED);
            renderTriangles(geom._debug_outlineGeometry);

        }

        mapSr.end();
    }


    @Override
    public void onKeyDown(int key) {}


    @Override
    public void onKeyUp(int key) {}


    @Override
    public void dispose() {}



    void renderTriangles(Geometry geom){
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            Polygon triangle = (Polygon)geom.getGeometryN(i);
            Coordinate[] coords = triangle.getCoordinates();

            mapSr.triangle(
                (float)coords[0].x, (float)coords[0].y,
                (float)coords[1].x, (float)coords[1].y,
                (float)coords[2].x, (float)coords[2].y
            );
        }
    }

}
