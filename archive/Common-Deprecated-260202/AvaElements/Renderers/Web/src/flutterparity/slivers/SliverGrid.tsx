import React from 'react';

export interface SliverGridProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  crossAxisCount: number;
  mainAxisSpacing?: number;
  crossAxisSpacing?: number;
  childAspectRatio?: number;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * SliverGrid - Sliver-based grid component
 *
 * A sliver is a portion of a scrollable area. This component creates
 * a grid that can be used within a CustomScrollView or similar container.
 * For virtualized grids, use GridViewBuilder instead.
 */
export const SliverGrid: React.FC<SliverGridProps> = ({
  itemCount,
  itemBuilder,
  crossAxisCount,
  mainAxisSpacing = 0,
  crossAxisSpacing = 0,
  childAspectRatio = 1,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${crossAxisCount}, 1fr)`,
        gap: `${mainAxisSpacing}px ${crossAxisSpacing}px`,
        ...style,
      }}
      role="grid"
    >
      {Array.from({ length: itemCount }, (_, index) => (
        <div
          key={keyExtractor(index)}
          style={{
            aspectRatio: childAspectRatio.toString(),
          }}
          role="gridcell"
        >
          {itemBuilder(index)}
        </div>
      ))}
    </div>
  );
};
