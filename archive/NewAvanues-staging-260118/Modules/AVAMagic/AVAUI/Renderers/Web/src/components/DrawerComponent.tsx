import React from 'react';
import { Drawer, Box } from '@mui/material';

export interface DrawerComponentProps {
  open: boolean;
  onClose?: () => void;
  anchor?: 'left' | 'right' | 'top' | 'bottom';
  variant?: 'permanent' | 'persistent' | 'temporary';
  children?: React.ReactNode;
  width?: number | string;
}

export const DrawerComponent: React.FC<DrawerComponentProps> = ({
  open,
  onClose,
  anchor = 'left',
  variant = 'temporary',
  children,
  width = 240,
}) => {
  return (
    <Drawer
      anchor={anchor}
      open={open}
      onClose={onClose}
      variant={variant}
      PaperProps={{
        sx: {
          width: anchor === 'left' || anchor === 'right' ? width : 'auto',
        },
      }}
    >
      <Box sx={{ width: '100%', overflow: 'auto' }}>
        {children}
      </Box>
    </Drawer>
  );
};

export default DrawerComponent;
