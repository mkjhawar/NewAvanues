/**
 * Toast Component - Phase 3 Feedback Component
 *
 * Brief message notification (lighter than Snackbar)
 * Matches Android/iOS Toast behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Snackbar, Paper, Typography } from '@mui/material';

export interface ToastProps {
  /** Open state */
  open: boolean;
  /** Message content */
  message: string;
  /** Duration in ms */
  duration?: number;
  /** Close handler */
  onClose: () => void;
  /** Position */
  position?: {
    vertical: 'top' | 'bottom';
    horizontal: 'left' | 'center' | 'right';
  };
  /** Custom class name */
  className?: string;
}

export const Toast: React.FC<ToastProps> = ({
  open,
  message,
  duration = 3000,
  onClose,
  position = { vertical: 'bottom', horizontal: 'center' },
  className,
}) => {
  return (
    <Snackbar
      open={open}
      autoHideDuration={duration}
      onClose={onClose}
      anchorOrigin={position}
      className={className}
    >
      <Paper
        elevation={3}
        sx={{
          padding: '8px 16px',
          backgroundColor: 'rgba(0, 0, 0, 0.87)',
          color: 'white',
          borderRadius: 1,
        }}
      >
        <Typography variant="body2">{message}</Typography>
      </Paper>
    </Snackbar>
  );
};

export default Toast;
