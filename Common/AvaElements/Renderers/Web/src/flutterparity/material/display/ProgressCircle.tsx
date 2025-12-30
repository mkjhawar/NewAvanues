/**
 * ProgressCircle Component - Flutter Parity Display
 *
 * SVG-based circular progress indicator
 * Matches Flutter CircularProgressIndicator behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps } from './types';

export interface ProgressCircleProps extends BaseDisplayProps {
  /** Progress value (0-100) */
  value: number;
  /** Size of the circle */
  size?: number;
  /** Stroke width */
  strokeWidth?: number;
  /** Progress color */
  color?: string;
  /** Background circle color */
  backgroundColor?: string;
  /** Show percentage value */
  showValue?: boolean;
  /** Indeterminate mode (spinning) */
  indeterminate?: boolean;
}

export const ProgressCircle: React.FC<ProgressCircleProps> = ({
  value,
  size = 48,
  strokeWidth = 4,
  color = '#3b82f6',
  backgroundColor = '#e5e7eb',
  showValue = false,
  indeterminate = false,
  className,
  accessibilityLabel,
}) => {
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;
  const center = size / 2;

  return (
    <div
      className={className}
      role="progressbar"
      aria-label={accessibilityLabel || 'Progress indicator'}
      aria-valuenow={indeterminate ? undefined : value}
      aria-valuemin={0}
      aria-valuemax={100}
      style={{ position: 'relative', display: 'inline-flex' }}
    >
      <svg width={size} height={size}>
        {/* Background circle */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke={backgroundColor}
          strokeWidth={strokeWidth}
        />
        {/* Progress circle */}
        <circle
          cx={center}
          cy={center}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={indeterminate ? circumference * 0.75 : offset}
          strokeLinecap="round"
          style={{
            transform: 'rotate(-90deg)',
            transformOrigin: 'center',
            transition: indeterminate ? 'none' : 'stroke-dashoffset 0.3s ease',
            animation: indeterminate ? 'spin 1.4s linear infinite' : undefined,
          }}
        />
      </svg>
      {showValue && !indeterminate && (
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: size * 0.25,
            fontWeight: 500,
            color: '#374151',
          }}
        >
          {Math.round(value)}%
        </div>
      )}
    </div>
  );
};

export default ProgressCircle;
