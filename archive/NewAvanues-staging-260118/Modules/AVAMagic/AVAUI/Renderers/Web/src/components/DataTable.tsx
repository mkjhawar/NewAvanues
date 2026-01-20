import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TableSortLabel,
  TablePagination,
} from '@mui/material';

export interface DataTableColumn {
  id: string;
  label: string;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  width?: number | string;
}

export interface DataTableProps {
  columns: DataTableColumn[];
  rows: Record<string, React.ReactNode>[];
  sortColumn?: string;
  sortDirection?: 'asc' | 'desc';
  onSort?: (column: string) => void;
  pagination?: boolean;
  page?: number;
  rowsPerPage?: number;
  totalRows?: number;
  onPageChange?: (page: number) => void;
  onRowsPerPageChange?: (rowsPerPage: number) => void;
  stickyHeader?: boolean;
  maxHeight?: number | string;
}

export const DataTable: React.FC<DataTableProps> = ({
  columns,
  rows,
  sortColumn,
  sortDirection = 'asc',
  onSort,
  pagination = false,
  page = 0,
  rowsPerPage = 10,
  totalRows,
  onPageChange,
  onRowsPerPageChange,
  stickyHeader = false,
  maxHeight,
}) => {
  return (
    <Paper>
      <TableContainer sx={{ maxHeight }}>
        <Table stickyHeader={stickyHeader}>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align}
                  style={{ width: column.width }}
                >
                  {column.sortable && onSort ? (
                    <TableSortLabel
                      active={sortColumn === column.id}
                      direction={sortColumn === column.id ? sortDirection : 'asc'}
                      onClick={() => onSort(column.id)}
                    >
                      {column.label}
                    </TableSortLabel>
                  ) : (
                    column.label
                  )}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, index) => (
              <TableRow key={index} hover>
                {columns.map((column) => (
                  <TableCell key={column.id} align={column.align}>
                    {row[column.id]}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {pagination && (
        <TablePagination
          component="div"
          count={totalRows ?? rows.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={(_, newPage) => onPageChange?.(newPage)}
          onRowsPerPageChange={(e) => onRowsPerPageChange?.(parseInt(e.target.value, 10))}
        />
      )}
    </Paper>
  );
};

export default DataTable;
