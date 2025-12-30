/**
 * Sparkline Component - Flutter Parity Material Design
 *
 * A miniature inline chart showing trends over time.
 * Uses Recharts LineChart with minimal configuration.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  LineChart,
  Line,
  ResponsiveContainer,
  YAxis,
} from 'recharts';

export interface SparklineProps {
  data: number[];
  color?: string;
  width?: number;
  height?: number;
  lineWidth?: number;
  showArea?: boolean;
  areaOpacity?: number;
  showPoints?: boolean;
  pointSize?: number;
  highlightMin?: boolean;
  highlightMax?: boolean;
  showTrend?: boolean;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  className?: string;
}

export const Sparkline: React.FC<SparklineProps> = ({
  data = [],
  color = '#2196F3',
  width = 100,
  height = 30,
  lineWidth = 2,
  showArea = false,
  areaOpacity = 0.2,
  showPoints = false,
  pointSize = 4,
  highlightMin = false,
  highlightMax = false,
  showTrend = false,
  animated = true,
  animationDuration = 300,
  contentDescription,
  className,
}) => {
  // Transform data
  const chartData = React.useMemo(() => {
    return data.map((value, index) => ({ index, value }));
  }, [data]);

  // Calculate trend
  const trend = React.useMemo(() => {
    if (data.length < 2) return 'flat';
    const first = data[0];
    const last = data[data.length - 1];
    const threshold = 0.01;
    if (last > first * (1 + threshold)) return 'up';
    if (last < first * (1 - threshold)) return 'down';
    return 'flat';
  }, [data]);

  // Get min/max for highlighting
  const minValue = Math.min(...data);
  const maxValue = Math.max(...data);

  const renderDot = (props: any) => {
    const { cx, cy, payload } = props;
    const isMin = highlightMin && payload.value === minValue;
    const isMax = highlightMax && payload.value === maxValue;

    if (!showPoints && !isMin && !isMax) return null;

    return (
      <circle
        cx={cx}
        cy={cy}
        r={isMin || isMax ? pointSize * 1.5 : pointSize}
        fill={isMin ? '#F44336' : isMax ? '#4CAF50' : color}
        stroke="white"
        strokeWidth={1}
      />
    );
  };

  const getTrendIcon = () => {
    if (trend === 'up') return '↑';
    if (trend === 'down') return '↓';
    return '→';
  };

  return (
    <div
      className={className}
      style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}
      aria-label={contentDescription || `Sparkline: ${trend}`}
    >
      <ResponsiveContainer width={width} height={height}>
        <LineChart data={chartData} margin={{ top: 2, right: 2, left: 2, bottom: 2 }}>
          <YAxis domain={['dataMin', 'dataMax']} hide />
          <Line
            type="monotone"
            dataKey="value"
            stroke={color}
            strokeWidth={lineWidth}
            dot={renderDot}
            fill={showArea ? color : 'none'}
            fillOpacity={showArea ? areaOpacity : 0}
            isAnimationActive={animated}
            animationDuration={animationDuration}
          />
        </LineChart>
      </ResponsiveContainer>
      {showTrend && (
        <span
          style={{
            fontSize: '1rem',
            color: trend === 'up' ? '#4CAF50' : trend === 'down' ? '#F44336' : '#666',
          }}
        >
          {getTrendIcon()}
        </span>
      )}
    </div>
  );
};

export default Sparkline;
