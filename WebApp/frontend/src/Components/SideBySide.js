import { useState, useEffect } from 'react';
import styles from '../styles';
import Map from '../Components/Map.js';
import ResultBar from '../Components/ResultBar';

function SideBySide() {
    const [user, setUser] = useState(null);
    const [leftFullScreen, setLeftFullScreen] = useState(false);
    const [rightFullScreen, setRightFullScreen] = useState(false);

    // Sample data for the gauges - environmental risk indicators
    const gaugesLeft = [
        {
            value: 79,
            label: "Flood Risk",
            fillGradient: "#e0f2fe 0deg, #7dd3fc 90deg, #38bdf8 180deg, #0284c7 270deg, #0c4a6e 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        },
        {
            value: 36,
            label: "Mudslide Risk",
            fillGradient: "#F9F5EB 0deg, #E3D5CA 90deg, #D5A021 180deg, #8B5A2B 270deg, #4A3728 360deg",
            innerColor: "#f8fafc",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        },
        {
            value: 62,
            label: "Earthquake Risk",
            fillGradient: "#2f855a 0deg, #48bb78 144deg, #f6e05e 216deg, #ed8936 270deg, #c53030 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        }
    ];

    const gaugesRight = [
        {
            value: 55,
            label: "Flood Risk",
            fillGradient: "#e0f2fe 0deg, #7dd3fc 90deg, #38bdf8 180deg, #0284c7 270deg, #0c4a6e 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        },
        {
            value: 78,
            label: "Mudslide Risk",
            fillGradient: "#F9F5EB 0deg, #E3D5CA 90deg, #D5A021 180deg, #8B5A2B 270deg, #4A3728 360deg",
            innerColor: "#f8fafc",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        },
        {
            value: 92,
            label: "Earthquake Risk",
            fillGradient: "#2f855a 0deg, #48bb78 144deg, #f6e05e 216deg, #ed8936 270deg, #c53030 360deg",
            innerColor: "#f8f9fa",
            valueColor: "#2d3748",
            labelColor: "#4a5568"
        }
    ];

    useEffect(() => {
        // Get user data from localStorage
        const userData = localStorage.getItem('user');
        if (userData) {
            setUser(JSON.parse(userData));
        }
    }, []);

    return (
        <styles.assess.Container>
            <styles.assess.MapWrapper $isFullScreen={leftFullScreen || rightFullScreen}>
                <Map />
            </styles.assess.MapWrapper>

            <styles.assess.SideBySideResults>
                {/* Left Result Bar */}
                <div>
                    <styles.assess.ResultBarInner $isFullScreen={leftFullScreen}>
                        <ResultBar
                            gauges={gaugesLeft}
                            isFullScreen={leftFullScreen}
                            onToggleFullScreen={() => {
                                setLeftFullScreen(!leftFullScreen);
                            }}
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
                            onToggleFullScreen={() => {
                                setRightFullScreen(!rightFullScreen);
                            }}
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