/**
 * Flutter Parity Mappers Registration
 *
 * Registers Phase 2 Flutter parity components with the ComponentRegistry.
 * This includes list tiles and specialized cards.
 *
 * @module RegisterFlutterMappers
 * @since 3.2.0
 */

import { getComponentRegistry, ComponentCategory } from '../../renderer/ComponentRegistry';
import {
  FlutterListMappers,
  ExpansionTileMapper,
  CheckboxListTileMapper,
  SwitchListTileMapper,
  RadioListTileMapper,
} from './flutter-list-mappers';
import {
  FlutterCardMappers,
  PricingCardMapper,
  FeatureCardMapper,
  TestimonialCardMapper,
  ProductCardMapper,
  ArticleCardMapper,
  ImageCardMapper,
  HoverCardMapper,
  ExpandableCardMapper,
} from './flutter-card-mappers';
import React from 'react';

/**
 * Register all Flutter Phase 2 mappers with the component registry
 *
 * This function should be called during application initialization to ensure
 * all Phase 2 components are available for rendering.
 *
 * Components registered:
 * - List Components (4): ExpansionTile, CheckboxListTile, SwitchListTile, RadioListTile
 * - Card Components (8): PricingCard, FeatureCard, TestimonialCard, ProductCard,
 *                         ArticleCard, ImageCard, HoverCard, ExpandableCard
 * - Layout Components (1): ConstrainedBox (Center already exists)
 *
 * Total: 13 new components
 */
export function registerFlutterPhase2Mappers(): void {
  const registry = getComponentRegistry();

  // ============================================================================
  // LIST COMPONENTS (4)
  // ============================================================================

  registry.register(
    'ExpansionTile',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(ExpansionTileMapper, { component: component as any }),
    'A single-line ListTile with a trailing button that expands to reveal more content'
  );

  registry.register(
    'CheckboxListTile',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(CheckboxListTileMapper, { component: component as any }),
    'A ListTile with a leading or trailing Checkbox'
  );

  registry.register(
    'SwitchListTile',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(SwitchListTileMapper, { component: component as any }),
    'A ListTile with a trailing Switch'
  );

  registry.register(
    'RadioListTile',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(RadioListTileMapper, { component: component as any }),
    'A ListTile with a leading or trailing Radio button'
  );

  // ============================================================================
  // CARD COMPONENTS (8)
  // ============================================================================

  registry.register(
    'PricingCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(PricingCardMapper, { component: component as any }),
    'A card for displaying pricing tiers with features, price, and CTA button'
  );

  registry.register(
    'FeatureCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(FeatureCardMapper, { component: component as any }),
    'A card for highlighting product features with icon, title, and description'
  );

  registry.register(
    'TestimonialCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(TestimonialCardMapper, { component: component as any }),
    'A card for displaying user testimonials with avatar, quote, and author info'
  );

  registry.register(
    'ProductCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(ProductCardMapper, { component: component as any }),
    'A card for displaying e-commerce products with image, title, price, and rating'
  );

  registry.register(
    'ArticleCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(ArticleCardMapper, { component: component as any }),
    'A card for displaying blog/news articles with image, title, excerpt, and metadata'
  );

  registry.register(
    'ImageCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(ImageCardMapper, { component: component as any }),
    'A card with prominent image and optional overlay text/actions'
  );

  registry.register(
    'HoverCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(HoverCardMapper, { component: component as any }),
    'A card with hover effects and actions that appear on interaction'
  );

  registry.register(
    'ExpandableCard',
    ComponentCategory.MATERIAL,
    (component) => React.createElement(ExpandableCardMapper, { component: component as any }),
    'A card that can expand/collapse to show more content'
  );

  // Note: Center component already exists in Align.tsx
  // Note: ConstrainedBox component created but needs to be integrated with the renderer system
}

/**
 * Check if Phase 2 mappers are registered
 *
 * @returns true if all Phase 2 mappers are registered
 */
export function areFlutterPhase2MappersRegistered(): boolean {
  const registry = getComponentRegistry();

  const phase2Components = [
    'ExpansionTile',
    'CheckboxListTile',
    'SwitchListTile',
    'RadioListTile',
    'PricingCard',
    'FeatureCard',
    'TestimonialCard',
    'ProductCard',
    'ArticleCard',
    'ImageCard',
    'HoverCard',
    'ExpandableCard',
  ];

  return phase2Components.every((type) => registry.isRegistered(type));
}

/**
 * Get count of registered Phase 2 components
 *
 * @returns number of registered Phase 2 components
 */
export function getFlutterPhase2Count(): number {
  const registry = getComponentRegistry();

  const phase2Components = [
    'ExpansionTile',
    'CheckboxListTile',
    'SwitchListTile',
    'RadioListTile',
    'PricingCard',
    'FeatureCard',
    'TestimonialCard',
    'ProductCard',
    'ArticleCard',
    'ImageCard',
    'HoverCard',
    'ExpandableCard',
  ];

  return phase2Components.filter((type) => registry.isRegistered(type)).length;
}

export default registerFlutterPhase2Mappers;
