import React from 'react';
import { Snackbar as MuiSnackbar, Alert, AlertColor, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export interface SnackbarProps {
  open: boolean;
  message: string;
  severity?: AlertColor;
  autoHideDuration?: number;
  onClose?: () => void;
  action?: React.ReactNode;
  anchorOrigin?: {
    vertical: 'top' | 'bottom';
    horizontal: 'left' | 'center' | 'right';
  };
}

export const Snackbar: React.FC<SnackbarProps> = ({
  open,
  message,
  severity = 'info',
  autoHideDuration = 6000,
  onClose,
  action,
  anchorOrigin = { vertical: 'bottom', horizontal: 'left' },
}) => {
  const defaultAction = (
    <IconButton size="small" aria-label="close" color="inherit" onClick={onClose}>
      <CloseIcon fontSize="small" />
    </IconButton>
  );

  return (
    <MuiSnackbar
      open={open}
      autoHideDuration={autoHideDuration}
      onClose={onClose}
      anchorOrigin={anchorOrigin}
    >
      <Alert
        onClose={onClose}
        severity={severity}
        sx={{ width: '100%' }}
        action={action || defaultAction}
      >
        {message}
      </Alert>
    </MuiSnackbar>
  );
};

export default Snackbar;
