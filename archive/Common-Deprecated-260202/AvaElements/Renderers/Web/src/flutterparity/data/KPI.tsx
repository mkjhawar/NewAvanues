/**
 * KPI Component
 *
 * Key Performance Indicator with target, trend, and progress tracking.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { KPIProps } from './types';

/**
 * KPI - Key Performance Indicator component
 *
 * @example
 * ```tsx
 * <KPI
 *   title="Revenue"
 *   value={125000}
 *   target={150000}
 *   unit="$"
 *   progress={83.3}
 *   trend={{ value: 15.2, isUpGood: true }}
 *   status="success"
 * />
 * ```
 */
export const KPI: React.FC<KPIProps> = ({
  title,
  value,
  target,
  trend,
  unit,
  progress,
  color,
  icon,
  status = 'info',
  className = '',
  style,
  testId,
}) => {
  const statusColors = {
    success: '#10b981',
    warning: '#f59e0b',
    danger: '#ef4444',
    info: '#3b82f6',
  };

  const getTrendColor = () => {
    if (!trend) return '#6b7280';
    const isPositive = trend.value >= 0;
    const isGood = trend.isUpGood !== undefined ? trend.isUpGood : true;

    if (isPositive) {
      return isGood ? '#10b981' : '#ef4444';
    } else {
      return isGood ? '#ef4444' : '#10b981';
    }
  };

  const getTrendIcon = () => {
    if (!trend) return null;
    return trend.value >= 0 ? '↑' : '↓';
  };

  const containerClasses = ['kpi', `kpi--${status}`, className]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    borderLeftWidth: '4px',
    borderLeftColor: color || statusColors[status],
    ...style,
  };

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {/* Header */}
      <div
        className="kpi__header"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          {icon && <span className="kpi__icon">{icon}</span>}
          <h3
            className="kpi__title"
            style={{
              margin: 0,
              fontSize: '0.875rem',
              fontWeight: 600,
              color: '#6b7280',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
            }}
          >
            {title}
          </h3>
        </div>
        {trend && (
          <div
            className="kpi__trend"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.25rem',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: getTrendColor(),
            }}
          >
            <span>{getTrendIcon()}</span>
            <span>{Math.abs(trend.value)}%</span>
          </div>
        )}
      </div>

      {/* Value */}
      <div className="kpi__body">
        <div
          className="kpi__value"
          style={{
            fontSize: '2.5rem',
            fontWeight: 700,
            color: '#111827',
            lineHeight: 1.2,
          }}
        >
          {unit && <span style={{ fontSize: '0.6em' }}>{unit}</span>}
          {value}
        </div>
        {target !== undefined && (
          <div
            className="kpi__target"
            style={{
              fontSize: '0.875rem',
              color: '#6b7280',
              marginTop: '0.5rem',
            }}
          >
            Target: {unit}
            {target}
          </div>
        )}
      </div>

      {/* Progress Bar */}
      {progress !== undefined && (
        <div className="kpi__progress">
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '0.5rem',
              fontSize: '0.875rem',
              color: '#6b7280',
            }}
          >
            <span>Progress</span>
            <span style={{ fontWeight: 600 }}>{progress.toFixed(1)}%</span>
          </div>
          <div
            style={{
              width: '100%',
              height: '8px',
              backgroundColor: '#e5e7eb',
              borderRadius: '4px',
              overflow: 'hidden',
            }}
          >
            <div
              style={{
                width: `${Math.min(progress, 100)}%`,
                height: '100%',
                backgroundColor: color || statusColors[status],
                transition: 'width 0.3s ease',
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

KPI.displayName = 'KPI';
