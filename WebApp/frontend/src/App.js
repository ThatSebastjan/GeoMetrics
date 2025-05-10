import React, {useContext, useEffect} from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import {UserContext, UserProvider} from './Contexts/UserContext';
import Login from './Components/Login';
import Register from './Components/Register';
import MainLayout from './Components/MainLayout';
import './App.css';
import Settings from "./Components/Settings";
import Assess from "./Components/Assess";
import SideBySide from "./Components/SideBySide";
import SmartSelect from "./Components/SmartSelect";
import Edit from "./Components/Edit";



function App() {
    // useEffect(() => {
    //     authenticate();
    // })

    // async function authenticate() {
    //     const token = localStorage.getItem("token");
    //     if (!token) {
    //         return
    //     }
    //     const req = await fetch("http://localhost:3001/users/check-session", {headers:{authorization:token}});
    //     if(req.status !== 200){
    //         localStorage.removeItem("token");
    //         localStorage.removeItem("user");
    //     };
    // }



    return (
        <UserProvider>
            <Router>
                <div className="App">
                    <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />

                        {/* Protected routes with layout */}
                        <Route path="/" element={<MainLayout />}>
                            <Route index element={<Navigate to="/assess" replace />} />
                            <Route path="assess" element={<Assess />} />
                            <Route path="smart-select" element={<SmartSelect />} />
                            <Route path="side-by-side" element={<SideBySide />} />
                            <Route path="settings" element={<Settings />} />
                            <Route path="settings/edit-profile" element={<Edit />} />
                        </Route>
                    </Routes>
                </div>
            </Router>
        </UserProvider>
    );
}

export default App;