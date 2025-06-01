import { useRef, useEffect } from "react";
import mapboxgl from "mapbox-gl";
import * as turf from "@turf/turf";
import "mapbox-gl/dist/mapbox-gl.css";
import styles from "../styles";



//searchTerm = GeoJSON feature
const Map = ({ searchTerm }) => {
    const mapContainer = useRef(null);
    const map = useRef(null);
    const previousBounds = useRef({ data: [0,0,0,0] }); //Previous requested bbox for optimization

    //Rendered land lot features
    const landData = useRef({ 
        type: "FeatureCollection",
        features: [],
    });


    //Test data
    const labelData = useRef({ 
        type: "FeatureCollection",
        features: [],
    }); 



    useEffect(() => {
        mapboxgl.accessToken = process.env.REACT_APP_MAPBOX_TOKEN;

        map.current = new mapboxgl.Map({
            container: mapContainer.current,
            style: "mapbox://styles/mapbox/standard",
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
                type: "line",
                source: "land_data",
                paint: {
                    "line-color": "#005000",
                    "line-opacity": 0.3
                },
                minzoom: 13.5,
            });


            map.current.addLayer({
                id: "land_data_fill",
                type: "fill",
                source: "land_data", //Same source
                paint: {
                    "fill-color": "#000000",
                    "fill-opacity":  [
                        "case",
                        ["boolean", ["feature-state", "hover"], false],
                        0.1,
                        0
                    ],
                },
                minzoom: 13.5,
            });


            //Add label layer
            map.current.addSource("label_data", { type: "geojson", data: labelData.current });

            map.current.addLayer({
                id: "label_data",
                type: "symbol",
                source: "label_data",
                layout: {
                    "text-field": ["get", "label"],
                    "text-font": ["Open Sans Bold", "Arial Unicode MS Bold"],
                    "text-size": 11,
                    "text-letter-spacing": 0.05,
                    "text-offset": [0, 0]
                },
                paint: {
                    "text-color": "#202",
                    "text-halo-color": "#fff",
                    "text-halo-width": 2
                },

                minzoom: 13.5,
            });
        });



        //Hover interactions
        let hoveredPolygonId = null;

        map.current.on("mousemove", "land_data_fill", (e) => {
            if (e.features.length > 0) {
                if (hoveredPolygonId !== null) {
                    map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: false });
                };

                hoveredPolygonId = e.features[0].id;
                map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: true });
            };
        });


        map.current.on("mouseleave", "state-fills", () => {
            if (hoveredPolygonId !== null) {
                map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: false });
            };

            hoveredPolygonId = null;
        });


        //TODO: Add a click handler to select a land lot and display relevant information!


        
        //Clean-up handler
        return () => {
            map.current.remove();
        };
    }, []);


    //This handler gets called when search term is changed in parent component
    useEffect(() => {

        if(searchTerm != null){

            if(searchTerm.properties.bbox != null){
                map.current.fitBounds([
                    searchTerm.properties.bbox.slice(0, 2),
                    searchTerm.properties.bbox.slice(2)
                ]);
            }
            else {
                map.current.panTo(searchTerm.geometry.coordinates);
                //TODO: maybe add a marker here
            };
        };
        
    }, [searchTerm]);



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

        const from = turf.point(top_left.toArray());
        const to = turf.point(bottom_right.toArray());
        const distance = turf.distance(from, to); //in km

        //Diagonal distance max 10km
        if(distance > 10){
            return;
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


        //Set label data
        labelData.current.features = landData.current.features.map(f => {
            const point = turf.centerOfMass(turf.polygon(f.geometry.coordinates));
            point.properties.label = f.properties.ST_PARCELE;
            return point
        });

        map.current.getSource("label_data").setData(labelData.current);
    };


    return <styles.map.MapContainer ref={mapContainer} />;
};

export default Map;