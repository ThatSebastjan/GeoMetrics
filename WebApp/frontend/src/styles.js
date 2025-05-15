import styled from 'styled-components';

// Theme constants
const colors = {
    primary: '#000000',
    primaryLight: '#1b1b1b',
    primarySuperLight: '#454545',
    secondary: '#838383',
    secondaryLight: '#a8a8a8',
    secondarySuperLight: '#eeeeee',
    danger: '#f44336',
    dangerDark: '#d32f2f',
    error: '#fdedee',
    errorText: '#d32f2f',
    success: '#38a169',
    lightGray: '#f7f7f7',
    textDark: '#2d3748',
    textMedium: '#4a5568',
    disabled: '#a0aec0',
    light: '#f5f5f5',
    white: '#ffffff',
    offWhite: '#fdfdfd',
    border: '#ddd',
    text: '#333',
    highlight: '#e3f2fd',
};

const spacing = {
    xs: '0.5rem',
    sm: '0.75rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
};

const shadows = {
    small: '0 1px 3px rgba(0, 0, 0, 0.1)',
    medium: '0 4px 6px rgba(0, 0, 0, 0.1)',
};

const borderRadius = {
    small: '4px',
    medium: '8px',
    large: '10px',
    xLarge: '14px',
};

const loginStyles = {
    Container: styled.div`
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 100vh;
        background-color: ${colors.light};
    `,

    FormWrapper: styled.div`
        width: 100%;
        max-width: 400px;
        padding: ${spacing.xl};
        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.medium};
    `,

    FormTitle: styled.h2`
        text-align: center;
        margin-bottom: ${spacing.lg};
        color: ${colors.text};
    `,

    FormGroup: styled.div`
        margin-bottom: ${spacing.md};
    `,

    Label: styled.label`
        display: block;
        margin-bottom: ${spacing.xs};
        font-weight: 500;
    `,

    Input: styled.input`
        width: 100%;
        padding: ${spacing.sm};
        border: 1px solid ${colors.border};
        border-radius: ${borderRadius.small};
        font-size: 1rem;
    `,

    SubmitButton: styled.button`
        width: 100%;
        padding: ${spacing.sm};
        background-color: ${colors.primary};
        color: ${colors.white};
        border: none;
        border-radius: ${borderRadius.small};
        font-size: 1rem;
        cursor: pointer;
        margin-top: ${spacing.md};

        &:hover {
            background-color: ${colors.primaryDark};
        }

        &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
        }
    `,

    ErrorMessage: styled.div`
        background-color: ${colors.error};
        color: ${colors.errorText};
        padding: ${spacing.sm};
        border-radius: ${borderRadius.small};
        margin-bottom: ${spacing.md};
    `,

    FormSwitch: styled.div`
        margin-top: ${spacing.lg};
        text-align: center;
    `,

    SwitchButton: styled.button`
        background: none;
        border: none;
        color: ${colors.primary};
        cursor: pointer;
        font-size: 1rem;
        text-decoration: underline;
    `,
};

// Dashboard styles
const dashboardStyles = {





};

