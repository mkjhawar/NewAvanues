/**
 * AVAMagic Flutter Parity Type Definitions
 *
 * TypeScript type definitions for all 58 Flutter Parity components.
 * Organized by category: Layout, Material, Animation, Scrolling.
 *
 * @module Types
 * @since 2.1.0
 */

// ============================================================================
// CORE TYPES
// ============================================================================

/**
 * Base component interface - all components extend this
 */
export interface BaseComponent {
  type: string;
  key?: string;
}

/**
 * Theme configuration
 */
export interface Theme {
  colors: {
    primary: string;
    secondary: string;
    background: string;
    surface: string;
    error: string;
    onPrimary: string;
    onSecondary: string;
    onBackground: string;
    onSurface: string;
    onError: string;
    [key: string]: string;
  };
  typography: {
    fontFamily: string;
    fontSize: number;
    fontWeight: string | number;
    lineHeight: number;
    [key: string]: string | number;
  };
  spacing: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
    [key: string]: number;
  };
  borderRadius: {
    sm: number;
    md: number;
    lg: number;
    full: number;
    [key: string]: number;
  };
}

/**
 * Spacing/EdgeInsets configuration
 */
export interface Spacing {
  all?: number;
  horizontal?: number;
  vertical?: number;
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
}

/**
 * Size configuration
 */
export interface Size {
  width?: number;
  height?: number;
}

/**
 * Alignment enumeration
 */
export type Alignment =
  | 'topLeft'
  | 'topCenter'
  | 'topRight'
  | 'centerLeft'
  | 'center'
  | 'centerRight'
  | 'bottomLeft'
  | 'bottomCenter'
  | 'bottomRight';

/**
 * Box fit enumeration
 */
export type BoxFit = 'contain' | 'cover' | 'fill' | 'fitHeight' | 'fitWidth' | 'none' | 'scaleDown';

/**
 * Axis enumeration
 */
export type Axis = 'horizontal' | 'vertical';

/**
 * Cross axis alignment
 */
export type CrossAxisAlignment = 'start' | 'center' | 'end' | 'stretch' | 'baseline';

/**
 * Main axis alignment
 */
export type MainAxisAlignment =
  | 'start'
  | 'center'
  | 'end'
  | 'spaceBetween'
  | 'spaceAround'
  | 'spaceEvenly';

/**
 * Main axis size
 */
export type MainAxisSize = 'min' | 'max';

/**
 * Text style
 */
export interface TextStyle {
  color?: string;
  fontSize?: number;
  fontWeight?: string | number;
  fontFamily?: string;
  fontStyle?: 'normal' | 'italic';
  letterSpacing?: number;
  wordSpacing?: number;
  textDecoration?: 'none' | 'underline' | 'overline' | 'lineThrough';
  textDecorationStyle?: 'solid' | 'double' | 'dotted' | 'dashed' | 'wavy';
  height?: number;
}

/**
 * Animation curve
 */
export type Curve =
  | 'linear'
  | 'easeIn'
  | 'easeOut'
  | 'easeInOut'
  | 'fastOutSlowIn'
  | 'bounceIn'
  | 'bounceOut'
  | 'elasticIn'
  | 'elasticOut';

/**
 * Duration in milliseconds
 */
export type Duration = number;

// ============================================================================
// LAYOUT COMPONENTS (18)
// ============================================================================

/**
 * Align Component
 * A widget that aligns its child within itself
 */
export interface AlignComponent extends BaseComponent {
  type: 'Align';
  alignment?: Alignment;
  widthFactor?: number;
  heightFactor?: number;
  child: FlutterParityComponent;
}

/**
 * Center Component
 * A widget that centers its child within itself
 */
export interface CenterComponent extends BaseComponent {
  type: 'Center';
  widthFactor?: number;
  heightFactor?: number;
  child: FlutterParityComponent;
}

/**
 * ConstrainedBox Component
 * A widget that imposes additional constraints on its child
 */
export interface ConstrainedBoxComponent extends BaseComponent {
  type: 'ConstrainedBox';
  minWidth?: number;
  maxWidth?: number;
  minHeight?: number;
  maxHeight?: number;
  child: FlutterParityComponent;
}

/**
 * Expanded Component
 * A widget that expands a child of a Row, Column, or Flex
 */
