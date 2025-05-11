import React, { createContext, useState, useEffect } from 'react';

export const UserContext = createContext(null);

export function UserProvider({ children }) {
    const [userContext, setUserContext] = useState({
        user: null,
        token: null
    });

    useEffect(() => {
        // Load user data and token from localStorage when the app starts
        const token = localStorage.getItem('token');
        const savedUser = JSON.parse(localStorage.getItem('user'));

        if (token && savedUser) {
            setUserContext({ user: savedUser, token });
        }
    }, []);

    // Function to update user context
    const setUserContextAndSave = (newContext) => {
        setUserContext(newContext);

        // Save to localStorage
        if (newContext.user && newContext.token) {
            localStorage.setItem('token', newContext.token);
            localStorage.setItem('user', JSON.stringify(newContext.user));
        }
    };

    // Function to clear user context
    const clearUserContext = () => {
        setUserContext({ user: null, token: null });
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    };

    return (
        <UserContext.Provider value={{
            user: userContext.user,
            token: userContext.token,
            setUserContext: setUserContextAndSave,
            clearUserContext
        }}>
            {children}
        </UserContext.Provider>
    );
}