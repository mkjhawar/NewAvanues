import React from 'react';
import { Paper as MuiPaper, PaperProps as MuiPaperProps } from '@mui/material';

export interface PaperProps extends Omit<MuiPaperProps, 'elevation'> {
  elevation?: number;
  children?: React.ReactNode;
}

export const Paper: React.FC<PaperProps> = ({
  elevation = 1,
  children,
  sx,
  ...props
}) => {
  return (
    <MuiPaper
      elevation={elevation}
      sx={{
        padding: 2,
        borderRadius: 1,
        ...sx,
      }}
      {...props}
    >
      {children}
    </MuiPaper>
  );
};

export default Paper;
