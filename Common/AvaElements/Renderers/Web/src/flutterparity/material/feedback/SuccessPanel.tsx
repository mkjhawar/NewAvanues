import React from 'react';
import { BasePanelProps } from './types';

export interface SuccessPanelProps extends BasePanelProps {}

export const SuccessPanel: React.FC<SuccessPanelProps> = ({
  title,
  children,
  icon,
  dismissible = false,
  onDismiss,
  className,
}) => {
  return (
    <div
      className={`success-panel ${className || ''}`}
      role="status"
      style={{
        backgroundColor: 'var(--color-success-bg, #E8F5E9)',
        borderLeft: '4px solid var(--color-success, #4CAF50)',
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
              color: 'var(--color-success-dark, #2E7D32)',
            }}
          >
            {title}
          </div>
        )}
        <div
          className="panel-body"
          style={{ color: 'var(--color-success-text, #1B5E20)' }}
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
            color: 'var(--color-success-dark, #2E7D32)',
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
