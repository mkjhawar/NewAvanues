/**
 * Drawer Component - Phase 3 Layout Component
 *
 * Slide-out navigation panel
 * Matches Android/iOS Drawer behavior
 *
 * @package com.augmentalis.AvaMagic.elements.layout
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Drawer as MuiDrawer, Box, IconButton, Divider } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export interface DrawerProps {
  /** Open state */
  open: boolean;
  /** Close handler */
  onClose: () => void;
  /** Drawer content */
  children: React.ReactNode;
  /** Position */
  anchor?: 'left' | 'right' | 'top' | 'bottom';
  /** Variant */
  variant?: 'temporary' | 'permanent' | 'persistent';
  /** Width (for left/right) or height (for top/bottom) */
  size?: number | string;
  /** Show close button */
  showCloseButton?: boolean;
  /** Header content */
  header?: React.ReactNode;
  /** Custom class name */
  className?: string;
}

export const Drawer: React.FC<DrawerProps> = ({
  open,
  onClose,
  children,
  anchor = 'left',
  variant = 'temporary',
  size = 280,
  showCloseButton = true,
  header,
  className,
}) => {
  const isHorizontal = anchor === 'left' || anchor === 'right';

  return (
    <MuiDrawer
      open={open}
      onClose={onClose}
      anchor={anchor}
      variant={variant}
      className={className}
      sx={{
        '& .MuiDrawer-paper': {
          width: isHorizontal ? size : '100%',
          height: isHorizontal ? '100%' : size,
          boxSizing: 'border-box',
        },
      }}
    >
      {(header || showCloseButton) && (
        <>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              p: 2,
            }}
          >
            {header && <Box sx={{ flex: 1 }}>{header}</Box>}
            {showCloseButton && (
              <IconButton onClick={onClose} size="small" aria-label="Close drawer">
                <CloseIcon />
              </IconButton>
            )}
          </Box>
          <Divider />
        </>
      )}
      <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
        {children}
      </Box>
    </MuiDrawer>
  );
};

export default Drawer;
