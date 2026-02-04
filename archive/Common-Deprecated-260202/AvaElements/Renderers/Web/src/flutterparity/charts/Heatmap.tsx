/**
 * Heatmap Component - Flutter Parity Material Design
 *
 * A heatmap visualization for matrix data.
 * Custom implementation using CSS Grid and color mapping.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';

export type ColorScheme = 'blueRed' | 'greenRed' | 'blueYellowRed' | 'purpleWhiteOrange' | 'grayscale';

export interface HeatmapProps {
  data: number[][];
  rowLabels?: string[];
  columnLabels?: string[];
  title?: string;
  colorScheme?: ColorScheme;
  showValues?: boolean;
  showGrid?: boolean;
  cellSize?: number;
  minValue?: number;
  maxValue?: number;
  animated?: boolean;
  animationDuration?: number;
  contentDescription?: string;
  onCellClick?: (row: number, column: number, value: number) => void;
  className?: string;
}

export const Heatmap: React.FC<HeatmapProps> = ({
  data = [],
  rowLabels = [],
  columnLabels = [],
  title,
  colorScheme = 'blueRed',
  showValues = true,
  showGrid = true,
  cellSize = 50,
  minValue,
  maxValue,
  animated = true,
  contentDescription,
  onCellClick,
  className,
}) => {
  // Calculate value range
  const valueRange = React.useMemo(() => {
    const flatData = data.flat();
    const min = minValue ?? Math.min(...flatData);
    const max = maxValue ?? Math.max(...flatData);
    return { min, max };
  }, [data, minValue, maxValue]);

  // Get normalized value (0-1)
  const getNormalizedValue = (value: number) => {
    const { min, max } = valueRange;
    if (max === min) return 0.5;
    return (value - min) / (max - min);
  };

  // Color interpolation
  const interpolateColor = (color1: string, color2: string, ratio: number) => {
    const hex = (c: string) => parseInt(c.substring(1), 16);
    const r1 = (hex(color1) >> 16) & 0xff;
    const g1 = (hex(color1) >> 8) & 0xff;
    const b1 = hex(color1) & 0xff;
    const r2 = (hex(color2) >> 16) & 0xff;
    const g2 = (hex(color2) >> 8) & 0xff;
    const b2 = hex(color2) & 0xff;

    const r = Math.round(r1 + (r2 - r1) * ratio);
    const g = Math.round(g1 + (g2 - g1) * ratio);
    const b = Math.round(b1 + (b2 - b1) * ratio);

    return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
  };

  // Get color for value
  const getColorForValue = (value: number) => {
    const normalized = getNormalizedValue(value);

    switch (colorScheme) {
      case 'blueRed':
        return interpolateColor('#2196F3', '#F44336', normalized);
      case 'greenRed':
        return interpolateColor('#4CAF50', '#F44336', normalized);
      case 'blueYellowRed':
        if (normalized < 0.5) {
          return interpolateColor('#2196F3', '#FFEB3B', normalized * 2);
        }
        return interpolateColor('#FFEB3B', '#F44336', (normalized - 0.5) * 2);
      case 'purpleWhiteOrange':
        if (normalized < 0.5) {
          return interpolateColor('#9C27B0', '#FFFFFF', normalized * 2);
        }
        return interpolateColor('#FFFFFF', '#FF9800', (normalized - 0.5) * 2);
      case 'grayscale':
        const gray = Math.round(255 * (1 - normalized));
        const hex = gray.toString(16).padStart(2, '0');
        return `#${hex}${hex}${hex}`;
      default:
        return '#2196F3';
    }
  };

  const rows = data.length;
  const cols = data[0]?.length || 0;

  return (
    <div className={className} aria-label={contentDescription || title || 'Heatmap'}>
      {title && <h3 style={{ textAlign: 'center', marginBottom: '1rem' }}>{title}</h3>}
      <div style={{ display: 'inline-block' }}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: `${columnLabels.length > 0 ? 'auto ' : ''}repeat(${cols}, ${cellSize}px)`,
            gridTemplateRows: `${rowLabels.length > 0 ? 'auto ' : ''}repeat(${rows}, ${cellSize}px)`,
            gap: showGrid ? '1px' : '0',
            backgroundColor: showGrid ? '#e0e0e0' : 'transparent',
          }}
        >
          {/* Top-left corner cell */}
          {columnLabels.length > 0 && rowLabels.length > 0 && <div />}

          {/* Column labels */}
          {columnLabels.length > 0 &&
            columnLabels.map((label, idx) => (
              <div
                key={`col-${idx}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.75rem',
                  fontWeight: 'bold',
                  padding: '0.25rem',
                }}
              >
                {label}
              </div>
            ))}

          {/* Data cells with row labels */}
          {data.map((row, rowIdx) => (
            <React.Fragment key={`row-${rowIdx}`}>
              {rowLabels.length > 0 && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    fontSize: '0.75rem',
                    fontWeight: 'bold',
                    padding: '0.25rem',
                  }}
                >
                  {rowLabels[rowIdx]}
                </div>
              )}
              {row.map((value, colIdx) => {
                const bgColor = getColorForValue(value);
                const textColor = getNormalizedValue(value) > 0.5 ? 'white' : 'black';

                return (
                  <div
                    key={`cell-${rowIdx}-${colIdx}`}
                    style={{
                      backgroundColor: bgColor,
                      color: textColor,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.75rem',
                      cursor: onCellClick ? 'pointer' : 'default',
                      transition: animated ? 'all 0.3s ease' : 'none',
                    }}
                    onClick={() => onCellClick?.(rowIdx, colIdx, value)}
                  >
                    {showValues && value.toFixed(1)}
                  </div>
                );
              })}
            </React.Fragment>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Heatmap;
