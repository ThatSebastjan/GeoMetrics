// frontend/src/Contexts/UserContext.js
import React, { createContext, useState } from 'react';

export const UserContext = createContext({
    user: null,
    token: null,
    setUserContext : () => {},
    clearUserContext: () => {}
});

export const UserProvider = ({ children }) => {
    const [user, setUser] = useState(localStorage.user ? JSON.parse(localStorage.user) : null);
    const [token, setToken] = useState(localStorage.token);

    const setUserContext = (userData) => {
        setUser(userData.user);
        setToken(userData.token);
        localStorage.setItem('user', JSON.stringify(userData.user));
        localStorage.setItem('token', userData.token);
    };

    const clearUserContext = () => {
        setUser(null);
        setToken(null);
        localStorage.removeItem('user');
        localStorage.removeItem('token');
    };

    return (
        <UserContext.Provider value={{ user, token, setUserContext, clearUserContext }}>
            {children}
        </UserContext.Provider>
    );
};