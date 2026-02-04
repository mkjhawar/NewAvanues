/**
 * LoadingOverlay Component - Flutter Parity Display
 *
 * Full-screen or container overlay with loading spinner
 * Matches Flutter LoadingOverlay behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps } from './types';
import { ProgressCircle } from './ProgressCircle';

export interface LoadingOverlayProps extends BaseDisplayProps {
  /** Whether the overlay is visible */
  visible: boolean;
  /** Loading message */
  message?: string;
  /** Overlay opacity */
  opacity?: number;
  /** Spinner color */
  spinnerColor?: string;
  /** Spinner size */
  spinnerSize?: number;
  /** Full screen or contained */
  fullScreen?: boolean;
  /** Children to render under overlay */
  children?: React.ReactNode;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  visible,
  message,
  opacity = 0.8,
  spinnerColor = '#3b82f6',
  spinnerSize = 48,
  fullScreen = false,
  children,
  className,
  accessibilityLabel,
}) => {
  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {children}
      {visible && (
        <div
          className={className}
          role="status"
          aria-label={accessibilityLabel || message || 'Loading'}
          aria-busy="true"
          style={{
            position: fullScreen ? 'fixed' : 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: `rgba(255, 255, 255, ${opacity})`,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            gap: 16,
          }}
        >
          <ProgressCircle
            value={0}
            size={spinnerSize}
            color={spinnerColor}
            indeterminate
          />
          {message && (
            <div
              style={{
                fontSize: 14,
                color: '#374151',
                fontWeight: 500,
              }}
            >
              {message}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default LoadingOverlay;