export interface ExpandedComponent extends BaseComponent {
  type: 'Expanded';
  flex?: number;
  child: FlutterParityComponent;
}

/**
 * FittedBox Component
 * Scales and positions its child within itself according to fit
 */
export interface FittedBoxComponent extends BaseComponent {
  type: 'FittedBox';
  fit?: BoxFit;
  alignment?: Alignment;
  child: FlutterParityComponent;
}

/**
 * Flex Component
 * A widget that displays its children in a one-dimensional array
 */
export interface FlexComponent extends BaseComponent {
  type: 'Flex';
  direction: Axis;
  mainAxisAlignment?: MainAxisAlignment;
  mainAxisSize?: MainAxisSize;
  crossAxisAlignment?: CrossAxisAlignment;
  children: FlutterParityComponent[];
}

/**
 * Flexible Component
 * A widget that controls how a child of a Row, Column, or Flex flexes
 */
export interface FlexibleComponent extends BaseComponent {
  type: 'Flexible';
  flex?: number;
  fit?: 'tight' | 'loose';
  child: FlutterParityComponent;
}

/**
 * Padding Component
 * A widget that insets its child by the given padding
 */
export interface PaddingComponent extends BaseComponent {
  type: 'Padding';
  padding: Spacing;
  child: FlutterParityComponent;
}

/**
 * SizedBox Component
 * A box with a specified size
 */
export interface SizedBoxComponent extends BaseComponent {
  type: 'SizedBox';
  width?: number;
  height?: number;
  child?: FlutterParityComponent;
}

/**
 * Wrap Component
 * A widget that displays its children in multiple horizontal or vertical runs
 */
export interface WrapComponent extends BaseComponent {
  type: 'Wrap';
  direction?: Axis;
  alignment?: MainAxisAlignment;
  spacing?: number;
  runSpacing?: number;
  crossAxisAlignment?: CrossAxisAlignment;
  children: FlutterParityComponent[];
}

// ============================================================================
// SCROLLING COMPONENTS (7)
// ============================================================================

/**
 * CustomScrollView Component
 * A ScrollView that creates custom scroll effects using slivers
 */
export interface CustomScrollViewComponent extends BaseComponent {
  type: 'CustomScrollView';
  slivers: FlutterParityComponent[];
}

/**
 * GridViewBuilder Component
 * A scrollable, 2D array of widgets
 */
export interface GridViewBuilderComponent extends BaseComponent {
  type: 'GridViewBuilder';
  itemCount: number;
  crossAxisCount: number;
  mainAxisSpacing?: number;
  crossAxisSpacing?: number;
  childAspectRatio?: number;
  itemBuilder: (index: number) => FlutterParityComponent;
}

/**
 * ListViewBuilder Component
 * A scrollable list of widgets arranged linearly
 */
export interface ListViewBuilderComponent extends BaseComponent {
  type: 'ListViewBuilder';
  itemCount: number;
  itemBuilder: (index: number) => FlutterParityComponent;
  separatorBuilder?: (index: number) => FlutterParityComponent;
}

/**
 * ListViewSeparated Component
 * A scrollable list of widgets with separators
 */
export interface ListViewSeparatedComponent extends BaseComponent {
  type: 'ListViewSeparated';
  itemCount: number;
  itemBuilder: (index: number) => FlutterParityComponent;
  separatorBuilder: (index: number) => FlutterParityComponent;
}

/**
 * PageView Component
 * A scrollable list that works page by page
 */
export interface PageViewComponent extends BaseComponent {
  type: 'PageView';
  children: FlutterParityComponent[];
  onPageChanged?: (index: number) => void;
}

/**
 * ReorderableListView Component
 * A list whose items can be reordered by dragging
 */
export interface ReorderableListViewComponent extends BaseComponent {
  type: 'ReorderableListView';
  children: FlutterParityComponent[];
  onReorder: (oldIndex: number, newIndex: number) => void;
}

/**
 * Slivers Component (container for sliver widgets)
 */
export interface SliversComponent extends BaseComponent {
  type: 'Slivers';
  children: FlutterParityComponent[];
}

// ============================================================================
// MATERIAL COMPONENTS (15)
// ============================================================================

/**
 * ActionChip Component
 * A Material Design action chip
 */
