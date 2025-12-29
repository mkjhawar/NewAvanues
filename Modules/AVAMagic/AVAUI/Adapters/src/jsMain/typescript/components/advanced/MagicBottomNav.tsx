import React from 'react';
import { BottomNavigation, BottomNavigationAction } from '@mui/material';

/**
 * MagicBottomNav - React/Material-UI Bottom Navigation Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface NavItem {
  label: string;
  icon: React.ReactNode;
}

export interface MagicBottomNavProps {
  selectedIndex: number;
  onChange: (index: number) => void;
  items: NavItem[];
  className?: string;
}

export const MagicBottomNav: React.FC<MagicBottomNavProps> = ({
  selectedIndex,
  onChange,
  items,
  className
}) => {
  return (
    <BottomNavigation
      value={selectedIndex}
      onChange={(_, newValue) => onChange(newValue)}
      showLabels
      className={className}
    >
      {items.map((item, index) => (
        <BottomNavigationAction
          key={index}
          label={item.label}
          icon={item.icon}
        />
      ))}
    </BottomNavigation>
  );
};

export default MagicBottomNav;
