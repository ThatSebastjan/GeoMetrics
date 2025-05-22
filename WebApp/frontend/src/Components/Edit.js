import { useState, useEffect, useContext, useRef } from 'react';
import { UserContext } from '../Contexts/UserContext';
import styles from '../styles';
import { useNavigate } from 'react-router-dom';

function Edit() {
    const { user, token, setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [profileImage, setProfileImage] = useState(null);
    const [imagePreview, setImagePreview] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [showPasswordSection, setShowPasswordSection] = useState(false);
    const [passwordMatchError, setPasswordMatchError] = useState('');

    // Add new state variables for animation
    const [passwordSectionVisible, setPasswordSectionVisible] = useState(false);
    const [passwordSectionHeight, setPasswordSectionHeight] = useState(0);
    const passwordSectionRef = useRef(null);

    useEffect(() => {
        if (!user) {
            navigate('/login');
            return;
        }

        setFormData(prev => ({
            ...prev,
            username: user.username || ''
        }));

        if (user.profileImage && user.profileImage.path) {
            setImagePreview(`http://localhost:3001${user.profileImage.path}`);
        }
    }, [navigate, user]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        
        // Real-time password match validation
        if (name === 'newPassword' || name === 'confirmPassword') {
            const newPassword = name === 'newPassword' ? value : formData.newPassword;
            const confirmPassword = name === 'confirmPassword' ? value : formData.confirmPassword;
            
            if (newPassword && confirmPassword) {
                if (newPassword !== confirmPassword) {
                    setPasswordMatchError('Passwords do not match');
                } else {
                    setPasswordMatchError('');
                }
            } else {
                setPasswordMatchError('');
            }
        }
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setProfileImage(file);
            setImagePreview(URL.createObjectURL(file));
        }
    };

    const handleImageUpload = async () => {
        if (!profileImage) return null;

        try {
            const formData = new FormData();
            formData.append('profileImage', profileImage);

            const response = await fetch('http://localhost:3001/users/profile/image', {
                method: 'POST',
                headers: { 'Authorization': token },
                body: formData
            });

            if (!response.ok) throw new Error('Failed to upload image');
            return await response.json();
        } catch (error) {
            console.error('Error uploading image:', error);
            setError(error.message);
            return null;
        }
    };

    const handleDeleteImage = async () => {
        try {
            setLoading(true);
            const response = await fetch('http://localhost:3001/users/profile/image', {
                method: 'DELETE',
                headers: { 'Authorization': token }
            });

            if (!response.ok) throw new Error('Failed to delete image');
            const data = await response.json();

            setUserContext({ user: data.user, token });
            setImagePreview(null);
            setProfileImage(null);
            setSuccess('Profile image deleted successfully');
        } catch (error) {
            console.error('Error deleting image:', error);
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);

        try {
            if (!token) throw new Error('Authentication required');

            // Add password confirmation validation
            if (showPasswordSection && formData.newPassword) {
                if (formData.newPassword !== formData.confirmPassword) {
                    throw new Error('New password and confirmation do not match');
                }
            }
            
            let imageUploadResult = null;
            if (profileImage) {
                imageUploadResult = await handleImageUpload();
                if (!imageUploadResult) throw new Error('Image upload failed');
            }

            const updateData = {};

            if (showPasswordSection) {
                if (!formData.currentPassword) {
                    throw new Error('Current password is required when changing password');
                }
                updateData.currentPassword = formData.currentPassword;
                if (formData.newPassword) updateData.newPassword = formData.newPassword;
            }

            if (formData.username) updateData.username = formData.username;

            let updatedUser = user;

            if (Object.keys(updateData).length > 0) {
                const response = await fetch('http://localhost:3001/users/profile/update', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token
                    },
                    body: JSON.stringify(updateData)
                });

                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'Update failed');

                updatedUser = {
                    ...user,
                    ...data.user || {},
                    username: data.username || formData.username || user.username
                };
            }

            if (imageUploadResult && imageUploadResult.user) {
                updatedUser = {
                    ...updatedUser,
                    profileImage: imageUploadResult.user.profileImage
                };
            }

            setUserContext({ user: updatedUser, token });
            setSuccess('Profile updated successfully. Redirecting to Settings ...');
            setFormData(prev => ({
                ...prev,
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
            }));

            setTimeout(() => {
                navigate('/settings');
            }, 1500);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    // Replace togglePasswordSection with this enhanced version
    const togglePasswordSection = () => {
        if (showPasswordSection) {
            // Begin closing animation
            setPasswordSectionVisible(false);
            setTimeout(() => {
                setShowPasswordSection(false);
                setFormData(prev => ({
                    ...prev,
                    currentPassword: '',
                    newPassword: '',
                    confirmPassword: ''
                }));
                setPasswordMatchError('');
            }, 300); // Match transition duration
        } else {
            // Open immediately, then animate in
            setShowPasswordSection(true);
            // Small delay to ensure DOM has updated
            setTimeout(() => {
                if (passwordSectionRef.current) {
                    const height = passwordSectionRef.current.scrollHeight;
                    setPasswordSectionHeight(height);
                    setPasswordSectionVisible(true);
                }
            }, 50);
        }
    };

    // Add useEffect to measure height when content changes
    useEffect(() => {
        if (showPasswordSection && passwordSectionRef.current) {
            const height = passwordSectionRef.current.scrollHeight;
            setPasswordSectionHeight(height);
        }
    }, [showPasswordSection, formData.newPassword, formData.confirmPassword, passwordMatchError]);

    // Define a common button style for all main buttons
    const buttonStyle = {
        minWidth: '140px',
        padding: '0.4rem 1.2rem',
        transition: 'background 0.2s, box-shadow 0.2s'
    };

    return (
        <styles.settings.Section style={{
            minHeight: 'calc(100vh - 100px)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '2rem 1rem'
        }}>
            <styles.settings.Header style={{ maxWidth: '500px', width: '100%', textAlign: 'center' }}>
                <styles.common.PageTitle>Edit Profile</styles.common.PageTitle>
            </styles.settings.Header>

            {error && <styles.common.Message $type="error" style={{ maxWidth: '500px', width: '100%' }}>{error}</styles.common.Message>}
            {success && <styles.common.Message $type="success" style={{ maxWidth: '500px', width: '100%' }}>{success}</styles.common.Message>}

            {/* Main form container - increase padding and gap */}
            <form onSubmit={handleSubmit} style={{
                width: '100%',
                maxWidth: '550px',  // Increased from 500px
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '2.2rem',      // Increased from 1.5rem
                marginTop: '2rem',  // Increased from 1.5rem
                background: '#fff',
                borderRadius: '16px',
                boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
                padding: '2.5rem'   // Increased from 2rem
            }}>
                {/* Profile picture section */}
                <styles.settings.FormGroup style={{ alignItems: 'center', maxWidth: '380px', margin: '0 auto', width: '100%' }}>
                    <styles.settings.Label style={{ marginBottom: '0.75rem', fontSize: '1.05rem' }}>Profile Picture</styles.settings.Label>
                    {imagePreview && (
                        <img
                            src={imagePreview}
                            alt="Profile"
                            style={{
                                width: '160px',    // Increased from 140px
                                height: '160px',   // Increased from 140px
                                borderRadius: '50%',
                                objectFit: 'cover',
                                border: `3px solid ${styles.colors.primary}`,
                                margin: '1.8rem 0 1.2rem 0',  // Increased margins
                                boxShadow: '0 2px 8px rgba(0,0,0,0.06)'
                            }}
                        />
                    )}
                    <div style={{ 
                        display: 'flex', 
                        gap: '15px',   // Increased from 10px
                        justifyContent: 'center', 
                        width: '100%', 
                        marginTop: '0.8rem'  // Increased from 0.5rem
                    }}>
                        <styles.settings.Button
                            type="button"
                            onClick={() => document.getElementById('imageInput').click()}
                            style={{
                                ...buttonStyle,
                            }}
                            onMouseOver={e => e.currentTarget.style.backgroundColor = styles.colors.primaryLight}
                            onMouseOut={e => e.currentTarget.style.backgroundColor = ''}
                        >
                            Choose Image
                            <input
                                id="imageInput"
                                type="file"
                                accept="image/*"
                                onChange={handleImageChange}
                                style={{ display: 'none' }}
                            />
                        </styles.settings.Button>
                        {imagePreview && (
                            <styles.settings.Button
                                type="button"
                                onClick={handleDeleteImage}
                                disabled={loading}
                                style={{
                                    ...buttonStyle,
                                    backgroundColor: styles.colors.danger
                                }}
                                onMouseOver={e => e.currentTarget.style.backgroundColor = '#ff4d4f'}
                                onMouseOut={e => e.currentTarget.style.backgroundColor = styles.colors.danger}
                            >
                                Delete
                            </styles.settings.Button>
                        )}
                    </div>
                </styles.settings.FormGroup>

                {/* Username form group */}
                <styles.settings.FormGroup style={{ 
                    maxWidth: '400px', 
                    margin: '0 auto', 
                    width: '100%',
                    display: 'flex', 
                    flexDirection: 'column', 
                    alignItems: 'center'
                }}>
                    <styles.settings.Label htmlFor="username" style={{ 
                        alignSelf: 'flex-start',  // Changed from center to flex-start
                        fontSize: '1.05rem',      // Increased font size
                        marginBottom: '0.5rem'    // Added margin
                    }}>
                        Username
                    </styles.settings.Label>
                    <styles.settings.Input
                        id="username"
                        name="username"
                        type="text"
                        value={formData.username}
                        onChange={handleChange}
                        style={{ 
                            width: "100%",
                            padding: "0.7rem 1rem",  // Increased padding
                            fontSize: "1rem"         // Increased font size
                        }}
                    />
                </styles.settings.FormGroup>


                {/* Password toggle button */}
                <div style={{ display: 'flex', justifyContent: 'center', width: '100%', margin: '0.5rem 0' }}>
                    <styles.settings.Button
                        type="button"
                        onClick={togglePasswordSection}
                        style={{
                            ...buttonStyle,
                            backgroundColor: styles.colors.secondary,
                            margin: '0.5rem 0',
                            padding: '0.6rem 1.5rem',  // Increased padding
                            fontSize: '1rem'           // Increased font size
                        }}
                    >
                        {showPasswordSection ? 'Cancel Password Update' : 'Update Password'}
                    </styles.settings.Button>
                </div>

                {/* Password section */}
                {showPasswordSection && (
                    <div 
                        ref={passwordSectionRef}
                        style={{
                            backgroundColor: '#f9f9f9',
                            border: `1px solid ${styles.colors.border}`,
                            borderRadius: '8px',
                            padding: '1.5rem',   // Increased from 1rem
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '1.5rem',       // Increased from 1rem
                            maxWidth: '420px',   // Increased from 400px
                            margin: '0 auto',   
                            width: '100%',
                            opacity: passwordSectionVisible ? 1 : 0,
                            maxHeight: passwordSectionVisible ? `${passwordSectionHeight}px` : '0',
                            overflow: 'hidden',
                            transition: 'opacity 300ms ease, max-height 300ms ease',
                        }}
                    >
                        <styles.settings.FormGroup style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                            <styles.settings.Label htmlFor="currentPassword" style={{ 
                                alignSelf: 'flex-start', 
                                width: "90%",     // Keep at 90% as requested
                                fontSize: '1.05rem',
                                marginBottom: '0.5rem'
                            }}>
                                Current Password *
                            </styles.settings.Label>
                            <styles.settings.Input
                                id="currentPassword"
                                name="currentPassword"
                                type="password"
                                value={formData.currentPassword}
                                onChange={handleChange}
                                required
                                style={{ 
                                    width: "90%",   // Keep at 90% as requested
                                    padding: "0.7rem 1rem",
                                    fontSize: "1rem"
                                }}
                            />
                        </styles.settings.FormGroup>

                        <styles.settings.FormGroup style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                            <styles.settings.Label htmlFor="newPassword" style={{ alignSelf: 'flex-start' }}>New Password</styles.settings.Label>
                            <styles.settings.Input
                                id="newPassword"
                                name="newPassword"
                                type="password"
                                value={formData.newPassword}
                                onChange={handleChange}
                                style={{ width: "90%" }}
                            />
                        </styles.settings.FormGroup>

                        <styles.settings.FormGroup style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                            <styles.settings.Label htmlFor="confirmPassword" style={{ alignSelf: 'flex-start' }}>Confirm New Password</styles.settings.Label>
                            <styles.settings.Input
                                id="confirmPassword"
                                name="confirmPassword"
                                type="password"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                style={{ width: "90%" }}
                            />
                            {passwordMatchError && (
                                <div style={{ 
                                    color: '#ff4d4f', 
                                    fontSize: '0.85rem', 
                                    marginTop: '0.5rem',
                                    fontWeight: '500',
                                    width: '100%'
                                }}>
                                    {passwordMatchError}
                                </div>
                            )}
                        </styles.settings.FormGroup>
                    </div>
                )}

                {/* Form buttons */}
                <div style={{ display: 'flex', gap: '1.5rem', justifyContent: 'center', marginTop: '1rem' }}>
                    <styles.settings.Button
                        type="submit"
                        disabled={loading}
                        style={{
                            ...buttonStyle,
                            padding: '0.7rem 1.5rem',  // Increased padding
                            fontSize: '1rem'           // Increased font size
                        }}
                    >
                        {loading ? 'Saving...' : 'Save Changes'}
                    </styles.settings.Button>
                    <styles.settings.EditButton
                        type="button"
                        onClick={() => navigate('/settings')}
                        style={{
                            ...buttonStyle,
                            padding: '0.7rem 1.5rem',  // Increased padding
                            fontSize: '1rem'           // Increased font size
                        }}
                    >
                        Cancel
                    </styles.settings.EditButton>
                </div>
            </form>
        </styles.settings.Section>
    );
}

export default Edit;
