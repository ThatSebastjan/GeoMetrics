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
    warning: '#dd6b20',
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
    large: '0 7px 9px rgba(0, 0, 0, 0.1)',
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
        background: linear-gradient(135deg, ${colors.secondary} 0%, ${colors.white} 100%);
    `,

    FormWrapper: styled.div`
        width: 100%;
        max-width: 600px;
        padding: ${spacing.xl} ${spacing.xl} ${spacing.xl};
        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08), 0 5px 10px rgba(0, 0, 0, 0.05);
        position: relative;
    `,

    LogoSection: styled.div`
        text-align: center;
        margin-bottom: ${spacing.xl};
    `,

    LogoText: styled.h1`
        font-size: 2.5rem;
        font-weight: 700;
        color: ${colors.primary};
        margin: 0;
        letter-spacing: -0.5px;
    `,

    AppDescription: styled.p`
        color: ${colors.textMedium};
        font-size: 1.1rem;
        margin: ${spacing.xs} 0 0;
        padding-bottom: ${spacing.xl};
    `,

    FormTitle: styled.h2`
        text-align: center;
        margin-bottom: ${spacing.lg};
        color: ${colors.textDark};
        font-weight: 600;
        font-size: 1.5rem;
    `,

    FormGroup: styled.div`
        margin-bottom: ${spacing.lg};
        text-align: left;
        width: 60%;
        margin-left: auto;
        margin-right: auto;
    `,

    Label: styled.label`
        display: block;
        margin-bottom: ${spacing.xs};
        font-weight: 500;
        color: ${colors.textDark};
        font-size: 0.95rem;
    `,

    Input: styled.input`
        width: 100%;
        padding: ${spacing.md};
        border: 1px solid ${colors.border};
        border-radius: ${borderRadius.small};
        font-size: 1rem;
        transition: all 0.2s;
        height: 48px;
        box-sizing: border-box;

        &:focus {
            outline: none;
            border-color: ${colors.primary};
            box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05);
        }
    `,

    SubmitButton: styled.button`
        width: 40%;
        padding: ${spacing.md};
        background-color: ${colors.primary};
        color: ${colors.white};
        border: none;
        border-radius: ${borderRadius.small};
        font-size: 1.1rem;
        font-weight: 600;
        cursor: pointer;
        margin-top: ${spacing.xl};
        transition: background-color 0.2s;
        height: 50px;
        display: block;
        margin-left: auto;
        margin-right: auto;

        &:hover {
            background-color: ${colors.primaryLight};
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
        font-size: 0.9rem;
        text-align: center;
    `,

    FormSwitch: styled.div`
        margin-top: ${spacing.xl};
        text-align: center;
        color: ${colors.textMedium};

        p {
            margin-bottom: ${spacing.xs};
            font-size: 0.95rem;
        }
    `,

    SwitchButton: styled.button`
        background: none;
        border: none;
        color: ${colors.primary};
        cursor: pointer;
        font-size: 1rem;
							
        font-weight: 500;
        text-decoration: none;
        &:hover {
            text-decoration: underline;
        }
    `,

    BackButton: styled.button`
        position: absolute;
        top: 20px;
        left: 20px;
        background: none;
        border: none;
        color: ${colors.primary};
        font-size: 16px;
        cursor: pointer;
        display: flex;
        align-items: center;
        padding: 8px 12px;
        border-radius: 4px;
        border: 1px solid rgba(0, 0, 0, 0.25);
        transition: all 0.2s;

        &:hover {
            background-color: rgba(0, 0, 0, 0.05);
            border: 1px solid rgba(0, 0, 0, 0.05);
            
        }
    `,
};

const commonStyles = {

    HeroUserName: styled.h1`
        font-size: 4rem;
        padding: 0px;
        margin-bottom: 0px;
    `,

    HeroTitle: styled.h1`
        font-size: 2.5rem;
        padding: 0px;
        margin-bottom: 0px;
    `,

    PageTitle: styled.h1`
        text-align: center;
        margin: 0 0 ${spacing.md} 0;
        color: ${colors.textDark};
        font-size: 1.8rem;
        font-weight: 600;
        text-align: left;
    `,

    PageSubtitle: styled.h1`
        text-align: center;
        margin: 0 0 ${spacing.md} 0;
        color: ${colors.secondary};
        font-size: 1.3rem;
        font-weight: 500;
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

    Button: styled.button`
        background: ${props => props.$secondary ? colors.secondaryLight: colors.primary};
            //color: ${props => props.$secondary ? colors.primary : colors.white};
        color: white;
        border: none;
        padding: ${spacing.sm} ${spacing.md};
        border-radius: ${borderRadius.medium};
        margin-right: 10px;
        cursor: pointer;
        box-shadow: ${shadows.medium};
        transition: all 0.3s;

        &:hover {
            background: ${props => props.$secondary ? colors.secondary : colors.primarySuperLight};
            box-shadow: ${shadows.large};
        }

        &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
        }
    `,
    RiskPill: styled.div`
        position: absolute;
        right: 20px;
        top: 50%;
        transform: translateY(-50%);
        display: flex;
        flex-direction: column;
        background-color: rgba(255, 255, 255, 0.9);
        border-radius: 30px;
        padding: 10px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
        z-index: 10;
    `,

    RiskButton: styled.button`
        width: 40px;
        height: 40px;
        border-radius: 50%;
        border: none;
        margin: 5px 0;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        cursor: pointer;
            background-color: ${props => props.$isActive ? props.$customColor || '#3498db' : 'white'};
        color: ${props => props.$isActive ? 'white' : '#333'};
        transition: all 0.2s ease;
            box-shadow: ${props => props.$isActive ? 
                `0 0 10px ${props.$customColor ? props.$customColor.replace(')', ', 0.7)').replace('rgb', 'rgba') : 'rgba(52, 152, 219, 0.7)'}` : 
                '0 1px 3px rgba(0, 0, 0, 0.1)'};

        &:hover {
            transform: scale(1.1);
        }
    `
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
        width: ;
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

    SingleNavItemWithIcon: styled.div`
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
        background-color: ${colors.white};
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
        background: rgba(101, 101, 101, 0.65);
        color: ${colors.white};
        padding: ${spacing.xl};
        box-shadow: ${shadows.small};
        margin: 0px;
        backdrop-filter: blur(20px) saturate(180%);
        -webkit-backdrop-filter: blur(20px) saturate(180%);
        position: relative;
        overflow: hidden;
        border: 1px solid rgba(255, 255, 255, 0.08);

        &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='4' height='4' viewBox='0 0 4 4'%3E%3Cpath fill='%23ffffff' fill-opacity='0.03' d='M1 3h1v1H1V3zm2-2h1v1H3V1z'%3E%3C/path%3E%3C/svg%3E");
            pointer-events: none;
            z-index: -1;
        }

        &::after {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(120deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.02) 70%);
            pointer-events: none;
            z-index: -1;
        }
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
        background: ${colors.secondary};
        color: white;
        border: none;
        margin-right: ${spacing.md};
        padding: ${spacing.sm} ${spacing.md};
        border-radius: 10px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.3s;
        margin-top: ${spacing.md};
        box-shadow: ${shadows.medium};

        &:hover {
            box-shadow: ${shadows.large};
            background: ${colors.primaryDark};
        }

        &:disabled {
            background: ${colors.disabled};
            cursor: not-allowed;
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

    EditButton: styled.label`
        background: ${colors.primary};
        color: white;
        border: none;
        padding: ${spacing.sm} ${spacing.md};
        border-radius: 10px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.3s;
        margin-top: ${spacing.md};
        box-shadow: ${shadows.medium};

        &:hover {
            box-shadow: ${shadows.large};
            background: ${colors.primaryLight};
        }

        &:disabled {
            background: ${colors.danger};
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
        max-height: ${props => props.$isFullScreen ? '95vh' : '180px'};
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

    // Add these to resultStyles object
    ResultsGrid: styled.div`
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: ${spacing.lg};
        width: 100%;
    `,

    ResultCard: styled.div`
        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.small};
        overflow: hidden;
        transition: transform 0.2s, box-shadow 0.2s;

        &:hover {
            transform: scale(1.01);
            box-shadow: ${shadows.large};
            z-index: 1;
        }
    `,

    ResultCardHeader: styled.div`
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: ${spacing.md};
        border-bottom: 1px solid ${colors.border};
    `,

    ResultTitle: styled.h3`
        margin: 0;
        font-size: 1.2rem;
        color: ${colors.textDark};
    `,

    ScoreBadge: styled.div`
        background-color: ${props => {
            const score = props.score;
            if (score >= 90) return colors.success;
            if (score >= 70) return 'orange';
            return colors.danger;
        }};
        color: white;
        font-weight: bold;
        padding: ${spacing.xs} ${spacing.sm};
        border-radius: ${borderRadius.small};
    `,

    ResultAddress: styled.div`
        display: flex;
        align-items: center;
        gap: ${spacing.xs};
        padding: ${spacing.sm} ${spacing.md};
        font-size: 0.9rem;
        color: ${colors.textMedium};
    `,

    MapContainer: styled.div`
        width: 100%;
        height: 200px;
        border-top: 1px solid ${colors.border};
        border-bottom: 1px solid ${colors.border};
        overflow: hidden;
    `,

    ResultSummary: styled.div`
        padding: ${spacing.md};
    `,

    SummaryTitle: styled.h4`
        margin: 0 0 ${spacing.xs} 0;
        color: ${colors.textDark};
    `,

    ResultActions: styled.div`
        display: flex;
        justify-content: center;
        padding: ${spacing.md};
        border-top: 1px solid ${colors.border};
    `,

    // Add these to resultStyles object

// Detail page layout
    DetailGrid: styled.div`
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: ${spacing.lg};
        width: 100%;

        @media (max-width: 1024px) {
            grid-template-columns: 1fr;
        }
    `,

    DetailMainSection: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.md};
    `,

    DetailSidebar: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.md};
    `,

// Cards
    DetailCard: styled.div`
        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.small};
        overflow: hidden;
        margin-bottom: ${spacing.md};
    `,

    DetailCardHeader: styled.div`
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: ${spacing.md};
        border-bottom: 1px solid ${colors.border};
        background-color: ${colors.lightGray};
    `,

    DetailCardTitle: styled.h3`
        margin: 0;
        font-size: 1.2rem;
        color: ${colors.textDark};
        font-weight: 600;
    `,

    DetailCardContent: styled.div`
        padding: ${spacing.md};
    `,

// Risk metrics
    RiskMetricsGrid: styled.div`
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: ${spacing.md};
        margin-top: ${spacing.lg};
    `,

    RiskMetricCard: styled.div`
        background-color: ${props => {
            const risk = props.risk;
            if (risk < 30) return 'rgba(56, 161, 105, 0.1)';
            if (risk < 70) return 'rgba(221, 107, 32, 0.1)';
            return 'rgba(229, 62, 62, 0.1)';
        }};
        padding: ${spacing.md};
        border-radius: ${borderRadius.small};
        position: relative;
    `,

    RiskMetricTitle: styled.h4`
        margin: 0 0 ${spacing.xs} 0;
        font-size: 0.9rem;
        color: ${colors.textDark};
        font-weight: 500;
    `,

    RiskMetricValue: styled.div`
        font-size: 1.8rem;
        font-weight: bold;
        margin-bottom: ${spacing.sm};
    `,

    RiskMetricBar: styled.div`
        height: 6px;
        width: 100%;
        background-color: rgba(255, 255, 255, 0.6);
        border-radius: 3px;
        overflow: hidden;
        position: relative;

        &:after {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            height: 100%;
            width: ${props => props.risk}%;
            background-color: ${props => {
                const risk = props.risk;
                if (risk < 30) return colors.success;
                if (risk < 70) return '#dd6b20';
                return colors.danger;
            }};
            border-radius: 3px;
        }
    `,

// Map
    MapDetailContainer: styled.div`
        width: 100%;
        height: 250px;
        border-bottom: 1px solid ${colors.border};
        overflow: hidden;
    `,

    AddressDetail: styled.div`
        display: flex;
        align-items: center;
        gap: ${spacing.xs};
        font-size: 1rem;
        color: ${colors.textMedium};
        padding: ${spacing.sm} 0;
    `,

// Detail sections
    DetailSection: styled.div`
        margin-bottom: ${spacing.lg};

        &:last-child {
            margin-bottom: 0;
        }
    `,

    DetailSectionTitle: styled.h4`
        margin: 0 0 ${spacing.sm} 0;
        font-size: 1.1rem;
        color: ${colors.textDark};
        font-weight: 500;
    `,

// Property info
    PropertyInfoList: styled.ul`
        list-style: none;
        padding: 0;
        margin: 0;
    `,

    PropertyInfoItem: styled.li`
        display: flex;
        padding: ${spacing.xs} 0;
        border-bottom: 1px solid ${colors.border};

        &:last-child {
            border-bottom: none;
        }
    `,

    PropertyInfoLabel: styled.span`
        font-weight: 500;
        flex: 1;
        color: ${colors.textDark};
    `,

    PropertyInfoValue: styled.span`
        flex: 2;
        color: ${colors.textMedium};
    `,

// Actions
    ActionButtonsContainer: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.sm};

        & > button {
            width: 100%;
        }
    `,

// Navigation
    BackButton: styled.button`
        background: transparent;
        border: none;
        display: flex;
        align-items: center;
        gap: ${spacing.xs};
        color: ${colors.white};
        font-size: 1rem;
        cursor: pointer;
        padding: ${spacing.xs} ${spacing.sm};
        margin-right: ${spacing.md};
        border-radius: ${borderRadius.small};

        &:hover {
            background: rgba(255, 255, 255, 0.1);
        }

        svg {
            width: 16px;
            height: 16px;
        }
    `,
    AssessmentSummary: styled.div`
        padding: ${spacing.sm};
        background: ${colors.offWhite};`

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
        max-height: 95vh;
    `,

    AdvancedBarWrapper: styled.div`
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
        max-height: ${props => Math.min(props.$numResults, 8) * 37.33}px;
        transition: all 0.2s ease-in-out;
        overflow-y: auto;
    `,

    SearchResult: styled.div`
        padding: ${spacing.xs} ${spacing.sm};
        border: none;
        outline: none;
        text-align: left;
        background-color: ${colors.white};
        cursor: pointer;
        display: flex;
        align-items: center;
        flex-direction: row;

        &:hover {
            background-color: ${colors.lightGray};
        }

        &:last-child {
            border-radius: 0 0 ${borderRadius.medium} ${borderRadius.medium};
        }
    `,

    SearchResultInfo: styled.div`
        color: ${colors.textMedium};
        font-size: 0.8rem;
        display: inline;
        padding-left: 1rem;
    `,
};


