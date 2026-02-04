import React from 'react';
import { BasePanelProps } from './types';

export interface InfoPanelProps extends BasePanelProps {}

export const InfoPanel: React.FC<InfoPanelProps> = ({
  title,
  children,
  icon,
  dismissible = false,
  onDismiss,
  className,
}) => {
  return (
    <div
      className={`info-panel ${className || ''}`}
      role="note"
      style={{
        backgroundColor: 'var(--color-info-bg, #E3F2FD)',
        borderLeft: '4px solid var(--color-info, #2196F3)',
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
              color: 'var(--color-info-dark, #1976D2)',
            }}
          >
            {title}
          </div>
        )}
        <div
          className="panel-body"
          style={{ color: 'var(--color-info-text, #0D47A1)' }}
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
            color: 'var(--color-info-dark, #1976D2)',
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
