import { useState, useEffect, useContext } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styles from '../styles';
import Map from '../Components/Map.js';
import { UserContext } from '../Contexts/UserContext.js';
import icons from "./Icons";

function RiskLens() {
    const navigate = useNavigate();
    const { risk } = useParams();
    const [mode, setMode] = useState(risk || 'flood');
    const { user } = useContext(UserContext);

    useEffect(() => {
        if (!risk) {
            // If no risk parameter is provided, default to flood and update URL
            navigate('/risk-lens/flood', { replace: true });
        } else {
            setMode(risk);
        }
    }, [risk, navigate]);

    const handleModeChange = (newMode) => {
        navigate(`/risk-lens/${newMode}`, { replace: true });
    };

    return (
        <styles.assess.Container style={{ border: "none" }}>
            <styles.assess.MapWrapper>
                <Map risk={mode}/>
                <styles.common.RiskPill>
                    <styles.common.RiskButton
                        onClick={() => handleModeChange('flood')}
                        $isActive={mode === 'flood'}
                        $customColor="#0066cc"
                        title="Flood Risk"
                    >
                        <styles.layout.IconWrapper><icons.DropIcon/></styles.layout.IconWrapper>

                    </styles.common.RiskButton>

                    <styles.common.RiskButton
                        onClick={() => handleModeChange('landslide')}
                        $isActive={mode === 'landslide'}
                        $customColor="#9f6e28"
                        title="Landslide Risk"
                    >
                        <styles.layout.IconWrapper><icons.LandslideIcon/></styles.layout.IconWrapper>
                    </styles.common.RiskButton>

                    <styles.common.RiskButton
                        onClick={() => handleModeChange('earthquake')}
                        $isActive={mode === 'earthquake'}
                        $customColor="#e8541e"
                        title="Earthquake Risk"
                    >
                        <styles.layout.IconWrapper><icons.WaveIcon/></styles.layout.IconWrapper>
                    </styles.common.RiskButton>
                </styles.common.RiskPill>
            </styles.assess.MapWrapper>
        </styles.assess.Container>
    );
}

export default RiskLens;