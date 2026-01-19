package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector2;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.algorithm.Centroid;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.polygon.PolygonTriangulator;
import si.um.feri.GeoMetrics.config.AppConfig;
import si.um.feri.GeoMetrics.map.Bbox;
import si.um.feri.GeoMetrics.map.GeoPoint;
import java.util.Objects;


public class GeoJsonGeometry {

    public final String type;
    private int id;
    public final GeoPoint[] coordinates;
    private Vector2[] worldCoordinates;
    private Bbox bounds;
    private Vector2 center;
    private JSONObject properties;

    public Mesh infillMesh = null;
    public Mesh outlineMesh = null;
    private boolean cutout;

    public Polygon infillPolygon = null;


    public GeoJsonGeometry(String type, int id, GeoPoint[] coordinates, boolean cutout, JSONObject properties){
        this.type = type;
        this.id = id;
        this.coordinates = coordinates;
        this.worldCoordinates = null;
        bounds = computeBbox();
        this.cutout = cutout;
        this.properties = properties;

        this.center = new Vector2(); //Set in createInfillMesh...

        createInfillMesh();
        createOutlineMesh();
    }


    private Bbox computeBbox(){
        double xMin = coordinates[0].longitude;
        double xMax = xMin;
        double yMin = coordinates[0].latitude;
        double yMax = yMin;

        for(GeoPoint p : coordinates){
            xMin = Math.min(xMin, p.longitude);
            xMax = Math.max(xMax, p.longitude);

            yMin = Math.min(yMin, p.latitude);
            yMax = Math.max(yMax, p.latitude);
        }

        return new Bbox(xMin, yMin, xMax, yMax);
    }


    public Envelope getEnvelopeBounds(){
        return new Envelope(bounds.xMin, bounds.xMax, bounds.yMin, bounds.yMax);
    }


    public Vector2[] getWorldCoordinates(){
        if(worldCoordinates == null){

            System.out.printf("DEBUG: transforming %d points to world coordinates!\n", coordinates.length);

            worldCoordinates = new Vector2[coordinates.length];

            for(int i = 0; i < coordinates.length; i++){
                worldCoordinates[i] = GeoPoint.toWorldCoordinates(coordinates[i]);
            }
        }

        return worldCoordinates;
    }


    public Bbox getBbox(){
        return bounds;
    }


    public Vector2 getCenter(){
        return center;
    }


    public int getId(){
        return id;
    }


    public JSONObject getProperties(){
        return properties;
    }


    private void createInfillMesh(){
        Vector2[] worldPoints = getWorldCoordinates();
        Coordinate[] coordList = new Coordinate[worldPoints.length];

        for (int i = 0; i < worldPoints.length; i++) {
            Vector2 point = worldPoints[i];
            coordList[i] = new Coordinate(point.x, point.y);
        }

        GeometryFactory factory = new GeometryFactory();

        infillPolygon = factory.createPolygon(coordList);

        Geometry infillTriangles = null;


        //TODO: triangulate can throw an exception if geometry is weird! CATCH EXCEPTION TO PREVENT CRASH AND HANDLE STUFF!

        try {

            //Normal polygon
            if (!cutout) {
                infillTriangles = PolygonTriangulator.triangulate(infillPolygon);
            }

            //Inverse - fill whole world, center is a hole
            else {

                Coordinate[] worldBoundCoords = {
                    new Coordinate(0, 0),
                    new Coordinate(AppConfig.WORLD_WIDTH, 0),
                    new Coordinate(AppConfig.WORLD_WIDTH, AppConfig.WORLD_HEIGHT),
                    new Coordinate(0, AppConfig.WORLD_HEIGHT),
                    new Coordinate(0, 0)
                };

                Geometry quadGeom = factory.createPolygon(worldBoundCoords);
                Geometry holed = quadGeom.difference(infillPolygon);
                infillTriangles = PolygonTriangulator.triangulate(holed);
            }

            Centroid c = new Centroid(infillPolygon);
            Coordinate centerCoord = c.getCentroid();
            center.set((float) centerCoord.x, (float) centerCoord.y);


            int numTris = infillTriangles.getNumGeometries();
            float[] vertices = new float[numTris * 3 * 3]; //3 vertices per triangle * 3 floats per vertex

            for (int i = 0; i < numTris; i++) {
                Polygon triangle = (Polygon) infillTriangles.getGeometryN(i);
                Coordinate[] triCoords = triangle.getCoordinates();

                int idx = 3 * 3 * i;
                vertices[idx] = (float) triCoords[0].x;
                vertices[idx + 1] = (float) triCoords[0].y;
                vertices[idx + 2] = 0;

                vertices[idx + 3] = (float) triCoords[1].x;
                vertices[idx + 4] = (float) triCoords[1].y;
                vertices[idx + 5] = 0;

                vertices[idx + 6] = (float) triCoords[2].x;
                vertices[idx + 7] = (float) triCoords[2].y;
                vertices[idx + 8] = 0;
            }


            VertexAttribute vtxAttributes = new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position");

            infillMesh = new Mesh(true, 3 * numTris, 0, vtxAttributes);
            infillMesh.setVertices(vertices);

            //System.out.printf("Created infill mesh with %d triangles (%d vertices)\n", numTris, vertices.length);
        } catch (Exception e) {
            System.out.println("Failed to create infill for polygon!");
        }
    }


    private void createOutlineMesh(){
        Vector2[] worldPoints = getWorldCoordinates();

        float[] vertices = new float[worldPoints.length * 3]; //3 vertices per point

        for (int i = 0; i < worldPoints.length; i++) {
            Vector2 point = worldPoints[i];

            int idx = 3 * i;
            vertices[idx] = point.x;
            vertices[idx + 1] = point.y;
            vertices[idx + 2] = 0;
        }


        VertexAttribute vtxAttributes = new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position");

        outlineMesh = new Mesh(true, worldPoints.length, 0, vtxAttributes);
        outlineMesh.setVertices(vertices);

        System.out.printf("Created outline mesh with %d lines (%d vertices)\n", worldPoints.length, vertices.length);
    }


    //TODO: support for other features?
    public static GeoJsonGeometry fromJson(JSONObject feature, int id, boolean cutout){
        JSONObject geometry = feature.getJSONObject("geometry");
        String geometryType = geometry.getString("type");

        if(!Objects.equals(geometryType, "Polygon")){
            throw new IllegalArgumentException("Only polygon geometry supported for now!");
        }

        JSONObject properties = feature.getJSONObject("properties");
        JSONArray coordinates = geometry.getJSONArray("coordinates");

        //TODO: add support for holes! JTS supports it, we just need to handle mesh generation differently here
        /*
        if(coordinates.length() != 1){
            throw new IllegalArgumentException("Only polygons with single ring are supported for now!");
        }
        */

        JSONArray ring = coordinates.getJSONArray(0);
        GeoPoint[] cList = new GeoPoint[ring.length()];

        for(int cIdx = 0; cIdx < ring.length(); cIdx++){
            JSONArray cPair = ring.getJSONArray(cIdx);
            cList[cIdx] = new GeoPoint(cPair.getDouble(0), cPair.getDouble(1));
        }

        return new GeoJsonGeometry(geometryType, id, cList, cutout, properties);
    }
}
