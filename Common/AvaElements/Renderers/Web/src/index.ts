/**
 * AVAMagic Web Renderer - Main Entry Point
 *
 * Exports all public APIs for the web renderer package.
 * Supports both legacy Phase 1 components and new Flutter Parity components.
 *
 * @module AvaElements/Web
 * @since 2.1.0
 */

// ============================================================================
// LEGACY PHASE 1 COMPONENTS (13 components)
// ============================================================================

// Main Renderer
export { MagicElementsRenderer, default as Renderer } from './MagicElementsRenderer';
export type { RendererProps } from './MagicElementsRenderer';

// Individual Component Renderers
export {
  RenderButton,
  RenderTextField,
  RenderCheckbox,
  RenderSwitch,
  RenderText,
  RenderImage,
  RenderIcon,
  RenderContainer,
  RenderRow,
  RenderColumn,
  RenderCard,
  RenderScrollView,
  RenderList,
} from './components/Phase1Components';

// Legacy Type Definitions
export type {
  Theme as LegacyTheme,
  Component,
  ButtonComponent,
  TextFieldComponent,
  CheckboxComponent,
  SwitchComponent,
  TextComponent,
  ImageComponent,
  IconComponent,
  ContainerComponent,
  RowComponent,
  ColumnComponent,
  CardComponent,
  ScrollViewComponent,
  ListComponent,
  AnyComponent,
} from './types/components';

// ============================================================================
// FLUTTER PARITY COMPONENTS (58 components)
// ============================================================================

// Core Renderer
export {
  ReactRenderer,
  renderChildren,
  withRendererSupport,
  useRendererConfig,
} from './renderer/ReactRenderer';
export type {
  ReactRendererProps,
  RendererConfig,
  ErrorFallbackProps,
} from './renderer/ReactRenderer';

// Component Registry
export {
  ComponentRegistry,
  getComponentRegistry,
  ComponentCategory,
  COMPONENT_TYPES,
} from './renderer/ComponentRegistry';
export type {
  BaseComponent,
  ComponentRenderer,
  RegistryEntry,
  ComponentType,
} from './renderer/ComponentRegistry';

// Resource Management
export {
  IconResourceManager,
  getIconResourceManager,
  IconType,
} from './resources/IconResourceManager';
export type {
  IconResource,
  MaterialIconRef,
  ResolvedIcon,
} from './resources/IconResourceManager';

export { ImageLoader } from './resources/ImageLoader';
export type {
  ImageLoaderProps,
  ImageSource,
  PlaceholderProps,
  ErrorProps,
} from './resources/ImageLoader';

// Type Definitions (All 58 Flutter Parity Components)
export type {
  Theme,
  Spacing,
  Size,
  Alignment,
  BoxFit,
  Axis,
  CrossAxisAlignment,
  MainAxisAlignment,
  MainAxisSize,
  TextStyle,
  Curve,
  Duration,
  // Layout Components
  AlignComponent,
  CenterComponent,
  ConstrainedBoxComponent,
  ExpandedComponent,
  FittedBoxComponent,
  FlexComponent,
  FlexibleComponent,
  PaddingComponent,
  SizedBoxComponent,
  WrapComponent,
  // Scrolling Components
  CustomScrollViewComponent,
  GridViewBuilderComponent,
  ListViewBuilderComponent,
  ListViewSeparatedComponent,
  PageViewComponent,
  ReorderableListViewComponent,
  SliversComponent,
  // Material Components
  ActionChipComponent,
  CheckboxListTileComponent,
  ChoiceChipComponent,
  CircleAvatarComponent,
  EndDrawerComponent,
  ExpansionTileComponent,
  FilledButtonComponent,
  FilterChipComponent,
  InputChipComponent,
  PopupMenuButtonComponent,
  RefreshIndicatorComponent,
  RichTextComponent,
  SelectableTextComponent,
  SwitchListTileComponent,
  VerticalDividerComponent,
  // Animation Components
  AnimatedAlignComponent,
  AnimatedContainerComponent,
  AnimatedCrossFadeComponent,
  AnimatedDefaultTextStyleComponent,
  AnimatedListComponent,
  AnimatedModalBarrierComponent,
  AnimatedOpacityComponent,
  AnimatedPaddingComponent,
  AnimatedPositionedComponent,
  AnimatedScaleComponent,
  AnimatedSizeComponent,
  AnimatedSwitcherComponent,
  AlignTransitionComponent,
  DecoratedBoxTransitionComponent,
  DefaultTextStyleTransitionComponent,
  FadeTransitionComponent,
  PositionedTransitionComponent,
  RelativePositionedTransitionComponent,
  RotationTransitionComponent,
  ScaleTransitionComponent,
  SizeTransitionComponent,
  SlideTransitionComponent,
  // Special Components
  FadeInImageComponent,
  HeroComponent,
  IndexedStackComponent,
  LayoutUtilitiesComponent,
  // Union Type
  FlutterParityComponent,
  isComponentType,
} from './types';

