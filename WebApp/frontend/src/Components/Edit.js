import { useState, useEffect, useContext, useRef } from 'react';
import { UserContext } from '../Contexts/UserContext';
import styles from '../styles';
import { useNavigate } from 'react-router-dom';

function Edit() {
    const { user, token, setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        username: '',
        email: '',
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

    // State variables for animation
    const [passwordSectionVisible, setPasswordSectionVisible] = useState(false);
    const [passwordSectionHeight, setPasswordSectionHeight] = useState(0);
    const passwordSectionRef = useRef(null);

    useEffect(() => {
        const checkAuth = async () => {
            const token = localStorage.getItem("token");
            if (!token) {
                navigate('/login');
                return;
            }

            try {
                // Verify the token is still valid with the server
                const response = await fetch("http://localhost:3001/users/check-session", {
                    headers: { authorization: token }
                });

                if (!response.ok) {
                    navigate('/login');
                    return;
                }

                // Get user data directly from the server response
                const userData = await response.json();

                // Update user context with fresh data from server
                setUserContext({ user: userData.user, token });

                // Set form data using the fresh user data
                setFormData(prev => ({
                    ...prev,
                    username: userData.user.username || '',
                    email: userData.email || '',
                }));

                if (userData.user.profileImage && userData.user.profileImage.path) {
                    setImagePreview(`http://localhost:3001${userData.user.profileImage.path}`);
                }
            } catch (error) {
                console.error("Authentication error:", error);
                navigate('/login');
            }
        };

        // Only run the authentication check if we don't already have user data
        if (!user || !user.username) {
        checkAuth();
        } else {
            // If we already have user data, just set form data accordingly
            setFormData(prev => ({
                ...prev,
                username: user.username || ''
            }));

            if (user.profileImage && user.profileImage.path) {
                setImagePreview(`http://localhost:3001${user.profileImage.path}`);
            }
        }
    }, [navigate, setUserContext, user]);

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
            if (formData.email) updateData.email = formData.email;

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

                    <styles.common.PageTitle>Edit Profile</styles.common.PageTitle>
                    <styles.settings.Label style={{ marginBottom: '0.75rem', fontSize: '1.05rem' }}>Profile Picture</styles.settings.Label>
                    {imagePreview ? (
                        <div
                            onClick={() => document.getElementById('imageInput').click()}
                            onDragOver={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                e.currentTarget.classList.add('drag-active');
                            }}
                            onDragLeave={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                e.currentTarget.classList.remove('drag-active');
                            }}
                            onDrop={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                e.currentTarget.classList.remove('drag-active');
                                const file = e.dataTransfer.files[0];
                                if (file && file.type.startsWith('image/')) {
                                    setProfileImage(file);
                                    setImagePreview(URL.createObjectURL(file));
                                }
                            }}
                            className="dropzone-image"
                            style={{
                                width: '160px',
                                height: '160px',
                                borderRadius: '50%',
                                position: 'relative',
                                margin: '1.8rem auto 1.2rem auto',
                                boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                                cursor: 'pointer',
                                overflow: 'hidden',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }}
                        >
                       <img
                           src={imagePreview}
                           alt="Profile"
                           style={{
                                    width: '100%',
                                    height: '100%',
                               objectFit: 'cover',
                               border: `3px solid ${styles.colors.primary}`,
                                    borderRadius: '50%',
                                    boxSizing: 'border-box'
                           }}
                       />
                            <div className="overlay">
                                <div>Drop new image here<br/>or click to browse</div>
                            </div>
                            <style jsx="true">{`
                                .dropzone-image .overlay {
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    width: 100%;
                                    height: 100%;
                                    background: rgba(0, 0, 0, 0.5);
                                    display: none;
                                    justify-content: center;
                                    align-items: center;
                                    border-radius: 50%;
                                    color: white;
                                    font-size: 0.9rem;
                                    text-align: center;
                                    padding: 10px;
                                    box-sizing: border-box;
                                }
                                .dropzone-image:hover .overlay {
                                    display: flex;
                                }
                                .drag-active .overlay {
                                    display: flex !important;
                                    background: rgba(0, 0, 0, 0.7) !important;
                                }
                            `}</style>
                        </div>
                    ) : (
                               <div
                                   onDragOver={(e) => {
                                       e.preventDefault();
                                       e.stopPropagation();
                                       e.currentTarget.classList.add('drag-active');
                                   }}
                                   onDragLeave={(e) => {
                                       e.preventDefault();
                                       e.stopPropagation();
                                       e.currentTarget.classList.remove('drag-active');
                                   }}
                                   onDrop={(e) => {
                                       e.preventDefault();
                                       e.stopPropagation();
                                       e.currentTarget.classList.remove('drag-active');
                                       const file = e.dataTransfer.files[0];
                                       if (file && file.type.startsWith('image/')) {
                                           setProfileImage(file);
                                           setImagePreview(URL.createObjectURL(file));
                                       }
                                   }}
                            onClick={() => document.getElementById('imageInput').click()}
                                   className="dropzone"
                                   style={{
                                       display: 'flex',
                                       justifyContent: 'center',
                                       alignItems: 'center',
                                width: '160px',
                                height: '160px',
                                borderRadius: '50%',
                                       border: '2px dashed #ccc',
                                margin: '1.8rem auto 1.2rem auto',
                                       cursor: 'pointer',
                                       transition: 'all 0.2s ease',
                                       boxSizing: 'border-box',
                                       position: 'relative',
                                       overflow: 'hidden'
                                   }}
                               >
                                   <div className="default-message">
                                Drag & Drop<br/>Image Here<br/>or Click to Browse
                                   </div>
                                   <div className="drag-message">
                                       Drop Here
                               </div>
                               <style jsx="true">{`
                                   .dropzone .default-message {
                                       display: flex;
                                       justify-content: center;
                                       align-items: center;
                                       width: 100%;
                                       height: 100%;
                                    text-align: center;
                                    padding: 10px;
                                    box-sizing: border-box;
                                   }
                                   .dropzone .drag-message {
                                       display: none;
                                       justify-content: center;
                                       align-items: center;
                                       position: absolute;
                                       top: 0;
                                       left: 0;
                                       right: 0;
                                       bottom: 0;
                                       font-size: 1.2rem;
                                       font-weight: bold;
                                       color: ${styles.colors.primary};
                                   }
                                   .drag-active {
                                       border: 2px dashed ${styles.colors.primary} !important;
                                       background-color: ${styles.colors.primarySuperLight};
                                   }
                                   .drag-active .default-message {
                                       display: none;
                                   }
                                   .drag-active .drag-message {
                                       display: flex;
                                   }
                               `}</style>
                           </div>
                    )}

                    <div style={{
                        display: 'flex',
                        justifyContent: 'center',
                        gap: '15px',
                        marginTop: '0.8rem'
                    }}>
                               <styles.settings.Button
                                   type="button"
                                   onClick={() => document.getElementById('imageInput').click()}
                                   style={{
                                       ...buttonStyle,
                                       backgroundColor: styles.colors.secondary
                                   }}
                               >
                                   Choose Image
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

                           <input
                               id="imageInput"
                               type="file"
                               accept="image/*"
                               onChange={handleImageChange}
                               style={{ opacity: 0, position: "absolute", left: "-9999px" }}
                           />
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
                        <styles.settings.Label htmlFor="email" style={{
                            alignSelf: 'flex-start',
                            fontSize: '1.05rem',
                            marginBottom: '0.5rem'
                        }}>
                            Email
                        </styles.settings.Label>
                        <styles.settings.Input
                            id="email"
                            name="email"
                            type="email"
                            value={formData.email}
                            onChange={handleChange}
                            style={{
                                width: "100%",
                                padding: "0.7rem 1rem",
                                fontSize: "1rem"
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
