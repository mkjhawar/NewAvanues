/**
 * Gauge Component - Flutter Parity Material Design
 *
 * A circular gauge/meter displaying a value within a range.
 * Uses Recharts RadialBarChart for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  RadialBarChart,
  RadialBar,
  PolarAngleAxis,
  ResponsiveContainer,
} from 'recharts';

export interface GaugeSegment {
  start: number;
  end: number;
  color: string;
  label?: string;
}

export interface GaugeProps {
  value: number;
  min?: number;
  max?: number;
  label?: string;
  unit?: string;
  startAngle?: number;
  sweepAngle?: number;
  thickness?: number;
  segments?: GaugeSegment[];
  size?: number;
  showValue?: boolean;
  valueFormat?: string;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  className?: string;
}

export const Gauge: React.FC<GaugeProps> = ({
  value,
  min = 0,
  max = 100,
  label,
  unit,
  startAngle = 135,
  sweepAngle = 270,
  thickness = 20,
  segments = [],
  size = 200,
  showValue = true,
  animated = true,
  animationDuration = 1000,
  contentDescription,
  className,
}) => {
  // Calculate normalized value
  const normalizedValue = React.useMemo(() => {
    const range = max - min;
    if (range === 0) return 0;
    return Math.max(0, Math.min(100, ((value - min) / range) * 100));
  }, [value, min, max]);

  // Get color based on segments
  const getValueColor = () => {
    if (segments.length === 0) return '#2196F3';
    const segment = segments.find(s => value >= s.start && value <= s.end);
    return segment?.color || '#2196F3';
  };

  const color = getValueColor();

  // Format value for display
  const displayValue = unit ? `${value.toFixed(1)}${unit}` : value.toFixed(1);

  // Data for RadialBar
  const data = [
    {
      name: label || 'Value',
      value: normalizedValue,
      fill: color,
    },
  ];

  return (
    <div className={className} aria-label={contentDescription || `Gauge: ${displayValue}`}>
      <div style={{ position: 'relative', width: size, height: size }}>
        <ResponsiveContainer width="100%" height="100%">
          <RadialBarChart
            cx="50%"
            cy="50%"
            innerRadius={`${60 - (thickness / 2)}%`}
            outerRadius={`${60 + (thickness / 2)}%`}
            barSize={thickness}
            data={data}
            startAngle={startAngle}
            endAngle={startAngle - sweepAngle}
          >
            <PolarAngleAxis
              type="number"
              domain={[0, 100]}
              angleAxisId={0}
              tick={false}
            />
            <RadialBar
              background
              dataKey="value"
              cornerRadius={10}
              isAnimationActive={animated}
              animationDuration={animationDuration}
            />
          </RadialBarChart>
        </ResponsiveContainer>
        {showValue && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color }}>
              {displayValue}
            </div>
            {label && (
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary, #666)', marginTop: '0.25rem' }}>
                {label}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Gauge;
