import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';
import styled from 'styled-components';
// Import React Icons
import { FaMapMarkerAlt, FaLayerGroup, FaColumns, FaChevronDown, FaChevronUp, FaCog, FaSignInAlt, FaUserPlus, FaSave, FaChartBar } from 'react-icons/fa';

// Styled components for dropdown functionality
const DropdownContainer = styled.div`
  width: 100%;
`;

const DropdownMenu = styled.div`
  display: ${props => (props.$isOpen ? 'block' : 'none')};
  width: 100%;
  background-color: #ffffff;
  overflow: hidden;
  max-height: ${props => (props.$isOpen ? '200px' : '0')};
  transition: max-height 0.3s ease;
  margin-top: 2px; /* Add a small margin for visual separation */
`;

const DropdownItem = styled.div`
  padding: ${styles.spacing.md} ${styles.spacing.md};
  cursor: pointer;
  color: ${styles.colors.text || '#000000'};
  text-align: left;
  transition: background-color 0.3s, border-radius 0.3s, margin 0.3s;
  padding-left: calc(${styles.spacing.lg} * 2);
  font-family: inherit;
  font-size: inherit;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
  
  &:hover, &:active {
    background-color: #f0f0f0;
    border-radius: 8px;
    margin: 0 ${styles.spacing.sm};
  }
  
  
`;

const NavItemWithIcon = styled(styles.layout.NavItem)`
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.3s, border-radius 0.3s, margin 0.3s;
  
  ${props => props.$isActive && `
    background-color: #f0f0f0;
    border-radius: 8px;
    margin: 0 ${styles.spacing.sm};
  `}
  
  &:hover {
    background-color: #f0f0f0;
    border-radius: 8px;
    margin: 0 ${styles.spacing.sm};
  }
`;

const NavItemContent = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const IconWrapper = styled.span`
  display: flex;
  align-items: center;
  font-size: 1rem;
`;

const StyledNavItem = styled(styles.layout.NavItem)`
  display: flex;
  align-items: center;
  transition: background-color 0.3s, border-radius 0.3s, margin 0.3s;
  
  &:hover, &:active {
    background-color: #f0f0f0;
    border-radius: 8px;
    margin: 0 ${styles.spacing.sm};
  }
`;