export interface ActionChipComponent extends BaseComponent {
  type: 'ActionChip';
  label: string;
  onPressed?: () => void;
  avatar?: string;
}

/**
 * CheckboxListTile Component
 * A ListTile with a Checkbox
 */
export interface CheckboxListTileComponent extends BaseComponent {
  type: 'CheckboxListTile';
  title: string;
  subtitle?: string;
  value: boolean;
  onChanged?: (value: boolean) => void;
}

/**
 * ChoiceChip Component
 * A Material Design choice chip
 */
export interface ChoiceChipComponent extends BaseComponent {
  type: 'ChoiceChip';
  label: string;
  selected: boolean;
  onSelected?: (selected: boolean) => void;
}

/**
 * CircleAvatar Component
 * A circle that represents a user
 */
export interface CircleAvatarComponent extends BaseComponent {
  type: 'CircleAvatar';
  radius?: number;
  backgroundImage?: string;
  backgroundColor?: string;
  child?: FlutterParityComponent;
}

/**
 * EndDrawer Component
 * A Material Design panel that slides in from the edge
 */
export interface EndDrawerComponent extends BaseComponent {
  type: 'EndDrawer';
  child: FlutterParityComponent;
}

/**
 * ExpansionTile Component
 * A single-line ListTile with a trailing button that expands to reveal more
 */
export interface ExpansionTileComponent extends BaseComponent {
  type: 'ExpansionTile';
  title: string;
  subtitle?: string;
  children: FlutterParityComponent[];
  initiallyExpanded?: boolean;
}

/**
 * FilledButton Component
 * A Material Design filled button
 */
export interface FilledButtonComponent extends BaseComponent {
  type: 'FilledButton';
  text: string;
  onPressed?: () => void;
  enabled?: boolean;
}

/**
 * FilterChip Component
 * A Material Design filter chip
 */
export interface FilterChipComponent extends BaseComponent {
  type: 'FilterChip';
  label: string;
  selected: boolean;
  onSelected?: (selected: boolean) => void;
}

/**
 * InputChip Component
 * A Material Design input chip
 */
export interface InputChipComponent extends BaseComponent {
  type: 'InputChip';
  label: string;
  onDeleted?: () => void;
  avatar?: string;
}

/**
 * PopupMenuButton Component
 * Displays a menu when pressed
 */
export interface PopupMenuButtonComponent extends BaseComponent {
  type: 'PopupMenuButton';
  items: Array<{ value: string; label: string }>;
  onSelected?: (value: string) => void;
}

/**
 * RefreshIndicator Component
 * A widget that supports pull-to-refresh
 */
export interface RefreshIndicatorComponent extends BaseComponent {
  type: 'RefreshIndicator';
  onRefresh: () => Promise<void>;
  child: FlutterParityComponent;
}

/**
 * RichText Component
 * A paragraph of rich text
 */
export interface RichTextComponent extends BaseComponent {
  type: 'RichText';
  spans: Array<{ text: string; style?: TextStyle }>;
}

/**
 * SelectableText Component
 * A text widget that allows selection
 */
export interface SelectableTextComponent extends BaseComponent {
  type: 'SelectableText';
  content: string;
  style?: TextStyle;
}

/**
 * SwitchListTile Component
 * A ListTile with a Switch
 */
export interface SwitchListTileComponent extends BaseComponent {
  type: 'SwitchListTile';
  title: string;
  subtitle?: string;
  value: boolean;
  onChanged?: (value: boolean) => void;
}

/**
 * VerticalDivider Component
 * A one device pixel thick vertical line
 */
export interface VerticalDividerComponent extends BaseComponent {
  type: 'VerticalDivider';
  width?: number;
  thickness?: number;
  color?: string;
}

// ============================================================================
// ANIMATION COMPONENTS (18)
// ============================================================================

/**
 * AnimatedAlign Component
 * Animated version of Align
 */