const commonStyles = {

    HeroTitle: styled.h1`
        font-size: 2.5rem;
        padding: 0px;
        margin-bottom: 0px;
    `,

    PageTitle: styled.h1`
        margin: 0 0 ${spacing.md} 0;
        color: ${colors.textDark};
        font-size: 1.8rem;
        font-weight: 600;
        text-align: left;
    `,

    PageTitleHero: styled.h1`
        margin: 0 0 ${spacing.md} 0;
        font-size: 1.8rem;
        font-weight: 600;
    `,

    SectionTitle: styled.h2`
        margin-bottom: ${spacing.md};
        color: ${colors.text};
    `,

    SectionTitleHero: styled.h2`
        margin-bottom: ${spacing.md};
    `,

    Card: styled.div`
        background-color: ${colors.white};
        padding: ${spacing.lg};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.small};
        margin-bottom: ${spacing.lg};
    `,

    LoadingIndicator: styled.div`
        padding: ${spacing.md};
        text-align: center;
        color: ${colors.textMedium};
    `,

    Loading: styled.div`
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
        font-size: 1.5rem;
    `,

    // Add these to commonStyles object
    FormGroup: styled.div`
        margin-bottom: 15px;
    `,
    Label: styled.label`
        display: block;
        margin-bottom: 5px;
        font-weight: 500;
    `,
    Input: styled.input`
        width: 100%;
        padding: 8px;
        border: 1px solid #ddd;
        border-radius: 4px;
    `,
    Message: styled.div`
        padding: 10px;
        margin: 15px 0;
        border-radius: 4px;
        background-color: ${props => props.$type === 'error' ? '#ffdddd' : '#ddffdd'};
        border: 1px solid ${props => props.$type === 'error' ? '#f44336' : '#4CAF50'};
    `,

// Add this to settingsStyles object
    Button: styled.button`
        background: ${props => props.$secondary ? '#f1f1f1' : colors.primary};
        color: ${props => props.$secondary ? '#333' : 'white'};
        border: none;
        padding: ${spacing.sm} ${spacing.md};
        border-radius: ${borderRadius.small};
        margin-right: 10px;
        cursor: pointer;

        &:hover {
            background: ${props => props.$secondary ? '#e1e1e1' : colors.primaryDark};
        }

        &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
        }
    `,
};

const layoutStyles = {
    Container: styled.div`
        display: flex;
        min-height: 100vh;
        overflow: hidden; /* Prevent content overflow */
    `,

    Sidebar: styled.div`
        width: 250px;
        min-width: 250px; /* Ensures sidebar doesn't shrink */
        height: 100vh;
        background-color: ${colors.primaryDark};
        display: flex;
        flex-direction: column;
        padding: ${spacing.md} 0;
        position: fixed; /* Fix the sidebar */
        left: 0;
        top: 0;
        z-index: 100;
        overflow-y: auto; /* Make sidebar content scrollable if needed */
    `,

    Logo: styled.div`
        font-size: 1.5rem;
        font-weight: bold;
        color: ${colors.text};
        padding: ${spacing.md};
        margin-bottom: ${spacing.lg};
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    `,

    NavMenu: styled.nav`
        flex: 1;
    `,

    NavItem: styled.div`
        padding: ${spacing.md} ${spacing.xl};
        cursor: pointer;
        color: ${colors.text};
        font-weight: bold;
        text-align: left;
        transition: background-color 0.3s;

        &:hover {
            background-color: rgba(255, 255, 255, 0.1);
        }
    `,

    UserSection: styled.div`
        margin-top: auto;
        margin-bottom: 1rem;
        padding: ${spacing.md};
    `,

    UserInfo: styled.div`
        margin-bottom: ${spacing.sm};

        small {
            opacity: 0.7;
        }
    `,

    LogoutButton: styled.button`
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: ${colors.text};
        padding: ${spacing.xs} ${spacing.sm};
        border-radius: ${borderRadius.small};
        cursor: pointer;

        &:hover {
            background-color: rgba(255, 255, 255, 0.1);
        }
    `,

    Content: styled.main`
        flex: 1;
        padding: 0;
        margin-left: 250px; /* Match the sidebar width */
        background-color: ${colors.light};
        overflow-y: auto; /* Make the content scrollable */
        min-height: 100vh;
        width: calc(100% - 250px);
    `,


    DropdownContainer: styled.div`
        margin-left: ${spacing.md};
        margin-right: ${spacing.md};
        margin-bottom: ${spacing.xs};
        border-radius: ${borderRadius.medium};
        background-color: ${colors.secondarySuperLight};
    `,

    DropdownMenu: styled.div`
        overflow: hidden;
        max-height: ${props => (props.$isOpen ? '500px' : '0')};
        opacity: ${props => (props.$isOpen ? '1' : '0')};
        transform-origin: top;
        transform: ${props => (props.$isOpen ? 'scaleY(1)' : 'scaleY(0.95)')};
        transition:
                max-height 0.35s ease-out,
                opacity 0.25s ease-in-out,
                transform 0.25s ease-in-out,
                margin 0.2s ease-out;
        background-color: rgba(255, 255, 255, 0.05);
            // border-radius: ${props => (props.$isOpen ? borderRadius.small : '0')};

        & > * {
            opacity: ${props => (props.$isOpen ? '1' : '0')};
            transform: translateY(${props => (props.$isOpen ? '0' : '-4px')});
            transition: opacity 0.25s ease-in-out, transform 0.25s ease-out;
            transition-delay: ${props => (props.$isOpen ? '0.05s' : '0s')};
        }
    `,

    NavItemWithIcon: styled.div`
        text-indent: ${spacing.xs};
        background-color: ${colors.white};
        padding: ${spacing.md} ${spacing.lg};
        border-radius: ${borderRadius.medium};
        cursor: pointer;
        color: ${colors.text};
        font-weight: bold;
        text-align: left;
        display: flex;
        align-items: center;
        transition: all 0.25s;

        ${props => props.$isActive && `
            background: ${colors.primary};
            color: ${colors.white};
            border-radius: ${borderRadius.medium} ${borderRadius.medium} 0 0;
        `}
        &:hover {
            background: ${colors.primary};
            color: white;

        }
    `,

    DropdownItem: styled.div`
        text-indent: ${spacing.xs};
        padding: ${spacing.md} ${spacing.md};
        background-color: transparent;
        cursor: pointer;
        color: ${colors.text || '#000000'};
        text-align: left;
        transition: background-color 0.3s, border-radius 0.3s;
        padding-left: ${spacing.lg};
        font-family: inherit;
        font-size: inherit;
        font-weight: bold;
        display: flex;
        align-items: center;
        gap: 8px;
        border-radius: 0;

        &:hover, &:active {
            background-color: rgba(168, 168, 168, 0.5); /* Semi-transparent version of secondaryLight */

        }

        &:last-child {
            border-radius: 0 0 ${borderRadius.medium} ${borderRadius.medium} ;
        }

    `,

    StyledNavItem: styled.div`
        text-indent: ${spacing.xs};
        padding: ${spacing.md} ${spacing.lg};
        border-radius: ${borderRadius.medium};
        cursor: pointer;
        color: ${colors.text};
        font-weight: bold;
        text-align: left;
        display: flex;
        align-items: center;
        transition: all 0.25s;

        &:hover, &:active {
            background-color: ${colors.primary};
            color: white;
            backdrop-filter: blur(10px);

        }
    `,

    NavItemContent: styled.div`
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 1rem;
    `,

    IconWrapper: styled.span`
        display: flex;
        align-items: center;
        font-size: 1rem;

        &hover, svg {
            filter: drop-shadow(0 0 4px ${colors.offWhite});
        }

    `,
};


