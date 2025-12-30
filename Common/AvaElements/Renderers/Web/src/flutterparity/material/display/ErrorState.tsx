/**
 * ErrorState Component - Flutter Parity Display
 *
 * Error message display with icon and retry button
 * Matches Flutter ErrorState behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps } from './types';

export interface ErrorStateProps extends BaseDisplayProps {
  /** Error title */
  title?: string;
  /** Error message */
  message: string;
  /** Retry button label */
  retryLabel?: string;
  /** Retry callback */
  onRetry?: () => void;
  /** Custom icon */
  icon?: React.ReactNode;
  /** Full height container */
  fullHeight?: boolean;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message,
  retryLabel = 'Try again',
  onRetry,
  icon,
  fullHeight = false,
  className,
  accessibilityLabel,
}) => {
  const defaultIcon = (
    <svg
      width="64"
      height="64"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#ef4444"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
  );

  return (
    <div
      className={className}
      role="alert"
      aria-label={accessibilityLabel || title}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 32,
        textAlign: 'center',
        height: fullHeight ? '100%' : 'auto',
        minHeight: fullHeight ? 300 : 'auto',
      }}
    >
      <div style={{ marginBottom: 16 }}>
        {icon || defaultIcon}
      </div>
      <h3
        style={{
          fontSize: 18,
          fontWeight: 600,
          color: '#111827',
          margin: '0 0 8px 0',
        }}
      >
        {title}
      </h3>
      <p
        style={{
          fontSize: 14,
          color: '#6b7280',
          margin: '0 0 24px 0',
          maxWidth: 400,
        }}
      >
        {message}
      </p>
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            backgroundColor: '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            padding: '10px 20px',
            fontSize: 14,
            fontWeight: 500,
            cursor: 'pointer',
            transition: 'background-color 0.2s',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#2563eb')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#3b82f6')}
        >
          {retryLabel}
        </button>
      )}
    </div>
  );
};

export default ErrorState;
