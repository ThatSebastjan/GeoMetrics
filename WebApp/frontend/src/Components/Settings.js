import { useState, useEffect, useContext} from "react";
import styles from "../styles";
import {useNavigate} from "react-router-dom";
import userContext, {UserContext} from "../Contexts/UserContext";

function Settings() {
    const navigate = useNavigate();
    const context = useContext(UserContext);

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/');
    };


    if (!context.user) return (
        <>
            <styles.settings.Section>
                <styles.common.PageTitle>Here is where your user profile lives ...</styles.common.PageTitle>
                <styles.common.SectionTitle>Once you login that is.</styles.common.SectionTitle>
                <br/>
                <styles.settings.LoginButton
                    onClick={() => {
                        window.location.href = '/login';
                    }}
                >
                    Login
                </styles.settings.LoginButton>
            </styles.settings.Section>

            <styles.settings.Section>
                <styles.common.SectionTitle>App Settings</styles.common.SectionTitle>
                <styles.settings.List>
                    <styles.settings.Button
                        onClick={() => alert('Dark mode setting would be toggled here')}
                    >
                        Toggle Dark Mode
                    </styles.settings.Button>
                    <styles.settings.Button
                        onClick={() => alert('Notifications would be disabled here')}
                    >
                        Disable Notifications
                    </styles.settings.Button>
                    <styles.settings.Button
                        onClick={() => alert('Language would be changed here')}
                    >
                        Change Language
                    </styles.settings.Button>
                </styles.settings.List>

            </styles.settings.Section>
        </>
    );

    return (
        <div>

            {/* User info at the top */}
            <styles.settings.Section>
                <styles.common.HeroTitle> {context.user.username}</styles.common.HeroTitle>
                <styles.common.SectionTitle> {context.user.email}</styles.common.SectionTitle>
                <p><strong>Joined:</strong> {context.user.registrationDate
                    ? new Date(context.user.registrationDate).toLocaleDateString()
                    : 'Not available'}</p>

                <styles.settings.Button
                    onClick={() => handleLogout()}
                >
                    Logout
                </styles.settings.Button>
                <styles.settings.EditButton
                    onClick={() => {window.location.href = '/settings/edit-profile';}}>
                    Edit Profile
                </styles.settings.EditButton>
            </styles.settings.Section>

            <styles.settings.Section>
                <styles.common.PageTitle>Settings</styles.common.PageTitle>
            </styles.settings.Section>

        </div>
    );
}

export default Settings;