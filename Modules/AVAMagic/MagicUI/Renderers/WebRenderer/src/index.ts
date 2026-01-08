/**
 * MagicUI Web Renderer
 *
 * React/TypeScript renderer for MagicUI components with Material-UI integration
 */

// Export all components
export * from './components/AllComponents';
export { Button } from './components/Button';

// Export theme utilities
export { convertTheme, createDefaultTheme } from './theme/ThemeConverter';

// Export types
export * from './types';

// Version
export const VERSION = '1.0.0';
