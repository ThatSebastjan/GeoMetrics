import { useState, useEffect, useContext } from "react";
import styles from "../styles";
import icons from "./Icons";
import { useNavigate } from "react-router-dom";
import Minimap from "./Minimap";
import { UserContext } from "../Contexts/UserContext";
import { PopupContext } from "../Contexts/CustomPopups";


function Results() {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const context = useContext(UserContext);
    const { alert } = useContext(PopupContext);


    useEffect(() => {

        const getResults = async () => {

            const req = await fetch(`http://${window.location.hostname}:3001/users/getReports`, {
                headers: { "Authorization": context.token },
            });

            const resp = await req.json();

            if(req.status != 200){
                return alert(`Error: ${resp.message}`);
            };

            const data = resp.map((r) => {
                return {
                    id: r._id,
                    lotId: r.lotId,
                    title: r.title,
                    address: r.address,
                    scores: {
                        floodRisk: +r.results.floodRisk.toFixed(2),
                        landslideRisk: +r.results.landSlideRisk.toFixed(2),
                        earthquakeRisk: +r.results.earthQuakeRisk.toFixed(2)
                    },
                    coordinates: { lng: r.coordinates[0], lat: r.coordinates[1] },
                    summary: r.summary,
                };
            });

            setResults(data);
            setLoading(false);
        };

        getResults();
    }, []);



    const renderRiskIndicators = (scores) => {
        return (
            <div style={{ display: "flex", gap: "8px", marginTop: "12px" }}>
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
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                backgroundColor: "#f7f7f7",
                padding: "6px 10px",
                borderRadius: "4px",
                flex: 1
            }}>
                <span style={{ fontSize: "0.8rem", color: styles.colors.textMedium }}>{label}</span>
                <span style={{
                    fontWeight: "bold",
                    color: getColor(value),
                    fontSize: "1.1rem"
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
                                <Minimap
                                    lat={result.coordinates.lat}
                                    lng={result.coordinates.lng}
                                    zoom={16}/>
                            </styles.results.MapContainer>

                            <styles.results.ResultSummary>
                                <styles.results.SummaryTitle>Risk Assessment</styles.results.SummaryTitle>
                                {renderRiskIndicators(result.scores)}
                                <p style={{ marginTop: "12px" }}>{result.summary}</p>
                            </styles.results.ResultSummary>

                            <styles.results.ResultActions>
                                <styles.common.Button onClick={() => navigate(`/result-details/${result.id}`)} >View Details</styles.common.Button> {/*for testing*/}
                            </styles.results.ResultActions>
                        </styles.results.ResultCard>
                    ))}
                </styles.results.ResultsGrid>
            </styles.settings.Container>
        </div>
    );
}

export default Results;