const settingsStyles = {
    ContainerHero: styled.div`
        align-items: center;
        background: rgba(0, 0, 0, 0.85);
        color: ${colors.white};
        padding: ${spacing.xl};
        box-shadow: ${shadows.small};
        margin: 0px;
        backdrop-filter: blur(5px);
    `,

    Container: styled.div`
        max-width: 1200px;
        margin: 0 auto;
        padding: ${spacing.xl};
    `,

    FormContainer: styled.div`
        max-width: 1200px;
        margin: 0 auto;
        padding: ${spacing.xl};
        align-items: center;
    `,

    Header: styled.header`
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: ${spacing.lg};
        padding-bottom: ${spacing.md};
        border-bottom: 1px solid ${colors.border};
    `,

    Section: styled.div`
        align-items: center;
        border: 1px solid ${colors.border};
        background: ${colors.offWhite};
        padding: ${spacing.xl};
        margin: 0px;
    `,

    TitleSection: styled.div`
        align-items: center;
        border-left: 1px solid ${colors.border};
        border-right: 1px solid ${colors.border};
        background: ${colors.offWhite};
        padding: ${spacing.xl};
        margin: 0px;
    `,

    FormGroup: styled.div`
        margin-bottom: ${spacing.lg};
        width: 100%;
    `,

    List: styled.div`
        display: flex;
        gap: ${spacing.md};
    `,

    Label: styled.label`
        display: block;
        margin-bottom: ${spacing.sm};
        font-weight: 500;
        color: ${colors.textDark};
    `,

    Input: styled.input`
        width: 100%;
        padding: ${spacing.md};
        border: 1px solid ${colors.border};
        border-radius: ${borderRadius.small};
        font-size: 1rem;
        transition: border-color 0.2s, box-shadow 0.2s;

        &:focus {
            outline: none;
            border-color: ${colors.primary};
            box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
        }
    `,

    StaticValue: styled.div`
        padding: ${spacing.sm};
        background-color: ${colors.lightGray};
        border-radius: ${borderRadius.small};
        color: ${colors.textMedium};
    `,

    LogoutButton: styled.button`
        padding: ${spacing.xs} ${spacing.md};
        background-color: ${colors.primary};
        color: ${colors.white};
        border: none;
        border-radius: ${borderRadius.small};
        cursor: pointer;

        &:hover {
            background-color: ${colors.dangerDark};
        }
    `,

    UserInfo: styled.div`
        background-color: ${colors.white};
        padding: ${spacing.xl};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.small};
    `,

    Button: styled.button`
        background: ${colors.primary};
        color: white;
        border: none;
        margin-right: ${spacing.md};
        padding: ${spacing.sm} ${spacing.md};
        border-radius: 10px;
        font-size: 1rem;
        cursor: pointer;
        transition: background-color 0.3s;
        margin-top: ${spacing.md};

        &:hover {
            background: ${colors.primaryDark};
        }

        &:disabled {
            background: ${colors.disabled};
            cursor: not-allowed;
        }
    `,

    EditButton: styled.button`
        background: ${colors.primarySuperLight};
        color: white;
        border: none;
        padding: ${spacing.sm} ${spacing.md};
        border-radius: 10px;
        font-size: 1rem;
        cursor: pointer;
        transition: background-color 0.3s;
        margin-top: ${spacing.md};

        &:hover {
            background: ${colors.primaryDark};
        }

        &:disabled {
            background: ${colors.disabled};
            cursor: not-allowed;
        }
    `,

    LoginButton: styled.button`
        background: ${colors.primary};
        color: ${colors.white};
        border: none;
        padding: ${spacing.sm} ${spacing.md};
        border-radius: 10px;
        font-size: 1rem;
        cursor: pointer;
        transition: background-color 0.3s;

        &:hover {
            background: ${colors.primaryDark};
        }
    `,
};

