import React from 'react';
import { Box, List } from '@mui/material';

export interface LazyColumnProps {
  items: React.ReactNode[];
  spacing?: number;
  reverseLayout?: boolean;
  sx?: object;
}

export const LazyColumn: React.FC<LazyColumnProps> = ({
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
        flexDirection: 'column',
        gap: spacing,
        overflow: 'auto',
        ...sx,
      }}
    >
      {displayItems.map((item, index) => (
        <Box key={index}>{item}</Box>
      ))}
    </Box>
  );
};

export default LazyColumn;
