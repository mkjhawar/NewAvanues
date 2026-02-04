/**
 * RadarChart Component - Flutter Parity Material Design
 *
 * A radar/spider chart for multivariate data visualization.
 * Uses Recharts RadarChart for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  Radar,
  RadarChart as RechartsRadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Legend,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

export interface RadarSeries {
  label: string;
  values: number[];
  color?: string;
}

export interface RadarChartProps {
  axes: string[];
  series: RadarSeries[];
  title?: string;
  maxValue?: number;
  showLegend?: boolean;
  showGrid?: boolean;
  gridLevels?: number;
  size?: number;
  fillOpacity?: number;
  strokeWidth?: number;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  className?: string;
}

export const RadarChart: React.FC<RadarChartProps> = ({
  axes = [],
  series = [],
  title,
  maxValue = 100,
  showLegend = true,
  showGrid = true,
  gridLevels = 5,
  size = 300,
  fillOpacity = 0.3,
  strokeWidth = 2,
  animated = true,
  animationDuration = 500,
  contentDescription,
  className,
}) => {
  // Transform data for Recharts
  const chartData = React.useMemo(() => {
    return axes.map((axis, idx) => {
      const dataPoint: any = { axis };
      series.forEach((s, seriesIdx) => {
        dataPoint[`series${seriesIdx}`] = s.values[idx] || 0;
      });
      return dataPoint;
    });
  }, [axes, series]);

  const defaultColors = ['#2196F3', '#4CAF50', '#FF9800', '#F44336', '#9C27B0', '#00BCD4'];

  return (
    <div className={className} aria-label={contentDescription || title || 'Radar chart'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <ResponsiveContainer width="100%" height={size}>
        <RechartsRadarChart data={chartData}>
          {showGrid && <PolarGrid gridType="polygon" />}
          <PolarAngleAxis dataKey="axis" />
          <PolarRadiusAxis angle={90} domain={[0, maxValue]} />
          {series.map((s, idx) => (
            <Radar
              key={idx}
              name={s.label}
              dataKey={`series${idx}`}
              stroke={s.color || defaultColors[idx % defaultColors.length]}
              fill={s.color || defaultColors[idx % defaultColors.length]}
              fillOpacity={fillOpacity}
              strokeWidth={strokeWidth}
              isAnimationActive={animated}
              animationDuration={animationDuration}
            />
          ))}
          {showLegend && <Legend />}
          <Tooltip />
        </RechartsRadarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default RadarChart;
