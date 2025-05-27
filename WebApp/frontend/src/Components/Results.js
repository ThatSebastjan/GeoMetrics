import { useState, useEffect } from 'react';
import styles from '../styles';
import icons from './Icons';
import {useNavigate} from "react-router-dom";

function Results() {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    // Mock data for demonstration
    useEffect(() => {
            setResults([
                {
                    id: 1,
                    title: "Property Assessment #1",
                    address: "122 Main Street, Cityville",
                    scores: {
                        floodRisk: 13,
                        landslideRisk: 78,
                        earthquakeRisk: 92
                    },
                    coordinates: { lat: 40.7128, lng: -74.0060 },
                    summary: "This property shows moderate overall risk with excellent flood safety but high earthquake vulnerability. Located in a seismic activity zone within 5km of a major fault line. The steep 12° slope presents moderate landslide concerns."
                },
                {
                    id: 2,
                    title: "Property Assessment #2",
                    address: "456 Oak Avenue, Townsburg",
                    scores: {
                        floodRisk: 67,
                        landslideRisk: 24,
                        earthquakeRisk: 45
                    },
                    coordinates: { lat: 34.0522, lng: -118.2437 },
                    summary: "Medium risk property with significant flood concerns but good stability metrics. The property sits on relatively flat terrain with adequate soil composition, though it is within a floodplain with historical flooding events."
                },
                {
                    id: 3,
                    title: "Property Assessment #3",
                    address: "789 Pine Road, Villageton",
                    scores: {
                        floodRisk: 33,
                        landslideRisk: 18,
                        earthquakeRisk: 21
                    },
                    coordinates: { lat: 41.8781, lng: -87.6298 },
                    summary: "Low risk property with excellent stability metrics across all assessment categories. The property is elevated 25m above the nearest flood plain with stable bedrock and minimal historical seismic activity."
                },
                {
                    id: 4,
                    title: "Property Assessment #4",
                    address: "789 Pine Road, Villageton",
                    scores: {
                        floodRisk: 73,
                        landslideRisk: 13,
                        earthquakeRisk: 21
                    },
                    coordinates: { lat: 41.8781, lng: -87.6298 },
                    summary: "Low risk property with excellent stability metrics across all assessment categories. The property is elevated 25m above the nearest flood plain with stable bedrock and minimal historical seismic activity."
                }
            ]);
            setLoading(false);
    }, []);

    const renderMapPlaceholder = (coordinates) => {
        // In a real implementation, this would be a proper map component
        return (
            <div style={{
                backgroundColor: '#b8ffab',
                width: '100%',
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
            }}>
                <div>Map: {coordinates.lat}, {coordinates.lng}</div>
            </div>
        );
    };

    const renderRiskIndicators = (scores) => {
        return (
            <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                <RiskIndicator label="Flood" value={scores.floodRisk} />
                <RiskIndicator label="Landslide" value={scores.landslideRisk} />
                <RiskIndicator label="Earthquake" value={scores.earthquakeRisk} />
            </div>
        );
    };

    const RiskIndicator = ({ label, value }) => {
        const getColor = (value) => {
            if (value < 30) return styles.colors.success; // Green for low risk
            if (value < 70) return styles.colors.warning; // Orange for medium risk
            return styles.colors.danger; // Red for high risk
        };

        return (
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                backgroundColor: '#f7f7f7',
                padding: '6px 10px',
                borderRadius: '4px',
                flex: 1
            }}>
                <span style={{ fontSize: '0.8rem', color: styles.colors.textMedium }}>{label}</span>
                <span style={{
                    fontWeight: 'bold',
                    color: getColor(value),
                    fontSize: '1.1rem'
                }}>
                    {value}
                </span>
            </div>
        );
    };

    if (loading) {
        return <styles.common.Loading>Loading results...</styles.common.Loading>;
    }

    return (
        <div>
            <styles.settings.ContainerHero>
                <styles.common.HeroTitle>Assessment Results</styles.common.HeroTitle>
                <p>Review and compare your property assessments</p>
            </styles.settings.ContainerHero>

            <styles.settings.Container>
                <styles.results.ResultsGrid>
                    {results.map(result => (
                        <styles.results.ResultCard key={result.id}>
                            <styles.results.ResultCardHeader>
                                <styles.results.ResultTitle>{result.title}</styles.results.ResultTitle>
                            </styles.results.ResultCardHeader>

                            <styles.results.ResultAddress>
                                <icons.MapIcon /> {result.address}
                            </styles.results.ResultAddress>

                            <styles.results.MapContainer>
                                {renderMapPlaceholder(result.coordinates)}
                            </styles.results.MapContainer>

                            <styles.results.ResultSummary>
                                <styles.results.SummaryTitle>Risk Assessment</styles.results.SummaryTitle>
                                {renderRiskIndicators(result.scores)}
                                <p style={{ marginTop: '12px' }}>{result.summary}</p>
                            </styles.results.ResultSummary>

                            <styles.results.ResultActions>
                                {/*<styles.common.Button onClick={() => window.location.href = `/result-details/${result.id}`}>View Details</styles.common.Button> */} {/*after we implement backend */}
                                <styles.common.Button onClick={() => navigate("/result-details")} >View Details</styles.common.Button> {/*for testing*/}
                            </styles.results.ResultActions>
                        </styles.results.ResultCard>
                    ))}
                </styles.results.ResultsGrid>
            </styles.settings.Container>
        </div>
    );
}

export default Results;