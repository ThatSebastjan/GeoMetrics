package si.um.feri.GeoMetrics.map.heatmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;


public class Heights {

    private static ShaderProgram heightsShader = null;
    private static final int maxPoints = 1024 * 1024;

    private final Node nodeBack;
    private final Node nodeFront;
    private int width;
    private int height;

    private final Mesh mesh;
    private final float[] vboData;

    private int bufferIdx = 0;
    private int pointNum = 0;
    private final int vertexSize; //vertex size in number of floats not bytes!


    public Heights(int width, int height){
        nodeBack = new Node(width, height);
        nodeFront = new Node(width, height);

        this.width = width;
        this.height = height;


        VertexAttribute posAttrib = new VertexAttribute(VertexAttributes.Usage.Position, 4, "a_position");
        VertexAttribute intensityAttrib = new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_intensity");

        mesh = new Mesh(false, maxPoints * 6, 0, posAttrib, intensityAttrib);
        vertexSize = mesh.getVertexSize() / 4;

        vboData = new float[maxPoints * vertexSize * 6];
    }


    public void resize(int w, int h){
        width = w;
        height = h;
        nodeFront.resize(w, h);
        nodeBack.resize(w, h);
    }


    public void render(Matrix4 projMat){

        if(pointNum == 0){
            return;
        }

        //Clear framebuffer
        nodeFront.use();
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        nodeFront.end();


        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

        //Bind FBO
        nodeFront.use();

        //Set params
        ShaderProgram shader = getHeightsShader();
        shader.bind();
        shader.setUniformMatrix("u_projTrans", projMat);

        //Render into FBO
        mesh.render(shader, GL20.GL_TRIANGLES);

        //Unbind
        nodeFront.end();
    }


    private void addVertex(float x, float y, float xs, float ys, float intensity){
        vboData[bufferIdx++] = x;
        vboData[bufferIdx++] = y;
        vboData[bufferIdx++] = xs;
        vboData[bufferIdx++] = ys;
        vboData[bufferIdx++] = intensity;
    }


    public void addPoint(float x, float y, float size, float intensity) {
        float s = size / 2.f;

        addVertex(x, y, -s, -s, intensity);
        addVertex(x, y, +s, -s, intensity);
        addVertex(x, y, -s, +s, intensity);

        addVertex(x, y, -s, +s, intensity);
        addVertex(x, y, +s, -s, intensity);
        addVertex(x, y, +s, +s, intensity);

        pointNum++;
    }


    //Set vertices when needed
    public void updateMesh(){
        mesh.setVertices(vboData, 0, pointNum * 6 * vertexSize);
    }


    public void clear(){
        mesh.setVertices(new float[]{});
        pointNum = 0;
        bufferIdx = 0;
    }


    public Node getNodeFront(){
        return nodeFront;
    }


    private static ShaderProgram getHeightsShader(){
        if(heightsShader == null){
            String vertexShader = Gdx.files.internal("assets/shaders/heights_vertex.glsl").readString();
            String fragmentShader = Gdx.files.internal("assets/shaders/heights_fragment.glsl").readString();
            heightsShader = new ShaderProgram(vertexShader, fragmentShader);
        }

        return heightsShader;
    }

}
