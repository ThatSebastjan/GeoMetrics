import { useRef, useEffect, useState } from "react";
import mapboxgl from "mapbox-gl";
import * as turf from "@turf/turf";
import "mapbox-gl/dist/mapbox-gl.css";
import styles from "../styles";

import addFloodHeatmap from "../RiskLens/floods";
import addLandslideHeatmap from "../RiskLens/landslides";
import addEarthquakeHeatmap from "../RiskLens/earthquakes";
import ContextMenu from "./ContextMenu";



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

    const overlayCanvas = useRef(null);
    const overlayContext = useRef(null);
    const overlayAnimFrameId = useRef(-1);
    const overlayPoints = useRef({ id: null, data: null });

    //Context menu
    const [showCtxMenu, setShowCtxMenu] = useState(false);
    const ctxMenuPos = useRef({ x: 0, y: 0 });
    const ctxMenuFeature = useRef(null);



    useEffect(() => {
        mapboxgl.accessToken = process.env.REACT_APP_MAPBOX_TOKEN;

        map.current = new mapboxgl.Map({
            container: mapContainer.current,
            style: "mapbox://styles/mapbox/standard",
            center: [14.9955, 46.1512], // Slovenia center coordinates
            zoom: 8
        });

        //DEBUG ONLY
        window.map = map.current;
        window.turf = turf;


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
        let hoveredPropId = null;

        map.current.on("mousemove", "land_data_fill", (e) => {
            if (e.features.length > 0) {
                if (hoveredPolygonId !== null) {
                    map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: false });
                };

                hoveredPolygonId = e.features[0].id;
                hoveredPropId = e.features[0].properties.OBJECTID;
                map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: true });
            };
        });


        map.current.on("mouseleave", "land_data_fill", () => {
            if (hoveredPolygonId !== null) {
                map.current.setFeatureState({ source: "land_data", id: hoveredPolygonId }, { hover: false });
            };

            hoveredPolygonId = null;
            hoveredPropId = null;
        });



        map.current.on("click", "land_data_fill", (e) => {
            window.selectedLot = e.features[0]; //DEBUG ONLY!
            selectedLot.current = e.features[0];
            assessLandLot(e.features[0]);
        });




        const mapCanvas = map.current.getCanvas();

        const onCtxMenu = (ev) => {
            ev.preventDefault();

            if(hoveredPropId == null){
                return;
            };

            ctxMenuFeature.current = landData.current.features.find(f => f.properties.OBJECTID == hoveredPropId);

            const bounds = mapCanvas.getBoundingClientRect()
            ctxMenuPos.current.x = ev.clientX - bounds.x;
            ctxMenuPos.current.y = ev.clientY - bounds.y;
            setShowCtxMenu(true);

            onLandLotSelected(ctxMenuFeature.current);
        };
        
        
        if(risk == null){

            //Context menu listeners
            mapCanvas.addEventListener("contextmenu", onCtxMenu);

            //Init overlay
            overlayContext.current = overlayCanvas.current.getContext("2d");

            const resizeListener = () => {
                overlayCanvas.current.width = mapCanvas.width;
                overlayCanvas.current.height = mapCanvas.height;
            };

            resizeListener();
            map.current.on("resize", resizeListener);
        };

        window.overlayCanvas = overlayCanvas.current; //DEBUG ONLY
        window.ctx = overlayContext.current; //DEBUG ONLY


        
        //Clean-up handler
        return () => {
            if(risk != null){
                mapCanvas.removeEventListener("contextmenu", onCtxMenu);
            };

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

        if(map.current.getZoom() < 13.5){
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



    const assessLandLot = async (f) => {
        if(onAssessment == null){
            return; //Don't assess on other pages with no onAssessment callback
        };

        onLandLotSelected(f);

        const sleep = (ms) => new Promise(r => setTimeout(r, ms));

        onAssessmentBegin();
        await sleep(500); //Simulate some delay so the loading bar doesn't disappear instantly

        //Find the feature manually as one provided in event might be clipped off due to map tile boundary...
        const feature = landData.current.features.find(e => e.properties.OBJECTID == f.properties.OBJECTID);

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



    const onLandLotSelected = (f) => {

        if(f.properties.OBJECTID == overlayPoints.current.id){
            return; //Same, already selected feature
        };

        overlayPoints.current.id = f.properties.OBJECTID;

        const feature = landData.current.features.find(e => e.properties.OBJECTID == f.properties.OBJECTID);

        const outerRing = turf.lineString(feature.geometry.coordinates[0]);
        const chunk = turf.lineChunk(outerRing, 0.001); //Point every 1m
        overlayPoints.current.data = chunk.features.map(f => f.geometry.coordinates[0]).reverse();
    };


    //Land lot animation thing
    useEffect(() => {

        if(risk != null){
            return; //Only execute on main assess page
        };

        let startIndex = 0;
        let prevTime = null;
        const maxThickness = 5;
        const lengthRatio = 0.45;
        const loopDuration = 3000; //In ms
        const highlightColor = "rgb(230, 115, 255)";

        const clipMaskAlpha = 0.5;
        const fadeDuration = 300; //In ms
        let currentAlpha = 0; //Clip mask alpha for fading
        let fadeStart = null;

        const renderOverlayAnimation = (timestamp) => {
            const ctx = overlayContext.current;
            const pList = overlayPoints.current.data;

            if(overlayCanvas.current === null){
                return;
            };

            ctx.clearRect(0, 0, overlayCanvas.current.width, overlayCanvas.current.height);


            if(!prevTime){
                prevTime = timestamp;
            };

            if(!pList || (map.current.getZoom() < 13.5)){
                overlayAnimFrameId.current = requestAnimationFrame(renderOverlayAnimation);
                return;
            };

            const points2d = pList.map(p => map.current.project(p));


            //Draw clipping mask if context menu land lot is selected
            if(ctxMenuFeature.current != null){
                ctx.save();

                ctx.beginPath();
                ctx.rect(0, 0, overlayCanvas.current.width, overlayCanvas.current.height);
                
                ctx.moveTo(points2d[0].x, points2d[0].y);

                for(let i = points2d.length-1; i != 0; i--){
                    ctx.lineTo(points2d[i].x, points2d[i].y);
                };

                ctx.lineTo(points2d[0].x, points2d[0].y);
                ctx.clip();

                ctx.fillStyle = `rgba(0, 0, 0, ${currentAlpha})`;
                ctx.fillRect(0, 0, overlayCanvas.current.width, overlayCanvas.current.height);

                ctx.restore();

                if(fadeStart === null){
                    fadeStart = timestamp;
                };

                currentAlpha = Math.min((timestamp - fadeStart) / fadeDuration, 1) * clipMaskAlpha;
            }
            else if(fadeStart !== null){
                currentAlpha = 0;
                fadeStart = null;
            };


            ctx.strokeStyle = highlightColor;

            const numThick = Math.floor(points2d.length * lengthRatio);

            for(let i = 0; i < numThick; i++){
                const pIdx = (startIndex + i) % points2d.length;
                const thickness = Math.max((i / numThick) * maxThickness, 0.001);

                const p = points2d[pIdx];
                const n = points2d[(pIdx + 1) % points2d.length];

                ctx.lineWidth = thickness;
                ctx.beginPath();
                ctx.moveTo(p.x, p.y);
                ctx.lineTo(n.x, n.y);
                ctx.stroke();
            };


            //Cycle with constant time
            const tDelta = timestamp - prevTime;
            const numChunks = Math.round(pList.length * (tDelta / loopDuration));

            if(numChunks > 0){
                startIndex = (startIndex + numChunks) % points2d.length;
                prevTime = timestamp;
            };

            overlayAnimFrameId.current = requestAnimationFrame(renderOverlayAnimation);
        };

        overlayAnimFrameId.current = requestAnimationFrame(renderOverlayAnimation);
        
        return () => {
            cancelAnimationFrame(overlayAnimFrameId.current);
        }
    }, [risk]);





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


    return (
    <styles.map.MapContainer ref={mapContainer}>
        <canvas ref={overlayCanvas} style={{zIndex: 10, position: "absolute", top: 0, left: 0, pointerEvents: "none"}}></canvas>
        { 
            showCtxMenu ? 
            (<ContextMenu 
                x={ctxMenuPos.current.x}
                y={ctxMenuPos.current.y}
                onClose={() => {
                    setShowCtxMenu(false);
                    ctxMenuFeature.current = null;
                }}
                onSave={() => { alert("TODO: save lot"); }}
                onAddToComparison={() => { alert("TODO: add lot to comparison"); }}
            ></ContextMenu>) 
            : (<></>)
        }
    </styles.map.MapContainer>);
};

export default Map;