import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

export interface StickyHeaderProps {
  content: string;
  children?: React.ReactNode;
  position?: 'top' | 'bottom';
  elevation?: number;
}

export const StickyHeader: React.FC<StickyHeaderProps> = ({
  content,
  children,
  position = 'top',
  elevation = 2,
}) => {
  return (
    <Paper
      elevation={elevation}
      sx={{
        position: 'sticky',
        [position]: 0,
        zIndex: 100,
        p: 2,
      }}
    >
      <Typography variant="subtitle1" fontWeight="medium">
        {content}
      </Typography>
      {children && <Box sx={{ mt: 1 }}>{children}</Box>}
    </Paper>
  );
};

export default StickyHeader;
