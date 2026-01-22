import React from 'react';
import { Modal as MuiModal, Box, Typography, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  title?: string;
  children?: React.ReactNode;
  width?: number | string;
  disableBackdropClick?: boolean;
}

const style = {
  position: 'absolute' as const,
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  bgcolor: 'background.paper',
  boxShadow: 24,
  borderRadius: 2,
  p: 4,
};

export const Modal: React.FC<ModalProps> = ({
  open,
  onClose,
  title,
  children,
  width = 400,
  disableBackdropClick = false,
}) => {
  const handleClose = (_: object, reason: 'backdropClick' | 'escapeKeyDown') => {
    if (disableBackdropClick && reason === 'backdropClick') {
      return;
    }
    onClose?.();
  };

  return (
    <MuiModal open={open} onClose={handleClose}>
      <Box sx={{ ...style, width }}>
        {title && (
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">{title}</Typography>
            {onClose && (
              <IconButton onClick={onClose} size="small">
                <CloseIcon />
              </IconButton>
            )}
          </Box>
        )}
        {children}
      </Box>
    </MuiModal>
  );
};

export default Modal;
