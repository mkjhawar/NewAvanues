/**
 * DescriptionList Component
 *
 * HTML description list (dl/dt/dd) with flexible layouts.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { DescriptionListProps } from './types';

/**
 * DescriptionList - Definition list component
 *
 * @example
 * ```tsx
 * <DescriptionList
 *   items={[
 *     { term: 'Product Name', description: 'AvaElements' },
 *     { term: 'Version', description: '3.0.0' },
 *     { term: 'License', description: 'Proprietary' }
 *   ]}
 *   layout="horizontal"
 *   termWidth="200px"
 * />
 * ```
 */
export const DescriptionList: React.FC<DescriptionListProps> = ({
  items,
  layout = 'horizontal',
  termWidth = '150px',
  divider = false,
  className = '',
  style,
  testId,
}) => {
  const containerClasses = [
    'description-list',
    `description-list--${layout}`,
    divider && 'description-list--divider',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    margin: 0,
    ...style,
  };

  const isHorizontal = layout === 'horizontal';

  return (
    <dl className={containerClasses} style={baseStyle} data-testid={testId}>
      {items.map((item, index) => (
        <div
          key={`${item.term}-${index}`}
          className="description-list-item"
          style={{
            display: isHorizontal ? 'flex' : 'block',
            marginBottom: index < items.length - 1 ? '1rem' : '0',
            paddingBottom: divider && index < items.length - 1 ? '1rem' : '0',
            borderBottom: divider && index < items.length - 1 ? '1px solid #e5e7eb' : 'none',
          }}
        >
          <dt
            className="description-list-item__term"
            style={{
              fontWeight: 600,
              color: '#111827',
              width: isHorizontal ? termWidth : 'auto',
              flexShrink: 0,
              marginBottom: isHorizontal ? '0' : '0.25rem',
            }}
          >
            {item.term}
          </dt>
          <dd
            className="description-list-item__description"
            style={{
              margin: 0,
              color: '#6b7280',
              flex: isHorizontal ? 1 : 'none',
            }}
          >
            {item.description}
          </dd>
        </div>
      ))}
    </dl>
  );
};

DescriptionList.displayName = 'DescriptionList';
