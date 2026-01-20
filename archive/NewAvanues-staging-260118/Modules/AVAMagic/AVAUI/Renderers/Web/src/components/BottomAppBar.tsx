import React from 'react';
import { AppBar, Toolbar, Box, Fab } from '@mui/material';

export interface BottomAppBarProps {
  children?: React.ReactNode;
  fab?: React.ReactNode;
  fabPosition?: 'start' | 'center' | 'end';
  color?: 'default' | 'inherit' | 'primary' | 'secondary' | 'transparent';
  sx?: object;
}

export const BottomAppBar: React.FC<BottomAppBarProps> = ({
  children,
  fab,
  fabPosition = 'center',
  color = 'primary',
  sx,
}) => {
  const fabPositionStyles = {
    start: { left: 16 },
    center: { left: '50%', transform: 'translateX(-50%)' },
    end: { right: 16 },
  };

  return (
    <AppBar
      position="fixed"
      color={color}
      sx={{
        top: 'auto',
        bottom: 0,
        ...sx,
      }}
    >
      <Toolbar>
        {fab && (
          <Box
            sx={{
              position: 'absolute',
              top: -28,
              ...fabPositionStyles[fabPosition],
            }}
          >
            {fab}
          </Box>
        )}
        {children}
      </Toolbar>
    </AppBar>
  );
};

export default BottomAppBar;
