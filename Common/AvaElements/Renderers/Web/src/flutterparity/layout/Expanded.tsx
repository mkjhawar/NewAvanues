import React from 'react';

export interface ExpandedProps {
  children: React.ReactNode;
  flex?: number;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * Expanded - Expand to fill available space
 *
 * Expands to fill available space in a Row or Column.
 * Must be used as a child of a flex container.
 */
export const Expanded: React.FC<ExpandedProps> = ({
  children,
  flex = 1,
  className = '',
  style = {},
}) => {
  return (
    <div
      className={className}
      style={{
        flex,
        minWidth: 0,
        minHeight: 0,
        ...style,
      }}
    >
      {children}
    </div>
  );
};
