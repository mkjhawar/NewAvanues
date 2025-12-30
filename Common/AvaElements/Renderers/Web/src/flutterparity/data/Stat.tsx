/**
 * Stat Component
 *
 * Single statistic display with optional trend indicator.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { StatProps } from './types';

/**
 * Stat - Single statistic display component
 *
 * @example
 * ```tsx
 * <Stat
 *   label="Total Users"
 *   value={12345}
 *   prefix="$"
 *   suffix="k"
 *   trend={{ value: 12.5, isUpGood: true }}
 *   size="md"
 * />
 * ```
 */
export const Stat: React.FC<StatProps> = ({
  label,
  value,
  prefix,
  suffix,
  helpText,
  trend,
  size = 'md',
  color,
  icon,
  className = '',
  style,
  testId,
}) => {
  const sizeStyles = {
    sm: {
      labelSize: '0.75rem',
      valueSize: '1.5rem',
      trendSize: '0.875rem',
      padding: '0.75rem',
    },
    md: {
      labelSize: '0.875rem',
      valueSize: '2rem',
      trendSize: '1rem',
      padding: '1rem',
    },
    lg: {
      labelSize: '1rem',
      valueSize: '2.5rem',
      trendSize: '1.125rem',
      padding: '1.25rem',
    },
  };

  const styles = sizeStyles[size];

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

  const containerClasses = ['stat', `stat--${size}`, className]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    padding: styles.padding,
    borderRadius: '0.5rem',
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    ...style,
  };

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      <div
        className="stat__header"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
        }}
      >
        {icon && <span className="stat__icon">{icon}</span>}
        <span
          className="stat__label"
          style={{
            fontSize: styles.labelSize,
            fontWeight: 500,
            color: '#6b7280',
          }}
        >
          {label}
        </span>
        {helpText && (
          <span
            className="stat__help"
            title={helpText}
            style={{
              display: 'inline-block',
              width: '1rem',
              height: '1rem',
              borderRadius: '50%',
              backgroundColor: '#e5e7eb',
              color: '#6b7280',
              fontSize: '0.75rem',
              lineHeight: '1rem',
              textAlign: 'center',
              cursor: 'help',
            }}
          >
            ?
          </span>
        )}
      </div>

      <div
        className="stat__value"
        style={{
          fontSize: styles.valueSize,
          fontWeight: 700,
          color: color || '#111827',
          lineHeight: 1.2,
        }}
      >
        {prefix && <span className="stat__prefix">{prefix}</span>}
        {value}
        {suffix && <span className="stat__suffix">{suffix}</span>}
      </div>

      {trend && (
        <div
          className="stat__trend"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.25rem',
            fontSize: styles.trendSize,
            fontWeight: 600,
            color: getTrendColor(),
          }}
        >
          <span className="stat__trend-icon">{getTrendIcon()}</span>
          <span className="stat__trend-value">{Math.abs(trend.value)}%</span>
          {trend.label && (
            <span
              className="stat__trend-label"
              style={{
                fontSize: '0.875em',
                fontWeight: 400,
                color: '#6b7280',
              }}
            >
              {trend.label}
            </span>
          )}
        </div>
      )}
    </div>
  );
};

Stat.displayName = 'Stat';
