/**
 * Tabs Component - Phase 3 Layout Component
 *
 * Tabbed navigation interface
 * Matches Android/iOS Tabs behavior
 *
 * @package com.augmentalis.AvaMagic.elements.layout
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Tabs as MuiTabs, Tab, Box } from '@mui/material';

export interface TabItem {
  /** Tab key */
  key: string;
  /** Tab label */
  label: string;
  /** Tab icon */
  icon?: React.ReactNode;
  /** Disabled state */
  disabled?: boolean;
}

export interface TabsProps {
  /** Active tab key */
  value: string;
  /** Change handler */
  onChange: (key: string) => void;
  /** Tabs list */
  tabs: TabItem[];
  /** Variant */
  variant?: 'standard' | 'scrollable' | 'fullWidth';
  /** Orientation */
  orientation?: 'horizontal' | 'vertical';
  /** Centered tabs */
  centered?: boolean;
  /** Indicator color */
  indicatorColor?: 'primary' | 'secondary';
  /** Text color */
  textColor?: 'primary' | 'secondary' | 'inherit';
  /** Custom class name */
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({
  value,
  onChange,
  tabs,
  variant = 'standard',
  orientation = 'horizontal',
  centered = false,
  indicatorColor = 'primary',
  textColor = 'primary',
  className,
}) => {
  const handleChange = (_event: React.SyntheticEvent, newValue: string) => {
    onChange(newValue);
  };

  return (
    <Box className={className}>
      <MuiTabs
        value={value}
        onChange={handleChange}
        variant={variant}
        orientation={orientation}
        centered={centered}
        indicatorColor={indicatorColor}
        textColor={textColor}
      >
        {tabs.map((tab) => (
          <Tab
            key={tab.key}
            value={tab.key}
            label={tab.label}
            icon={tab.icon}
            disabled={tab.disabled}
            iconPosition="start"
          />
        ))}
      </MuiTabs>
    </Box>
  );
};

export default Tabs;
