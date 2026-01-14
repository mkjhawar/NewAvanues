/**
 * TreeMap Component - Flutter Parity Material Design
 *
 * A hierarchical treemap visualization.
 * Uses Recharts Treemap for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  Treemap,
  ResponsiveContainer,
} from 'recharts';

export interface TreeMapItem {
  label: string;
  value: number;
  color?: string;
  category?: string;
}

export interface TreeMapProps {
  items: TreeMapItem[];
  title?: string;
  showLabels?: boolean;
  showValues?: boolean;
  minArea?: number;
  height?: number;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  onItemClick?: (item: TreeMapItem) => void;
  className?: string;
}

export const TreeMap: React.FC<TreeMapProps> = ({
  items = [],
  title,
  showLabels = true,
  showValues = true,
  height = 400,
  animated = true,
  animationDuration = 500,
  contentDescription,
  onItemClick,
  className,
}) => {
  const defaultColors = [
    '#2196F3', '#4CAF50', '#FF9800', '#F44336',
    '#9C27B0', '#00BCD4', '#FFEB3B', '#795548'
  ];

  // Transform data for Recharts Treemap
  const chartData = React.useMemo(() => {
    return items.map((item, idx) => ({
      name: item.label,
      size: item.value,
      fill: item.color || defaultColors[idx % defaultColors.length],
      originalItem: item,
    }));
  }, [items]);

  const CustomContent = (props: any) => {
    const { x, y, width, height, name, size } = props;

    // Don't render if too small
    if (width < 30 || height < 30) return null;

    return (
      <g>
        <rect
          x={x}
          y={y}
          width={width}
          height={height}
          style={{
            fill: props.fill,
            stroke: '#fff',
            strokeWidth: 2,
          }}
        />
        {showLabels && width > 50 && height > 30 && (
          <text
            x={x + width / 2}
            y={y + height / 2 - (showValues ? 8 : 0)}
            textAnchor="middle"
            fill="#fff"
            fontSize={12}
            fontWeight="bold"
          >
            {name}
          </text>
        )}
        {showValues && width > 50 && height > 40 && (
          <text
            x={x + width / 2}
            y={y + height / 2 + 12}
            textAnchor="middle"
            fill="#fff"
            fontSize={10}
          >
            {size}
          </text>
        )}
      </g>
    );
  };

  return (
    <div className={className} aria-label={contentDescription || title || 'TreeMap'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <Treemap
          data={chartData}
          dataKey="size"
          aspectRatio={4 / 3}
          stroke="#fff"
          fill="#8884d8"
          content={<CustomContent />}
          isAnimationActive={animated}
          animationDuration={animationDuration}
        />
      </ResponsiveContainer>
    </div>
  );
};

export default TreeMap;
