import React from 'react';
import { Box } from '@mui/material';
import Masonry from '@mui/lab/Masonry';

export interface MasonryGridProps {
  columns?: number;
  spacing?: number;
  children: React.ReactNode;
}

export const MasonryGrid: React.FC<MasonryGridProps> = ({
  columns = 2,
  spacing = 1,
  children,
}) => {
  return (
    <Box sx={{ width: '100%' }}>
      <Masonry columns={columns} spacing={spacing}>
        {React.Children.map(children, (child) => (
          <Box>{child}</Box>
        ))}
      </Masonry>
    </Box>
  );
};

export default MasonryGrid;
