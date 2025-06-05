import React from 'react';
                import styles from '../styles';

                function AdvancedBar({ isFullScreen, onToggleFullScreen, settings, onUpdateSettings, hideIfOtherExpanded }) {
                    // Create a copy of settings to work with
                    const [localSettings, setLocalSettings] = React.useState({
                        ...settings,
                        floodWeights: settings.floodWeights || {
                            floodType: 40,
                            landUse: 30,
                            slope: 20,
                            proximityToWater: 10
                        },
                        landslideWeights: settings.landslideWeights || {
                            landslideSeverity: 40,
                            slope: 30,
                            landUse: 25,
                            proximityToWater: 5
                        }
                    });

                    // Apply changes when update button is clicked
                    const handleUpdate = () => {
                        onUpdateSettings(localSettings);
                    };

                    // Update local settings when input changes
                    const handleChange = (key, value) => {
                        setLocalSettings({
                            ...localSettings,
                            [key]: value
                        });
                    };

                    // Update weight parameters
                    const handleWeightChange = (riskType, parameter, value) => {
                        const numValue = parseInt(value);
                        setLocalSettings({
                            ...localSettings,
                            [`${riskType}Weights`]: {
                                ...localSettings[`${riskType}Weights`],
                                [parameter]: numValue
                            }
                        });
                    };

                    // Reset local settings when prop changes
                    React.useEffect(() => {
                        if (settings) {
                            setLocalSettings({
                                ...settings,
                                floodWeights: settings.floodWeights || {
                                    floodType: 40,
                                    landUse: 30,
                                    slope: 20,
                                    proximityToWater: 10
                                },
                                landslideWeights: settings.landslideWeights || {
                                    landslideSeverity: 40,
                                    slope: 30,
                                    landUse: 25,
                                    proximityToWater: 5
                                }
                            });
                        }
                    }, [settings]);

                    if (hideIfOtherExpanded) {
                        return (
                            <styles.assess.ToggleButton onClick={() => onToggleFullScreen(true)}>
                                <span>⚙️</span>
                            </styles.assess.ToggleButton>
                        );
                    }

                    return (
                        <styles.advanced.Container>
                            <styles.advanced.HeaderSection>
                                <styles.advanced.Title $isFullScreen={isFullScreen}>
                                    Advanced Assessment Settings
                                </styles.advanced.Title>
                                <styles.advanced.Subtitle>
                                    Customize risk assessment parameters to tailor analysis to your specific requirements
                                </styles.advanced.Subtitle>
                            </styles.advanced.HeaderSection>

                            <styles.advanced.TwoColumnLayout $isFullScreen={isFullScreen}>
                                {/* Left column */}
                                <styles.advanced.SettingsPanel>
                                    <styles.advanced.PanelHeading>
                                        General Settings
                                    </styles.advanced.PanelHeading>

                                    <styles.advanced.SettingGroup>
                                        <styles.advanced.SettingLabel>
                                            Time Horizon: {localSettings.timeHorizon} years
                                        </styles.advanced.SettingLabel>
                                        <styles.advanced.RangeControl>
                                            <styles.advanced.RangeLabel>10</styles.advanced.RangeLabel>
                                            <styles.advanced.RangeSlider
                                                type="range"
                                                min="10"
                                                max="100"
                                                value={localSettings.timeHorizon}
                                                onChange={(e) => handleChange('timeHorizon', parseInt(e.target.value))}
                                                $percentage={(localSettings.timeHorizon - 10) / 90 * 100}
                                            />
                                            <styles.advanced.RangeLabel>100</styles.advanced.RangeLabel>
                                        </styles.advanced.RangeControl>
                                    </styles.advanced.SettingGroup>

                                    <styles.advanced.SettingGroup>
                                        <styles.advanced.SettingLabel>
                                            Model Accuracy
                                        </styles.advanced.SettingLabel>
                                        <styles.advanced.OptionGroup>
                                            {['low', 'medium', 'high'].map(option => (
                                                <styles.advanced.Option
                                                    key={option}
                                                    onClick={() => handleChange('modelAccuracy', option)}
                                                    $isSelected={localSettings.modelAccuracy === option}
                                                >
                                                    {option.charAt(0).toUpperCase() + option.slice(1)}
                                                </styles.advanced.Option>
                                            ))}
                                        </styles.advanced.OptionGroup>
                                        <styles.advanced.OptionDescription>
                                            {localSettings.modelAccuracy === 'low' && 'Faster results, less detailed analysis'}
                                            {localSettings.modelAccuracy === 'medium' && 'Balanced speed and accuracy'}
                                            {localSettings.modelAccuracy === 'high' && 'More detailed analysis, longer processing time'}
                                        </styles.advanced.OptionDescription>
                                    </styles.advanced.SettingGroup>

                                    <styles.advanced.SettingGroup>
                                        <styles.advanced.SettingLabel>
                                            Risk Tolerance
                                        </styles.advanced.SettingLabel>
                                        <styles.advanced.OptionGroup>
                                            {['low', 'medium', 'high'].map(option => (
                                                <styles.advanced.Option
                                                    key={option}
                                                    onClick={() => handleChange('riskTolerance', option)}
                                                    $isSelected={localSettings.riskTolerance === option}
                                                >
                                                    {option.charAt(0).toUpperCase() + option.slice(1)}
                                                </styles.advanced.Option>
                                            ))}
                                        </styles.advanced.OptionGroup>
                                        <styles.advanced.OptionDescription>
                                            {localSettings.riskTolerance === 'low' && 'Conservative estimates - higher safety margin'}
                                            {localSettings.riskTolerance === 'medium' && 'Balanced risk assessment approach'}
                                            {localSettings.riskTolerance === 'high' && 'Progressive estimates - accepts more uncertainty'}
                                        </styles.advanced.OptionDescription>
                                    </styles.advanced.SettingGroup>

                                    <styles.advanced.CheckboxGroup>
                                        <styles.advanced.CheckboxOption
                                            $isSelected={localSettings.includePredictions}
                                        >
                                            <input
                                                type="checkbox"
                                                checked={localSettings.includePredictions}
                                                onChange={(e) => handleChange('includePredictions', e.target.checked)}
                                            />
                                            Include Predictions
                                        </styles.advanced.CheckboxOption>
                                        <styles.advanced.CheckboxOption
                                            $isSelected={localSettings.includeHistoricalData}
                                        >
                                            <input
                                                type="checkbox"
                                                checked={localSettings.includeHistoricalData}
                                                onChange={(e) => handleChange('includeHistoricalData', e.target.checked)}
                                            />
                                            Include Historical Data
                                        </styles.advanced.CheckboxOption>
                                    </styles.advanced.CheckboxGroup>
                                </styles.advanced.SettingsPanel>

                                {/* Right column */}
                                <styles.advanced.RiskPanel>
                                    {/* Flood Risk Parameters */}
                                    <styles.advanced.FloodPanel>
                                        <styles.advanced.RiskPanelHeading>
                                            <styles.advanced.RiskIcon>💧</styles.advanced.RiskIcon> Flood Risk Weights
                                        </styles.advanced.RiskPanelHeading>

                                        <styles.advanced.WeightGroup>
                                            {Object.entries(localSettings.floodWeights).map(([key, value]) => (
                                                <div key={key}>
                                                    <styles.advanced.WeightControl>
                                                        <styles.advanced.WeightLabel>
                                                            {key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1')}
                                                        </styles.advanced.WeightLabel>
                                                        <styles.advanced.FloodWeightBadge>
                                                            {value}%
                                                        </styles.advanced.FloodWeightBadge>
                                                    </styles.advanced.WeightControl>
                                                    <styles.advanced.FloodWeightSlider
                                                        type="range"
                                                        min={key === 'floodType' ? '10' : '5'}
                                                        max={key === 'floodType' ? '70' : key === 'landUse' ? '50' : key === 'slope' ? '35' : '20'}
                                                        value={value}
                                                        onChange={(e) => handleWeightChange('flood', key, e.target.value)}
                                                        $percentage={value}
                                                    />
                                                </div>
                                            ))}
                                        </styles.advanced.WeightGroup>
                                        <styles.advanced.WeightNote>
                                            These weights determine how different factors influence the flood risk calculation.
                                            The sum of weights should remain close to 100%.
                                        </styles.advanced.WeightNote>
                                    </styles.advanced.FloodPanel>

                                    {/* Landslide Risk Parameters */}
                                    <styles.advanced.LandslidePanel>
                                        <styles.advanced.RiskPanelHeading>
                                            <styles.advanced.RiskIcon>⛰️</styles.advanced.RiskIcon> Landslide Risk Weights
                                        </styles.advanced.RiskPanelHeading>

                                        <styles.advanced.WeightGroup>
                                            {Object.entries(localSettings.landslideWeights).map(([key, value]) => (
                                                <div key={key}>
                                                    <styles.advanced.WeightControl>
                                                        <styles.advanced.WeightLabel>
                                                            {key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1')}
                                                        </styles.advanced.WeightLabel>
                                                        <styles.advanced.LandslideWeightBadge>
                                                            {value}%
                                                        </styles.advanced.LandslideWeightBadge>
                                                    </styles.advanced.WeightControl>
                                                    <styles.advanced.LandslideWeightSlider
                                                        type="range"
                                                        min={key === 'proximityToWater' ? '0' : key === 'landUse' ? '10' : key === 'slope' ? '15' : '20'}
                                                        max={key === 'landslideSeverity' ? '60' : key === 'slope' ? '45' : key === 'landUse' ? '40' : '15'}
                                                        value={value}
                                                        onChange={(e) => handleWeightChange('landslide', key, e.target.value)}
                                                        $percentage={value}
                                                    />
                                                </div>
                                            ))}
                                        </styles.advanced.WeightGroup>
                                        <styles.advanced.WeightNote>
                                            These weights determine how different factors influence the landslide risk calculation.
                                            The sum of weights should remain close to 100%.
                                        </styles.advanced.WeightNote>
                                    </styles.advanced.LandslidePanel>
                                </styles.advanced.RiskPanel>
                            </styles.advanced.TwoColumnLayout>

                            <styles.advanced.ButtonGroup>
                                <styles.common.Button
                                    onClick={handleUpdate}
                                    className="update-button"
                                >
                                    Update Risk Analysis
                                </styles.common.Button>
                            </styles.advanced.ButtonGroup>
                        </styles.advanced.Container>
                    );
                }

                export default AdvancedBar;