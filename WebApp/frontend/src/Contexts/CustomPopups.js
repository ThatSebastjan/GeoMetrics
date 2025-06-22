import { useState, useRef, createContext } from "react";
import Slider from "@mui/material/Slider";
import Radio from "@mui/material/Radio";
import RadioGroup from "@mui/material/RadioGroup";
import FormControlLabel from "@mui/material/FormControlLabel";

import styles from "../styles";


export const PopupContext = createContext();


const CustomPopups = ({ children }) => {
    const [showAlert, setShowAlert] = useState(false);
    const alertData = useRef({ title: null, message: null, cb: null });

    const [showPrompt, setShowPrompt] = useState(false);
    const promptData = useRef({ title: null, message: null, defaultValue: null, cb: null });
    const [promptInput, setPromptInput] = useState("");

    const [showConfirm, setShowConfirm] = useState(false);
    const confirmData = useRef({ title: null, message: null, cb: null });

    const [showSaveLot, setShowSaveLot] = useState(false);
    const saveLotCallback = useRef(null);
    const [saveLotValues, setSaveLotValues] = useState({ name: null, address: null })

    const [showSSF, setShowSSF] = useState(false); //SSF = SmartSelect Filter
    const [SSFData, setSSFData] = useState({ 
        mode: 0, //Mode: 0 -> top %; 1 -> custom stat filters
        top: 5, //Top % value

        //Custom filter values
        flood: 50,
        landslide: 50,
        earthquake: 50,
    }); 

    const SSFCallback = useRef(null);


    const customAlert = (message, title = "Alert") => {

        return new Promise(resolve => {

            alertData.current = {
                title, 
                message,
                cb: () => {
                    setShowAlert(false);
                    resolve();
                }
            };

            setShowAlert(true);
        });
    };


    const customPrompt = (message, defaultValue = null, title = "Prompt") => {

        return new Promise(resolve => {

            promptData.current = {
                title, 
                message,
                defaultValue,
                cb: (value) => {
                    setShowPrompt(false);
                    resolve(value || defaultValue);
                }
            };

            setPromptInput(defaultValue || "");
            setShowPrompt(true);
        });
    };


    const customConfirm = (message, title = "Confirm action") => {

        return new Promise(resolve => {

            confirmData.current = {
                title, 
                message,
                cb: (result) => {
                    setShowConfirm(false);
                    resolve(result);
                }
            };

            setShowConfirm(true);
        });
    };


    const saveLotPrompt = (defaultAddress) => {

        return new Promise(resolve => {
            setSaveLotValues({ name: "", address: defaultAddress }); //Set default (clear previous)

            saveLotCallback.current = (result) => {
                setShowSaveLot(false);
                resolve(result);
            };
  
            setShowSaveLot(true);
        });
    };


    const smartSelectFilters = () => {

        return new Promise(resolve => {
            setSSFData({ mode: 0, top: 5, flood: 50, landslide: 50, earthquake: 50 }); //Set defaults

            SSFCallback.current = (result) => {
                setShowSSF(false);
                resolve(result);
            };
  
            setShowSSF(true);
        });
    };


    const ps = styles.popup;

    return (
    <PopupContext.Provider value={{
        alert: customAlert,
        prompt: customPrompt,
        confirm: customConfirm,
        saveLot: saveLotPrompt,

        SSF: smartSelectFilters,
    }}>



        {/* Popup backdrop */}

        { 
            (showAlert || showPrompt || showConfirm || showSaveLot || showSSF) && 
            <div 
                onContextMenu={(e) => e.preventDefault()}
                style={{
                    position: "absolute",
                    zIndex: 100000,
                    width: "100%",
                    height: "100%",
                    top: "0px",
                    left: "0px",
                    backdropFilter: "blur(2px)"
            }}></div> 
        }
        

        {/* Custom alert dialog */}
        
        { showAlert &&
        (<ps.PopupElement onContextMenu={(e) => e.preventDefault()}>
            <ps.PopupHeader>
                <ps.PopupTitle>{ alertData.current.title }</ps.PopupTitle>
            </ps.PopupHeader>

            <ps.PopupContent>
                <p style={{ marginTop: '12px' }}>{ alertData.current.message }</p>
            </ps.PopupContent>

            <ps.PopupActions>
                <styles.common.Button onClick={ alertData.current.cb }>OK</styles.common.Button>
            </ps.PopupActions>
        </ps.PopupElement>)
        }


        {/* Custom prompt dialog */}

        { showPrompt &&
        (<ps.PopupElement onContextMenu={(e) => e.preventDefault()}>
            <ps.PopupHeader>
                <ps.PopupTitle>{ promptData.current.title }</ps.PopupTitle>
            </ps.PopupHeader>

            <ps.PopupContent>
                <p style={{ marginTop: '12px' }}>{ promptData.current.message }</p>
                <ps.Input 
                    type="text"
                    value={promptInput}
                    onChange={(e) => setPromptInput(e.target.value)}
                />
            </ps.PopupContent>

            <ps.PopupActions>
                <styles.common.Button onClick={() => { promptData.current.cb(promptInput); } }>Yes</styles.common.Button>
                <styles.common.Button style={{ backgroundColor: styles.colors.danger }} onClick={() => { promptData.current.cb(null); } }>No</styles.common.Button>
            </ps.PopupActions>
        </ps.PopupElement>)
        }


        {/* Custom confirm dialog */}

        { showConfirm &&
        (<ps.PopupElement onContextMenu={(e) => e.preventDefault()}>
            <ps.PopupHeader>
                <ps.PopupTitle>{ confirmData.current.title }</ps.PopupTitle>
            </ps.PopupHeader>

            <ps.PopupContent>
                <p style={{ marginTop: '12px' }}>{ confirmData.current.message }</p>
            </ps.PopupContent>

            <ps.PopupActions>
                <styles.common.Button onClick={() => { confirmData.current.cb(true); } }>Yes</styles.common.Button>
                <styles.common.Button style={{ backgroundColor: styles.colors.danger }} onClick={() => { confirmData.current.cb(false); } }>No</styles.common.Button>
            </ps.PopupActions>
        </ps.PopupElement>)
        }


        {/* Save Lot popup */}

        { showSaveLot &&
        (<ps.PopupElement onContextMenu={(e) => e.preventDefault()}>
            <ps.PopupHeader>
                <ps.PopupTitle>Save Lot</ps.PopupTitle>
            </ps.PopupHeader>

            <ps.PopupContent>
                { /*<p style={{ marginTop: '12px' }}>Enter save details</p>*/ }

                <ps.FormGroup>
                    <ps.Label>Save name</ps.Label>
                    <ps.Input 
                        type="text"
                        value={saveLotValues.name}
                        onChange={(e) => setSaveLotValues({ ...saveLotValues, name: e.target.value })}
                    />
                </ps.FormGroup>

                <ps.FormGroup>
                    <ps.Label>Address</ps.Label>
                    <ps.Input 
                        type="text"
                        value={saveLotValues.address}
                        onChange={(e) => setSaveLotValues({ ...saveLotValues, address: e.target.value })}
                    />
                </ps.FormGroup>
            </ps.PopupContent>

            <ps.PopupActions>
                <styles.common.Button onClick={() => { saveLotCallback.current(saveLotValues); } }>Save</styles.common.Button>
                <styles.common.Button style={{ backgroundColor: styles.colors.danger }} onClick={() => { saveLotCallback.current(null); } }>Cancel</styles.common.Button>
            </ps.PopupActions>
        </ps.PopupElement>)
        }


        {/* Smart select filter popup */}

        { showSSF &&
        (<ps.PopupElement onContextMenu={(e) => e.preventDefault()}>
            <ps.PopupHeader>
                <ps.PopupTitle>Set filters</ps.PopupTitle>
            </ps.PopupHeader>

            <ps.PopupContent>

                <ps.FormGroup>
                    <ps.Label>Filter mode</ps.Label>
                    <RadioGroup
                        row
                        aria-labelledby="demo-row-radio-buttons-group-label"
                        name="row-radio-buttons-group"
                        defaultValue="0"
                        onChange={(e) => { setSSFData({ ...SSFData, mode: parseInt(e.target.value) }); }}
                    >
                        <FormControlLabel value="0" control={<Radio />} label="Overall best" />
                        <FormControlLabel value="1" control={<Radio />} label="Custom filter" />
                    </RadioGroup>
                    
                </ps.FormGroup>
                
                { (SSFData.mode == 0) &&  
                    (<ps.FormGroup>
                        <ps.Label>% of best to show</ps.Label>
                        <Slider size="small" value={SSFData.top} min={1} max={50} aria-label="Small" valueLabelDisplay="auto"
                            onChange={(e) => { setSSFData({ ...SSFData, top: e.target.value }); } }
                        />
                    </ps.FormGroup>)
                }

                { (SSFData.mode == 1) &&  
                    (<ps.FormGroup>
                        <ps.Label>Max flood risk</ps.Label>
                        <Slider size="small" value={SSFData.flood} aria-label="Small" valueLabelDisplay="auto"
                            sx={{ color: "rgb(147, 219, 255)" }}
                            onChange={(e) => { setSSFData({ ...SSFData, flood: e.target.value }); } }
                        />

                        <ps.Label>Max landslide risk</ps.Label>
                        <Slider size="small" value={SSFData.landslide} aria-label="Small" valueLabelDisplay="auto"
                            sx={{ color: "rgb(255, 218, 126)" }}
                            onChange={(e) => { setSSFData({ ...SSFData, landslide: e.target.value }); } }
                        />

                        <ps.Label>Max earthquake risk</ps.Label>
                        <Slider size="small" value={SSFData.earthquake} aria-label="Small" valueLabelDisplay="auto"
                            sx={{ color: "rgb(240, 198, 194)" }}
                            onChange={(e) => { setSSFData({ ...SSFData, earthquake: e.target.value }); } }
                        />
                    </ps.FormGroup>)
                }

            </ps.PopupContent>

            <ps.PopupActions>
                <styles.common.Button onClick={() => { SSFCallback.current(SSFData); }}>Apply</styles.common.Button>
                <styles.common.Button onClick={() => { SSFCallback.current(null); }} style={{ backgroundColor: styles.colors.danger }}>Cancel</styles.common.Button>
            </ps.PopupActions>
        </ps.PopupElement>)
        }


        { children }
    </PopupContext.Provider>
    )
};


export default CustomPopups;