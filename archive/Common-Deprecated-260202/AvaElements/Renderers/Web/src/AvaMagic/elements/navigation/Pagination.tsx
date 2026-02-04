/**
 * Pagination Component - Phase 3 Navigation Component
 *
 * Page navigation controls
 * Matches Android/iOS Pagination behavior
 *
 * @package com.augmentalis.AvaMagic.elements.navigation
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Pagination as MuiPagination, Box } from '@mui/material';

export interface PaginationProps {
  /** Current page (1-indexed) */
  page: number;
  /** Total number of pages */
  count: number;
  /** Change handler */
  onChange: (page: number) => void;
  /** Variant */
  variant?: 'text' | 'outlined';
  /** Shape */
  shape?: 'circular' | 'rounded';
  /** Color */
  color?: 'primary' | 'secondary' | 'standard';
  /** Size */
  size?: 'small' | 'medium' | 'large';
  /** Show first/last buttons */
  showFirstButton?: boolean;
  showLastButton?: boolean;
  /** Disabled state */
  disabled?: boolean;
  /** Sibling count (pages around current) */
  siblingCount?: number;
  /** Boundary count (pages at start/end) */
  boundaryCount?: number;
  /** Custom class name */
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  page,
  count,
  onChange,
  variant = 'text',
  shape = 'circular',
  color = 'primary',
  size = 'medium',
  showFirstButton = false,
  showLastButton = false,
  disabled = false,
  siblingCount = 1,
  boundaryCount = 1,
  className,
}) => {
  const handleChange = (_event: React.ChangeEvent<unknown>, value: number) => {
    onChange(value);
  };

  return (
    <Box className={className} sx={{ display: 'flex', justifyContent: 'center' }}>
      <MuiPagination
        page={page}
        count={count}
        onChange={handleChange}
        variant={variant}
        shape={shape}
        color={color}
        size={size}
        showFirstButton={showFirstButton}
        showLastButton={showLastButton}
        disabled={disabled}
        siblingCount={siblingCount}
        boundaryCount={boundaryCount}
      />
    </Box>
  );
};

export default Pagination;
