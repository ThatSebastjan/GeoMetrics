import { useState, useRef, createContext } from "react";
import styles from "../styles";


export const PopupContext = createContext();


const CustomPopups = ({ children }) => {
    const [showAlert, setShowAlert] = useState(false);
    const alertData = useRef({ title: null, message: null, cb: null });

    const [showPrompt, setShowPrompt] = useState(false);
    const promptData = useRef({ title: null, message: null, defaultValue: null, cb: null });
    const [promptInput, setPromptInput] = useState("");


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


    const ps = styles.popup;

    return (
    <PopupContext.Provider value={{
        alert: customAlert,
        prompt: customPrompt,
    }}>
        { (showAlert || showPrompt) && <div style={{ position: "absolute", zIndex: 100000, width: "100%", height: "100%", top: "0px", left: "0px", backdropFilter: "blur(2px)" }}></div> }
        { showAlert &&
        (<ps.PopupElement>
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

        { showPrompt &&
        (<ps.PopupElement>
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

        { children }
    </PopupContext.Provider>
    )
};


export default CustomPopups;