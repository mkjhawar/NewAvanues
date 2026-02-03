import React from 'react';
import { FullPageLoadingProps } from './types';

const DefaultSpinner: React.FC = () => (
  <div
    className="default-spinner"
    style={{
      width: '48px',
      height: '48px',
      border: '4px solid rgba(0, 0, 0, 0.1)',
      borderTopColor: 'var(--primary-color, #2196F3)',
      borderRadius: '50%',
      animation: 'spin 1s linear infinite',
    }}
  />
);

export const FullPageLoading: React.FC<FullPageLoadingProps> = ({
  message,
  spinner,
  className,
}) => {
  return (
    <div
      className={`full-page-loading ${className || ''}`}
      role="status"
      aria-live="polite"
      aria-busy="true"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'var(--loading-bg, rgba(255, 255, 255, 0.95))',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '16px',
        zIndex: 9999,
      }}
    >
      <style>
        {`
          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }
        `}
      </style>
      {spinner || <DefaultSpinner />}
      {message && (
        <div
          className="loading-message"
          style={{
            fontSize: '16px',
            color: 'var(--text-secondary, #666)',
            maxWidth: '80%',
            textAlign: 'center',
          }}
        >
          {message}
        </div>
      )}
    </div>
  );
};
