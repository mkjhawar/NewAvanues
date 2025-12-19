/**
 * Grid Component - Phase 3 Layout Component
 *
 * Responsive grid layout
 * Matches Android/iOS Grid behavior
 *
 * @package com.augmentalis.AvaMagic.elements.layout
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Grid as MuiGrid } from '@mui/material';

export interface GridProps {
  /** Grid children */
  children: React.ReactNode;
  /** Container or item */
  container?: boolean;
  item?: boolean;
  /** Columns (1-12) */
  xs?: number | 'auto';
  sm?: number | 'auto';
  md?: number | 'auto';
  lg?: number | 'auto';
  xl?: number | 'auto';
  /** Spacing between items */
  spacing?: number;
  /** Row spacing */
  rowSpacing?: number;
  /** Column spacing */
  columnSpacing?: number;
  /** Direction */
  direction?: 'row' | 'row-reverse' | 'column' | 'column-reverse';
  /** Justify content */
  justifyContent?: 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly';
  /** Align items */
  alignItems?: 'flex-start' | 'center' | 'flex-end' | 'stretch' | 'baseline';
  /** Wrap */
  wrap?: 'nowrap' | 'wrap' | 'wrap-reverse';
  /** Custom class name */
  className?: string;
}

export const Grid: React.FC<GridProps> = ({
  children,
  container = false,
  item = false,
  xs,
  sm,
  md,
  lg,
  xl,
  spacing,
  rowSpacing,
  columnSpacing,
  direction,
  justifyContent,
  alignItems,
  wrap,
  className,
}) => {
  return (
    <MuiGrid
      container={container}
      item={item}
      xs={xs}
      sm={sm}
      md={md}
      lg={lg}
      xl={xl}
      spacing={spacing}
      rowSpacing={rowSpacing}
      columnSpacing={columnSpacing}
      direction={direction}
      justifyContent={justifyContent}
      alignItems={alignItems}
      wrap={wrap}
      className={className}
    >
      {children}
    </MuiGrid>
  );
};

export default Grid;
