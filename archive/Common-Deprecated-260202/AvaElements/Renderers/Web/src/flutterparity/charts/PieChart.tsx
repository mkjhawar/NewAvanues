/**
 * PieChart Component - Flutter Parity Material Design
 *
 * A pie/donut chart with support for slices, labels, and interactivity.
 * Uses Recharts library for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export interface PieSlice {
  value: number;
  label: string;
  color?: string;
}

export interface PieChartProps {
  slices: PieSlice[];
  title?: string;
  donutMode?: boolean;
  donutRatio?: number;
  showLabels?: boolean;
  showPercentages?: boolean;
  showLegend?: boolean;
  startAngle?: number;
  size?: number;
  strokeWidth?: number;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  onSliceClick?: (sliceIndex: number, slice: PieSlice) => void;
  className?: string;
}

export const PieChart: React.FC<PieChartProps> = ({
  slices = [],
  title,
  donutMode = false,
  donutRatio = 0.6,
  showLabels = true,
  showPercentages = true,
  showLegend = true,
  startAngle = -90,
  size = 250,
  strokeWidth = 2,
  animated = true,
  animationDuration = 500,
  contentDescription,
  onSliceClick,
  className,
}) => {
  const defaultColors = [
    '#2196F3', '#4CAF50', '#FF9800', '#F44336',
    '#9C27B0', '#00BCD4', '#FFEB3B', '#795548'
  ];

  // Transform data for Recharts
  const chartData = React.useMemo(() => {
    return slices.map(slice => ({
      name: slice.label,
      value: slice.value,
    }));
  }, [slices]);

  const renderLabel = (entry: any) => {
    if (!showLabels) return null;
    if (showPercentages) {
      const percent = ((entry.value / slices.reduce((sum, s) => sum + s.value, 0)) * 100).toFixed(1);
      return `${entry.name}: ${percent}%`;
    }
    return entry.name;
  };

  return (
    <div className={className} aria-label={contentDescription || title || 'Pie chart'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={size}>
        <RechartsPieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            labelLine={showLabels}
            label={showLabels ? renderLabel : false}
            outerRadius={size / 2.5}
            innerRadius={donutMode ? (size / 2.5) * donutRatio : 0}
            fill="#8884d8"
            dataKey="value"
            startAngle={startAngle}
            endAngle={startAngle + 360}
            isAnimationActive={animated}
            animationDuration={animationDuration}
            onClick={(data, index) => {
              if (onSliceClick) {
                onSliceClick(index, slices[index]);
              }
            }}
          >
            {chartData.map((entry, index) => (
              <Cell
                key={`cell-${index}`}
                fill={slices[index].color || defaultColors[index % defaultColors.length]}
                stroke="white"
                strokeWidth={strokeWidth}
              />
            ))}
          </Pie>
          {showLegend && <Legend />}
          <Tooltip />
        </RechartsPieChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PieChart;
