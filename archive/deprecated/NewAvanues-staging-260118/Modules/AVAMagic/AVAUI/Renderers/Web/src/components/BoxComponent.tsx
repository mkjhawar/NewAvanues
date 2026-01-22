import React from 'react';
import { Box } from '@mui/material';

export interface BoxComponentProps {
  children?: React.ReactNode;
  contentAlignment?: 'topStart' | 'topCenter' | 'topEnd' | 'centerStart' | 'center' | 'centerEnd' | 'bottomStart' | 'bottomCenter' | 'bottomEnd';
  sx?: object;
}

const alignmentMap = {
  topStart: { justifyContent: 'flex-start', alignItems: 'flex-start' },
  topCenter: { justifyContent: 'center', alignItems: 'flex-start' },
  topEnd: { justifyContent: 'flex-end', alignItems: 'flex-start' },
  centerStart: { justifyContent: 'flex-start', alignItems: 'center' },
  center: { justifyContent: 'center', alignItems: 'center' },
  centerEnd: { justifyContent: 'flex-end', alignItems: 'center' },
  bottomStart: { justifyContent: 'flex-start', alignItems: 'flex-end' },
  bottomCenter: { justifyContent: 'center', alignItems: 'flex-end' },
  bottomEnd: { justifyContent: 'flex-end', alignItems: 'flex-end' },
};

export const BoxComponent: React.FC<BoxComponentProps> = ({
  children,
  contentAlignment = 'topStart',
  sx,
}) => {
  const alignment = alignmentMap[contentAlignment];

  return (
    <Box
      sx={{
        display: 'flex',
        position: 'relative',
        ...alignment,
        ...sx,
      }}
    >
      {children}
    </Box>
  );
};

export default BoxComponent;
