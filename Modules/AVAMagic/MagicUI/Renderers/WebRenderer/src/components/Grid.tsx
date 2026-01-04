import React from 'react';
import { Grid as MuiGrid } from '@mui/material';

export interface GridProps {
  children?: React.ReactNode;
  columns?: number;
  spacing?: number;
  rowSpacing?: number;
  columnSpacing?: number;
  container?: boolean;
  item?: boolean;
  xs?: number | 'auto' | boolean;
  sm?: number | 'auto' | boolean;
  md?: number | 'auto' | boolean;
  lg?: number | 'auto' | boolean;
  xl?: number | 'auto' | boolean;
}

export const Grid: React.FC<GridProps> = ({
  children,
  columns = 12,
  spacing,
  rowSpacing,
  columnSpacing,
  container = false,
  item = false,
  xs,
  sm,
  md,
  lg,
  xl,
}) => {
  return (
    <MuiGrid
      container={container}
      item={item}
      columns={columns}
      spacing={spacing}
      rowSpacing={rowSpacing}
      columnSpacing={columnSpacing}
      xs={xs}
      sm={sm}
      md={md}
      lg={lg}
      xl={xl}
    >
      {children}
    </MuiGrid>
  );
};

export default Grid;
