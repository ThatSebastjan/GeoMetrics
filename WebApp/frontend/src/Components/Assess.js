import { useState, useEffect, useContext, useRef } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';
import AdvancedBar from '../Components/AdvancedBar';
import SearchBar from "./SearchBar";
import { UserContext } from '../Contexts/UserContext.js';
import { PopupContext } from "../Contexts/CustomPopups";

import { getHighlightPoints, sleep, defaultGauges, getFeatureAddress } from '../utility.js';



function Assess() {
    const context = useContext(UserContext);
    const [isFullScreen, setIsFullScreen] = useState(false);
    const [isAdvancedFullScreen, setIsAdvancedFullScreen] = useState(false);
    const [searchTerm, setSearchTerm] = useState(null);
    const [isLoadingAssessment, setIsLoadingAssessment] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();
    const { param } = useParams();

    const highlightData = useRef({ id: null, data: null, startIndex: 0 });
    const assessedFeature = useRef(null);
    const assessementResults = useRef(null);

    const [floodDetails, setFloodDetails] = useState(null);
    const [landSlideDetails, setLandSlideDetails] = useState(null);
    const [earthQuakeDetails, setEarthQuakeDetails] = useState(null);

    const { alert, confirm, saveLot } = useContext(PopupContext);



    const searchParams = new URLSearchParams(location.search);
    let initInfo = null;

    if(searchParams.has("lng") && searchParams.has("lat") && searchParams.has("id") && (searchTerm == null)){
        initInfo = {
            lng: parseFloat(searchParams.get("lng")),
            lat: parseFloat(searchParams.get("lat")),
            zoom: 17,
            id: parseInt(searchParams.get("id")),
        };
    };


    const handleSearch = (query) => {
        setSearchTerm(query);
    };

    // Initial settings for the risk assessment
    const [settings, setSettings] = useState({
        timeHorizon: 30,
        modelAccuracy: 'medium',
        riskTolerance: 'medium',
        includePredictions: true,
        includeHistoricalData: true,
        floodWeights: {
            floodType: 40,
            landUse: 30,
            slope: 20,
            proximityToWater: 10
        },
        landslideWeights: {
            landslideSeverity: 40,
            slope: 30,
            landUse: 25,
            proximityToWater: 5
        }
    });

    // Handle settings updates from the advanced bar
    const handleUpdateSettings = (newSettings) => {

        alert("Updating risk analysis with new settings has to be implemented :)");
    };

    useEffect(() => {
        if (!param) {
            // If no param is provided, default to basic and update URL
            navigate('/assess/basic', { replace: true });
        }
    }, [param, navigate]);


    //Create a copy of gauge data for local use
    const [gauges, setGauges] = useState(defaultGauges.map(obj => Object.assign({}, obj)));


    const onAssessmentResult = (res) => {
        const newGauges = gauges.map(g => Object.assign({}, g));

        newGauges[0].value = res.results.floodRisk;
        newGauges[1].value = res.results.landSlideRisk;
        newGauges[2].value = res.results.earthQuakeRisk;

        setIsLoadingAssessment(false);
        setGauges(newGauges);

        setFloodDetails(res.details.flood);
        setLandSlideDetails(res.details.landSlide);
        setEarthQuakeDetails(res.details.earthQuake);
    };


    const assessLandLot = async (feature) => {
        setIsLoadingAssessment(true);
        await sleep(500); //Simulate some delay so the loading bar doesn't disappear instantly

        const req = await fetch(`http://${window.location.hostname}:3001/map/assess`, {
            method: "POST",
            body: JSON.stringify({bounds: feature.geometry.coordinates}),
            headers: { "Content-Type": "application/json" }
        });

        const result = await req.json();

        if(req.status == 200){
            assessedFeature.current = feature;
            assessementResults.current = result;
            onAssessmentResult(result);
        }
        else {
            alert(`Assessment error: ${result.message}`);
        };
    };


    const onLandLotSelected = (feature) => {
    
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


    //Save current assessment report
    const saveReport = async () => {
        const approxAddress = await getFeatureAddress(assessedFeature.current);
        const info = await saveLot(approxAddress, "Save Report"); //Re-purpose save lot dialog as same info is needed (address, save name)

        if(info == null){
            return; //Canceled
        };

        if((info.address.length == 0) || (info.name.length == 0)){
            return alert("Invalid input!");
        };


        const req = await fetch(`http://${window.location.hostname}:3001/users/saveReport`, {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                "Authorization": context.token,
            },
            body: JSON.stringify({
                name: info.name,
                address: info.address,
                lotId: assessedFeature.current.properties.OBJECTID,
                results: assessementResults.current.results,
            }),
        });

        const res = await req.json();

        if(req.status == 200){
            if(await confirm(`Report "${info.name}" saved successfully\n Do you want to view it?`)){
                navigate(`/result-details/${res.id}`);
            };
        }
        else {
            alert(`Error saving report: ${res.message}`);
        };
    };


    return (
        <styles.assess.Container style={{ border: "none" }}>
            <styles.search.SearchBarWrapper>
                <SearchBar
                    placeholder="Search locations..."
                    onSearch={handleSearch}
                />
            </styles.search.SearchBarWrapper>
            <styles.assess.MapWrapper $isFullScreen={param === "advanced" ? isAdvancedFullScreen : isFullScreen}>
                <Map
                    searchTerm={searchTerm}
                    assessLandLot={assessLandLot}
                    initInfo={initInfo}
                    onLandLotSelected={onLandLotSelected}
                    getHighlightData={getHighlightData}
                />
            </styles.assess.MapWrapper>

            <styles.assess.ResultBarWrapper>

                    <styles.assess.ResultBarInner $isFullScreen={isFullScreen}>
                        <ResultBar
                            gauges={gauges}
                            isFullScreen={isFullScreen}
                            onToggleFullScreen={() => setIsFullScreen(!isFullScreen)}
                            isLoading={isLoadingAssessment}
                        >
                            {/* Additional content for the expanded view */}
                            { assessementResults.current && 
                                <div style={{ padding: '20px 0' }}>
                                    <h3 style={{ marginBottom: '15px', fontSize: '1.2rem', fontWeight: '600' }}>Detailed Risk Analysis</h3>

                                    <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                                        <div style={{ flex: '1', minWidth: '250px' }}>
                                            <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Flood Risk Assessment</h4>
                                            <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                                { floodDetails || "N/A" }
                                            </p>
                                        </div>

                                        <div style={{ flex: '1', minWidth: '250px' }}>
                                            <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Mudslide Risk Assessment</h4>
                                            <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                                { landSlideDetails || "N/A" }
                                            </p>
                                        </div>

                                        <div style={{ flex: '1', minWidth: '250px' }}>
                                            <h4 style={{ fontSize: '1rem', marginBottom: '10px', fontWeight: '500' }}>Earthquake Risk Assessment</h4>
                                            <p style={{ lineHeight: '1.5', color: '#4a5568' }}>
                                                { earthQuakeDetails || "N/A" }
                                            </p>
                                        </div>
                                    </div>

                                    {context.user && (
                                        <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
                                            <styles.common.Button onClick={saveReport}>Save Report</styles.common.Button>
                                        </div>
                                    )}
                                </div>
                            }
                        </ResultBar>
                    </styles.assess.ResultBarInner>
                
            </styles.assess.ResultBarWrapper>
        </styles.assess.Container>
    );
}

export default Assess;