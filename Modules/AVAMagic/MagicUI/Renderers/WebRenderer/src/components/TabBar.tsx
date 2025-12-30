import React from 'react';
import { Tabs, Tab, Box } from '@mui/material';

export interface TabBarItem {
  label: string;
  icon?: React.ReactNode;
  disabled?: boolean;
}

export interface TabBarProps {
  tabs: TabBarItem[];
  value?: number;
  onChange?: (index: number) => void;
  variant?: 'standard' | 'scrollable' | 'fullWidth';
  centered?: boolean;
  indicatorColor?: 'primary' | 'secondary';
  textColor?: 'primary' | 'secondary' | 'inherit';
}

export const TabBar: React.FC<TabBarProps> = ({
  tabs,
  value = 0,
  onChange,
  variant = 'standard',
  centered = false,
  indicatorColor = 'primary',
  textColor = 'primary',
}) => {
  return (
    <Tabs
      value={value}
      onChange={(_, newValue) => onChange?.(newValue)}
      variant={variant}
      centered={centered}
      indicatorColor={indicatorColor}
      textColor={textColor}
    >
      {tabs.map((tab, index) => (
        <Tab
          key={index}
          label={tab.label}
          icon={tab.icon}
          iconPosition="start"
          disabled={tab.disabled}
        />
      ))}
    </Tabs>
  );
};

export default TabBar;
