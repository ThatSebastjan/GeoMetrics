import { useState } from 'react';
import styles from '../styles';
import Gauge from './Gauge';

const ResultBar = ({
                       title = "Assessment Results",
                       gauges = [],
                       isFullScreen = false,
                       onToggleFullScreen,
                       children // Add children prop to accept additional content
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
                {gauges.map((gauge, index) => (
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