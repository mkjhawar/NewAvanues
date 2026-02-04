/**
 * Component Registry for AVAMagic Flutter Parity Components
 *
 * Maps component type strings to their React renderer implementations.
 * This registry supports lazy-loading of component modules for optimal tree-shaking.
 *
 * Component Categories:
 * - Layout (18): Align, Center, ConstrainedBox, Expanded, FittedBox, Flex, Flexible, Padding, SizedBox, Wrap, etc.
 * - Material (15): ActionChip, CheckboxListTile, ChoiceChip, CircleAvatar, EndDrawer, ExpansionTile, etc.
 * - Animation (25): AnimatedAlign, AnimatedContainer, FadeTransition, ScaleTransition, etc.
 *
 * @module ComponentRegistry
 * @since 2.1.0
 */

import type { ReactElement } from 'react';

/**
 * Component type categories for organizational purposes
 */
export enum ComponentCategory {
  LAYOUT = 'layout',
  MATERIAL = 'material',
  ANIMATION = 'animation',
  SCROLLING = 'scrolling',
}

/**
 * Base component interface - all components must have a type field
 */
export interface BaseComponent {
  type: string;
  key?: string;
}

/**
 * Renderer function signature
 * Takes a component configuration and returns a React element
 */
export type ComponentRenderer<T extends BaseComponent = BaseComponent> = (
  component: T
) => ReactElement | null;

/**
 * Registry entry metadata
 */
export interface RegistryEntry {
  category: ComponentCategory;
  renderer: ComponentRenderer;
  description: string;
}

/**
 * Component type map for all 58 Flutter Parity components
 */
export const COMPONENT_TYPES = {
  // Layout Components (18)
  ALIGN: 'Align',
  CENTER: 'Center',
  CONSTRAINED_BOX: 'ConstrainedBox',
  EXPANDED: 'Expanded',
  FITTED_BOX: 'FittedBox',
  FLEX: 'Flex',
  FLEXIBLE: 'Flexible',
  PADDING: 'Padding',
  SIZED_BOX: 'SizedBox',
  WRAP: 'Wrap',

  // Scrolling Components (7)
  CUSTOM_SCROLL_VIEW: 'CustomScrollView',
  GRID_VIEW_BUILDER: 'GridViewBuilder',
  LIST_VIEW_BUILDER: 'ListViewBuilder',
  LIST_VIEW_SEPARATED: 'ListViewSeparated',
  PAGE_VIEW: 'PageView',
  REORDERABLE_LIST_VIEW: 'ReorderableListView',
  SLIVERS: 'Slivers',

  // Material Components (15)
  ACTION_CHIP: 'ActionChip',
  CHECKBOX_LIST_TILE: 'CheckboxListTile',
  CHOICE_CHIP: 'ChoiceChip',
  CIRCLE_AVATAR: 'CircleAvatar',
  END_DRAWER: 'EndDrawer',
  EXPANSION_TILE: 'ExpansionTile',
  FILLED_BUTTON: 'FilledButton',
  FILTER_CHIP: 'FilterChip',
  INPUT_CHIP: 'InputChip',
  POPUP_MENU_BUTTON: 'PopupMenuButton',
  REFRESH_INDICATOR: 'RefreshIndicator',
  RICH_TEXT: 'RichText',
  SELECTABLE_TEXT: 'SelectableText',
  SWITCH_LIST_TILE: 'SwitchListTile',
  VERTICAL_DIVIDER: 'VerticalDivider',

  // Animation Components (18)
  ANIMATED_ALIGN: 'AnimatedAlign',
  ANIMATED_CONTAINER: 'AnimatedContainer',
  ANIMATED_CROSS_FADE: 'AnimatedCrossFade',
  ANIMATED_DEFAULT_TEXT_STYLE: 'AnimatedDefaultTextStyle',
  ANIMATED_LIST: 'AnimatedList',
  ANIMATED_MODAL_BARRIER: 'AnimatedModalBarrier',
  ANIMATED_OPACITY: 'AnimatedOpacity',
  ANIMATED_PADDING: 'AnimatedPadding',
  ANIMATED_POSITIONED: 'AnimatedPositioned',
  ANIMATED_SCALE: 'AnimatedScale',
  ANIMATED_SIZE: 'AnimatedSize',
  ANIMATED_SWITCHER: 'AnimatedSwitcher',
  ALIGN_TRANSITION: 'AlignTransition',
  DECORATED_BOX_TRANSITION: 'DecoratedBoxTransition',
  DEFAULT_TEXT_STYLE_TRANSITION: 'DefaultTextStyleTransition',
  FADE_TRANSITION: 'FadeTransition',
  POSITIONED_TRANSITION: 'PositionedTransition',
  RELATIVE_POSITIONED_TRANSITION: 'RelativePositionedTransition',
  ROTATION_TRANSITION: 'RotationTransition',
  SCALE_TRANSITION: 'ScaleTransition',
  SIZE_TRANSITION: 'SizeTransition',
  SLIDE_TRANSITION: 'SlideTransition',

  // Special Components (4)
  FADE_IN_IMAGE: 'FadeInImage',
  HERO: 'Hero',
  INDEXED_STACK: 'IndexedStack',
  LAYOUT_UTILITIES: 'LayoutUtilities',
} as const;

/**
 * Component type union
 */
export type ComponentType = typeof COMPONENT_TYPES[keyof typeof COMPONENT_TYPES];

/**
 * Component registry class
 * Manages component renderers and provides lookup functionality
 */
export class ComponentRegistry {
  private static instance: ComponentRegistry;
  private registry: Map<string, RegistryEntry> = new Map();

  private constructor() {
    // Private constructor for singleton pattern
  }

  /**
   * Get singleton instance
   */
  public static getInstance(): ComponentRegistry {
    if (!ComponentRegistry.instance) {
      ComponentRegistry.instance = new ComponentRegistry();
    }
    return ComponentRegistry.instance;
  }

  /**
   * Register a component renderer
   */
  public register(
    type: string,
    category: ComponentCategory,
    renderer: ComponentRenderer,
    description: string = ''
  ): void {
    this.registry.set(type, { category, renderer, description });
  }

  /**
   * Get renderer for a component type
   */
  public getRenderer(type: string): ComponentRenderer | undefined {
    return this.registry.get(type)?.renderer;
  }

  /**
   * Get all registered component types
   */
  public getRegisteredTypes(): string[] {
    return Array.from(this.registry.keys());
  }

  /**
   * Get components by category
   */
  public getByCategory(category: ComponentCategory): string[] {
    return Array.from(this.registry.entries())
      .filter(([, entry]) => entry.category === category)
      .map(([type]) => type);
  }

  /**
   * Check if a component type is registered
   */
  public isRegistered(type: string): boolean {
    return this.registry.has(type);
  }

  /**
   * Get total number of registered components
   */
  public getCount(): number {
    return this.registry.size;
  }

  /**
   * Clear all registrations (mainly for testing)
   */
  public clear(): void {
    this.registry.clear();
  }
}

/**
 * Get the global component registry instance
 */
export const getComponentRegistry = (): ComponentRegistry => {
  return ComponentRegistry.getInstance();
};