function Sidebar() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  
  // Track multiple open dropdowns
  const [openDropdowns, setOpenDropdowns] = useState({
    assess: false,
    'smart-select': false,
    'side-by-side': false
  });

  // Track active route
  const [activeRoute, setActiveRoute] = useState(() => {
    const path = window.location.pathname;
    return path;
  });

  // Toggle dropdown function
  const toggleDropdown = (name) => {
    setOpenDropdowns({
      ...openDropdowns,
      [name]: !openDropdowns[name]
    });
  };

  // Handle navigation
  const handleNavigation = (path, dropdownName) => {
    navigate(path);
    setActiveRoute(path);
    setOpenDropdowns({
      ...openDropdowns,
      [dropdownName]: false
    });
  };

  return (
    <styles.layout.Sidebar>
      <styles.layout.Logo>GeoMetrics</styles.layout.Logo>
      <styles.layout.NavMenu>
        {/* Assess Dropdown */}
        <DropdownContainer>
          <NavItemWithIcon 
            onClick={() => toggleDropdown('assess')} 
            $isActive={openDropdowns.assess}
          >
            <NavItemContent>
              <IconWrapper><FaMapMarkerAlt /></IconWrapper>
              Assess
            </NavItemContent>
            <IconWrapper>
              {openDropdowns.assess ? <FaChevronUp /> : <FaChevronDown />}
            </IconWrapper>
          </NavItemWithIcon>
          <DropdownMenu $isOpen={openDropdowns.assess}>
            <DropdownItem 
              onClick={() => handleNavigation('/assess', 'assess')} 
              $isActive={activeRoute === '/assess'}
            >
              Assessment
            </DropdownItem>
            <DropdownItem 
              onClick={() => handleNavigation('/assess/advanced', 'assess')} 
              $isActive={activeRoute === '/assess/advanced'}
            >
              Advanced
            </DropdownItem>
          </DropdownMenu>
        </DropdownContainer>

        {/* Smart Select Dropdown */}
        <DropdownContainer>
          <NavItemWithIcon 
            onClick={() => toggleDropdown('smart-select')} 
            $isActive={openDropdowns['smart-select']}
          >
            <NavItemContent>
              <IconWrapper><FaLayerGroup /></IconWrapper>
              Smart Select
            </NavItemContent>
            <IconWrapper>
              {openDropdowns['smart-select'] ? <FaChevronUp /> : <FaChevronDown />}
            </IconWrapper>
          </NavItemWithIcon>
          <DropdownMenu $isOpen={openDropdowns['smart-select']}>
            <DropdownItem 
              onClick={() => handleNavigation('/smart-select', 'smart-select')}
              $isActive={activeRoute === '/smart-select'}
            >
              Select Area
            </DropdownItem>
            <DropdownItem 
              onClick={() => handleNavigation('/smart-select/parameters', 'smart-select')}
              $isActive={activeRoute === '/smart-select/parameters'}
            >
              Parameters
            </DropdownItem>
          </DropdownMenu>
        </DropdownContainer>

        {/* Side by Side Dropdown */}
        <DropdownContainer>
          <NavItemWithIcon 
            onClick={() => toggleDropdown('side-by-side')} 
            $isActive={openDropdowns['side-by-side']}
          >
            <NavItemContent>
              <IconWrapper><FaColumns /></IconWrapper>
              Side By Side
            </NavItemContent>
            <IconWrapper>
              {openDropdowns['side-by-side'] ? <FaChevronUp /> : <FaChevronDown />}
            </IconWrapper>
          </NavItemWithIcon>
          <DropdownMenu $isOpen={openDropdowns['side-by-side']}>
            <DropdownItem 
              onClick={() => handleNavigation('/side-by-side', 'side-by-side')}
              $isActive={activeRoute === '/side-by-side'}
            >
              Select
            </DropdownItem>
            <DropdownItem 
              onClick={() => handleNavigation('/side-by-side/compare', 'side-by-side')}
              $isActive={activeRoute === '/side-by-side/compare'}
            >
              Compare
            </DropdownItem>
          </DropdownMenu>
        </DropdownContainer>
      </styles.layout.NavMenu>

      {!user ? (
        <styles.layout.UserSection>
          <StyledNavItem onClick={() => navigate('/settings')}>
            <NavItemContent>
              <IconWrapper><FaCog /></IconWrapper>
              Settings
            </NavItemContent>
          </StyledNavItem>
          <StyledNavItem onClick={() => navigate('/login')}>
            <NavItemContent>
              <IconWrapper><FaSignInAlt /></IconWrapper>
              Login
            </NavItemContent>
          </StyledNavItem>
          <StyledNavItem onClick={() => navigate('/register')}>
            <NavItemContent>
              <IconWrapper><FaUserPlus /></IconWrapper>
              Register
            </NavItemContent>
          </StyledNavItem>
        </styles.layout.UserSection>
      ) : (
        <styles.layout.UserSection>
          <StyledNavItem onClick={() => navigate('/saved-lots')}>
            <NavItemContent>
              <IconWrapper><FaSave /></IconWrapper>
              Saved Lots
            </NavItemContent>
          </StyledNavItem>
          <StyledNavItem onClick={() => navigate('/results')}>
            <NavItemContent>
              <IconWrapper><FaChartBar /></IconWrapper>
              Results
            </NavItemContent>
          </StyledNavItem>
          <StyledNavItem onClick={() => navigate('/settings')}>
            <NavItemContent>
              <IconWrapper><FaCog /></IconWrapper>
              Preferences
            </NavItemContent>
          </StyledNavItem>
        </styles.layout.UserSection>
      )}
    </styles.layout.Sidebar>
  );
}

export default Sidebar;