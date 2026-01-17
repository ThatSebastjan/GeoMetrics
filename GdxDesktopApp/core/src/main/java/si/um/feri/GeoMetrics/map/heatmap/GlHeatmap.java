package si.um.feri.GeoMetrics.map.heatmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;


public class GlHeatmap {

    private static ShaderProgram heatmapShader = null;

    private int width;
    private int height;

    private Mesh quad;
    private Heights heights;

    private final float[] colorMap = { //5 color map; (r, g, b) each
        0.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
    };


    //TODO: param for color map
    public GlHeatmap(int width, int height){
        this.width = width;
        this.height = height;


        float[] vertices = {
            -1, -1, 0, 1,
            1, -1, 0, 1,
            -1,  1, 0, 1,
            -1,  1, 0, 1,
            1, -1, 0, 1,
            1,  1, 0, 1
        };

        quad = new Mesh(true, 6, 0, new VertexAttribute(VertexAttributes.Usage.Position, 4, "a_position"));
        quad.setVertices(vertices);

        heights = new Heights(width, height);
    }


    public void resize(int w, int h){
        width = w;
        height = h;
        heights.resize(w, h);
    }


    public void updateMesh(){
        heights.updateMesh();
    }


    public void render(Matrix4 projectionMat){

        //Render heatmap to FBO
        heights.render(projectionMat);


        //Render FBO to screen with color post-processing
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); //This is important, otherwise weird this happen!

        heights.getNodeFront().bind(0);

        ShaderProgram displayShader = getHeatmapShader();

        displayShader.bind();
        displayShader.setUniformi("u_source", 0);
        displayShader.setUniform3fv("u_colorMap", colorMap, 0, 15);

        quad.render(displayShader, GL20.GL_TRIANGLES);
    }


    public void addPoint(float x, float y, float size, float intensity){
        heights.addPoint(x, y, size, intensity);
    }


    public Heights getHeights(){
        return heights;
    }


    public void setColorMap(float[] newColors){

        if(newColors.length != 15){
            throw new RuntimeException("Color map must have 15 components!");
        }

        for(int i = 0; i < 15; i++){
            colorMap[i] = newColors[i];
        }
    }


    private static ShaderProgram getHeatmapShader(){
        if(heatmapShader == null){
            String vertexShader = Gdx.files.internal("assets/shaders/heatmap_vertex.glsl").readString();
            String fragmentShader = Gdx.files.internal("assets/shaders/heatmap_fragment.glsl").readString();
            heatmapShader = new ShaderProgram(vertexShader, fragmentShader);
        }

        return heatmapShader;
    }
}
