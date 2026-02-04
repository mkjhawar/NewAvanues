/**
 * ScatterChart Component - Flutter Parity Material Design
 *
 * A scatter plot with support for multiple data series and bubble sizing.
 * Uses Recharts ScatterChart for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  ScatterChart as RechartsScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export interface ScatterPoint {
  x: number;
  y: number;
  size?: number;
  label?: string;
}

export interface ScatterSeries {
  label: string;
  points: ScatterPoint[];
  color?: string;
}

export interface ScatterChartProps {
  series: ScatterSeries[];
  title?: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  showLegend?: boolean;
  showGrid?: boolean;
  showTrendLine?: boolean;
  pointSize?: number;
  animated?: boolean;
  animationDuration?: number;
  height?: number;
  minX?: number;
  maxX?: number;
  minY?: number;
  maxY?: number;
  contentDescription?: string;
  onPointClick?: (seriesIndex: number, pointIndex: number, point: ScatterPoint) => void;
  className?: string;
}

export const ScatterChart: React.FC<ScatterChartProps> = ({
  series = [],
  title,
  xAxisLabel,
  yAxisLabel,
  showLegend = true,
  showGrid = true,
  pointSize = 6,
  animated = true,
  animationDuration = 500,
  height = 300,
  minX,
  maxX,
  minY,
  maxY,
  contentDescription,
  onPointClick,
  className,
}) => {
  const defaultColors = ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0', '#00BCD4'];

  return (
    <div className={className} aria-label={contentDescription || title || 'Scatter chart'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <RechartsScatterChart margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
          {showGrid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis
            type="number"
            dataKey="x"
            name={xAxisLabel || 'X'}
            domain={[minX ?? 'auto', maxX ?? 'auto']}
            label={xAxisLabel ? { value: xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
          />
          <YAxis
            type="number"
            dataKey="y"
            name={yAxisLabel || 'Y'}
            domain={[minY ?? 'auto', maxY ?? 'auto']}
            label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
          />
          <Tooltip cursor={{ strokeDasharray: '3 3' }} />
          {showLegend && <Legend />}
          {series.map((s, idx) => (
            <Scatter
              key={idx}
              name={s.label}
              data={s.points}
              fill={s.color || defaultColors[idx % defaultColors.length]}
              isAnimationActive={animated}
              animationDuration={animationDuration}
              shape="circle"
            />
          ))}
        </RechartsScatterChart>
      </ResponsiveContainer>
    </div>
  );
};

export default ScatterChart;
