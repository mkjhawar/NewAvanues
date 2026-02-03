/**
 * Chart Components - Flutter Parity Material Design
 *
 * A collection of chart components for data visualization.
 * All components use Recharts library for rendering.
 *
 * @since 3.0.0-flutter-parity
 */

export { LineChart } from './LineChart';
export type { LineChartProps, ChartSeries, ChartPoint } from './LineChart';

export { BarChart } from './BarChart';
export type { BarChartProps, BarGroup, BarData, BarMode, Orientation } from './BarChart';

export { PieChart } from './PieChart';
export type { PieChartProps, PieSlice } from './PieChart';

export { AreaChart } from './AreaChart';
export type { AreaChartProps, AreaSeries } from './AreaChart';

export { Gauge } from './Gauge';
export type { GaugeProps, GaugeSegment } from './Gauge';

export { Sparkline } from './Sparkline';
export type { SparklineProps } from './Sparkline';

export { RadarChart } from './RadarChart';
export type { RadarChartProps, RadarSeries } from './RadarChart';

export { ScatterChart } from './ScatterChart';
export type { ScatterChartProps, ScatterSeries, ScatterPoint } from './ScatterChart';

export { Heatmap } from './Heatmap';
export type { HeatmapProps, ColorScheme } from './Heatmap';

export { TreeMap } from './TreeMap';
export type { TreeMapProps, TreeMapItem } from './TreeMap';
