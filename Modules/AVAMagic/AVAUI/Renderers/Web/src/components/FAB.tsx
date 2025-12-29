import React from 'react';
import { Fab, Icon } from '@mui/material';

export interface FABProps {
  icon: string;
  label?: string;
  extended?: boolean;
  size?: 'small' | 'medium' | 'large';
  color?: 'primary' | 'secondary' | 'default';
  onClick?: () => void;
}

export const FAB: React.FC<FABProps> = ({
  icon,
  label,
  extended = false,
  size = 'medium',
  color = 'primary',
  onClick,
}) => {
  if (extended && label) {
    return (
      <Fab variant="extended" color={color} onClick={onClick} size={size}>
        <Icon sx={{ mr: 1 }}>{icon}</Icon>
        {label}
      </Fab>
    );
  }

  return (
    <Fab color={color} onClick={onClick} size={size} aria-label={label}>
      <Icon>{icon}</Icon>
    </Fab>
  );
};

export default FAB;
