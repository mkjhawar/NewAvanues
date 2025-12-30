import React from 'react';
import { Tabs as MuiTabs, Tab, Box } from '@mui/material';

export interface TabItem {
  label: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  content?: React.ReactNode;
}

export interface TabsProps {
  tabs: TabItem[];
  value?: number;
  onChange?: (index: number) => void;
  orientation?: 'horizontal' | 'vertical';
  variant?: 'standard' | 'scrollable' | 'fullWidth';
  centered?: boolean;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => {
  return (
    <div role="tabpanel" hidden={value !== index}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
};

export const Tabs: React.FC<TabsProps> = ({
  tabs,
  value = 0,
  onChange,
  orientation = 'horizontal',
  variant = 'standard',
  centered = false,
}) => {
  const [selected, setSelected] = React.useState(value);

  const handleChange = (_: React.SyntheticEvent, newValue: number) => {
    setSelected(newValue);
    onChange?.(newValue);
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <MuiTabs
          value={selected}
          onChange={handleChange}
          orientation={orientation}
          variant={variant}
          centered={centered}
        >
          {tabs.map((tab, index) => (
            <Tab
              key={index}
              label={tab.label}
              icon={tab.icon}
              disabled={tab.disabled}
              iconPosition="start"
            />
          ))}
        </MuiTabs>
      </Box>
      {tabs.map((tab, index) => (
        <TabPanel key={index} value={selected} index={index}>
          {tab.content}
        </TabPanel>
      ))}
    </Box>
  );
};

export default Tabs;
