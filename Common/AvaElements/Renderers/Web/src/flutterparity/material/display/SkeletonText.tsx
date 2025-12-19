/**
 * SkeletonText Component - Flutter Parity Display
 *
 * Animated placeholder for text content with shimmer effect
 * Matches Flutter SkeletonText behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps, AnimationType } from './types';
import './skeleton.css';

export interface SkeletonTextProps extends BaseDisplayProps {
  /** Number of lines */
  lines?: number;
  /** Width of each line */
  width?: number | string | (number | string)[];
  /** Height of each line */
  height?: number;
  /** Animation type */
  animation?: AnimationType;
  /** Spacing between lines */
  spacing?: number;
}

export const SkeletonText: React.FC<SkeletonTextProps> = ({
  lines = 1,
  width = '100%',
  height = 16,
  animation = 'pulse',
  spacing = 8,
  className,
  accessibilityLabel,
}) => {
  const getLineWidth = (index: number): number | string => {
    if (Array.isArray(width)) {
      return width[index] || width[width.length - 1];
    }
    return width;
  };

  return (
    <div
      className={className}
      role="status"
      aria-label={accessibilityLabel || 'Loading content'}
      aria-busy="true"
    >
      {Array.from({ length: lines }).map((_, index) => (
        <div
          key={index}
          className={animation ? `skeleton-${animation}` : ''}
          style={{
            width: getLineWidth(index),
            height,
            backgroundColor: '#e5e7eb',
            borderRadius: 4,
            marginBottom: index < lines - 1 ? spacing : 0,
          }}
        />
      ))}
    </div>
  );
};

export default SkeletonText;
