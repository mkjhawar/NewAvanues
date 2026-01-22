import React, { useState, useEffect } from 'react';
import { Snackbar, Alert as MuiAlert } from '@mui/material';
import { ToastProps } from '../types';

export const Toast: React.FC<ToastProps> = ({
  message,
  open: initialOpen = true,
  duration = 3000,
  severity = 'info',
  position = { vertical: 'bottom', horizontal: 'left' },
  onClose,
  ...props
}) => {
  const [open, setOpen] = useState(initialOpen);

  useEffect(() => {
    setOpen(initialOpen);
  }, [initialOpen]);

  const handleClose = (_?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') return;
    setOpen(false);
    onClose?.();
  };

  return (
    <Snackbar
      open={open}
      autoHideDuration={duration}
      onClose={handleClose}
      anchorOrigin={position}
      {...props}
    >
      <MuiAlert onClose={handleClose} severity={severity} sx={{ width: '100%' }}>
        {message}
      </MuiAlert>
    </Snackbar>
  );
};
