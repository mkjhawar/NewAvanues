/**
 * BarChart Component - Flutter Parity Material Design
 *
 * A bar chart with support for grouped and stacked bars, animations, and interactivity.
 * Uses Recharts library for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export interface BarData {
  value: number;
  label?: string;
  color?: string;
}

export interface BarGroup {
  label: string;
  bars: BarData[];
}

export type BarMode = 'grouped' | 'stacked';
export type Orientation = 'vertical' | 'horizontal';

export interface BarChartProps {
  data: BarGroup[];
  title?: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  mode?: BarMode;
  orientation?: Orientation;
  showLegend?: boolean;
  showGrid?: boolean;
  showValues?: boolean;
  barWidth?: number;
  groupSpacing?: number;
  barSpacing?: number;
  animated?: boolean;
  animationDuration?: number;
  height?: number;
  minValue?: number;
  maxValue?: number;
  contentDescription?: string;
  onBarClick?: (groupIndex: number, barIndex: number, bar: BarData) => void;
  className?: string;
}

export const BarChart: React.FC<BarChartProps> = ({
  data = [],
  title,
  xAxisLabel,
  yAxisLabel,
  mode = 'grouped',
  orientation = 'vertical',
  showLegend = true,
  showGrid = true,
  showValues = false,
  animated = true,
  animationDuration = 500,
  height = 300,
  minValue,
  maxValue,
  contentDescription,
  onBarClick,
  className,
}) => {
  // Transform data for Recharts format
  const transformedData = React.useMemo(() => {
    return data.map(group => {
      const dataPoint: any = { name: group.label };
      group.bars.forEach((bar, idx) => {
        dataPoint[`bar${idx}`] = bar.value;
      });
      return dataPoint;
    });
  }, [data]);

  const defaultColors = ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0', '#00BCD4'];

  // Get unique bar labels for legend
  const barLabels = React.useMemo(() => {
    if (data.length === 0) return [];
    return data[0].bars.map((bar, idx) => bar.label || `Series ${idx + 1}`);
  }, [data]);

  const ChartComponent = orientation === 'horizontal' ? RechartsBarChart : RechartsBarChart;

  return (
    <div className={className} aria-label={contentDescription || title || 'Bar chart'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <ChartComponent
          data={transformedData}
          layout={orientation === 'horizontal' ? 'vertical' : 'horizontal'}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          {showGrid && <CartesianGrid strokeDasharray="3 3" />}
          {orientation === 'vertical' ? (
            <>
              <XAxis
                dataKey="name"
                label={xAxisLabel ? { value: xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
              />
              <YAxis
                domain={[minValue ?? 'auto', maxValue ?? 'auto']}
                label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
              />
            </>
          ) : (
            <>
              <XAxis
                type="number"
                domain={[minValue ?? 'auto', maxValue ?? 'auto']}
                label={xAxisLabel ? { value: xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
              />
              <YAxis
                type="category"
                dataKey="name"
                label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
              />
            </>
          )}
          <Tooltip />
          {showLegend && <Legend />}
          {data[0]?.bars.map((_, idx) => (
            <Bar
              key={idx}
              dataKey={`bar${idx}`}
              name={barLabels[idx]}
              fill={data[0].bars[idx].color || defaultColors[idx % defaultColors.length]}
              stackId={mode === 'stacked' ? 'stack' : undefined}
              isAnimationActive={animated}
              animationDuration={animationDuration}
            />
          ))}
        </ChartComponent>
      </ResponsiveContainer>
    </div>
  );
};

export default BarChart;