const advancedStyles = {
    Container: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.xl};
        padding: ${spacing.lg};
        width: 90%;
        max-width: 1200px;
        margin: 0 auto;
    `,

    HeaderSection: styled.div`
        text-align: center;
        padding: ${spacing.md};
        margin-bottom: ${spacing.sm};
    `,

    Title: styled.h2`
        font-size: ${props => props.$isFullScreen ? '1.8rem' : '1.5rem'};
        color: ${colors.textDark};
        margin: 0 0 ${spacing.xs} 0;
    `,

    Subtitle: styled.p`
        color: ${colors.textMedium};
        max-width: 600px;
        margin: 0 auto;
    `,

    TwoColumnLayout: styled.div`
        display: grid;
        grid-template-columns: ${props => props.$isFullScreen ? '1fr 1fr' : '1fr'};
        gap: 30px;
    `,

    SettingsPanel: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.lg};
        background-color: ${colors.lightGray};
        padding: ${spacing.lg};
        border-radius: ${borderRadius.medium};
    `,

    PanelHeading: styled.h3`
        margin: 0 0 ${spacing.sm} 0;
        font-size: 1.2rem;
        color: ${colors.primary};
        border-bottom: 2px solid ${colors.primary};
        padding-bottom: ${spacing.xs};
    `,

    SettingGroup: styled.div`
        margin-bottom: ${spacing.md};
    `,

    SettingLabel: styled.label`
        display: block;
        margin-bottom: ${spacing.sm};
        font-weight: 600;
        color: ${colors.textDark};
    `,

    RangeControl: styled.div`
        display: flex;
        align-items: center;
        gap: ${spacing.sm};
    `,

    RangeLabel: styled.span`
        min-width: 40px;
        text-align: center;
    `,

    RangeSlider: styled.input`
        width: 100%;
        height: 8px;
        appearance: none;
        background: ${props => `linear-gradient(to right, 
            ${colors.primary} 0%, 
            ${colors.primary} ${props.$percentage}%, 
            #e0e0e0 ${props.$percentage}%, 
            #e0e0e0 100%)`};
        border-radius: 4px;
        cursor: pointer;

        &::-webkit-slider-thumb {
            appearance: none;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: ${colors.primary};
            cursor: pointer;
        }

        &::-moz-range-thumb {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: ${colors.primary};
            border: none;
            cursor: pointer;
        }
    `,

    OptionGroup: styled.div`
        display: flex;
        gap: ${spacing.sm};
        flex-wrap: wrap;
    `,

    Option: styled.div`
        flex: 1;
        min-width: 80px;
        padding: ${spacing.sm};
        text-align: center;
        background-color: ${props => props.$isSelected ? colors.primary : colors.white};
        color: ${props => props.$isSelected ? colors.white : colors.textMedium};
        border-radius: ${borderRadius.small};
        cursor: pointer;
        font-weight: ${props => props.$isSelected ? '600' : 'normal'};
        transition: all 0.2s ease;
    `,

    OptionDescription: styled.div`
        font-size: 0.85rem;
        margin-top: ${spacing.xs};
        color: ${colors.textMedium};
    `,

    CheckboxGroup: styled.div`
        display: flex;
        gap: ${spacing.lg};
        margin-top: ${spacing.sm};
    `,

    CheckboxOption: styled.label`
        display: flex;
        align-items: center;
        gap: ${spacing.xs};
        cursor: pointer;
        padding: ${spacing.xs};
        border-radius: ${borderRadius.small};
        background-color: ${props => props.$isSelected ? colors.primarySuperLight : 'transparent'};

        input {
            accent-color: ${colors.primary};
            width: 18px;
            height: 18px;
            cursor: pointer;
        }
    `,

    RiskPanel: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.lg};
    `,

    FloodPanel: styled.div`
        background-color: #e6f7ff;
        padding: ${spacing.lg};
        border-radius: ${borderRadius.medium};
        border: 1px solid #bae7ff;
    `,

    LandslidePanel: styled.div`
        background-color: #f9f0ff;
        padding: ${spacing.lg};
        border-radius: ${borderRadius.medium};
        border: 1px solid #d3adf7;
    `,

    RiskPanelHeading: styled.h3`
        margin: 0 0 ${spacing.md} 0;
        font-size: 1.2rem;
        padding-bottom: ${spacing.xs};
        display: flex;
        align-items: center;
        gap: ${spacing.xs};
        border-bottom: 2px solid;

        ${props => props.$type === 'flood' ? `
            color: #0066cc;
            border-bottom-color: #0066cc;
        ` : `
            color: #722ed1;
            border-bottom-color: #722ed1;
        `}
    `,

    RiskIcon: styled.span`
        font-size: 1.2em;
    `,

    WeightControl: styled.div`
        display: flex;
        justify-content: space-between;
        margin-bottom: ${spacing.xs};
        align-items: center;
    `,

    WeightLabel: styled.label`
        font-weight: 500;
    `,

    FloodWeightBadge: styled.span`
        font-weight: 600;
        background-color: #0066cc;
        color: white;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 0.85rem;
    `,

    LandslideWeightBadge: styled.span`
        font-weight: 600;
        background-color: #722ed1;
        color: white;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 0.85rem;
    `,

    FloodWeightSlider: styled.input`
        width: 100%;
        height: 8px;
        appearance: none;
        background: ${props => `linear-gradient(to right, #0066cc 0%, #0066cc ${props.$percentage}%, #e0e0e0 ${props.$percentage}%, #e0e0e0 100%)`};
        border-radius: 4px;

        &::-webkit-slider-thumb {
            appearance: none;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #0066cc;
            cursor: pointer;
        }

        &::-moz-range-thumb {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #0066cc;
            cursor: pointer;
            border: none;
        }
    `,

    LandslideWeightSlider: styled.input`
        width: 100%;
        height: 8px;
        appearance: none;
        background: ${props => `linear-gradient(to right, #722ed1 0%, #722ed1 ${props.$percentage}%, #e0e0e0 ${props.$percentage}%, #e0e0e0 100%)`};
        border-radius: 4px;

        &::-webkit-slider-thumb {
            appearance: none;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #722ed1;
            cursor: pointer;
        }

        &::-moz-range-thumb {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #722ed1;
            cursor: pointer;
            border: none;
        }
    `,

    WeightGroup: styled.div`
        display: flex;
        flex-direction: column;
        gap: ${spacing.md};
    `,

    WeightNote: styled.div`
        font-size: 0.85rem;
        margin-top: ${spacing.md};
        padding: ${spacing.sm};
        background-color: rgba(255, 255, 255, 0.7);
        border-radius: ${borderRadius.small};
    `,

    ButtonGroup: styled.div`
        display: flex;
        justify-content: center;
        margin-top: ${spacing.lg};
        padding: ${spacing.md};
        border-top: 1px solid ${colors.border};

        .update-button {
            padding: 12px 24px;
            font-size: 1.1rem;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
    `,
};

// Add this new object to styles.js before the final export
const savedStyles = {
    LotsGrid: styled.div`
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: ${spacing.lg};
        width: 100%;
    `,

    LotCard: styled.div`
        display: flex;
        flex-direction: column;
        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.medium};
        overflow: hidden;
        transition: transform 0.2s ease, box-shadow 0.3s ease;
        height: 100%;
        position: relative;
        transform-origin: center;

        &:hover {
            transform: scale(1.02);
            box-shadow: ${shadows.large};
            z-index: 1;
        }
    `,

    MapContainer: styled.div`
        width: 100%;
        height: 220px;
        position: relative;
        overflow: hidden;
    `,

    LotInfo: styled.div`
        padding: ${spacing.md};
        display: flex;
        flex-direction: column;
        flex-grow: 1;
        justify-content: space-evenly;
        align-items: center;
    `,

    LotTitle: styled.h3`
        margin: 0 0 ${spacing.xs} 0;
        font-size: 1.2rem;
        font-weight: 600;
        color: ${colors.textDark};
    `,

    LotAddress: styled.div`
        color: ${colors.textMedium};
        font-size: 0.9rem;
        margin-bottom: ${spacing.sm};
    `,

    RiskScores: styled.div`
        display: flex;
        gap: ${spacing.xs};
        flex-wrap: wrap;
        margin-bottom: ${spacing.md};
    `,

    RiskBadge: styled.span`
        padding: ${spacing.xs} ${spacing.sm};
        border-radius: ${borderRadius.small};
        font-size: 0.8rem;
        font-weight: 500;
        background-color: ${props => {
            const risk = props.$risk;
            if (risk < 30) return 'rgba(56, 161, 105, 0.2)';
            if (risk < 70) return 'rgba(221, 107, 32, 0.2)';
            return 'rgba(229, 62, 62, 0.2)';
        }};
        color: ${props => {
            const risk = props.$risk;
            if (risk < 30) return '#2f855a';
            if (risk < 70) return '#c05621';
            return '#c53030';
        }};
    `,

    ButtonGroup: styled.div`
        display: flex;
        gap: ${spacing.sm};
        flex-wrap: wrap;
        padding: 0;
        margin: 0;

        button {
            flex: 1;
            padding: ${spacing.xs} ${spacing.sm};
            font-size: 0.9rem;
        }
    `
};


const contextMenuStyles = {

    ContextMenu: styled.div`
        position: absolute;
        z-index: 1000000;
        top: ${props => props.$top}px;
        left: ${props => props.$left}px;
        padding: 0px;
        border: 1px solid #cacaca;
        border-radius: ${borderRadius.medium};
        display: flex;
        flex-direction: column;
        flex-wrap: nowrap;
        align-items: flex-start;
        width: 220px;
        overflow: hidden;
        background-color: #ffffff;
    `,

    Header: styled.div`
        width: 100%;
        border-bottom: 1px solid #cacaca;
        background-color: #f0f0f0;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
        padding: 6px 10px;
        box-sizing: border-box;
        position: relative;
        font-weight: 700;
    `,

    Close: styled.div`
        margin: 0px;
        padding: 0px;
        display: flex;
        transition: all 0.3s;
        border-radius: 50%;
        padding: 3px;
        box-sizing: border-box;
        cursor: pointer;

        &:hover {
            background-color: #e0e0e0;
        }
    `,

    Item: styled.div`
        border-bottom: 1px solid #cacaca;
        width: 100%;
        box-sizing: border-box;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        cursor: pointer;

        &:last-child {
            border-bottom: none;
        }
    `,

    ItemInline: styled.div`
        width: 50%;
        text-align: center;
    `,

    Button: styled.div`
        padding: 8px 10px;
        white-space: nowrap;
        width: 100%;
        transition: all 0.3s;
        text-align: left;

        &:hover {
            background-color: #e0e0e0;
        }
    `,
};


const popupStyles = {
    PopupElement: styled.div`
        position: fixed;
        z-index: 1000000;
        left: 50%;
        top: 50%;
        width: 500px;
        transform: translate(-50%, -50%);

        background-color: ${colors.white};
        border-radius: ${borderRadius.medium};
        box-shadow: ${shadows.small};
        overflow: hidden;
        transition: transform 0.2s, box-shadow 0.2s;

        &:hover {
            box-shadow: ${shadows.large};
        }
    `,

    PopupHeader: styled.div`
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: ${spacing.md};
        border-bottom: 1px solid ${colors.border};
    `,

    PopupTitle: styled.h3`
        margin: 0;
        font-size: 1.2rem;
        color: ${colors.textDark};
    `,

    
    PopupContent: styled.div`
        padding: ${spacing.md};
        text-align: left;
    `,

    PopupActions: styled.div`
        display: flex;
        justify-content: right;
        padding: ${spacing.md};
        border-top: 1px solid ${colors.border};
    `,

    Input: styled.input`
        width: 100%;
        padding: ${spacing.md};
        border: 1px solid ${colors.border};
        border-radius: ${borderRadius.small};
        font-size: 1rem;
        transition: all 0.2s;
        height: 48px;
        box-sizing: border-box;

        &:focus {
            outline: none;
            border-color: ${colors.primary};
            box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05);
        }
    `,

    FormGroup: styled.div`
        margin-bottom: ${spacing.md};
        text-align: left;
        width: 100%;
        padding: 0px ${spacing.md};
        box-sizing: border-box;
    `,

    Label: styled.label`
        display: block;
        margin-bottom: ${spacing.xs};
        font-weight: 500;
        color: ${colors.textDark};
        font-size: 0.95rem;
    `,
};


const appStyles = {
    colors,
    spacing,
    shadows,
    borderRadius,
    login: loginStyles,
    common: commonStyles,
    layout: layoutStyles,
    settings: settingsStyles,
    map: mapStyles,
    gauge: gaugeStyles,
    results: resultStyles,
    assess: assessStyles,
    search: searchBarStyles,
    advanced: advancedStyles,
    saved: savedStyles, // Add this line
    ctxMenu: contextMenuStyles,
    popup: popupStyles,
};


export default appStyles;