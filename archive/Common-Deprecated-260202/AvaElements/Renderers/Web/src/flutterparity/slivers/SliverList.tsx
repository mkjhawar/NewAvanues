import React from 'react';

export interface SliverListProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * SliverList - Sliver-based list component
 *
 * A sliver is a portion of a scrollable area. This component creates
 * a list that can be used within a CustomScrollView or similar container.
 * For virtualized lists, use ListViewBuilder instead.
 */
export const SliverList: React.FC<SliverListProps> = ({
  itemCount,
  itemBuilder,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'flex',
        flexDirection: 'column',
        ...style,
      }}
      role="list"
    >
      {Array.from({ length: itemCount }, (_, index) => (
        <div key={keyExtractor(index)} role="listitem">
          {itemBuilder(index)}
        </div>
      ))}
    </div>
  );
};
