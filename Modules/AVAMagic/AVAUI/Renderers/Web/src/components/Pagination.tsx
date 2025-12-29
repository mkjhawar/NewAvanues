import React from 'react';
import { Pagination as MuiPagination } from '@mui/material';

export interface PaginationProps {
  count: number;
  page?: number;
  onChange?: (page: number) => void;
  color?: 'primary' | 'secondary' | 'standard';
  size?: 'small' | 'medium' | 'large';
  variant?: 'text' | 'outlined';
  shape?: 'circular' | 'rounded';
  showFirstButton?: boolean;
  showLastButton?: boolean;
  disabled?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  count,
  page = 1,
  onChange,
  color = 'primary',
  size = 'medium',
  variant = 'text',
  shape = 'circular',
  showFirstButton = false,
  showLastButton = false,
  disabled = false,
}) => {
  return (
    <MuiPagination
      count={count}
      page={page}
      onChange={(_, value) => onChange?.(value)}
      color={color}
      size={size}
      variant={variant}
      shape={shape}
      showFirstButton={showFirstButton}
      showLastButton={showLastButton}
      disabled={disabled}
    />
  );
};

export default Pagination;