const mapStyles = {
    MapContainer: styled.div`
        width: 100%;
        height: 100%;
        margin: 0;
        padding: 0;
        overflow: hidden;
    `,


    MapControls: styled.div`
        position: absolute;
        top: ${spacing.md};
        right: ${spacing.md};
        z-index: 1;
        display: flex;
        flex-direction: column;
        gap: ${spacing.xs};
    `
}

const gaugeStyles = {
    GaugeContainer: styled.div`
        position: relative;
        width: ${props => props.$size}px;
        display: flex;
        flex-direction: column;
        align-items: center;
    `,

    GaugeCircle: styled.div`
        position: relative;
        width: ${props => props.$size}px;
        height: ${props => props.$size}px;
    `,

    GaugeTrack: styled.div`
        position: absolute;
        top: 10%;
        left: 10%;
        width: 80%;
        height: 80%;
        border-radius: 50%;
        background: ${props => props.$trackColor || '#f0f0f0'};
    `,

    GaugeFill: styled.div`
        position: absolute;
        top: 10%;
        left: 10%;
        width: 80%;
        height: 80%;
        border-radius: 50%;
        background: ${props => {
            const degrees = props.$percentage * 3.6;
            if (props.$fillGradient) {
                // Use the gradient colors but mask them with another conic gradient
                return `conic-gradient(
                from 0deg,
                transparent ${degrees}deg,
                rgba(255,255,255,0.7) ${degrees}deg
            ), conic-gradient(${props.$fillGradient})`;
            } else {
                return `conic-gradient(${props.$fillColor || '#007aff'} 0deg ${degrees}deg, transparent ${degrees}deg 360deg)`;
            }
        }};
        transition: all 0.5s ease-in-out;
    `,

    GaugeInnerCircle: styled.div`
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: ${props => props.$size * 0.6}px;
        height: ${props => props.$size * 0.6}px;
        border-radius: 50%;
        background: ${props => props.$innerColor || '#fff'};
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
    `,

    GaugeValue: styled.div`
        font-size: ${props => props.$size * 0.2}px;
        font-weight: bold;
        color: ${props => props.$valueColor || colors.textDark};
    `,

    GaugeLabel: styled.div`
        font-size: ${props => props.$size * 0.12}px;
        color: ${props => props.$labelColor || colors.textDark};
        margin-top: ${spacing.sm};
        text-align: center;
        width: 100%;
        font-weight: 500;
    `
}

