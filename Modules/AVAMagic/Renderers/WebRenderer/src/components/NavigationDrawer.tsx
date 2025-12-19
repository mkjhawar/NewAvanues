import React from 'react';
import { Drawer, Box, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Divider, Typography } from '@mui/material';

export interface NavigationDrawerItem {
  label: string;
  icon?: React.ReactNode;
  onClick?: () => void;
  selected?: boolean;
  disabled?: boolean;
}

export interface NavigationDrawerProps {
  open: boolean;
  onClose?: () => void;
  items: NavigationDrawerItem[];
  header?: React.ReactNode;
  footer?: React.ReactNode;
  width?: number;
  variant?: 'permanent' | 'persistent' | 'temporary';
  anchor?: 'left' | 'right';
}

export const NavigationDrawer: React.FC<NavigationDrawerProps> = ({
  open,
  onClose,
  items,
  header,
  footer,
  width = 280,
  variant = 'temporary',
  anchor = 'left',
}) => {
  return (
    <Drawer
      open={open}
      onClose={onClose}
      variant={variant}
      anchor={anchor}
      PaperProps={{
        sx: { width },
      }}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {header && (
          <>
            <Box sx={{ p: 2 }}>{header}</Box>
            <Divider />
          </>
        )}

        <List sx={{ flexGrow: 1 }}>
          {items.map((item, index) => (
            <ListItem key={index} disablePadding>
              <ListItemButton
                selected={item.selected}
                disabled={item.disabled}
                onClick={() => {
                  item.onClick?.();
                  if (variant === 'temporary') {
                    onClose?.();
                  }
                }}
              >
                {item.icon && <ListItemIcon>{item.icon}</ListItemIcon>}
                <ListItemText primary={item.label} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>

        {footer && (
          <>
            <Divider />
            <Box sx={{ p: 2 }}>{footer}</Box>
          </>
        )}
      </Box>
    </Drawer>
  );
};

export default NavigationDrawer;
