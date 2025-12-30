/**
 * Alert Component - Phase 3 Feedback Component
 *
 * Displays important messages with severity levels
 * Matches Android/iOS Alert behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Alert as MuiAlert, AlertTitle } from '@mui/material';

export interface AlertProps {
  /** Alert severity */
  severity?: 'error' | 'warning' | 'info' | 'success';
  /** Alert variant */
  variant?: 'filled' | 'outlined' | 'standard';
  /** Title text */
  title?: string;
  /** Message content */
  message: React.ReactNode;
  /** Show close button */
  closable?: boolean;
  /** Close handler */
  onClose?: () => void;
  /** Custom icon */
  icon?: React.ReactNode;
  /** Hide icon */
  hideIcon?: boolean;
  /** Custom action */
  action?: React.ReactNode;
  /** Custom class name */
  className?: string;
}

export const Alert: React.FC<AlertProps> = ({
  severity = 'info',
  variant = 'standard',
  title,
  message,
  closable = false,
  onClose,
  icon,
  hideIcon = false,
  action,
  className,
}) => {
  return (
    <MuiAlert
      severity={severity}
      variant={variant}
      onClose={closable ? onClose : undefined}
      icon={hideIcon ? false : icon}
      action={action}
      className={className}
    >
      {title && <AlertTitle>{title}</AlertTitle>}
      {message}
    </MuiAlert>
  );
};

export default Alert;
