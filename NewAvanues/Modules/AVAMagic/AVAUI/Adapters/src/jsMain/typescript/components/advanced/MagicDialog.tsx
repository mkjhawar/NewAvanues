import React from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material';

/**
 * MagicDialog - React/Material-UI Dialog Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface DialogButton {
  label: string;
  onClick: () => void;
}

export interface MagicDialogProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  content: React.ReactNode;
  primaryButton?: DialogButton;
  secondaryButton?: DialogButton;
  className?: string;
}

export const MagicDialog: React.FC<MagicDialogProps> = ({
  isOpen,
  onClose,
  title,
  content,
  primaryButton,
  secondaryButton,
  className
}) => {
  return (
    <Dialog open={isOpen} onClose={onClose} className={className}>
      {title && <DialogTitle>{title}</DialogTitle>}
      <DialogContent>{content}</DialogContent>
      {(primaryButton || secondaryButton) && (
        <DialogActions>
          {secondaryButton && (
            <Button onClick={secondaryButton.onClick}>
              {secondaryButton.label}
            </Button>
          )}
          {primaryButton && (
            <Button onClick={primaryButton.onClick} variant="contained">
              {primaryButton.label}
            </Button>
          )}
        </DialogActions>
      )}
    </Dialog>
  );
};

export default MagicDialog;
