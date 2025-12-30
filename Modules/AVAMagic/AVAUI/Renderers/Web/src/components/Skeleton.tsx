import React from 'react';
import { Skeleton as MuiSkeleton } from '@mui/material';

export interface SkeletonProps {
  variant?: 'text' | 'rectangular' | 'rounded' | 'circular';
  width?: number | string;
  height?: number | string;
  animation?: 'pulse' | 'wave' | false;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  variant = 'text',
  width,
  height,
  animation = 'pulse',
}) => {
  return (
    <MuiSkeleton
      variant={variant}
      width={width}
      height={height}
      animation={animation}
    />
  );
};

export default Skeleton;
