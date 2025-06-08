import { useState, useEffect, useContext } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';
import AdvancedBar from '../Components/AdvancedBar';
import SearchBar from "./SearchBar";
import { UserContext } from '../Contexts/UserContext.js';

function Assess() {
    const { user } = useContext(UserContext);
    const [isFullScreen, setIsFullScreen] = useState(false);
    const [isAdvancedFullScreen, setIsAdvancedFullScreen] = useState(false);
    const [searchTerm, setSearchTerm] = useState(null);
    const [isLoadingAssessment, setIsLoadingAssessment] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();
    const { param } = useParams();



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


    // Sample data for the gauges - environmental risk indicators
    const [gauges, setGauges] = useState([
        {
            value: null,
            label: "Flood Risk",
            fillGradient: "#e0f2fe 0deg, #7dd3fc 90deg, #38bdf8 180deg, #0284c7 270deg, #0c4a6e 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568",
            trackColor: "#cacaca",
        },
        {
            value: null,
            label: "Landslide Risk",
            fillGradient: "#F9F5EB 0deg, #E3D5CA 90deg, #D5A021 180deg, #8B5A2B 270deg, #4A3728 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568",
            trackColor: "#cacaca",
        },
        {
            value: null,
            label: "Earthquake Risk",
            fillGradient: "#2f855a 0deg, #48bb78 144deg, #f6e05e 216deg, #ed8936 270deg, #c53030 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568",
            trackColor: "#cacaca",
        }
    ]);


    const onAssessmentBegin = () => {
        setIsLoadingAssessment(true);
    };


    const onAssessmentResult = (result) => {
        const newGauges = gauges.map(g => Object.assign({}, g));

        newGauges[0].value = result.floodRisk;
        newGauges[1].value = result.landSlideRisk;
        newGauges[2].value = null;

        setIsLoadingAssessment(false);
        setGauges(newGauges);
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
                <Map searchTerm={searchTerm} onAssessment={onAssessmentResult} onAssessmentBegin={onAssessmentBegin}/>
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