import React from 'react';
import { Pagination, PaginationProps } from '@mui/material';

/**
 * MagicPagination - React/Material-UI Pagination Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicPaginationProps {
  currentPage: number;
  totalPages: number;
  onChange: (page: number) => void;
  className?: string;
}

export const MagicPagination: React.FC<MagicPaginationProps> = ({
  currentPage,
  totalPages,
  onChange,
  className
}) => {
  const paginationProps: PaginationProps = {
    count: totalPages,
    page: currentPage + 1, // MUI uses 1-based indexing
    onChange: (_, page) => onChange(page - 1), // Convert back to 0-based
    color: 'primary',
    className
  };

  return <Pagination {...paginationProps} />;
};

export default MagicPagination;
