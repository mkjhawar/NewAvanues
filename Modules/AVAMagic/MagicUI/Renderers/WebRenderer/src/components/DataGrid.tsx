import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  TablePagination,
  Paper,
  Checkbox,
  Box,
} from '@mui/material';

export interface DataGridColumn {
  key: string;
  label: string;
  width?: number;
  sortable?: boolean;
}

export interface DataGridProps {
  columns: DataGridColumn[];
  rows: Record<string, any>[];
  sortable?: boolean;
  selectable?: boolean;
  paginated?: boolean;
  pageSize?: number;
  selectedRows?: number[];
  onRowSelect?: (indices: number[]) => void;
  onSort?: (column: string, direction: 'asc' | 'desc') => void;
}

export const DataGrid: React.FC<DataGridProps> = ({
  columns,
  rows,
  sortable = true,
  selectable = false,
  paginated = false,
  pageSize = 10,
  selectedRows = [],
  onRowSelect,
  onSort,
}) => {
  const [page, setPage] = React.useState(0);
  const [orderBy, setOrderBy] = React.useState<string>('');
  const [order, setOrder] = React.useState<'asc' | 'desc'>('asc');
  const [selected, setSelected] = React.useState<number[]>(selectedRows);

  const handleSort = (column: string) => {
    const isAsc = orderBy === column && order === 'asc';
    const newOrder = isAsc ? 'desc' : 'asc';
    setOrder(newOrder);
    setOrderBy(column);
    onSort?.(column, newOrder);
  };

  const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.checked) {
      const newSelected = rows.map((_, i) => i);
      setSelected(newSelected);
      onRowSelect?.(newSelected);
    } else {
      setSelected([]);
      onRowSelect?.([]);
    }
  };

  const handleSelect = (index: number) => {
    const newSelected = selected.includes(index)
      ? selected.filter(i => i !== index)
      : [...selected, index];
    setSelected(newSelected);
    onRowSelect?.(newSelected);
  };

  const displayRows = paginated
    ? rows.slice(page * pageSize, page * pageSize + pageSize)
    : rows;

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      <TableContainer>
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow>
              {selectable && (
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={selected.length > 0 && selected.length < rows.length}
                    checked={rows.length > 0 && selected.length === rows.length}
                    onChange={handleSelectAll}
                  />
                </TableCell>
              )}
              {columns.map(column => (
                <TableCell
                  key={column.key}
                  style={{ width: column.width }}
                  sortDirection={orderBy === column.key ? order : false}
                >
                  {sortable && column.sortable !== false ? (
                    <TableSortLabel
                      active={orderBy === column.key}
                      direction={orderBy === column.key ? order : 'asc'}
                      onClick={() => handleSort(column.key)}
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
            {displayRows.map((row, index) => {
              const actualIndex = paginated ? page * pageSize + index : index;
              const isSelected = selected.includes(actualIndex);
              return (
                <TableRow
                  key={actualIndex}
                  selected={isSelected}
                  onClick={() => selectable && handleSelect(actualIndex)}
                  hover={selectable}
                  sx={{ cursor: selectable ? 'pointer' : 'default' }}
                >
                  {selectable && (
                    <TableCell padding="checkbox">
                      <Checkbox checked={isSelected} />
                    </TableCell>
                  )}
                  {columns.map(column => (
                    <TableCell key={column.key}>
                      {row[column.key]?.toString() ?? ''}
                    </TableCell>
                  ))}
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
      {paginated && (
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={rows.length}
          rowsPerPage={pageSize}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          onRowsPerPageChange={() => {}}
        />
      )}
    </Paper>
  );
};

export default DataGrid;
