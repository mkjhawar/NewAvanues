import React from 'react';
import { BasePanelProps } from './types';

export interface WarningPanelProps extends BasePanelProps {}

export const WarningPanel: React.FC<WarningPanelProps> = ({
  title,
  children,
  icon,
  dismissible = false,
  onDismiss,
  className,
}) => {
  return (
    <div
      className={`warning-panel ${className || ''}`}
      role="alert"
      style={{
        backgroundColor: 'var(--color-warning-bg, #FFF3E0)',
        borderLeft: '4px solid var(--color-warning, #FF9800)',
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
              color: 'var(--color-warning-dark, #E65100)',
            }}
          >
            {title}
          </div>
        )}
        <div
          className="panel-body"
          style={{ color: 'var(--color-warning-text, #E65100)' }}
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
            color: 'var(--color-warning-dark, #E65100)',
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