// Flutter Parity version and metadata
export {
  FLUTTER_PARITY_VERSION,
  COMPONENT_COUNTS,
} from './flutterparity';

// Flutter Phase 2 Mappers (Lists & Cards)
export {
  FlutterListMappers,
  FlutterCardMappers,
  registerFlutterPhase2Mappers,
  areFlutterPhase2MappersRegistered,
  getFlutterPhase2Count,
  PHASE_2_COUNTS,
  PHASE_2_COMPONENTS,
} from './mappers/flutter';

export type {
  RadioListTileComponent,
  PricingCardComponent,
  FeatureCardComponent,
  TestimonialCardComponent,
  ProductCardComponent,
  ArticleCardComponent,
  ImageCardComponent,
  HoverCardComponent,
  ExpandableCardComponent,
} from './mappers/flutter';

// ============================================================================
// MEL PLUGIN SYSTEM (Tier 1 + Tier 2 Support)
// ============================================================================

// MEL Components
export {
  MELPlugin,
  StatefulMELPlugin,
  MELPluginPreview,
  MELComponentFactory,
  renderUINode,
  registerComponent,
  getRegisteredComponents,
} from './mel';

export type {
  MELPluginProps,
  StatefulMELPluginProps,
  MELPluginPreviewProps,
  MELComponentFactoryProps,
} from './mel';

// MEL Hooks
export { useMELPlugin } from './mel';
export type { UseMELPluginOptions, UseMELPluginResult } from './mel';

// MEL Expression Evaluator
export { MELExpressionParser, MELExpressionEvaluator } from './mel';

// MEL Types
export type {
  PluginDefinition,
  PluginMetadata,
  PluginTier,
  StateVariable,
  StateType,
  PluginState,
  Reducer,
  Effect,
  Expression,
  ExpressionNode,
  LiteralNode,
  StateRefNode,
  FunctionCallNode,
  BinaryOpNode,
  UnaryOpNode,
  ConditionalNode,
  ArrayNode,
  ObjectNode,
  UINode,
  EventHandler,
  Script,
  PluginRuntime,
  BuiltInFunction,
  FunctionRegistry,
  EvaluationContext,
} from './mel';

export {
  MELError,
  MELSyntaxError,
  MELSecurityError,
  MELRuntimeError,
} from './mel';

// MEL Version
export { MEL_VERSION, MEL_SUPPORTED_TIERS } from './mel';

// ============================================================================
// VERSION INFO
// ============================================================================

export const VERSION = '3.3.0'; // Updated for MEL Plugin System
export const RENDERER_TYPE = 'Web/React';
export const SUPPORTED_PLATFORMS = ['Web', 'Progressive Web App', 'Electron'];
export const TOTAL_COMPONENTS = 126; // 13 legacy + 113 Flutter Parity
export const MEL_PLUGIN_SUPPORT = true;
export const MEL_TIER_SUPPORT = ['data', 'logic']; // Full Tier 2 support on Web
