/**
 * SkeletonCircle Component - Flutter Parity Display
 *
 * Circular skeleton placeholder with shimmer animation
 * Matches Flutter SkeletonCircle behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps, AnimationType, Size } from './types';
import './skeleton.css';

export interface SkeletonCircleProps extends BaseDisplayProps {
  /** Size of the circle */
  size?: Size | number;
  /** Animation type */
  animation?: AnimationType;
}

const getSizeValue = (size?: Size | number): number => {
  if (typeof size === 'number') return size;
  switch (size) {
    case 'small': return 32;
    case 'large': return 56;
    case 'medium':
    default: return 40;
  }
};

export const SkeletonCircle: React.FC<SkeletonCircleProps> = ({
  size = 'medium',
  animation = 'pulse',
  className,
  accessibilityLabel,
}) => {
  const sizeValue = getSizeValue(size);

  return (
    <div
      className={`${animation ? `skeleton-${animation}` : ''} ${className || ''}`}
      role="status"
      aria-label={accessibilityLabel || 'Loading'}
      aria-busy="true"
      style={{
        width: sizeValue,
        height: sizeValue,
        backgroundColor: '#e5e7eb',
        borderRadius: '50%',
      }}
    />
  );
};

export default SkeletonCircle;
