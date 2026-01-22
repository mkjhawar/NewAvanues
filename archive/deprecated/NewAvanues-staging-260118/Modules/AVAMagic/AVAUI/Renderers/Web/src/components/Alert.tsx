import React from 'react';
import { Alert as MuiAlert, AlertTitle } from '@mui/material';
import { AlertProps } from '../types';

export const Alert: React.FC<AlertProps> = ({
  message,
  title,
  severity = 'info',
  variant = 'standard',
  icon,
  onClose,
  sx,
  ...props
}) => {
  return (
    <MuiAlert
      severity={severity}
      variant={variant}
      icon={icon}
      onClose={onClose}
      sx={{ width: '100%', ...sx }}
      {...props}
    >
      {title && <AlertTitle>{title}</AlertTitle>}
      {message}
    </MuiAlert>
  );
};
