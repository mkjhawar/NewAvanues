import React from 'react';
import { Snackbar, Alert, AlertColor } from '@mui/material';

/**
 * MagicToast - React/Material-UI Toast Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicToastProps {
  isOpen: boolean;
  message: string;
  severity?: AlertColor;
  duration?: number;
  onClose: () => void;
  className?: string;
}

export const MagicToast: React.FC<MagicToastProps> = ({
  isOpen,
  message,
  severity = 'info',
  duration = 3000,
  onClose,
  className
}) => {
  return (
    <Snackbar
      open={isOpen}
      autoHideDuration={duration}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      className={className}
    >
      <Alert onClose={onClose} severity={severity} sx={{ width: '100%' }}>
        {message}
      </Alert>
    </Snackbar>
  );
};

export default MagicToast;
