import { useRef, useEffect } from "react";
import mapboxgl from "mapbox-gl";
import * as turf from "@turf/turf";
import "mapbox-gl/dist/mapbox-gl.css";
import styles from "../styles";



//TODO: This is here temporarily as it needs to be integrated with the search bar which is currently on another branch
//Uses MapBox foward Geocoding to find nearby locations that match the specified address (limited to Slovenia)
//Returns an array of 5 nearest matches as GeoJson features
const proximitySearchByAddress = async (address) => {
    try {
        const req = await fetch(`https://api.mapbox.com/search/geocode/v6/forward?q=${address}&proximity=ip&country=si&access_token=${process.env.REACT_APP_MAPBOX_TOKEN}`);
        const resp = await req.json();
        return resp.features;
    }
    catch(err){
        console.log(`Error in proximitySearchByAddress:`, err);
    };

    return [];
};



const Map = () => {
    const mapContainer = useRef(null);
    const map = useRef(null);
    const previousBounds = useRef({ data: [0,0,0,0] }); //Previous requested bbox for optimization

    //Rendered land lot features
    const landData = useRef({ 
        type: "FeatureCollection",
        features: [],
    }); 



    useEffect(() => {
        mapboxgl.accessToken = process.env.REACT_APP_MAPBOX_TOKEN;

        map.current = new mapboxgl.Map({
            container: mapContainer.current,
            style: "mapbox://styles/thatsebastjan/cmal96wtw013501qyglis664t",
            center: [14.9955, 46.1512], // Slovenia center coordinates
            zoom: 8
        });


        //Add events
        map.current.on("moveend", handleViewChange); //Zoom, drag
        //map.current.on("zoomend", () => { console.log("zoomend"); handleViewChange() });

        map.current.on("load", () => {

            //Add render layers
            map.current.addSource("land_data", { type: "geojson", data: landData.current });

            map.current.addLayer({
                id: "land_data",
                type: "fill",
                source: "land_data",
                paint: {
                    "fill-color": "#000000",
                    "fill-opacity": 0.2
                },
                minzoom: 13.5,
            });
        });


        //Clean-up handler
        return () => {
            map.current.remove();
        };
    }, []);



    //Get map view bounds
    const get_view_bounds = () => {
        const map_bounds = map.current.getBounds();
        return [map_bounds.getEast(), map_bounds.getSouth(), map_bounds.getWest(), map_bounds.getNorth()]; //minx, miny, maxx, maxy
    };


    //BBOX (array: [minx, miny, maxx, maxy]) to turf.polygon
    const make_polygon = (bbox) => turf.polygon([[ [bbox[0], bbox[1]], [bbox[0], bbox[3]], [bbox[2], bbox[3]], [bbox[2], bbox[1]], [bbox[0], bbox[1]] ]]);



    //On map view change event
    const handleViewChange = async () => {
        const m = map.current;
        const top_left = m.unproject({x: 0, y: 0});
        const bottom_right = m.unproject({x: m._canvas.clientWidth, y: m._canvas.clientHeight});

        //Diagonal distance max 10km
        if(top_left.distanceTo(bottom_right) > 10000){
            return
        };

        
        const bounds = get_view_bounds();
        
        try {
            const req = await fetch(`http://localhost:3001/map/query/${[...previousBounds.current.data, ...bounds].join(",")}`);

            if(req.status === 200){ //Returns 304 if there no new data
                const resp = await req.json();
                handleMapData(resp);
            };
        }
        catch(err){
            console.log("Map data query error:", err);
        };

        previousBounds.current.data = bounds;
    };



    //TODO: Better caching / optimization that does not discard data that could potentially be used when going back to previous location
    //View change -> new map data response processing
    const handleMapData = (resp) => {
        const map_view = make_polygon(get_view_bounds());

        //Keep visible features
        const old_data = landData.current.features.filter(f => turf.booleanIntersects(map_view, f));
        const new_data = resp.data.filter(f => old_data.findIndex(o => o.id === f.id) === -1); //Remove duplicates already present in old data

        //Set new data
        landData.current.features = old_data.concat(new_data);
        map.current.getSource("land_data").setData(landData.current);
    };


    return <styles.map.MapContainer ref={mapContainer} />;
};

export default Map;