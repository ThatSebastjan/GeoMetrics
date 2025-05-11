import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { UserContext, UserProvider } from './Contexts/UserContext';
import Login from './Components/Login';
import Register from './Components/Register';
import MainLayout from './Components/MainLayout';
import './App.css';
import Settings from "./Components/Settings";
import Assess from "./Components/Assess";
import SideBySide from "./Components/SideBySide";
import SmartSelect from "./Components/SmartSelect";
import Edit from "./Components/Edit";
import Results from "./Components/Results";
import SavedLots from "./Components/SavedLots";

// Create a separate component for routes that uses the context
function AppRoutes() {
    const { setUserContext } = React.useContext(UserContext);

    React.useEffect(() => {
        authenticate();
    }, []);

    async function authenticate() {
        const token = localStorage.getItem("token");
        if (!token) {
            return;
        }
        debugger
        try {
            const req = await fetch("http://localhost:3001/users/check-session", {
                headers: { authorization: token }
            });

            if (req.status !== 200) {
                // Clear user data if authentication fails
                localStorage.removeItem("token");
                localStorage.removeItem("user");
                setUserContext({ user: null, token: null });
            } else {
                // Session is valid, get user data from response
                const userData = await req.json();

                // Set user context with data from server
                setUserContext({
                    user: userData.user,
                    token: token
                });
            }
        } catch (error) {
            console.error("Invalid session:", error);
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            setUserContext({ user: null, token: null });
        }
    }

    return (
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
                        <Route path="results" element={<Results />} />
                        <Route path="saved-lots" element={<SavedLots />} />
                    </Route>
                </Routes>
            </div>
        </Router>
    );
}

// Main App component that provides the context
function App() {
    return (
        <UserProvider>
            <AppRoutes />
        </UserProvider>
    );
}

export default App;