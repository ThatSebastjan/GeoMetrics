import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles';
import icons from './Icons.js'
import { FaMapMarkerAlt, FaLayerGroup, FaColumns, FaChevronDown, FaChevronUp, FaCog, FaSignInAlt, FaUserPlus, FaSave, FaChartBar } from 'react-icons/fa';


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
          <styles.layout.DropdownContainer>
            <styles.layout.NavItemWithIcon
                onClick={() => toggleDropdown('assess')}
                $isActive={openDropdowns.assess}
            >
              <styles.layout.NavItemContent>
                <styles.layout.IconWrapper><icons.MapIcon /></styles.layout.IconWrapper>
                Assess
              </styles.layout.NavItemContent>
              <styles.layout.IconWrapper style={{ marginLeft: 'auto' }}>
                {openDropdowns.assess ? <FaChevronUp /> : <FaChevronDown />}
              </styles.layout.IconWrapper>
            </styles.layout.NavItemWithIcon>
            <styles.layout.DropdownMenu $isOpen={openDropdowns.assess}>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/assess/basic', 'assess')}
                  $isActive={activeRoute === '/assess/basic'}
              > <styles.layout.IconWrapper><icons.RippleIcon /></styles.layout.IconWrapper>

                Assessment
              </styles.layout.DropdownItem>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/assess/advanced', 'assess')}
                  $isActive={activeRoute === '/assess/advanced'}
              > <styles.layout.IconWrapper><icons.ZapIcon /></styles.layout.IconWrapper>
                Advanced
              </styles.layout.DropdownItem>
            </styles.layout.DropdownMenu>
          </styles.layout.DropdownContainer>

          {/* Smart Select Dropdown */}
          <styles.layout.DropdownContainer>
            <styles.layout.NavItemWithIcon
                onClick={() => toggleDropdown('smart-select')}
                $isActive={openDropdowns['smart-select']}
            >
              <styles.layout.NavItemContent>
                <styles.layout.IconWrapper><icons.TargetIcon /></styles.layout.IconWrapper>
                Smart Select
              </styles.layout.NavItemContent>
              <styles.layout.IconWrapper style={{ marginLeft: 'auto' }}>
                {openDropdowns['smart-select'] ? <FaChevronUp /> : <FaChevronDown />}
              </styles.layout.IconWrapper>
            </styles.layout.NavItemWithIcon>
            <styles.layout.DropdownMenu $isOpen={openDropdowns['smart-select']}>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/smart-select', 'smart-select')}
                  $isActive={activeRoute === '/smart-select'}
              > <styles.layout.IconWrapper><icons.SelectIcon /></styles.layout.IconWrapper>
                Select Area
              </styles.layout.DropdownItem>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/smart-select/parameters', 'smart-select')}
                  $isActive={activeRoute === '/smart-select/parameters'}
              > <styles.layout.IconWrapper><icons.CheckboxIcon /></styles.layout.IconWrapper>
                Parameters
              </styles.layout.DropdownItem>
            </styles.layout.DropdownMenu>
          </styles.layout.DropdownContainer>

          {/* Side by Side Dropdown */}
          <styles.layout.DropdownContainer>
            <styles.layout.NavItemWithIcon
                onClick={() => toggleDropdown('side-by-side')}
                $isActive={openDropdowns['side-by-side']}
            >
              <styles.layout.NavItemContent>
                <styles.layout.IconWrapper><icons.SideBySideIcon /></styles.layout.IconWrapper>
                Side By Side
              </styles.layout.NavItemContent>
              <styles.layout.IconWrapper style={{ marginLeft: 'auto' }}>
                {openDropdowns['side-by-side'] ? <FaChevronUp /> : <FaChevronDown />}
              </styles.layout.IconWrapper>
            </styles.layout.NavItemWithIcon>
            <styles.layout.DropdownMenu $isOpen={openDropdowns['side-by-side']}>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/side-by-side', 'side-by-side')}
                  $isActive={activeRoute === '/side-by-side'}
              > <styles.layout.IconWrapper><icons.SearchIcon /></styles.layout.IconWrapper>
                Select
              </styles.layout.DropdownItem>
              <styles.layout.DropdownItem
                  onClick={() => handleNavigation('/side-by-side/compare', 'side-by-side')}
                  $isActive={activeRoute === '/side-by-side/compare'}
              > <styles.layout.IconWrapper><icons.ShuffleIcon /></styles.layout.IconWrapper>
                Compare
              </styles.layout.DropdownItem>
            </styles.layout.DropdownMenu>
          </styles.layout.DropdownContainer>
          <styles.layout.DropdownContainer>
            <styles.layout.StyledNavItem onClick={() => navigate('/risk-lens')}>
              <styles.layout.NavItemContent>
                <styles.layout.IconWrapper><icons.UmbrellaIcon /></styles.layout.IconWrapper>
                Risk Lens
              </styles.layout.NavItemContent>
            </styles.layout.StyledNavItem>
          </styles.layout.DropdownContainer>
        </styles.layout.NavMenu>

        {!user ? (
            <styles.layout.UserSection>

              <styles.layout.StyledNavItem onClick={() => navigate('/login')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.LoginIcon /></styles.layout.IconWrapper>
                  Login
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>

              <styles.layout.StyledNavItem onClick={() => navigate('/register')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.RegisterIcon /></styles.layout.IconWrapper>
                  Register
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>
              <styles.layout.StyledNavItem onClick={() => navigate('/settings')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.PrefferencesIcon /></styles.layout.IconWrapper>
                  Preferences
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>
            </styles.layout.UserSection>

        ) : (
            <styles.layout.UserSection>
              <styles.layout.StyledNavItem onClick={() => navigate('/saved-lots')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.SaveIcon /></styles.layout.IconWrapper>
                  Locations
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>
              <styles.layout.StyledNavItem onClick={() => navigate('/results')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.ListIcon /></styles.layout.IconWrapper>
                  Results
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>
              <styles.layout.StyledNavItem onClick={() => navigate('/settings')}>
                <styles.layout.NavItemContent>
                  <styles.layout.IconWrapper><icons.PrefferencesIcon /></styles.layout.IconWrapper>
                  Preferences
                </styles.layout.NavItemContent>
              </styles.layout.StyledNavItem>
            </styles.layout.UserSection>
        )}
      </styles.layout.Sidebar>
  );
}

export default Sidebar;