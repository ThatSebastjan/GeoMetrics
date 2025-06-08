import { useRef, useEffect } from "react";
import mapboxgl from "mapbox-gl";
import * as turf from "@turf/turf";
import "mapbox-gl/dist/mapbox-gl.css";
import styles from "../styles";

import addFloodHeatmap from "../RiskLens/floods";
import addLandslideHeatmap from "../RiskLens/landslides";
import addEarthquakeHeatmap from "../RiskLens/earthquakes";



//searchTerm = GeoJSON feature
//risk = risk lens risk type
//onAssessment = assessment callback function
//onAssessmentBegin = assessment start callback function (shows loading icons)
const Map = ({ searchTerm, risk, onAssessment, onAssessmentBegin }) => {
    const mapContainer = useRef(null);
    const map = useRef(null);
    const previousBounds = useRef({ data: [0,0,0,0] }); //Previous requested bbox for optimization

    //Rendered land lot features
    const landData = useRef({ type: "FeatureCollection", features: [] });

    //Label data
    const labelData = useRef({ type: "FeatureCollection", features: [] }); 

    //Heat map points for risk lens tab
    const floodPointsData = useRef({ type: "FeatureCollection", features: [] }); 
    const landslidePointsData = useRef({ type: "FeatureCollection", features: [] });  
    const earthquakePointsData = useRef({ type: "FeatureCollection", features: [] });  

    const loadedData = useRef({ value: false }); //...

    //Selected land lot
    const selectedLot = useRef({ value: null });



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


        map.current.on("mouseleave", "land_data_fill", () => {
            if (hoveredPolygonId !== null) {
                map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: false });
            };

            hoveredPolygonId = null;
        });



        map.current.on("click", "land_data_fill", (e) => {
            selectedLot.current = e.features[0];
            assessLandLot(e.features[0]);
        });


        
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
            const req = await fetch(`http://${window.location.hostname}:3001/map/query/${[...previousBounds.current.data, ...bounds].join(",")}`);

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



    const assessLandLot = async (feature) => {
        if(onAssessment == null){
            return; //Don't assess on other pages with no onAssessment callback
        };

        const sleep = (ms) => new Promise(r => setTimeout(r, ms));

        onAssessmentBegin();
        await sleep(500); //Simulate some delay so the loading bar doesn't disappear instantly

        const req = await fetch(`http://${window.location.hostname}:3001/map/assess`, {
            method: "POST",
            body: JSON.stringify({bounds: feature.geometry.coordinates}),
            headers: { "Content-Type": "application/json" }
        });

        const result = await req.json();

        if(req.status == 200){
            onAssessment(result);
        }
        else {
            alert(`Assessment error: ${result.message}`);
        };
    };





    //This code only gets executed on risk lens sub-page
    useEffect(() => {

        if(risk == null){
            return; 
        };

        if(loadedData.current.value === true){
            toggleRiskLayerVisiblity(risk); //Risk type has been switched
            return; //Only fetch data on risk lens tab and only once!
        };


        map.current.on("load", () => {
            loadedData.current.value = true; //Only load once

            //Add risk heatmap layers
            addFloodHeatmap(map.current, floodPointsData.current);
            addLandslideHeatmap(map.current, landslidePointsData.current);
            addEarthquakeHeatmap(map.current, earthquakePointsData.current, mapboxgl);

            //Show default layer
            toggleRiskLayerVisiblity(risk);

        
            //Load heatmap data points
            (async () => {

                //Flood data is static as there is just so much processing that needs to be done in order to generate heatmap points from it
                const floodReq = await fetch(`http://${window.location.hostname}:3001/flood_point_features.geojson`);
                floodPointsData.current = await floodReq.json();
                map.current.getSource("flood_heatmap").setData(floodPointsData.current);


                //Landslide data is also static. Same reason as above
                const lsReq = await fetch(`http://${window.location.hostname}:3001/landslide_point_features.geojson`);
                landslidePointsData.current = await lsReq.json();
                map.current.getSource("landslide_heatmap").setData(landslidePointsData.current);


                const eqReq = await fetch(`http://${window.location.hostname}:3001/map/earthquakes`);
                earthquakePointsData.current.features = await eqReq.json();
                map.current.getSource("earthquake_heatmap").setData(earthquakePointsData.current);

            })();

        });
        
    }, [risk]);



    const toggleRiskLayerVisiblity = (risk_name) => {
        const all_layers = {
            "flood": ["flood_heatmap"],
            "landslide": ["landslide_heatmap"],
            "earthquake": ["earthquake_heatmap", "earthquake_points"],
        };

        Object.keys(all_layers).forEach(k => {
            if(k !== risk_name){
                all_layers[k].forEach(layerId => {
                    map.current.setLayoutProperty(layerId, "visibility", "none");
                });
            };
        });

        all_layers[risk_name].forEach(layerId => {
            map.current.setLayoutProperty(layerId, "visibility", "visible");
        });
    };


    return <styles.map.MapContainer ref={mapContainer} />;
};

export default Map;