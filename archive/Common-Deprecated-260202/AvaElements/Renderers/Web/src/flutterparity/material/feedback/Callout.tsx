import React from 'react';
import { CalloutProps } from './types';

const variantStyles = {
  info: {
    bg: 'var(--color-info-bg, #E3F2FD)',
    border: 'var(--color-info, #2196F3)',
    text: 'var(--color-info-text, #0D47A1)',
    title: 'var(--color-info-dark, #1976D2)',
  },
  warning: {
    bg: 'var(--color-warning-bg, #FFF3E0)',
    border: 'var(--color-warning, #FF9800)',
    text: 'var(--color-warning-text, #E65100)',
    title: 'var(--color-warning-dark, #E65100)',
  },
  error: {
    bg: 'var(--color-error-bg, #FFEBEE)',
    border: 'var(--color-error, #F44336)',
    text: 'var(--color-error-text, #B71C1C)',
    title: 'var(--color-error-dark, #C62828)',
  },
  success: {
    bg: 'var(--color-success-bg, #E8F5E9)',
    border: 'var(--color-success, #4CAF50)',
    text: 'var(--color-success-text, #1B5E20)',
    title: 'var(--color-success-dark, #2E7D32)',
  },
  neutral: {
    bg: 'var(--color-neutral-bg, #F5F5F5)',
    border: 'var(--color-neutral, #9E9E9E)',
    text: 'var(--color-neutral-text, #424242)',
    title: 'var(--color-neutral-dark, #212121)',
  },
};

export const Callout: React.FC<CalloutProps> = ({
  children,
  variant = 'neutral',
  icon,
  title,
  className,
}) => {
  const styles = variantStyles[variant];

  return (
    <div
      className={`callout callout-${variant} ${className || ''}`}
      role="note"
      style={{
        backgroundColor: styles.bg,
        border: `2px solid ${styles.border}`,
        borderRadius: '8px',
        padding: '16px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
      }}
    >
      {icon && (
        <div className="callout-icon" style={{ flexShrink: 0, color: styles.border }}>
          {icon}
        </div>
      )}
      <div className="callout-content" style={{ flex: 1 }}>
        {title && (
          <div
            className="callout-title"
            style={{
              fontWeight: 700,
              marginBottom: '8px',
              color: styles.title,
              fontSize: '16px',
            }}
          >
            {title}
          </div>
        )}
        <div className="callout-body" style={{ color: styles.text }}>
          {children}
        </div>
      </div>
    </div>
  );
};
