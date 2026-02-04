/**
 * StatGroup Component
 *
 * Container for grouping multiple Stat components.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { StatGroupProps } from './types';

/**
 * StatGroup - Group of statistics component
 *
 * @example
 * ```tsx
 * <StatGroup columns={3} spacing={16} divider>
 *   <Stat label="Total Sales" value={45231} prefix="$" />
 *   <Stat label="New Users" value={1234} trend={{ value: 12.5, isUpGood: true }} />
 *   <Stat label="Conversion Rate" value={3.2} suffix="%" />
 * </StatGroup>
 * ```
 */
export const StatGroup: React.FC<StatGroupProps> = ({
  children,
  columns = 3,
  spacing = 16,
  divider = false,
  className = '',
  style,
  testId,
}) => {
  const containerClasses = [
    'stat-group',
    divider && 'stat-group--divider',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'grid',
    gridTemplateColumns: `repeat(${columns}, 1fr)`,
    gap: `${spacing}px`,
    ...style,
  };

  // Add dividers between stats if enabled
  const childrenArray = React.Children.toArray(children);
  const styledChildren = divider
    ? childrenArray.map((child, index) => {
        const isNotLast = index < childrenArray.length - 1;
        const isNotEndOfRow = (index + 1) % columns !== 0;

        return (
          <div
            key={index}
            style={{
              position: 'relative',
              borderRight: isNotLast && isNotEndOfRow ? '1px solid #e5e7eb' : 'none',
              paddingRight: isNotLast && isNotEndOfRow ? `${spacing}px` : '0',
            }}
          >
            {child}
          </div>
        );
      })
    : childrenArray;

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {divider ? styledChildren : children}
    </div>
  );
};

StatGroup.displayName = 'StatGroup';
