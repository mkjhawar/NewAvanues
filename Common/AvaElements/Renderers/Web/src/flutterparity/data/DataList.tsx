/**
 * DataList Component
 *
 * Displays key-value pairs in a structured list format.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { DataListProps } from './types';

/**
 * DataList - Key-value data display component
 *
 * @example
 * ```tsx
 * <DataList
 *   items={[
 *     { key: 'Name', value: 'John Doe' },
 *     { key: 'Email', value: 'john@example.com', icon: <EmailIcon /> },
 *     { key: 'Status', value: 'Active', helpText: 'User is currently active' }
 *   ]}
 *   layout="vertical"
 *   divider
 * />
 * ```
 */
export const DataList: React.FC<DataListProps> = ({
  items,
  layout = 'vertical',
  divider = false,
  size = 'md',
  className = '',
  style,
  testId,
}) => {
  const containerClasses = [
    'data-list',
    `data-list--${layout}`,
    `data-list--${size}`,
    divider && 'data-list--divider',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const sizeStyles = {
    sm: { fontSize: '0.875rem', padding: '0.5rem' },
    md: { fontSize: '1rem', padding: '0.75rem' },
    lg: { fontSize: '1.125rem', padding: '1rem' },
  };

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: layout === 'vertical' ? 'column' : 'row',
    gap: layout === 'vertical' ? '0' : '2rem',
    flexWrap: layout === 'horizontal' ? 'wrap' : 'nowrap',
    ...style,
  };

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {items.map((item, index) => (
        <div
          key={`${item.key}-${index}`}
          className="data-list-item"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: sizeStyles[size].padding,
            fontSize: sizeStyles[size].fontSize,
            borderBottom: divider && index < items.length - 1 ? '1px solid #e5e7eb' : 'none',
          }}
        >
          <div
            className="data-list-item__key"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              fontWeight: 500,
              color: '#6b7280',
            }}
          >
            {item.icon && <span className="data-list-item__icon">{item.icon}</span>}
            <span>{item.key}</span>
            {item.helpText && (
              <span
                className="data-list-item__help"
                title={item.helpText}
                style={{
                  display: 'inline-block',
                  width: '1rem',
                  height: '1rem',
                  borderRadius: '50%',
                  backgroundColor: '#e5e7eb',
                  color: '#6b7280',
                  fontSize: '0.75rem',
                  lineHeight: '1rem',
                  textAlign: 'center',
                  cursor: 'help',
                }}
              >
                ?
              </span>
            )}
          </div>
          <div
            className="data-list-item__value"
            style={{
              fontWeight: 600,
              color: '#111827',
            }}
          >
            {item.value}
          </div>
        </div>
      ))}
    </div>
  );
};

DataList.displayName = 'DataList';
