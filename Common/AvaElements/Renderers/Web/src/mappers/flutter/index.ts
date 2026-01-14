/**
 * Flutter Parity Mappers - Phase 2
 *
 * Central export point for all Phase 2 Flutter parity mappers.
 * Includes list tiles, specialized cards, and additional layout components.
 *
 * @module FlutterMappers
 * @since 3.2.0
 */

// List Mappers
export {
  FlutterListMappers,
  ExpansionTileMapper,
  CheckboxListTileMapper,
  SwitchListTileMapper,
  RadioListTileMapper,
} from './flutter-list-mappers';

export type {
  RadioListTileComponent,
} from './flutter-list-mappers';

// Card Mappers
export {
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

export type {
  PricingCardComponent,
  FeatureCardComponent,
  TestimonialCardComponent,
  ProductCardComponent,
  ArticleCardComponent,
  ImageCardComponent,
  HoverCardComponent,
  ExpandableCardComponent,
} from './flutter-card-mappers';

// Registration
export {
  registerFlutterPhase2Mappers,
  areFlutterPhase2MappersRegistered,
  getFlutterPhase2Count,
} from './register-flutter-mappers';

/**
 * Phase 2 Component Counts
 */
export const PHASE_2_COUNTS = {
  lists: 4,
  cards: 8,
  layout: 1,
  total: 13,
};

/**
 * Phase 2 Component Types
 */
export const PHASE_2_COMPONENTS = {
  lists: ['ExpansionTile', 'CheckboxListTile', 'SwitchListTile', 'RadioListTile'],
  cards: [
    'PricingCard',
    'FeatureCard',
    'TestimonialCard',
    'ProductCard',
    'ArticleCard',
    'ImageCard',
    'HoverCard',
    'ExpandableCard',
  ],
  layout: ['ConstrainedBox'],
} as const;
