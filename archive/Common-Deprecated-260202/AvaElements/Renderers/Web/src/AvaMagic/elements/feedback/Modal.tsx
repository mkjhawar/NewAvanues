/**
 * Modal Component - Phase 3 Feedback Component
 *
 * Overlay dialog for focused content
 * Matches Android/iOS Modal behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Modal as MuiModal, Box, IconButton, Fade, Backdrop } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export interface ModalProps {
  /** Modal open state */
  open: boolean;
  /** Close handler */
  onClose: () => void;
  /** Modal content */
  children: React.ReactNode;
  /** Modal title */
  title?: React.ReactNode;
  /** Show close button */
  showCloseButton?: boolean;
  /** Close on backdrop click */
  closeOnBackdropClick?: boolean;
  /** Close on escape key */
  closeOnEscape?: boolean;
  /** Max width */
  maxWidth?: number | string;
  /** Full screen mode */
  fullScreen?: boolean;
  /** Custom class name */
  className?: string;
}

const modalStyle = {
  position: 'absolute' as const,
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  bgcolor: 'background.paper',
  boxShadow: 24,
  borderRadius: 2,
  p: 0,
  outline: 'none',
};

export const Modal: React.FC<ModalProps> = ({
  open,
  onClose,
  children,
  title,
  showCloseButton = true,
  closeOnBackdropClick = true,
  closeOnEscape = true,
  maxWidth = 600,
  fullScreen = false,
  className,
}) => {
  const handleClose = (_event: {}, reason: 'backdropClick' | 'escapeKeyDown') => {
    if (reason === 'backdropClick' && !closeOnBackdropClick) return;
    if (reason === 'escapeKeyDown' && !closeOnEscape) return;
    onClose();
  };

  return (
    <MuiModal
      open={open}
      onClose={handleClose}
      closeAfterTransition
      slots={{ backdrop: Backdrop }}
      slotProps={{
        backdrop: {
          timeout: 500,
        },
      }}
      className={className}
      aria-labelledby="modal-title"
      aria-describedby="modal-description"
    >
      <Fade in={open}>
        <Box
          sx={{
            ...modalStyle,
            width: fullScreen ? '100vw' : '90%',
            height: fullScreen ? '100vh' : 'auto',
            maxWidth: fullScreen ? 'none' : maxWidth,
            maxHeight: fullScreen ? 'none' : '90vh',
            borderRadius: fullScreen ? 0 : 2,
          }}
        >
          {title && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                p: 2,
                borderBottom: '1px solid',
                borderColor: 'divider',
              }}
            >
              <Box id="modal-title" sx={{ flex: 1, fontWeight: 600, fontSize: '1.25rem' }}>
                {title}
              </Box>
              {showCloseButton && (
                <IconButton onClick={onClose} size="small" aria-label="Close modal">
                  <CloseIcon />
                </IconButton>
              )}
            </Box>
          )}
          <Box
            id="modal-description"
            sx={{
              p: 3,
              overflowY: 'auto',
              maxHeight: fullScreen ? 'calc(100vh - 64px)' : 'calc(90vh - 64px)',
            }}
          >
            {children}
          </Box>
        </Box>
      </Fade>
    </MuiModal>
  );
};

export default Modal;
