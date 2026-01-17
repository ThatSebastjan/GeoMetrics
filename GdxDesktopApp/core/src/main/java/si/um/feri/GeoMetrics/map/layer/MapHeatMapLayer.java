package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.heatmap.GlHeatmap;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;



public class MapHeatMapLayer extends MapLayer {

    protected Batch mapBatch;
    //private ShapeRenderer mapSr;

    protected GlHeatmap heatmap;

    int prevWidth = 0;
    int prevHeight = 0;


    public MapHeatMapLayer(){
        super();
        heatmap = new GlHeatmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }


    @Override
    public void onAddedToMap(Map map) {

        mapBatch = map.getBatch();
        //mapSr = map.getShapeRenderer();
    }


    @Override
    public void update(Map map, float dt) {

        //TODO: add map layer on resize event!
        int cWidth = Gdx.graphics.getWidth();
        int cHeight = Gdx.graphics.getHeight();

        if(cWidth != prevWidth || cHeight != prevHeight){
            heatmap.resize(cWidth, cHeight);

            prevWidth = cWidth;
            prevHeight = cHeight;

            //System.out.println("Resize!");
        }

    }


    @Override
    public void render(Map map, float dt, MapTileRegion visibleRegion) {

        if(!isEnabled()){
            return;
        }

        //DEBUG: Add points if mouse right held
        if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            float x = Gdx.input.getX();
            float y = Gdx.input.getY();

            for (int i = 0; i < 10; i++) {
                float offX = (float)(Math.random() * 2 - 1) * 0.01f; //offset in world space
                float offY = (float)(Math.random() * 2 - 1) * 0.01f;

                Vector3 p = map.unprojectToWorld(new Vector2(x, y));
                heatmap.addPoint(p.x + offX, p.y + offY, 0.1f, 2.f / 300.f);
            }

            heatmap.updateMesh();
        }

        heatmap.render(map.getCamera().combined);
    }


    @Override
    public void onKeyDown(int key) {}


    @Override
    public void onKeyUp(int key) {}


    @Override
    public void dispose() {}

}
