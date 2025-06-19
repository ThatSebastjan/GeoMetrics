import { useState, useEffect, useContext, useRef } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';
import AdvancedBar from '../Components/AdvancedBar';
import SearchBar from "./SearchBar";
import { UserContext } from '../Contexts/UserContext.js';

import { getHighlightPoints, sleep, defaultGauges } from '../utility.js';



function Assess() {
    const { user } = useContext(UserContext);
    const [isFullScreen, setIsFullScreen] = useState(false);
    const [isAdvancedFullScreen, setIsAdvancedFullScreen] = useState(false);
    const [searchTerm, setSearchTerm] = useState(null);
    const [isLoadingAssessment, setIsLoadingAssessment] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();
    const { param } = useParams();
    const highlightData = useRef({ id: null, data: null, startIndex: 0 });

    const searchParams = new URLSearchParams(location.search);

    let initInfo = null;

    if(searchParams.has("lng") && searchParams.has("lat") && searchParams.has("id")){
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


    const onAssessmentResult = (result) => {
        const newGauges = gauges.map(g => Object.assign({}, g));

        newGauges[0].value = result.floodRisk;
        newGauges[1].value = result.landSlideRisk;
        newGauges[2].value = result.earthQuakeRisk;

        setIsLoadingAssessment(false);
        setGauges(newGauges);
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
                {param === 'basic' ? (
                    <styles.assess.ResultBarInner $isFullScreen={isFullScreen}>
                        <ResultBar
                            gauges={gauges}
                            isFullScreen={isFullScreen}
                            onToggleFullScreen={() => setIsFullScreen(!isFullScreen)}
                            isLoading={isLoadingAssessment}
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
                                        <styles.common.Button>Save Report</styles.common.Button>
                                        <styles.common.Button $secondary style={{ marginLeft: '10px' }}>
                                            Save Location
                                        </styles.common.Button>
                                    </div>
                                )}
                            </div>
                        </ResultBar>
                    </styles.assess.ResultBarInner>
                ) : (
                    <styles.assess.ResultBarInner $isFullScreen={isAdvancedFullScreen}>
                        <ResultBar
                            gauges={gauges}
                            isFullScreen={isAdvancedFullScreen}
                            onToggleFullScreen={() => setIsAdvancedFullScreen(!isAdvancedFullScreen)}
                            isLoading={isLoadingAssessment}
                        >
                            <AdvancedBar
                                settings={settings}
                                isFullScreen={isAdvancedFullScreen}
                                onToggleFullScreen={() => setIsAdvancedFullScreen(!isAdvancedFullScreen)}
                                onUpdateSettings={handleUpdateSettings}
                            />
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
                                        <styles.common.Button>Save Report</styles.common.Button>
                                        <styles.common.Button $secondary style={{ marginLeft: '10px' }}>
                                            Save Location
                                        </styles.common.Button>
                                    </div>
                                )}
                            </div>
                        </ResultBar>
                    </styles.assess.ResultBarInner>
                )}
            </styles.assess.ResultBarWrapper>
        </styles.assess.Container>
    );
}

export default Assess;