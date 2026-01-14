/**
 * NoData Component - Flutter Parity Display
 *
 * Empty state display with icon and message
 * Matches Flutter EmptyState behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps } from './types';

export interface NoDataProps extends BaseDisplayProps {
  /** Title */
  title?: string;
  /** Message */
  message: string;
  /** Action button label */
  actionLabel?: string;
  /** Action callback */
  onAction?: () => void;
  /** Custom icon */
  icon?: React.ReactNode;
  /** Full height container */
  fullHeight?: boolean;
}

export const NoData: React.FC<NoDataProps> = ({
  title = 'No data',
  message,
  actionLabel,
  onAction,
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
      stroke="#9ca3af"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M3 3h18v18H3z" />
      <path d="M9 9h6v6H9z" />
    </svg>
  );

  return (
    <div
      className={className}
      role="status"
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
      <div style={{ marginBottom: 16, opacity: 0.5 }}>
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
      {onAction && actionLabel && (
        <button
          onClick={onAction}
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
          {actionLabel}
        </button>
      )}
    </div>
  );
};

export default NoData;
