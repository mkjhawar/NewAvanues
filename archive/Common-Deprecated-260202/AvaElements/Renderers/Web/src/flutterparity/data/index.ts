/**
 * Flutter Parity Data Components - Main Export
 *
 * Complete implementation of data display components for React/Web platform.
 * 12 core data components with full TypeScript support.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

// ============================================================================
// TYPE EXPORTS
// ============================================================================

export type {
  // Base types
  BaseDataComponentProps,
  Trend,
  TrendDirection,

  // Data list types
  DataItem,
  DataListProps,

  // Description list types
  DescriptionItem,
  DescriptionListProps,

  // Stat types
  StatProps,
  StatGroupProps,

  // KPI types
  KPIProps,

  // Metric card types
  MetricCardProps,

  // Leaderboard types
  RankingItem,
  RankingProps,
  LeaderboardProps,

  // Zoom types
  ZoomProps,

  // Virtual scroll types
  VirtualScrollProps,

  // Infinite scroll types
  InfiniteScrollProps,

  // QR code types
  QRCodeLevel,
  QRCodeProps,
} from './types';

// ============================================================================
// COMPONENT EXPORTS
// ============================================================================

// Data list components (2 components)
export { DataList } from './DataList';
export { DescriptionList } from './DescriptionList';

// Statistic components (4 components)
export { Stat } from './Stat';
export { StatGroup } from './StatGroup';
export { KPI } from './KPI';
export { MetricCard } from './MetricCard';

// Ranking components (2 components)
export { Ranking } from './Ranking';
export { Leaderboard } from './Leaderboard';

// Advanced components (4 components)
export { Zoom } from './Zoom';
export { VirtualScroll } from './VirtualScroll';
export { InfiniteScroll } from './InfiniteScroll';
export { QRCode } from './QRCode';

// ============================================================================
// COMPONENT COUNT: 12 CORE DATA COMPONENTS
// ============================================================================
// 1. DataList - Key-value data display
// 2. DescriptionList - Definition list (dt/dd)
// 3. Stat - Single statistic display
// 4. StatGroup - Group of statistics
// 5. KPI - Key performance indicator
// 6. MetricCard - Metric with icon and change
// 7. Ranking - Single rank item
// 8. Leaderboard - Ranked list
// 9. Zoom - Zoomable content wrapper
// 10. VirtualScroll - Virtualized list
// 11. InfiniteScroll - Load more on scroll
// 12. QRCode - QR code generator
// ============================================================================

/**
 * Usage Examples:
 *
 * ```tsx
 * import {
 *   DataList,
 *   Stat,
 *   StatGroup,
 *   KPI,
 *   Leaderboard,
 *   VirtualScroll,
 *   QRCode
 * } from './data';
 *
 * // DataList
 * <DataList
 *   items={[
 *     { key: 'Name', value: 'John Doe' },
 *     { key: 'Email', value: 'john@example.com' }
 *   ]}
 * />
 *
 * // StatGroup with Stats
 * <StatGroup columns={3} spacing={16}>
 *   <Stat
 *     label="Total Sales"
 *     value={45231}
 *     prefix="$"
 *     trend={{ value: 12.5, isUpGood: true }}
 *   />
 *   <Stat
 *     label="New Users"
 *     value={1234}
 *     trend={{ value: -5.2, isUpGood: true }}
 *   />
 *   <Stat
 *     label="Conversion"
 *     value={3.2}
 *     suffix="%"
 *   />
 * </StatGroup>
 *
 * // KPI
 * <KPI
 *   title="Revenue"
 *   value={125000}
 *   target={150000}
 *   unit="$"
 *   progress={83.3}
 *   trend={{ value: 15.2, isUpGood: true }}
 *   status="success"
 * />
 *
 * // Leaderboard
 * <Leaderboard
 *   items={[
 *     { rank: 1, name: 'John Doe', avatar: 'avatar1.jpg', score: 9523 },
 *     { rank: 2, name: 'Jane Smith', avatar: 'avatar2.jpg', score: 8954 }
 *   ]}
 *   showChange
 *   showAvatar
 *   highlightRank={1}
 * />
 *
 * // VirtualScroll
 * <VirtualScroll
 *   items={largeArray}
 *   itemHeight={50}
 *   height={500}
 *   renderItem={(item, index) => <div>{item.name}</div>}
 * />
 *
 * // InfiniteScroll
 * <InfiniteScroll
 *   hasMore={hasMore}
 *   loadMore={fetchMore}
 *   loader={<div>Loading...</div>}
 * >
 *   {items.map(item => <ItemCard key={item.id} item={item} />)}
 * </InfiniteScroll>
 *
 * // QRCode
 * <QRCode
 *   value="https://example.com"
 *   size={256}
 *   level="H"
 * />
 * ```
 */

/**
 * Component Features:
 *
 * DataList:
 * - Key-value pairs with icons
 * - Horizontal/vertical layouts
 * - Optional dividers
 * - Help text tooltips
 *
 * DescriptionList:
 * - Semantic HTML (dl/dt/dd)
 * - Flexible term width
 * - Horizontal/vertical layouts
 *
 * Stat:
 * - Trend indicators with colors
 * - Prefix/suffix support
 * - Size variants (sm/md/lg)
 * - Custom icons
 *
 * StatGroup:
 * - Grid layout with columns
 * - Configurable spacing
 * - Optional dividers
 *
 * KPI:
 * - Target comparison
 * - Progress bars
 * - Status colors
 * - Trend tracking
 *
 * MetricCard:
 * - Icon display
 * - Change percentage
 * - Hover effects
 * - Custom footer
 *
 * Ranking:
 * - Rank badges with colors (gold/silver/bronze)
 * - Avatar support
 * - Change indicators
 * - Clickable items
 *
 * Leaderboard:
 * - Multiple rankings
 * - Highlight specific rank
 * - Item click handlers
 * - Max items limit
 *
 * Zoom:
 * - Pinch zoom support
 * - Mouse wheel zoom
 * - Drag to pan
 * - Zoom controls
 *
 * VirtualScroll:
 * - High performance for large lists
 * - Configurable overscan
 * - Fixed item height
 * - Scroll callbacks
 *
 * InfiniteScroll:
 * - IntersectionObserver based
 * - Automatic loading
 * - Custom loader
 * - Inverse mode
 *
 * QRCode:
 * - SVG rendering
 * - Error correction levels
 * - Custom colors
 * - Image embedding
 */