const resultStyles = {
    Container: styled.div`
        background-color: ${props => props.$isFullScreen ? 'rgba(255, 255, 255, 0.95)' : colors.white};
        border-radius: ${borderRadius.medium} ${borderRadius.medium} 0 0;
        box-shadow: ${shadows.small};
        transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
        padding: ${props => props.$isFullScreen ? spacing.lg : spacing.md};
        position: relative;
        width: 100%;
        min-width: 450px;
        height: auto;
        max-height: ${props => props.$isFullScreen ? '90vh' : '180px'};
        z-index: 1;
        overflow-y: visible;
        overflow-x: visible;
        display: flex;
        flex-direction: column;
        align-items: center;
        backdrop-filter: ${props => props.$isFullScreen ? 'blur(10px)' : 'none'};
        margin-top: 25px;
    `,


    Header: styled.div`
        display: flex;
        justify-content: center;
        align-items: center;
        margin-bottom: ${spacing.md};
        border-bottom: 1px solid ${colors.border};
        padding-bottom: ${spacing.xs};
        width: 100%;
    `,

    Title: styled.h2`
        margin: 0;
        font-size: ${props => props.$isFullScreen ? '1.8rem' : '1.5rem'};
        color: ${colors.textDark};
        text-align: center;
    `,


    GaugeGrid: styled.div`
        display: flex;
        flex-wrap: ${props => props.$isFullScreen ? 'wrap' : 'nowrap'};
        justify-content: center;
        gap: ${props => props.$isFullScreen ? spacing.xl : spacing.md};
        flex: 1;
        overflow-y: auto;
        overflow-x: hidden;
        width: 100%;
        padding: ${props => props.$isFullScreen ? spacing.md : '0'};
        transition: all 1s cubic-bezier(0.16, 1, 0.3, 1);
    `,


    GaugeItem: styled.div`
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: ${spacing.md};
        min-width: ${props => props.$isFullScreen ? '150px' : 'auto'};
        max-width: ${props => props.$isFullScreen ? '350px' : 'none'};
    `,

    ExpandButtonWrapper: styled.div`
        position: absolute;
        top: -20px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 10;
    `,

    ExpandButton: styled.button`
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: ${colors.primary};
        color: white;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 20px;
        font-weight: bold;
        box-shadow: 0 2px 5px rgba(0,0,0,0.2);

        &:hover {
            background: ${colors.primarySuperLight};
        }
    `,
    ContentSection: styled.div`
        width: 100%;
        margin-top: ${spacing.lg};
        padding: ${props => props.$isFullScreen ? spacing.md : '0'};
        overflow-y: auto;
        max-height: ${props => props.$isFullScreen ? '300px' : '0'};
        opacity: ${props => props.$isFullScreen ? '1' : '0'};
        visibility: ${props => props.$isFullScreen ? 'visible' : 'hidden'};
        transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
        border-top: ${props => props.$isFullScreen ? `1px solid ${colors.border}` : 'none'};
    `,
};

