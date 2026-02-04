/**
 * Confirm Component - Phase 3 Feedback Component
 *
 * Confirmation dialog for critical actions
 * Matches Android/iOS Confirm behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
} from '@mui/material';

export interface ConfirmProps {
  /** Open state */
  open: boolean;
  /** Title */
  title: string;
  /** Message */
  message: React.ReactNode;
  /** Confirm button text */
  confirmText?: string;
  /** Cancel button text */
  cancelText?: string;
  /** Confirm button color */
  confirmColor?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  /** Confirm handler */
  onConfirm: () => void;
  /** Cancel handler */
  onCancel: () => void;
  /** Destructive action warning */
  destructive?: boolean;
  /** Custom class name */
  className?: string;
}

export const Confirm: React.FC<ConfirmProps> = ({
  open,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  confirmColor = 'primary',
  onConfirm,
  onCancel,
  destructive = false,
  className,
}) => {
  return (
    <Dialog
      open={open}
      onClose={onCancel}
      className={className}
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-description"
    >
      <DialogTitle id="confirm-dialog-title">{title}</DialogTitle>
      <DialogContent>
        <DialogContentText id="confirm-dialog-description">
          {message}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} color="inherit">
          {cancelText}
        </Button>
        <Button
          onClick={onConfirm}
          color={destructive ? 'error' : confirmColor}
          variant="contained"
          autoFocus
        >
          {confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default Confirm;
