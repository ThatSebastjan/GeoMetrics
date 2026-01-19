package si.um.feri.GeoMetrics.map.layer;

import com.badlogic.gdx.utils.Disposable;
import si.um.feri.GeoMetrics.map.Map;
import si.um.feri.GeoMetrics.map.tile.MapTileRegion;

import java.util.UUID;


//Base map layer class
public abstract class MapLayer implements Disposable {

    private boolean enabled;
    private final UUID uuid;


    public MapLayer(){
        enabled = true;
        uuid = UUID.randomUUID();
    }


    public abstract void onAddedToMap(Map map);


    public abstract void update(Map map, float dt);

    public abstract void render(Map map, float dt, MapTileRegion visibleRegion);


    public abstract void onKeyDown(int key);

    public abstract void onKeyUp(int key);


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e){
        enabled = e;
    }

    public UUID getUuid() {
        return uuid;
    }
}
