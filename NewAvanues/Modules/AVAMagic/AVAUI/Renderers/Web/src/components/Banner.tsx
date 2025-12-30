import React from 'react';
import { Alert, AlertTitle, IconButton, Box } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export type Severity = 'info' | 'success' | 'warning' | 'error';

export interface BannerProps {
  message: string;
  severity?: Severity;
  title?: string;
  dismissible?: boolean;
  icon?: React.ReactNode;
  onDismiss?: () => void;
}

export const Banner: React.FC<BannerProps> = ({
  message,
  severity = 'info',
  title,
  dismissible = true,
  icon,
  onDismiss,
}) => {
  return (
    <Alert
      severity={severity}
      icon={icon}
      action={
        dismissible ? (
          <IconButton
            aria-label="close"
            color="inherit"
            size="small"
            onClick={onDismiss}
          >
            <CloseIcon fontSize="inherit" />
          </IconButton>
        ) : undefined
      }
      sx={{ width: '100%' }}
    >
      {title && <AlertTitle>{title}</AlertTitle>}
      {message}
    </Alert>
  );
};

export default Banner;
