import React from 'react';
import { Alert, AlertTitle, AlertProps } from '@mui/material';

/**
 * MagicAlert - React/Material-UI Alert Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum AlertSeverity {
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info',
  SUCCESS = 'success'
}

export interface MagicAlertProps {
  title?: string;
  message: string;
  severity?: AlertSeverity;
  onClose?: () => void;
  className?: string;
}

export const MagicAlert: React.FC<MagicAlertProps> = ({
  title,
  message,
  severity = AlertSeverity.INFO,
  onClose,
  className
}) => {
  const alertProps: AlertProps = {
    severity: severity as any,
    onClose,
    className
  };

  return (
    <Alert {...alertProps}>
      {title && <AlertTitle>{title}</AlertTitle>}
      {message}
    </Alert>
  );
};

export default MagicAlert;
