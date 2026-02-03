import React from 'react';
import { BasePanelProps } from './types';

export interface ErrorPanelProps extends BasePanelProps {}

export const ErrorPanel: React.FC<ErrorPanelProps> = ({
  title,
  children,
  icon,
  dismissible = false,
  onDismiss,
  className,
}) => {
  return (
    <div
      className={`error-panel ${className || ''}`}
      role="alert"
      style={{
        backgroundColor: 'var(--color-error-bg, #FFEBEE)',
        borderLeft: '4px solid var(--color-error, #F44336)',
        padding: '16px',
        borderRadius: '4px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
        position: 'relative',
      }}
    >
      {icon && (
        <div className="panel-icon" style={{ flexShrink: 0 }}>
          {icon}
        </div>
      )}
      <div className="panel-content" style={{ flex: 1 }}>
        {title && (
          <div
            className="panel-title"
            style={{
              fontWeight: 600,
              marginBottom: '8px',
              color: 'var(--color-error-dark, #C62828)',
            }}
          >
            {title}
          </div>
        )}
        <div
          className="panel-body"
          style={{ color: 'var(--color-error-text, #B71C1C)' }}
        >
          {children}
        </div>
      </div>
      {dismissible && (
        <button
          onClick={onDismiss}
          aria-label="Dismiss"
          className="dismiss-btn"
          style={{
            background: 'none',
            border: 'none',
            fontSize: '24px',
            cursor: 'pointer',
            color: 'var(--color-error-dark, #C62828)',
            padding: '0 4px',
            lineHeight: 1,
            flexShrink: 0,
          }}
        >
          ×
        </button>
      )}
    </div>
  );
};
