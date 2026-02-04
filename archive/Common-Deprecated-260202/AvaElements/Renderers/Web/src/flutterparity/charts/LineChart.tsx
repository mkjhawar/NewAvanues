/**
 * LineChart Component - Flutter Parity Material Design
 *
 * A line chart with support for multiple series, animations, and interactivity.
 * Uses Recharts library for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  LineChart as RechartsLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export interface ChartPoint {
  x: number;
  y: number;
  label?: string;
  metadata?: Record<string, any>;
}

export interface ChartSeries {
  label: string;
  data: ChartPoint[];
  color?: string;
  strokeWidth?: number;
  fillArea?: boolean;
  dashed?: boolean;
}

export interface LineChartProps {
  series: ChartSeries[];
  title?: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  showLegend?: boolean;
  showGrid?: boolean;
  showPoints?: boolean;
  lineWidth?: number;
  pointSize?: number;
  animated?: boolean;
  animationDuration?: number;
  height?: number;
  minY?: number;
  maxY?: number;
  enableZoom?: boolean;
  enablePan?: boolean;
  contentDescription?: string;
  onPointClick?: (seriesIndex: number, pointIndex: number, point: ChartPoint) => void;
  className?: string;
}

export const LineChart: React.FC<LineChartProps> = ({
  series = [],
  title,
  xAxisLabel,
  yAxisLabel,
  showLegend = true,
  showGrid = true,
  showPoints = true,
  lineWidth = 2,
  pointSize = 6,
  animated = true,
  animationDuration = 500,
  height = 300,
  minY,
  maxY,
  contentDescription,
  onPointClick,
  className,
}) => {
  // Transform data for Recharts format
  const transformedData = React.useMemo(() => {
    if (series.length === 0) return [];

    const allXValues = new Set<number>();
    series.forEach(s => s.data.forEach(p => allXValues.add(p.x)));
    const sortedX = Array.from(allXValues).sort((a, b) => a - b);

    return sortedX.map(x => {
      const dataPoint: any = { x };
      series.forEach((s, idx) => {
        const point = s.data.find(p => p.x === x);
        dataPoint[`series${idx}`] = point?.y ?? null;
      });
      return dataPoint;
    });
  }, [series]);

  const defaultColors = ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0', '#00BCD4'];

  return (
    <div className={className} aria-label={contentDescription || title || 'Line chart'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <RechartsLineChart
          data={transformedData}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          {showGrid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis
            dataKey="x"
            label={xAxisLabel ? { value: xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
          />
          <YAxis
            domain={[minY ?? 'auto', maxY ?? 'auto']}
            label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
          />
          <Tooltip />
          {showLegend && <Legend />}
          {series.map((s, idx) => (
            <Line
              key={idx}
              type="monotone"
              dataKey={`series${idx}`}
              name={s.label}
              stroke={s.color || defaultColors[idx % defaultColors.length]}
              strokeWidth={s.strokeWidth || lineWidth}
              strokeDasharray={s.dashed ? '5 5' : undefined}
              dot={showPoints ? { r: pointSize } : false}
              fill={s.fillArea ? s.color || defaultColors[idx % defaultColors.length] : 'none'}
              isAnimationActive={animated}
              animationDuration={animationDuration}
            />
          ))}
        </RechartsLineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default LineChart;