const assessStyles = {
    Container: styled.div`
        border: 1px solid ${colors.border};
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
        position: relative;
    `,

    SideBySideContainer: styled.div`
        display: flex;
        width: 100%;
        height: 100%;

        & > div {
            flex: 1;
            height: 100%;
        }
    `,

    MapWrapper: styled.div`
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        filter: ${props => props.$isFullScreen ? 'blur(3px) brightness(0.8)' : 'none'};
        transition: filter 1s cubic-bezier(0.16, 1, 0.3, 1);
    `,

    ResultBarWrapper: styled.div`
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: 10;
        display: flex;
        justify-content: center;
        transition: all 0.3s ease;
        overflow: visible;
    `,

    ResultBarInner: styled.div`
        width: ${props => props.$isFullScreen ? '90%' : '50%'};
        max-width: ${props => props.$isFullScreen ? '1600px' : '800px'};
        padding-top: 20px;
        overflow: visible;
        position: relative;
        z-index: 5;
        display: flex;
        justify-content: center;
        transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    `,

    SideBySideResults: styled.div`
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        display: flex;
        justify-content: space-between;
        padding: 0 ${spacing.md};
        gap: ${spacing.md};
        z-index: 10;

        & > div {
            width: calc(50% - ${spacing.md} / 2);
            display: flex;
            justify-content: center;
            align-items: flex-end;
            position: relative;
        }
    `,
};

const searchBarStyles = {
    SearchBarContainer: styled.div`
        width: 100%;
        max-width: 600px;
        margin: 0 auto;
        padding: ${spacing.sm} ${spacing.md};
    `,

    SearchInputWrapper: styled.div`
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
        background-color: ${colors.white};
        border: 1px solid ${props => props.$hasResults ? "transparent" : colors.border};
        border-radius: 50px;
        transition: border-color 0.2s ease-in;
        box-sizing: border-box;

        &:focus-within {
            border-color: ${colors.primary};
            box-shadow: ${props => props.$hasResults ? "none" : "0 0 0 2px rgba(0, 0, 0, 0.1)"};
        }
    `,

    SearchIconWrapper: styled.div`
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 ${spacing.sm};
        color: ${colors.textMedium};
    `,

    SearchInput: styled.input`
        flex: 1;
        height: 40px;
        padding: ${spacing.xs} ${spacing.sm};
        border: none;
        outline: none;
        font-size: 1rem;
        background: transparent;

        &::placeholder {
            color: ${colors.secondary};
        }
    `,

    ClearButtonContainer: styled.div`
        margin-right: .60rem;
    `,

    ClearButton: styled.button`
        background: transparent;
        padding-left: 25px;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        padding: 0 8px;
        color: ${colors.textMedium};
        transition: color 0.2s ease;
        height: 100%;
        border-radius: 0 50px 50px 0;
        
        &:hover {
            color: ${colors.dangerDark};
        }
    `,

    SearchBarWrapper: styled.div`
        position: absolute;
        top: 15px;
        left: 15px;
        right: 15px;
        z-index: 1000;
    `,

    SearchResultsWrapper: styled.div`
        width: 100%;
        margin-top: calc((40px + ${spacing.xs}) / -2);
        background-color: ${colors.white};
        padding-top: ${props => props.$numResults > 0 ? "calc((40px + " + spacing.xs + ") / 2)" : "0px"};
        border-radius: 0 0 ${borderRadius.medium} ${borderRadius.medium};
        max-height: ${props => props.$numResults * 37.33}px;
        transition: all 0.2s ease-in-out;
        overflow-y: clip;
    `,

    SearchResult: styled.div`
        padding: ${spacing.xs} ${spacing.sm};
        border: none;
        outline: none;
        text-align: left;
        background-color: ${colors.white};
        cursor: pointer;

        &:hover {
            background-color: ${colors.lightGray};
        }

        &:last-child {
            border-radius: 0 0 ${borderRadius.medium} ${borderRadius.medium};
        }
    `,
};

const appStyles = {
    colors,
    spacing,
    shadows,
    borderRadius,
    login: loginStyles,
    dashboard: dashboardStyles,
    common: commonStyles,
    layout: layoutStyles,
    settings: settingsStyles,
    map: mapStyles,
    gauge: gaugeStyles,
    results: resultStyles,
    assess: assessStyles,
    search: searchBarStyles,
};


export default appStyles;