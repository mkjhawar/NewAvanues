/**
 * BottomNav Component - Phase 3 Navigation Component
 *
 * Bottom navigation bar
 * Matches Android/iOS BottomNav behavior
 *
 * @package com.augmentalis.AvaMagic.elements.navigation
 * @since 3.0.0-phase3
 */

import React from 'react';
import { BottomNavigation, BottomNavigationAction, Paper } from '@mui/material';

export interface BottomNavItem {
  /** Item key */
  key: string;
  /** Label */
  label: string;
  /** Icon */
  icon: React.ReactNode;
  /** Disabled state */
  disabled?: boolean;
}

export interface BottomNavProps {
  /** Selected item key */
  value: string;
  /** Change handler */
  onChange: (key: string) => void;
  /** Navigation items */
  items: BottomNavItem[];
  /** Show labels */
  showLabels?: boolean;
  /** Elevation */
  elevation?: number;
  /** Custom class name */
  className?: string;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  value,
  onChange,
  items,
  showLabels = true,
  elevation = 8,
  className,
}) => {
  const handleChange = (_event: React.SyntheticEvent, newValue: string) => {
    onChange(newValue);
  };

  return (
    <Paper elevation={elevation} className={className}>
      <BottomNavigation
        value={value}
        onChange={handleChange}
        showLabels={showLabels}
      >
        {items.map((item) => (
          <BottomNavigationAction
            key={item.key}
            value={item.key}
            label={item.label}
            icon={item.icon}
            disabled={item.disabled}
          />
        ))}
      </BottomNavigation>
    </Paper>
  );
};

export default BottomNav;
