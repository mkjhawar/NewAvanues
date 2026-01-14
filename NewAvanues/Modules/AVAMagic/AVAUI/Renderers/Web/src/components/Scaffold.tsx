import React from 'react';
import { Box, Fab } from '@mui/material';

export interface ScaffoldProps {
  topBar?: React.ReactNode;
  bottomBar?: React.ReactNode;
  floatingActionButton?: React.ReactNode;
  fabPosition?: 'start' | 'center' | 'end';
  children?: React.ReactNode;
}

export const Scaffold: React.FC<ScaffoldProps> = ({
  topBar,
  bottomBar,
  floatingActionButton,
  fabPosition = 'end',
  children,
}) => {
  const fabPositionStyles = {
    start: { left: 16 },
    center: { left: '50%', transform: 'translateX(-50%)' },
    end: { right: 16 },
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {topBar && <Box sx={{ flexShrink: 0 }}>{topBar}</Box>}

      <Box sx={{ flexGrow: 1, position: 'relative', overflow: 'auto' }}>
        {children}

        {floatingActionButton && (
          <Box
            sx={{
              position: 'fixed',
              bottom: bottomBar ? 80 : 16,
              ...fabPositionStyles[fabPosition],
            }}
          >
            {floatingActionButton}
          </Box>
        )}
      </Box>

      {bottomBar && <Box sx={{ flexShrink: 0 }}>{bottomBar}</Box>}
    </Box>
  );
};

export default Scaffold;
