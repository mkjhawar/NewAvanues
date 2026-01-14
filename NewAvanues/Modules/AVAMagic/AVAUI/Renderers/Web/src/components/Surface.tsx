import React from 'react';
import { Paper } from '@mui/material';

export interface SurfaceProps {
  children?: React.ReactNode;
  elevation?: number;
  variant?: 'elevation' | 'outlined';
  square?: boolean;
  sx?: object;
}

export const Surface: React.FC<SurfaceProps> = ({
  children,
  elevation = 1,
  variant = 'elevation',
  square = false,
  sx,
}) => {
  return (
    <Paper
      elevation={variant === 'elevation' ? elevation : 0}
      variant={variant}
      square={square}
      sx={sx}
    >
      {children}
    </Paper>
  );
};

export default Surface;
