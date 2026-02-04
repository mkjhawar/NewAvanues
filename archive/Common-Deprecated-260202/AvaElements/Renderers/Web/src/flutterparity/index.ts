/**
 * Flutter Parity Components - Main Export
 *
 * Re-exports all Flutter Parity component renderers.
 * Components are organized by category for Agent 2 and Agent 3.
 *
 * @module FlutterParity
 * @since 2.1.0
 */

// Layout components
export * from './layout';

// Navigation components
export * from './navigation';

// Data components
export * from './data';

// Charts components
export * from './charts';

// Material components
export * from './material';

// Animation components
export * from './animation';

// Transition components
export * from './transitions';

// Calendar components
export * from './calendar';

// Scrolling components
export * from './scrolling';

// Sliver components
export * from './slivers';

// Other components
export * from './other';

/**
 * Integration points for other agents:
 *
 * AGENT 2 (Layout Components):
 * - Implement ./layout/index.ts with 18 layout components
 * - Implement ./layout/scrolling/index.ts with 7 scrolling components
 * - Total: 25 components
 *
 * AGENT 3 (Material & Animation):
 * - Implement ./material/index.ts with 15 material components
 * - Implement ./animation/index.ts with 18 animation components
 * - Total: 33 components
 *
 * All components should:
 * 1. Use the ComponentRegistry to register themselves
 * 2. Accept component props from types/index.ts
 * 3. Return React elements
 * 4. Handle children rendering using renderChildren() from ReactRenderer
 * 5. Support theme configuration
 * 6. Include error boundaries
 */

export const FLUTTER_PARITY_VERSION = '2.1.0';

export const COMPONENT_COUNTS = {
  layout: 18,
  scrolling: 7,
  slivers: 4,
  navigation: 9,
  data: 12,
  charts: 10,
  material: 17, // Added PopupMenuButton and RefreshIndicator
  animation: 18,
  transitions: 11,
  calendar: 5,
  other: 2, // FadeInImage and CircleAvatar
  total: 113,
};
