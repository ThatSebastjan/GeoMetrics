import { useState } from 'react';
import styles from '../styles';
import Gauge from './Gauge';

import { Ring } from 'ldrs/react'
import 'ldrs/react/Ring.css'



const ResultBar = ({
                       title = "Assessment Results",
                       gauges = [],
                       isFullScreen = false,
                       onToggleFullScreen,
                       children,
                       isLoading,
                   }) => {



    return (
        <styles.results.Container $isFullScreen={isFullScreen}>
            <styles.results.ExpandButtonWrapper>
                <styles.results.ExpandButton
                    onClick={onToggleFullScreen}
                    aria-label={isFullScreen ? "Collapse" : "Expand"}
                >
                    {isFullScreen ? "⇣" : "⇡"}
                </styles.results.ExpandButton>
            </styles.results.ExpandButtonWrapper>

            <styles.results.GaugeGrid $isFullScreen={isFullScreen}>
                { isLoading ?  
                
                //Div with static height to match the height of gauges...
                (<div style={{height: "156px", display: "flex", alignItems: "center"}}>
                    <Ring
                        size="50"
                        stroke="5"
                        bgOpacity="0"
                        speed="2"
                        color="black" 
                    />
                </div>) :

                gauges.map((gauge, index) => (
                    <styles.results.GaugeItem key={index} $isFullScreen={isFullScreen}>
                        <Gauge
                            value={gauge.value}
                            min={gauge.min || 0}
                            max={gauge.max || 100}
                            size={isFullScreen ? 180 : 120}
                            label={gauge.label}
                            fillColor={gauge.fillColor}
                            trackColor={gauge.trackColor}
                            innerColor={gauge.innerColor}
                            valueColor={gauge.valueColor}
                            labelColor={gauge.labelColor}
                            fillGradient={gauge.fillGradient}
                            valueFormatter={gauge.valueFormatter}
                        />
                    </styles.results.GaugeItem>
                ))}
            </styles.results.GaugeGrid>


            <styles.results.ContentSection $isFullScreen={isFullScreen}>
                {children}
            </styles.results.ContentSection>
        </styles.results.Container>
    );
};

export default ResultBar;