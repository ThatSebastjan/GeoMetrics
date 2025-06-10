import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import styles from '../styles';
import icons from './Icons';
import html2pdf from 'html2pdf.js';
import Minimap from "./Minimap";

function ResultDetails() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(true);
    const exportTemplateRef = useRef(null);

    useEffect(() => {
        const mockResult = {
            id: parseInt(id) || 1,
            title: `Property Assessment #${id || 1}`,
            address: "123 Main Street, Cityville",
            date: new Date(2024, 4, 21, 12,30,60,23),
            scores: {
                floodRisk: 20,
                landslideRisk: 53,
                earthquakeRisk: 78
            },
            coordinates: { lat: 40.7128, lng: -74.0060 },
            summary: "AI generated summary shall be generated here :)",
            details: {
                flood: "Low risk area with a 10% probability of flooding in the next 30 years. The property sits 15m above the nearest flood plain and has adequate drainage infrastructure.",
                landslide: "Moderate risk with a 65% probability of soil instability issues. The property is on a 12° slope with historical instances of land movement within 500m of the location.",
                earthquake: "High risk area with 92% probability of significant seismic activity in the next 50 years. Location is within 5km of a major fault line with historical 6.5+ magnitude events."
            },
            propertyDetails: {
                size: "460 km²",
                elevation: "125m above sea level",
                proximityToWater: "500m to nearest stream",
                proximityToFirstResponders: "5km from nearest Fire station",
            }
        };

        setResult(mockResult);
        setLoading(false);
    }, [id]);

    const calculateSafetyScore = (riskScore) => {
        return Math.round(riskScore / 10);
    };


    const exportAsPDF = () => {
        if (!exportTemplateRef.current) {
            console.error("Export template reference not available");
            return;
        }

        // Create loading indicator
        const loadingIndicator = document.createElement('div');
        loadingIndicator.style.position = 'fixed';
        loadingIndicator.style.top = '50%';
        loadingIndicator.style.left = '50%';
        loadingIndicator.style.transform = 'translate(-50%, -50%)';
        loadingIndicator.style.padding = '20px';
        loadingIndicator.style.background = 'rgba(0,0,0,0.7)';
        loadingIndicator.style.color = 'white';
        loadingIndicator.style.borderRadius = '5px';
        loadingIndicator.style.zIndex = '9999';
        loadingIndicator.textContent = 'Generating PDF...';
        document.body.appendChild(loadingIndicator);

        const element = exportTemplateRef.current;
        const filename = `Property-Assessment-${result.title.replace(/\s+/g, '-')}.pdf`;

        // Clone the template to avoid modifying the original DOM
        const clonedElement = element.cloneNode(true);

        // Create a container that's absolutely positioned off-screen
        const container = document.createElement('div');
        container.style.position = 'absolute';
        container.style.left = '-9999px';
        container.style.top = '0';
        container.style.width = '210mm';
        container.style.backgroundColor = '#ffffff';
        container.style.zIndex = '-9999';

        // Make the cloned element visible but in our off-screen container
        clonedElement.style.display = 'block';
        clonedElement.style.width = '100%';
        clonedElement.style.visibility = 'visible';

        // Add to DOM temporarily
        container.appendChild(clonedElement);
        document.body.appendChild(container);

        // Let the browser render the cloned element
        setTimeout(() => {
            const opt = {
                margin: [15, 15, 15, 15],
                filename: filename,
                image: { type: 'jpeg', quality: 0.98 },
                html2canvas: {
                    scale: 2,
                    useCORS: true,
                    letterRendering: true,
                    logging: false,
                },
                jsPDF: {
                    unit: 'mm',
                    format: 'a4',
                    orientation: 'portrait',
                    compress: true
                }
            };

            html2pdf()
                .from(clonedElement)
                .set(opt)
                .save()
                .then(() => {
                    // Remove the cloned element
                    document.body.removeChild(container);
                    document.body.removeChild(loadingIndicator);
                    console.log("PDF exported successfully");
                })
                .catch(err => {
                    document.body.removeChild(container);
                    document.body.removeChild(loadingIndicator);
                    console.error("PDF export failed", err);
                });
        }, 100);
    };

    if (loading) {
        return <styles.common.Loading>Loading assessment details...</styles.common.Loading>;
    }

    return (
        <div>
            {/* Main UI for display */}
            <div>
                <styles.settings.ContainerHero>
                    <styles.results.BackButton onClick={() => navigate('/results')}>
                        <span style={{ display: 'flex', alignItems: 'center' }}>
                           <icons.BackIcon />
                        </span>
                        Back to Results
                    </styles.results.BackButton>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%' }}>
                        <styles.common.HeroTitle>{result.title}</styles.common.HeroTitle>
                    </div>
                    <p>{result.address} • Assessed on {result.date.toDateString()}</p>
                </styles.settings.ContainerHero>

                <styles.settings.Container>
                    <styles.results.DetailGrid>
                        {/* Main info section */}
                        <styles.results.DetailMainSection>
                            <styles.results.DetailCard>
                                <styles.results.MapDetailContainer>
                                    <Minimap
                                        lat={result.coordinates.lat}
                                        lng={result.coordinates.lng}
                                        zoom={16}/>
                                </styles.results.MapDetailContainer>
                                <styles.results.DetailCardContent>
                                    <styles.results.AddressDetail>
                                        <span style={{ display: 'flex', alignItems: 'center', marginRight: '8px' }}>
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                                <circle cx="12" cy="10" r="3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                            </svg>
                                        </span>
                                        {result.address}
                                    </styles.results.AddressDetail>
                                </styles.results.DetailCardContent>
                            </styles.results.DetailCard>

                            <styles.results.DetailCard>
                                <styles.results.DetailCardHeader>
                                    <styles.results.DetailCardTitle>Risk Assessment</styles.results.DetailCardTitle>
                                </styles.results.DetailCardHeader>
                                <styles.results.DetailCardContent>
                                    <styles.results.AssessmentSummary>
                                        <p>{result.summary}</p>
                                    </styles.results.AssessmentSummary>

                                    <styles.results.RiskMetricsGrid>
                                        <styles.results.RiskMetricCard risk={result.scores.floodRisk}>
                                            <styles.results.RiskMetricTitle>Flood Risk</styles.results.RiskMetricTitle>
                                            <styles.results.RiskMetricValue style={{
                                                color: result.scores.floodRisk < 30 ? styles.colors.success :
                                                    result.scores.floodRisk < 70 ? styles.colors.warning : styles.colors.danger
                                            }}>
                                                {result.scores.floodRisk}%
                                            </styles.results.RiskMetricValue>
                                            <styles.results.RiskMetricBar risk={result.scores.floodRisk} />
                                            <div style={{ marginTop: '12px', fontSize: '0.9rem' }}>
                                                <styles.results.DetailSection>
                                                    <p>{result.details.flood}</p>
                                                </styles.results.DetailSection>
                                            </div>
                                        </styles.results.RiskMetricCard>

                                        <styles.results.RiskMetricCard risk={result.scores.landslideRisk}>
                                            <styles.results.RiskMetricTitle>Landslide Risk</styles.results.RiskMetricTitle>
                                            <styles.results.RiskMetricValue style={{
                                                color: result.scores.landslideRisk < 30 ? styles.colors.success :
                                                    result.scores.landslideRisk < 70 ? styles.colors.warning : styles.colors.danger
                                            }}>
                                                {result.scores.landslideRisk}%
                                            </styles.results.RiskMetricValue>
                                            <styles.results.RiskMetricBar risk={result.scores.landslideRisk} />
                                            <div style={{ marginTop: '12px', fontSize: '0.9rem' }}>
                                                <styles.results.DetailSection>
                                                    <p>{result.details.landslide}</p>
                                                </styles.results.DetailSection>
                                            </div>
                                        </styles.results.RiskMetricCard>

                                        <styles.results.RiskMetricCard risk={result.scores.earthquakeRisk}>
                                            <styles.results.RiskMetricTitle>Earthquake Risk</styles.results.RiskMetricTitle>
                                            <styles.results.RiskMetricValue style={{
                                                color: result.scores.earthquakeRisk < 30 ? styles.colors.success :
                                                    result.scores.earthquakeRisk < 70 ? styles.colors.warning : styles.colors.danger
                                            }}>
                                                {result.scores.earthquakeRisk}%
                                            </styles.results.RiskMetricValue>
                                            <styles.results.RiskMetricBar risk={result.scores.earthquakeRisk} />
                                            <div style={{ marginTop: '12px', fontSize: '0.9rem' }}>
                                                <styles.results.DetailSection>
                                                    <p>{result.details.earthquake}</p>
                                                </styles.results.DetailSection>
                                            </div>
                                        </styles.results.RiskMetricCard>
                                    </styles.results.RiskMetricsGrid>
                                </styles.results.DetailCardContent>
                            </styles.results.DetailCard>
                        </styles.results.DetailMainSection>

                        {/* Sidebar */}
                        <styles.results.DetailSidebar>
                            <styles.results.DetailCard>
                                <styles.results.DetailCardHeader>
                                    <styles.results.DetailCardTitle>Property Details</styles.results.DetailCardTitle>
                                </styles.results.DetailCardHeader>
                                <styles.results.DetailCardContent>
                                    <styles.results.PropertyInfoList>
                                        {Object.entries(result.propertyDetails).map(([key, value]) => (
                                            <styles.results.PropertyInfoItem key={key}>
                                                <styles.results.PropertyInfoLabel>
                                                    {key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1')}
                                                </styles.results.PropertyInfoLabel>
                                                <styles.results.PropertyInfoValue>{value}</styles.results.PropertyInfoValue>
                                            </styles.results.PropertyInfoItem>
                                        ))}
                                    </styles.results.PropertyInfoList>
                                </styles.results.DetailCardContent>
                            </styles.results.DetailCard>

                            <styles.results.DetailCard>
                                <styles.results.DetailCardHeader>
                                    <styles.results.DetailCardTitle>Overall Safety Score</styles.results.DetailCardTitle>
                                </styles.results.DetailCardHeader>
                                <styles.results.DetailCardContent>
                                    <div style={{ textAlign: 'center', padding: '20px 0' }}>
                                        {(() => {
                                            const score = calculateSafetyScore(
                                                (result.scores.floodRisk + result.scores.landslideRisk + result.scores.earthquakeRisk) / 3
                                            );

                                            const scoreColor = score < 3 ? styles.colors.success :
                                                score < 7 ? styles.colors.warning : styles.colors.danger;

                                            return (
                                                <div style={{
                                                    fontSize: '3rem',
                                                    fontWeight: 'bold',
                                                    color: scoreColor
                                                }}>
                                                    {score}
                                                </div>
                                            );
                                        })()}
                                        <div style={{ marginTop: '10px', color: styles.colors.textMedium }}>
                                            Safety Rating
                                        </div>
                                    </div>
                                </styles.results.DetailCardContent>
                            </styles.results.DetailCard>
                            <styles.results.ActionButtonsContainer>
                                <styles.common.Button>Share Assessment</styles.common.Button>
                                <styles.common.Button onClick={exportAsPDF}>Export as PDF</styles.common.Button>
                            </styles.results.ActionButtonsContainer>
                        </styles.results.DetailSidebar>
                    </styles.results.DetailGrid>
                </styles.settings.Container>
            </div>

            {/* Hidden export template - only used for PDF generation */}
            <div
                ref={exportTemplateRef}
                style={{
                    display: 'none',
                    visibility: 'hidden',
                    backgroundColor: 'white',
                    fontFamily: 'Arial, sans-serif',
                    boxSizing: 'border-box',
                    padding: '10mm 20mm',
                    width: '210mm'
                }}
            >
                <div style={{ marginTop: '0', marginBottom: '32px', textAlign: 'center' }}> {/* Centered heading */}
                    <h1 style={{ fontSize: '22px', color: '#333', margin: '0 0 6px 0' }}>{result.title}</h1>
                    <p style={{ color: '#666', margin: '0' }}>{result.address} • Assessed on {result.date.toDateString()}</p>
                </div>

                <div style={{ marginBottom: '24px' }}> {/* Reduced spacing */}
                    <h2 style={{ fontSize: '16px', color: '#333', borderBottom: '1px solid #ddd', paddingBottom: '4px', margin: '0 0 6px 0' }}>
                        Property Overview
                    </h2>
                    <p style={{ margin: '0', lineHeight: '1.4' }}>{result.summary}</p>
                </div>

                <div style={{ marginBottom: '24px' }}>
                    <h2 style={{ fontSize: '16px', color: '#333', borderBottom: '1px solid #ddd', paddingBottom: '4px', margin: '0 0 8px 0' }}>
                        Risk Assessment
                    </h2>

                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <tbody>
                        <tr>
                            <td style={{ padding: '8px', border: '1px solid #ddd', width: '33%', verticalAlign: 'top' }}>
                                <h3 style={{ fontSize: '14px', margin: '0 0 6px 0' }}>Flood Risk</h3>
                                <p style={{
                                    fontSize: '18px',
                                    fontWeight: 'bold',
                                    margin: '0 0 6px 0',
                                    color: result.scores.floodRisk < 30 ? '#4caf50' :
                                        result.scores.floodRisk < 70 ? '#ff9800' : '#f44336'
                                }}>
                                    {result.scores.floodRisk}%
                                </p>
                                <p style={{ margin: '0', lineHeight: '1.3', fontSize: '12px' }}>{result.details.flood}</p>
                            </td>

                            <td style={{ padding: '8px', border: '1px solid #ddd', width: '33%', verticalAlign: 'top' }}>
                                <h3 style={{ fontSize: '14px', margin: '0 0 6px 0' }}>Landslide Risk</h3>
                                <p style={{
                                    fontSize: '18px',
                                    fontWeight: 'bold',
                                    margin: '0 0 6px 0',
                                    color: result.scores.landslideRisk < 30 ? '#4caf50' :
                                        result.scores.landslideRisk < 70 ? '#ff9800' : '#f44336'
                                }}>
                                    {result.scores.landslideRisk}%
                                </p>
                                <p style={{ margin: '0', lineHeight: '1.3', fontSize: '12px' }}>{result.details.landslide}</p>
                            </td>

                            <td style={{ padding: '8px', border: '1px solid #ddd', width: '33%', verticalAlign: 'top' }}>
                                <h3 style={{ fontSize: '14px', margin: '0 0 6px 0' }}>Earthquake Risk</h3>
                                <p style={{
                                    fontSize: '18px',
                                    fontWeight: 'bold',
                                    margin: '0 0 6px 0',
                                    color: result.scores.earthquakeRisk < 30 ? '#4caf50' :
                                        result.scores.earthquakeRisk < 70 ? '#ff9800' : '#f44336'
                                }}>
                                    {result.scores.earthquakeRisk}%
                                </p>
                                <p style={{ margin: '0', lineHeight: '1.3', fontSize: '12px' }}>{result.details.earthquake}</p>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <div style={{ marginBottom: '24px' }}>
                    <h2 style={{ fontSize: '16px', color: '#333', borderBottom: '1px solid #ddd', paddingBottom: '4px', margin: '0 0 8px 0' }}>
                        Property Details
                    </h2>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <tbody>
                        {Object.entries(result.propertyDetails).map(([key, value], index) => (
                            <tr key={key} style={{ borderBottom: index < Object.entries(result.propertyDetails).length - 1 ? '1px solid #eee' : 'none' }}>
                                <td style={{ padding: '6px 0', fontWeight: 'bold', width: '40%' }}>
                                    {key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1')}
                                </td>
                                <td style={{ padding: '6px 0' }}>{value}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <div style={{ marginBottom: '24x', textAlign: 'center' }}>
                    <h2 style={{ fontSize: '16px', color: '#333', borderBottom: '1px solid #ddd', paddingBottom: '4px', margin: '0 0 8px 0' }}>
                        Overall Safety Rating
                    </h2>
                    {(() => {
                        const score = calculateSafetyScore(
                            (result.scores.floodRisk + result.scores.landslideRisk + result.scores.earthquakeRisk) / 3
                        );

                        const scoreColor = score < 3 ? '#4caf50' : score < 7 ? '#ff9800' : '#f44336';

                        return (
                            <div style={{
                                fontSize: '36px',
                                fontWeight: 'bold',
                                color: scoreColor,
                                margin: '10px 0'
                            }}>
                                {score}
                            </div>
                        );
                    })()}
                    <p style={{ color: '#666', margin: '0', fontSize: '12px' }}>
                        Safety Rating (1-10 scale, lower is better)
                    </p>
                </div>

                <div style={{ marginTop: '20px', borderTop: '1px solid #ddd', paddingTop: '8px', fontSize: '10px', color: '#999', textAlign: 'center' }}>
                    <p style={{ margin: '0' }}>Generated on {new Date().toLocaleDateString()} by GeoMetrics</p>
                </div>
            </div>
        </div>
    );
}

export default ResultDetails;