/**
 * Skeleton Component - Phase 3 Display Component
 *
 * Loading placeholder that mimics content structure
 * Matches Android/iOS Skeleton behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Skeleton as MuiSkeleton } from '@mui/material';

export interface SkeletonProps {
  /** Shape variant */
  variant?: 'text' | 'rectangular' | 'rounded' | 'circular';
  /** Width */
  width?: number | string;
  /** Height */
  height?: number | string;
  /** Animation type */
  animation?: 'pulse' | 'wave' | false;
  /** Custom class name */
  className?: string;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  variant = 'text',
  width,
  height,
  animation = 'pulse',
  className,
}) => {
  return (
    <MuiSkeleton
      variant={variant}
      width={width}
      height={height}
      animation={animation}
      className={className}
    />
  );
};

export default Skeleton;
