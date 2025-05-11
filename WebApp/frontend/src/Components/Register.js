import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';

function Register() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await fetch('http://localhost:3001/users/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Registration failed');
            }

            // Show success and redirect to login
            alert('Registration successful! Please login.');
            navigate('/login');
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <styles.login.Container>
            <styles.login.FormWrapper>
                <styles.login.FormTitle>Create Account</styles.login.FormTitle>

                {error && <styles.login.ErrorMessage>{error}</styles.login.ErrorMessage>}

                <form onSubmit={handleSubmit}>
                    <styles.login.FormGroup>
                        <styles.login.Label htmlFor="username">Username</styles.login.Label>
                        <styles.login.Input
                            type="text"
                            id="username"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            required
                        />
                    </styles.login.FormGroup>

                    <styles.login.FormGroup>
                        <styles.login.Label htmlFor="email">Email</styles.login.Label>
                        <styles.login.Input
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                        />
                    </styles.login.FormGroup>

                    <styles.login.FormGroup>
                        <styles.login.Label htmlFor="password">Password</styles.login.Label>
                        <styles.login.Input
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                        />
                    </styles.login.FormGroup>

                    <styles.login.SubmitButton
                        type="submit"
                        disabled={loading}
                    >
                        {loading ? 'Processing...' : 'Register'}
                    </styles.login.SubmitButton>
                </form>

                <styles.login.FormSwitch>
                    <p>Already have an account?</p>
                    <styles.login.SwitchButton
                        onClick={() => navigate('/login')}
                        type="button"
                    >
                        Login
                    </styles.login.SwitchButton>
                </styles.login.FormSwitch>
            </styles.login.FormWrapper>
        </styles.login.Container>
    );
}

export default Register;