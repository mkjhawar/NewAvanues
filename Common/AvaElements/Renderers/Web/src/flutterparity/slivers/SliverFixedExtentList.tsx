import React from 'react';

export interface SliverFixedExtentListProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  itemExtent: number;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * SliverFixedExtentList - Fixed height sliver list
 *
 * A sliver list where all items have the same fixed height.
 * This allows for optimizations in layout calculations.
 */
export const SliverFixedExtentList: React.FC<SliverFixedExtentListProps> = ({
  itemCount,
  itemBuilder,
  itemExtent,
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
        <div
          key={keyExtractor(index)}
          style={{
            height: itemExtent,
            flexShrink: 0,
          }}
          role="listitem"
        >
          {itemBuilder(index)}
        </div>
      ))}
    </div>
  );
};
