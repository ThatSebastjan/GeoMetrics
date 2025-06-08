import { useState, useEffect } from 'react';
import styles from '../styles';

const Gauge = ({
                   value = 0,
                   min = 0,
                   max = 100,
                   size = 150,
                   label,
                   fillColor,
                   trackColor,
                   innerColor,
                   valueColor,
                   labelColor,
                   fillGradient,
                   valueFormatter = (val) => (val != null) ? `${Math.round(val)}%` : "N/A",
                   animated = true
               }) => {
    const [displayValue, setDisplayValue] = useState(value);

    // Calculate percentage of value between min and max
    const percentage = Math.min(100, Math.max(0, ((value - min) / (max - min)) * 100));
    const [fillPercentage, setFillPercentage] = useState(0);

    useEffect(() => {
        if(value == null) return;
        
        if (animated) {
            // Animate the value change
            let start = null;
            const startValue = displayValue;
            const duration = 1000; // Animation duration in ms

            const animate = (timestamp) => {
                if (!start) start = timestamp;
                const progress = timestamp - start;
                const newPercentage = Math.pow(progress / duration, 0.3);

                setFillPercentage(percentage * newPercentage);
                setDisplayValue(Math.max(startValue + (value - startValue) * newPercentage, 0));

                if (progress < duration) {
                    requestAnimationFrame(animate);
                }
            };

            requestAnimationFrame(animate);
        } else {
            setDisplayValue(value);
        }

    }, [value, animated/*, displayValue*/]);

    return (
        <styles.gauge.GaugeContainer $size={size}>
            <styles.gauge.GaugeCircle $size={size}>
                <styles.gauge.GaugeTrack $trackColor={trackColor} />
                <styles.gauge.GaugeFill
                    $percentage={fillPercentage}
                    $fillColor={fillColor}
                    $fillGradient={fillGradient}
                />
                <styles.gauge.GaugeInnerCircle $size={size} $innerColor={innerColor}>
                    <styles.gauge.GaugeValue $size={size} $valueColor={valueColor}>
                        {valueFormatter(displayValue)}
                    </styles.gauge.GaugeValue>
                </styles.gauge.GaugeInnerCircle>
            </styles.gauge.GaugeCircle>

            {label && (
                <styles.gauge.GaugeLabel $size={size} $labelColor={labelColor}>
                    {label}
                </styles.gauge.GaugeLabel>
            )}
        </styles.gauge.GaugeContainer>
    );
};

export default Gauge;