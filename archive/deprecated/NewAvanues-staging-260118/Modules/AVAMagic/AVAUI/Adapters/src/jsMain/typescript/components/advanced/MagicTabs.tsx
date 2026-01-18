import React from 'react';
import { Tabs, Tab, Box } from '@mui/material';

/**
 * MagicTabs - React/Material-UI Tabs Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface TabItem {
  label: string;
  content: React.ReactNode;
  icon?: React.ReactNode;
}

export interface MagicTabsProps {
  selectedIndex: number;
  onChange: (index: number) => void;
  tabs: TabItem[];
  className?: string;
}

export const MagicTabs: React.FC<MagicTabsProps> = ({
  selectedIndex,
  onChange,
  tabs,
  className
}) => {
  return (
    <Box className={className}>
      <Tabs value={selectedIndex} onChange={(_, newValue) => onChange(newValue)}>
        {tabs.map((tab, index) => (
          <Tab key={index} label={tab.label} icon={tab.icon} />
        ))}
      </Tabs>
      {tabs.map((tab, index) => (
        <Box
          key={index}
          role="tabpanel"
          hidden={selectedIndex !== index}
          sx={{ p: 3 }}
        >
          {selectedIndex === index && tab.content}
        </Box>
      ))}
    </Box>
  );
};

export default MagicTabs;
