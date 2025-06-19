import { useState, useEffect, useContext, useRef } from 'react';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';
import { UserContext } from "../Contexts/UserContext";
import { getHighlightPoints, defaultGauges } from '../utility.js';

import * as turf from "@turf/turf";




function SideBySide() {
    const [leftFullScreen, setLeftFullScreen] = useState(false);
    const [rightFullScreen, setRightFullScreen] = useState(false);

    const context = useContext(UserContext);
    const user = context.user;

    const [gaugesLeft, setGaugesLeft] = useState(defaultGauges.map(obj => Object.assign({}, obj)));
    const [gaugesRight, setGaugesRight] = useState(defaultGauges.map(obj => Object.assign({}, obj)));
    const [isLoading, setIsLoading] = useState(false);

    const selectedFeatures = useRef([]);
    const highlightData = useRef([]);
    const lastComparedFeatures = useRef([]);

    const mapRef = useRef(null); //Ref passed into map and assigned there


    const onAddToComparison = (feature) => {
        if(selectedFeatures.current.find(f => f.properties.OBJECTID == feature.properties.OBJECTID) != null){
            return alert("Error, already added!");
        };

        selectedFeatures.current.push(feature);
        
        //Gathered 2 features to compare
        if(selectedFeatures.current.length == 2){
            const cmp = selectedFeatures.current;
            selectedFeatures.current = []; //Reset

            compareLandLots(cmp);
        };
    };


    const getCtxMenuItems = (ctxFeature) => {
        if(selectedFeatures.current.find(f => f.properties.OBJECTID == ctxFeature.properties.OBJECTID) != null){
            return [{
                text: "Remove from comparison",
                onClick: (f) => {
                    selectedFeatures.current = [];
                    highlightData.current.shift();
                }
            }];
        };

        return [
            {
                text: (selectedFeatures.current.length != 0) ? "Compare with selected" : "Add to comparison",
                onClick: onAddToComparison,
            }
        ];
    };


    //Compare selected land lots
    const compareLandLots = async (list) => {
        if(list.length != 2){
            throw new Error("This could thoretically support comparison of more than 2 lots but is limited for now");
        };

        lastComparedFeatures.current = list;

        setIsLoading(true);

        const results = [];

        for(let i = 0; i < list.length; i++){
            const req = await fetch(`http://${window.location.hostname}:3001/map/assess`, {
                method: "POST",
                body: JSON.stringify({bounds: list[i].geometry.coordinates}),
                headers: { "Content-Type": "application/json" }
            });

            const result = await req.json();

            if(req.status == 200){
                results.push(result);
            }
            else {
                return alert(`Assessment error: ${result.message}`);
            };
        };


        const gaugeSetters = [setGaugesLeft, setGaugesRight];

        gaugeSetters.forEach((setter, idx) => {
            const newGauges = defaultGauges.map(g => Object.assign({}, g));
            const result = results[idx];

            newGauges[0].value = result.floodRisk;
            newGauges[1].value = result.landSlideRisk;
            newGauges[2].value = result.earthQuakeRisk;
            setter(newGauges);
        });

        setIsLoading(false);
    };


    const onLandLotSelected = (feature) => {

        //Highlight a single feature as there is none selected for comparison yet
        if(selectedFeatures.current.length == 0){

            if((highlightData.length == 1) && (highlightData.current[0].id == feature.properties.OBJECTID)){
                return; //Already selected feature
            };

            const data = getHighlightPoints(feature);

            const obj = {
                id: feature.properties.OBJECTID,
                data: data.features.map(f => f.geometry.coordinates[0]).reverse(),
                startIndex: 0,
            };

            highlightData.current = [obj];
            return;
        };


        //There is already a single selected feature present
        if(highlightData.current.findIndex(d => d.id == feature.properties.OBJECTID) != -1){
            return; //Already selected
        };

        const data = getHighlightPoints(feature);

        const obj = {
            id: feature.properties.OBJECTID,
            data: data.features.map(f => f.geometry.coordinates[0]).reverse(),
            startIndex: 0,
        };

        highlightData.current[1] = obj;
    };


    const getHighlightData = () => {
        return highlightData.current;
    };



    //When detailed view is toggled, move the land lot into view on the opposing side of the screen
    const onLeftToggleFullScreen = () => {
        const newLeft = !leftFullScreen;
        setLeftFullScreen(newLeft);

        if(lastComparedFeatures.current.length != 2){
            return;
        };


        //Display first feature on the right half
        if(newLeft && !rightFullScreen){
            zoomToFeature(lastComparedFeatures.current[0], "right");
        }
        else if(rightFullScreen && !newLeft){ //Both shown -> left hidden
            zoomToFeature(lastComparedFeatures.current[1], "left");
        };
    }; 


    const onRightToggleFullScreen = () => {
        const newRight = !rightFullScreen;
        setRightFullScreen(newRight);

        if(lastComparedFeatures.current.length != 2){
            return;
        };


        //Display first feature on the right half
        if(newRight && !leftFullScreen){
            zoomToFeature(lastComparedFeatures.current[1], "left");
        }
        else if(leftFullScreen && !newRight){ //Both shown -> right hidden
            zoomToFeature(lastComparedFeatures.current[0], "right");
        };
    };



    const zoomToFeature = (feature, side) => {
        //const mapCanvas = mapRef.current.getCanvas();
        const featureBBOX = turf.bbox(turf.polygon(feature.geometry.coordinates));
        

        //This works somewhat ok, but we are adding bbox padding relative to center, so not perfectly centered
        const bboxWidth = featureBBOX[2] - featureBBOX[0];

        if(side == "left"){
            featureBBOX[0] -= bboxWidth * 0.1;
            featureBBOX[2] += bboxWidth * 1.1;
        }
        else {
            featureBBOX[0] -= bboxWidth * 1.1;
            featureBBOX[2] += bboxWidth * 0.1;
        };
        
        const bboxHeight = featureBBOX[3] - featureBBOX[1];
        featureBBOX[1] -= bboxHeight * 0.1;
        featureBBOX[3] += bboxHeight * 0.1;
        
        mapRef.current.fitBounds(featureBBOX, {
            easing: (t) => Math.pow(t, 1/3),
            duration: 1000,

            //This works but does not behave as expected. For some reason 1/8th of width centers it instead of 1/4th...
            //offset: [((side == "left") ? -1 : 1) * (mapCanvas.clientWidth / 8), 0],
        });
    };



    return (
        <styles.assess.Container style={{ border: "none" }}>
            <styles.assess.MapWrapper $isFullScreen={leftFullScreen && rightFullScreen}>
                <Map
                    getCtxMenuItems={getCtxMenuItems}
                    onLandLotSelected={onLandLotSelected}
                    getHighlightData={getHighlightData}
                    outMapRef={mapRef}
                />
            </styles.assess.MapWrapper>

            <styles.assess.SideBySideResults>
                {/* Left Result Bar */}
                <div>
                    <styles.assess.ResultBarInner $isFullScreen={leftFullScreen}>
                        <ResultBar
                            gauges={gaugesLeft}
                            isFullScreen={leftFullScreen}
                            onToggleFullScreen={onLeftToggleFullScreen}
                            isLoading={isLoading}
                        >
                            {/* Left side content for the expanded view */}
                            <div style={{ padding: '20px 0' }}>
                                <h3 style={{ marginBottom: '15px', fontSize: '1.2rem', fontWeight: '600' }}>Detailed Risk Analysis - Location A</h3>

                                <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Flood Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            High risk area with a 79% probability of flooding in the next 30 years. The property is located
                                            only 5m above the nearest flood plain with limited drainage infrastructure.
                                        </p>
                                    </div>

                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Mudslide Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            Low risk with only a 36% probability of soil instability issues. The property is on a 5° slope
                                            with no historical instances of land movement within 1km of the location.
                                        </p>
                                    </div>

                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Earthquake Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            Moderate risk area with 62% probability of significant seismic activity in the next 50 years.
                                            Location is within 8km of a fault line with historical 5.0+ magnitude events.
                                        </p>
                                    </div>
                                </div>

                                {user && (
                                    <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
                                        <styles.common.Button>Download Full Report</styles.common.Button>
                                        <styles.common.Button $secondary style={{ marginLeft: '10px' }}>
                                            Save Location A
                                        </styles.common.Button>
                                    </div>
                                )}
                            </div>
                        </ResultBar>
                    </styles.assess.ResultBarInner>
                </div>

                {/* Right Result Bar */}
                <div>
                    <styles.assess.ResultBarInner $isFullScreen={rightFullScreen}>
                        <ResultBar
                            gauges={gaugesRight}
                            isFullScreen={rightFullScreen}
                            onToggleFullScreen={onRightToggleFullScreen}
                            isLoading={isLoading}
                        >
                            {/* Right side content for the expanded view */}
                            <div style={{ padding: '20px 0' }}>
                                <h3 style={{ marginBottom: '15px', fontSize: '1.2rem', fontWeight: '600' }}>Detailed Risk Analysis - Location B</h3>

                                <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Flood Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            Moderate risk area with a 55% probability of flooding in the next 30 years. The property sits
                                            10m above the nearest flood plain and has sufficient drainage infrastructure.
                                        </p>
                                    </div>

                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Mudslide Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            High risk with a 78% probability of soil instability issues. The property is on a 15° slope
                                            with multiple historical instances of land movement within 300m of the location.
                                        </p>
                                    </div>

                                    <div style={{ flex: '1', minWidth: '250px' }}>
                                        <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Earthquake Risk Assessment</h4>
                                        <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                            Very high risk area with 92% probability of significant seismic activity in the next 50 years.
                                            Location is within 3km of a major fault line with historical 7.0+ magnitude events.
                                        </p>
                                    </div>
                                </div>

                                {user && (
                                    <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
                                        <styles.common.Button>Download Full Report</styles.common.Button>
                                        <styles.common.Button $secondary style={{ marginLeft: '10px' }}>
                                            Save Location B
                                        </styles.common.Button>
                                    </div>
                                )}
                            </div>
                        </ResultBar>
                    </styles.assess.ResultBarInner>
                </div>
            </styles.assess.SideBySideResults>
        </styles.assess.Container>
    );
}

export default SideBySide;