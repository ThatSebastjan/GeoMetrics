package si.um.feri.GeoMetrics.map.heatmap;


import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class Node {

    private FrameBuffer fbo;


    public Node(int width, int height){

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

        Texture fboTx = fbo.getColorBufferTexture();
        fboTx.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fboTx.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    }


    public void use(){
        fbo.bind();
    }


    public void bind(int unit){
        fbo.getColorBufferTexture().bind(unit);
    }


    public void end(){
       FrameBuffer.unbind();
    }


    public void resize(int w, int h){
        fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    }


    public FrameBuffer getFbo(){
        return fbo;
    }

}
