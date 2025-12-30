import React from 'react';
import {
  Table as MuiTable,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
} from '@mui/material';

export interface TableProps {
  headers: string[];
  rows: string[][];
  striped?: boolean;
  bordered?: boolean;
  hoverable?: boolean;
}

export const Table: React.FC<TableProps> = ({
  headers,
  rows,
  striped = false,
  bordered = false,
  hoverable = false,
}) => {
  return (
    <TableContainer component={bordered ? Paper : 'div'}>
      <MuiTable>
        <TableHead>
          <TableRow sx={{ bgcolor: 'grey.100' }}>
            {headers.map((header, index) => (
              <TableCell key={index} sx={{ fontWeight: 'bold' }}>
                {header}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, rowIndex) => (
            <TableRow
              key={rowIndex}
              sx={{
                bgcolor: striped && rowIndex % 2 === 1 ? 'grey.50' : 'transparent',
                '&:hover': hoverable ? { bgcolor: 'action.hover' } : {},
              }}
            >
              {row.map((cell, cellIndex) => (
                <TableCell key={cellIndex}>{cell}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </MuiTable>
    </TableContainer>
  );
};

export default Table;
