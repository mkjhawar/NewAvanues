import React from 'react';
import { BottomNavigation, BottomNavigationAction, Paper } from '@mui/material';

export interface BottomNavItem {
  label: string;
  icon: React.ReactNode;
  value?: string;
}

export interface BottomNavProps {
  items: BottomNavItem[];
  value?: string | number;
  onChange?: (value: string | number) => void;
  showLabels?: boolean;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  items,
  value,
  onChange,
  showLabels = true,
}) => {
  const [selected, setSelected] = React.useState(value ?? 0);

  const handleChange = (_: React.SyntheticEvent, newValue: string | number) => {
    setSelected(newValue);
    onChange?.(newValue);
  };

  return (
    <Paper sx={{ position: 'fixed', bottom: 0, left: 0, right: 0 }} elevation={3}>
      <BottomNavigation value={selected} onChange={handleChange} showLabels={showLabels}>
        {items.map((item, index) => (
          <BottomNavigationAction
            key={index}
            label={item.label}
            icon={item.icon}
            value={item.value ?? index}
          />
        ))}
      </BottomNavigation>
    </Paper>
  );
};

export default BottomNav;
