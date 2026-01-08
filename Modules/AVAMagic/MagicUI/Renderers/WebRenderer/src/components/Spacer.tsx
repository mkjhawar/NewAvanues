import React from 'react';
import { Box } from '@mui/material';

export interface SpacerProps {
  width?: number | string;
  height?: number | string;
}

export const Spacer: React.FC<SpacerProps> = ({
  width,
  height,
}) => {
  return (
    <Box
      sx={{
        width: width ?? 'auto',
        height: height ?? 'auto',
        flexShrink: 0,
      }}
    />
  );
};

export default Spacer;