export interface AnimatedAlignComponent extends BaseComponent {
  type: 'AnimatedAlign';
  alignment: Alignment;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedContainer Component
 * A container that gradually changes its values over a period of time
 */
export interface AnimatedContainerComponent extends BaseComponent {
  type: 'AnimatedContainer';
  width?: number;
  height?: number;
  color?: string;
  padding?: Spacing;
  margin?: Spacing;
  duration: Duration;
  curve?: Curve;
  child?: FlutterParityComponent;
}

/**
 * AnimatedCrossFade Component
 * A widget that cross-fades between two children
 */
export interface AnimatedCrossFadeComponent extends BaseComponent {
  type: 'AnimatedCrossFade';
  firstChild: FlutterParityComponent;
  secondChild: FlutterParityComponent;
  crossFadeState: 'showFirst' | 'showSecond';
  duration: Duration;
  curve?: Curve;
}

/**
 * AnimatedDefaultTextStyle Component
 * Animated version of DefaultTextStyle
 */
export interface AnimatedDefaultTextStyleComponent extends BaseComponent {
  type: 'AnimatedDefaultTextStyle';
  style: TextStyle;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedList Component
 * A scrolling container that animates items when inserted or removed
 */
export interface AnimatedListComponent extends BaseComponent {
  type: 'AnimatedList';
  itemCount: number;
  itemBuilder: (index: number) => FlutterParityComponent;
}

/**
 * AnimatedModalBarrier Component
 * A widget that prevents the user from interacting with widgets behind itself
 */
export interface AnimatedModalBarrierComponent extends BaseComponent {
  type: 'AnimatedModalBarrier';
  color: string;
  dismissible?: boolean;
}

/**
 * AnimatedOpacity Component
 * Animated version of Opacity
 */
export interface AnimatedOpacityComponent extends BaseComponent {
  type: 'AnimatedOpacity';
  opacity: number;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedPadding Component
 * Animated version of Padding
 */
export interface AnimatedPaddingComponent extends BaseComponent {
  type: 'AnimatedPadding';
  padding: Spacing;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedPositioned Component
 * Animated version of Positioned
 */
export interface AnimatedPositionedComponent extends BaseComponent {
  type: 'AnimatedPositioned';
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
  width?: number;
  height?: number;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedScale Component
 * Animated version of Transform.scale
 */
export interface AnimatedScaleComponent extends BaseComponent {
  type: 'AnimatedScale';
  scale: number;
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedSize Component
 * A widget that automatically transitions its size over a duration
 */
export interface AnimatedSizeComponent extends BaseComponent {
  type: 'AnimatedSize';
  duration: Duration;
  curve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AnimatedSwitcher Component
 * A widget that fades and slides between new and old widgets
 */
export interface AnimatedSwitcherComponent extends BaseComponent {
  type: 'AnimatedSwitcher';
  duration: Duration;
  switchInCurve?: Curve;
  switchOutCurve?: Curve;
  child: FlutterParityComponent;
}

/**
 * AlignTransition Component
 * Animated version of Align that animates its alignment
 */
export interface AlignTransitionComponent extends BaseComponent {
  type: 'AlignTransition';
  alignment: Alignment;
  child: FlutterParityComponent;
}

/**
 * DecoratedBoxTransition Component
 * Animated version of a DecoratedBox
 */
export interface DecoratedBoxTransitionComponent extends BaseComponent {
  type: 'DecoratedBoxTransition';
  decoration: {
    color?: string;
    borderRadius?: number;
    border?: { width: number; color: string };
  };
  child: FlutterParityComponent;
}

/**
 * DefaultTextStyleTransition Component
 * Animated version of DefaultTextStyle
 */
export interface DefaultTextStyleTransitionComponent extends BaseComponent {
  type: 'DefaultTextStyleTransition';
  style: TextStyle;
  child: FlutterParityComponent;
}

/**
 * FadeTransition Component
 * Animates the opacity of a widget
 */
export interface FadeTransitionComponent extends BaseComponent {
  type: 'FadeTransition';
  opacity: number;
  child: FlutterParityComponent;
}

/**
 * PositionedTransition Component
 * Animated version of Positioned
 */
export interface PositionedTransitionComponent extends BaseComponent {
  type: 'PositionedTransition';
  rect: { left: number; top: number; right: number; bottom: number };
  child: FlutterParityComponent;
}

/**
 * RelativePositionedTransition Component
 * Animated version of Positioned with relative positioning
 */
export interface RelativePositionedTransitionComponent extends BaseComponent {
  type: 'RelativePositionedTransition';
  size: Size;
  rect: { left: number; top: number; right: number; bottom: number };
  child: FlutterParityComponent;
}

/**
 * RotationTransition Component
 * Animates the rotation of a widget
 */
export interface RotationTransitionComponent extends BaseComponent {
  type: 'RotationTransition';
  turns: number;
  child: FlutterParityComponent;
}

/**
 * ScaleTransition Component
 * Animates the scale of a widget
 */
export interface ScaleTransitionComponent extends BaseComponent {
  type: 'ScaleTransition';
  scale: number;
  child: FlutterParityComponent;
}

/**
 * SizeTransition Component
 * Animates its own size
 */
export interface SizeTransitionComponent extends BaseComponent {
  type: 'SizeTransition';
  sizeFactor: number;
  axis?: Axis;
  child: FlutterParityComponent;
}

/**
 * SlideTransition Component
 * Animates the position of a widget relative to its normal position
 */
export interface SlideTransitionComponent extends BaseComponent {
  type: 'SlideTransition';
  offset: { dx: number; dy: number };
  child: FlutterParityComponent;
}

// ============================================================================
// SPECIAL COMPONENTS (4)
// ============================================================================

/**
 * FadeInImage Component
 * An image that fades in when loaded
 */
export interface FadeInImageComponent extends BaseComponent {
  type: 'FadeInImage';
  placeholder: string;
  image: string;
  fit?: BoxFit;
  width?: number;
  height?: number;
}

/**
 * Hero Component
 * A widget that marks its child as being a hero for navigation
 */
export interface HeroComponent extends BaseComponent {
  type: 'Hero';
  tag: string;
  child: FlutterParityComponent;
}

/**
 * IndexedStack Component
 * A Stack that shows a single child from a list of children
 */
export interface IndexedStackComponent extends BaseComponent {
  type: 'IndexedStack';
  index: number;
  children: FlutterParityComponent[];
}

/**
 * LayoutUtilities Component
 * Helper component for layout calculations
 */
export interface LayoutUtilitiesComponent extends BaseComponent {
  type: 'LayoutUtilities';
  child: FlutterParityComponent;
}

// ============================================================================
// UNION TYPES
// ============================================================================

/**
 * Union type of all Flutter Parity components
 */
export type FlutterParityComponent =
  // Layout
  | AlignComponent
  | CenterComponent
  | ConstrainedBoxComponent
  | ExpandedComponent
  | FittedBoxComponent
  | FlexComponent
  | FlexibleComponent
  | PaddingComponent
  | SizedBoxComponent
  | WrapComponent
  // Scrolling
  | CustomScrollViewComponent
  | GridViewBuilderComponent
  | ListViewBuilderComponent
  | ListViewSeparatedComponent
  | PageViewComponent
  | ReorderableListViewComponent
  | SliversComponent
  // Material
  | ActionChipComponent
  | CheckboxListTileComponent
  | ChoiceChipComponent
  | CircleAvatarComponent
  | EndDrawerComponent
  | ExpansionTileComponent
  | FilledButtonComponent
  | FilterChipComponent
  | InputChipComponent
  | PopupMenuButtonComponent
  | RefreshIndicatorComponent
  | RichTextComponent
  | SelectableTextComponent
  | SwitchListTileComponent
  | VerticalDividerComponent
  // Animation
  | AnimatedAlignComponent
  | AnimatedContainerComponent
  | AnimatedCrossFadeComponent
  | AnimatedDefaultTextStyleComponent
  | AnimatedListComponent
  | AnimatedModalBarrierComponent
  | AnimatedOpacityComponent
  | AnimatedPaddingComponent
  | AnimatedPositionedComponent
  | AnimatedScaleComponent
  | AnimatedSizeComponent
  | AnimatedSwitcherComponent
  | AlignTransitionComponent
  | DecoratedBoxTransitionComponent
  | DefaultTextStyleTransitionComponent
  | FadeTransitionComponent
  | PositionedTransitionComponent
  | RelativePositionedTransitionComponent
  | RotationTransitionComponent
  | ScaleTransitionComponent
  | SizeTransitionComponent
  | SlideTransitionComponent
  // Special
  | FadeInImageComponent
  | HeroComponent
  | IndexedStackComponent
  | LayoutUtilitiesComponent;

/**
 * Type guard to check if a component is a specific type
 */
export function isComponentType<T extends FlutterParityComponent>(
  component: FlutterParityComponent,
  type: T['type']
): component is T {
  return component.type === type;
}
