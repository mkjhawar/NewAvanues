import React from 'react';
import { Box } from '@mui/material';

export interface LazyRowProps {
  items: React.ReactNode[];
  spacing?: number;
  reverseLayout?: boolean;
  sx?: object;
}

export const LazyRow: React.FC<LazyRowProps> = ({
  items,
  spacing = 0,
  reverseLayout = false,
  sx,
}) => {
  const displayItems = reverseLayout ? [...items].reverse() : items;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        gap: spacing,
        overflow: 'auto',
        ...sx,
      }}
    >
      {displayItems.map((item, index) => (
        <Box key={index} sx={{ flexShrink: 0 }}>{item}</Box>
      ))}
    </Box>
  );
};

export default LazyRow;
