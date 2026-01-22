import React from 'react';
import { Drawer, DrawerProps } from '@mui/material';

/**
 * MagicDrawer - React/Material-UI Drawer Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  position?: 'left' | 'right' | 'top' | 'bottom';
  children: React.ReactNode;
  className?: string;
}

export const MagicDrawer: React.FC<MagicDrawerProps> = ({
  isOpen,
  onClose,
  position = 'left',
  children,
  className
}) => {
  const drawerProps: DrawerProps = {
    open: isOpen,
    onClose,
    anchor: position,
    className
  };

  return <Drawer {...drawerProps}>{children}</Drawer>;
};

export default MagicDrawer;
