import { useState, useEffect, useContext, useRef } from 'react';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';
import { UserContext } from "../Contexts/UserContext";
import { defaultGauges, dist2D, getHighlightPoints } from '../utility.js';



function SmartSelect() {
    const user = useContext(UserContext).user;

    const [isFullScreen, setIsFullScreen] = useState(false);
    const [gauges, setGauges] = useState(defaultGauges.map(obj => Object.assign({}, obj)));

    const highlightData = useRef({ id: null, data: null, startIndex: 0 });

    const initStatus = useRef(false);
    const mapObj = useRef(null);

    const pointList = useRef([]); //A list of polygon points for area selection (pairs [lng, lat] - in world space)
    const nearPointIdx = useRef(null); //Nearest point (index) for rendering purposes
    const allowNewPoints = useRef(true);



    const onLandLotSelected = (feature, isCtxMenuSelect) => {

        if(isCtxMenuSelect != true){
            return; //Only select land lots on context menu open
        };
        
        if(feature.properties.OBJECTID == highlightData.current.id){
            return; //Same, already selected feature
        };

        const data = getHighlightPoints(feature);
        highlightData.current.data = data.features.map(f => f.geometry.coordinates[0]).reverse();

        highlightData.current.startIndex = 0;
        highlightData.current.id = feature.properties.OBJECTID;
    };


    const getHighlightData = () => {
        if(highlightData.current.id == null){
            return [];
        };

        return [highlightData.current];
    };


    let ldo = 0; //current line dash offset

    const postRender = (map, ctx) => {

        //Add listeners - only once
        if(initStatus.current == false){
            initStatus.current = true;
            mapObj.current = map;

            map.on("mousedown", handleMapMousedown);
            map.on("mousemove", handleMapMousemove);
        };


        if(pointList.current.length == 0){
            return;
        };


        const points = pointList.current.map(p => map.project(p));;

        //Draw polygon
        if(points.length > 1){

            ctx.beginPath()
            ctx.setLineDash([5, 10]);
            ctx.lineWidth = 2;
            ctx.strokeStyle = "rgb(60, 140, 70)";
            ctx.fillStyle = "rgba(0, 0, 0, 0.1)";

            for(let i = 0; i < points.length; i++){
                const p = points[i];

                if(i == 0){
                    ctx.moveTo(p.x, p.y);
                }
                else { 
                    ctx.lineTo(p.x, p.y);
                };
            };

            ctx.fill();
            ctx.stroke();
            ctx.lineWidth = 1;
            ctx.setLineDash([]);
        };


        //Draw points if placing / removing is allowed
        if(allowNewPoints.current){
            ctx.strokeStyle = "rgb(20, 59, 4)";

            console.log(nearPointIdx.current);

            points.forEach((p2d, i) => {

                if((i == 0) && (nearPointIdx.current == 0) && (pointList.current.length >= 3)){
                    ctx.fillStyle = "rgb(90, 190, 100)";
                }
                else {
                    ctx.fillStyle = (i == nearPointIdx.current) ? "rgb(237, 84, 112)" : "rgb(60, 140, 70)";
                };

                ctx.beginPath();
                ctx.arc(p2d.x, p2d.y, 2, 0, 2*Math.PI);
                ctx.stroke();
                ctx.fill();
            });
        };


        ldo = (ldo + 0.1) % 15;
        ctx.lineDashOffset = ldo;
    };


    const handleMapMousedown = (ev) => {

        if(allowNewPoints.current == false){
            return;
        };

        const map = ev.target;
        const points = pointList.current.map(p => map.project(p));

        const pDist = points.map((p, i) => { return { p, i, dist: dist2D(ev.point.x, ev.point.y, p.x, p.y) } }).sort((a, b) => a.dist - b.dist)[0];
        
        //Left click - add new point
        if(ev.originalEvent.which == 1){

            //Close area polygon loop
            if((pDist != null) && (pDist.i == 0) && (pDist.dist < 10) && (pointList.current.length >= 3)){
                pointList.current.push(pointList.current[0]);
                onAreaSelected();
            }
            else {
                pointList.current.push(ev.lngLat.toArray());
            };
        }

        //Right click - remove point / cancel selection
        else if(ev.originalEvent.which == 3){
            
            //Remove one near point
            if((pDist != null) && (pDist.dist < 10)){
                pointList.current.splice(pDist.i, 1);
            }

            //Cancel selection
            else {

                //TODO: when nicer dialogs are implemented, swith this one out
                if(window.confirm("Are you sure you want to reset area selection?")){
                    pointList.current = [];
                };
            };

            nearPointIdx.current = null;
        };

    };


    const handleMapMousemove = (ev) => {
        const map = ev.target;

        if(allowNewPoints.current == false){
            map.getCanvas().style.cursor = "default";
            return;
        };

        const points = pointList.current.map(p => map.project(p));
        const pDist = points.map((p, i) => { return { p, i, dist: dist2D(ev.point.x, ev.point.y, p.x, p.y) } }).sort((a, b) => a.dist - b.dist)[0];

        //Is within 10px radius
        if((pDist != null) && (pDist.dist < 10)){
            map.getCanvas().style.cursor = "pointer";
            nearPointIdx.current = pDist.i;
            return;
        };
        
        map.getCanvas().style.cursor = "crosshair";
        nearPointIdx.current = null;
    };


    //This only serves the purpose of removing the added event listneres
    useEffect(() => {

        return () => {
            if(mapObj.current != null){
                mapObj.current.off("mousedown", handleMapMousedown);
                mapObj.current.off("mousemove", handleMapMousemove);
            }; 
        };
    }, []);


    //Called when user completes the area selection
    const onAreaSelected = async () => {
        allowNewPoints.current = false;

        const req = await fetch(`http://${window.location.hostname}:3001/map/smartSelect`, {
            method: "POST",
            body: JSON.stringify({
                bounds: pointList.current,
                filter: "best",
            }),
            headers: { "Content-Type": "application/json" }
        });

        const result = await req.json();

        if(req.status == 200){
            console.log("Result:", result);
        }
        else {
            return alert(`An error occurred: ${result.message}`);
        };
    };


    return (
        <styles.assess.Container style={{ border: "none" }}>
            <styles.assess.MapWrapper $isFullScreen={isFullScreen}>
                <Map 
                    onLandLotSelected={onLandLotSelected}
                    getHighlightData={getHighlightData}
                    overlayPostRender={postRender}
                />
            </styles.assess.MapWrapper>

            <styles.assess.ResultBarWrapper>
                <styles.assess.ResultBarInner $isFullScreen={isFullScreen}>
                    <ResultBar
                        gauges={gauges}
                        isFullScreen={isFullScreen}
                        onToggleFullScreen={() => setIsFullScreen(!isFullScreen)}
                    >
                        {/* Additional content for the expanded view */}
                        <div style={{ padding: '20px 0' }}>
                            <h3 style={{ marginBottom: '15px', fontSize: '1.2rem', fontWeight: '600' }}>Detailed Risk Analysis</h3>

                            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                                <div style={{ flex: '1', minWidth: '250px' }}>
                                    <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Flood Risk Assessment</h4>
                                    <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                        Low risk area with a 10% probability of flooding in the next 30 years. The property sits
                                        15m above the nearest flood plain and has adequate drainage infrastructure.
                                    </p>
                                </div>

                                <div style={{ flex: '1', minWidth: '250px' }}>
                                    <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Mudslide Risk Assessment</h4>
                                    <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                        Moderate risk with a 65% probability of soil instability issues. The property is on a 12° slope
                                        with historical instances of land movement within 500m of the location.
                                    </p>
                                </div>

                                <div style={{ flex: '1', minWidth: '250px' }}>
                                    <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Earthquake Risk Assessment</h4>
                                    <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                        High risk area with 92% probability of significant seismic activity in the next 50 years.
                                        Location is within 5km of a major fault line with historical 6.5+ magnitude events.
                                    </p>
                                </div>
                            </div>

                            {user && (
                                <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
                                    <styles.common.Button>Download Full Report</styles.common.Button>
                                    <styles.common.Button $secondary style={{ marginLeft: '10px' }}>
                                        Save Location
                                    </styles.common.Button>
                                </div>
                            )}
                        </div>
                    </ResultBar>
                </styles.assess.ResultBarInner>
            </styles.assess.ResultBarWrapper>
        </styles.assess.Container>
    );
}

export default SmartSelect;