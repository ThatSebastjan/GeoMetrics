import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';
import { UserContext } from '../Contexts/UserContext';

function Login() {
    const navigate = useNavigate();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const userContext = useContext(UserContext);



    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await fetch(`http://${window.location.hostname}:3001/users/login`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: username,
                    password: password,
                }),
            });

            const data = await response.json();

            if (data.token !== undefined) {
                userContext.setUserContext(data);
                navigate('/');
            } else {
                setUsername('');
                setPassword('');
                setError('Invalid username or password');
            }
        } catch (error) {
            setError('An error occurred during login');
        } finally {
            setLoading(false);
        }
    };

    return (
        <styles.login.Container>
            <styles.login.FormWrapper>
                <styles.login.LogoSection>
                    <styles.login.LogoText>GeoMetrics</styles.login.LogoText>
                    <styles.login.AppDescription>Smart risk assessment app</styles.login.AppDescription>
                </styles.login.LogoSection>
                <styles.login.FormTitle>Login</styles.login.FormTitle>

                {error && <styles.login.ErrorMessage>{error}</styles.login.ErrorMessage>}

                <form onSubmit={handleLogin}>
                    <styles.login.FormGroup>
                        <styles.login.Label htmlFor="username">Username</styles.login.Label>
                        <styles.login.Input
                            type="text"
                            id="username"
                            name="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </styles.login.FormGroup>

                    <styles.login.FormGroup>
                        <styles.login.Label htmlFor="password">Password</styles.login.Label>
                        <styles.login.Input
                            type="password"
                            id="password"
                            name="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </styles.login.FormGroup>

                    <styles.login.SubmitButton
                        type="submit"
                        disabled={loading}
                    >
                        {loading ? 'Processing...' : 'Login'}
                    </styles.login.SubmitButton>
                </form>

                <styles.login.FormSwitch>
                    <p>Don't have an account yet?</p>
                    <styles.login.SwitchButton
                        onClick={() => navigate('/register')}
                        type="button"
                    >
                        Register
                    </styles.login.SwitchButton>
                </styles.login.FormSwitch>
            </styles.login.FormWrapper>
        </styles.login.Container>
    );
}

export default Login;