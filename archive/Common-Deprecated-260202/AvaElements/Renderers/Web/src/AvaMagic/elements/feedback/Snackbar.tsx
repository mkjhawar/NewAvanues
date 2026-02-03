/**
 * Snackbar Component - Phase 3 Feedback Component
 *
 * Temporary notification at screen bottom
 * Matches Android/iOS Snackbar behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Snackbar as MuiSnackbar, Alert, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export interface SnackbarProps {
  /** Open state */
  open: boolean;
  /** Message content */
  message: React.ReactNode;
  /** Severity level */
  severity?: 'error' | 'warning' | 'info' | 'success';
  /** Auto-hide duration in ms */
  autoHideDuration?: number;
  /** Close handler */
  onClose: () => void;
  /** Action button */
  action?: React.ReactNode;
  /** Position */
  position?: {
    vertical: 'top' | 'bottom';
    horizontal: 'left' | 'center' | 'right';
  };
  /** Custom class name */
  className?: string;
}

export const Snackbar: React.FC<SnackbarProps> = ({
  open,
  message,
  severity,
  autoHideDuration = 6000,
  onClose,
  action,
  position = { vertical: 'bottom', horizontal: 'center' },
  className,
}) => {
  const handleClose = (_event: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') return;
    onClose();
  };

  const closeButton = (
    <IconButton size="small" aria-label="close" color="inherit" onClick={onClose}>
      <CloseIcon fontSize="small" />
    </IconButton>
  );

  return (
    <MuiSnackbar
      open={open}
      autoHideDuration={autoHideDuration}
      onClose={handleClose}
      anchorOrigin={position}
      className={className}
    >
      {severity ? (
        <Alert
          onClose={onClose}
          severity={severity}
          variant="filled"
          sx={{ width: '100%' }}
          action={action}
        >
          {message}
        </Alert>
      ) : (
        <div>
          {message}
          {action || closeButton}
        </div>
      )}
    </MuiSnackbar>
  );
};

export default Snackbar;
