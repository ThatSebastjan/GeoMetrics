package si.um.feri.GeoMetrics.config;

import si.um.feri.GeoMetrics.map.Bbox;

public class AppConfig {

    private AppConfig(){}


    public static final Bbox MAP_BOUNDS = new Bbox(
        13.3754608,
        45.08532178477287,
        16.5966968,
        47.197776615227134
    );

    public static final float WORLD_WIDTH = 100.f;
    public static final float WORLD_HEIGHT = 100.f / 1.0563666f;

    public static final int WINDOW_WIDTH = 1600;
    public static final int WINDOW_HEIGHT = 900;

    //public static final int TILE_SIZE = 512;

    public static final String BACKEND_URL = "http://localhost:3001";

    public static final String TILE_DATA_PATH = "C:/Users/Rok/Desktop/tile_grabber/data/";

    public static final int TILE_MAP_MIN_ZOOM = 9;
    public static final int TILE_MAP_MAX_ZOOM = 15;
}
