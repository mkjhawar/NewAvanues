/**
 * Flutter Parity Layout Component Type Definitions
 * TypeScript type definitions matching Kotlin component models
 *
 * @since 3.0.0-flutter-parity-web
 */

// ============================================================================
// CORE LAYOUT TYPES
// ============================================================================

export interface Spacing {
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
}

export interface Size {
  value?: number;
  type: 'dp' | 'fill' | 'wrapContent';
}

export interface BoxConstraints {
  minWidth: number;
  maxWidth: number;
  minHeight: number;
  maxHeight: number;
}

export interface BoxDecoration {
  color?: string;
  border?: Border;
  borderRadius?: BorderRadius;
  boxShadow?: BoxShadow[];
  gradient?: Gradient;
}

export interface Border {
  top?: BorderSide;
  right?: BorderSide;
  bottom?: BorderSide;
  left?: BorderSide;
}

export interface BorderSide {
  color: string;
  width: number;
  style?: 'solid' | 'dashed' | 'dotted';
}

export interface BorderRadius {
  topLeft?: number;
  topRight?: number;
  bottomLeft?: number;
  bottomRight?: number;
}

export interface BoxShadow {
  color: string;
  offset: { x: number; y: number };
  blurRadius: number;
  spreadRadius?: number;
}

export type Gradient = LinearGradient | RadialGradient;

export interface LinearGradient {
  type: 'linear';
  colors: string[];
  begin: AlignmentGeometry;
  end: AlignmentGeometry;
  stops?: number[];
}

export interface RadialGradient {
  type: 'radial';
  colors: string[];
  center: AlignmentGeometry;
  radius: number;
  stops?: number[];
}

// ============================================================================
// ALIGNMENT TYPES
// ============================================================================

export type AlignmentGeometry =
  | { type: 'center' }
  | { type: 'custom'; x: number; y: number };

export const Alignment = {
  topLeft: { type: 'custom' as const, x: -1, y: -1 },
  topCenter: { type: 'custom' as const, x: 0, y: -1 },
  topRight: { type: 'custom' as const, x: 1, y: -1 },
  centerLeft: { type: 'custom' as const, x: -1, y: 0 },
  center: { type: 'center' as const },
  centerRight: { type: 'custom' as const, x: 1, y: 0 },
  bottomLeft: { type: 'custom' as const, x: -1, y: 1 },
  bottomCenter: { type: 'custom' as const, x: 0, y: 1 },
  bottomRight: { type: 'custom' as const, x: 1, y: 1 },
};

// ============================================================================
// FLEX LAYOUT TYPES
// ============================================================================

export enum FlexDirection {
  Horizontal = 'horizontal',
  Vertical = 'vertical',
}

export enum MainAxisAlignment {
  Start = 'start',
  End = 'end',
  Center = 'center',
  SpaceBetween = 'space-between',
  SpaceAround = 'space-around',
  SpaceEvenly = 'space-evenly',
}

export enum MainAxisSize {
  Min = 'min',
  Max = 'max',
}

export enum CrossAxisAlignment {
  Start = 'start',
  End = 'end',
  Center = 'center',
  Stretch = 'stretch',
  Baseline = 'baseline',
}

export enum TextDirection {
  LTR = 'ltr',
  RTL = 'rtl',
}

export enum VerticalDirection {
  Down = 'down',
  Up = 'up',
}

// ============================================================================
// FLEX FIT TYPES
// ============================================================================

export enum FlexFit {
  Tight = 'tight',
  Loose = 'loose',
}

// ============================================================================
// BOX FIT TYPES
// ============================================================================

export enum BoxFit {
  Fill = 'fill',
  Contain = 'contain',
  Cover = 'cover',
  FitWidth = 'fitWidth',
  FitHeight = 'fitHeight',
  None = 'none',
  ScaleDown = 'scaleDown',
}

// ============================================================================
// WRAP LAYOUT TYPES
// ============================================================================

export enum WrapDirection {
  Horizontal = 'horizontal',
  Vertical = 'vertical',
}

export enum WrapAlignment {
  Start = 'start',
  End = 'end',
  Center = 'center',
  SpaceBetween = 'spaceBetween',
  SpaceAround = 'spaceAround',
  SpaceEvenly = 'spaceEvenly',
}

export enum WrapCrossAlignment {
  Start = 'start',
  End = 'end',
  Center = 'center',
}

// ============================================================================
// CLIP BEHAVIOR
// ============================================================================

export enum Clip {
  None = 'none',
  HardEdge = 'hardEdge',
  AntiAlias = 'antiAlias',
  AntiAliasWithSaveLayer = 'antiAliasWithSaveLayer',
}

// ============================================================================
// STACK FIT
// ============================================================================

export enum StackFit {
  Loose = 'loose',
  Expand = 'expand',
  Passthrough = 'passthrough',
}

// ============================================================================
// COMPONENT PROPS INTERFACES
// ============================================================================

export interface BaseComponentProps {
  key?: string;
  testID?: string;
}

export interface ContainerProps extends BaseComponentProps {
  width?: Size;
  height?: Size;
  padding?: Spacing;
  margin?: Spacing;
  decoration?: BoxDecoration;
  alignment?: AlignmentGeometry;
  constraints?: BoxConstraints;
  child?: React.ReactNode;
}

export interface FlexProps extends BaseComponentProps {
  direction: FlexDirection;
  mainAxisAlignment?: MainAxisAlignment;
  mainAxisSize?: MainAxisSize;
  crossAxisAlignment?: CrossAxisAlignment;
  verticalDirection?: VerticalDirection;
  textDirection?: TextDirection;
  children?: React.ReactNode;
}

export interface RowProps extends Omit<FlexProps, 'direction'> {}
export interface ColumnProps extends Omit<FlexProps, 'direction'> {}

export interface ExpandedProps extends BaseComponentProps {
  flex?: number;
  child: React.ReactNode;
}

export interface FlexibleProps extends BaseComponentProps {
  flex?: number;
  fit?: FlexFit;
  child: React.ReactNode;
}

export interface PaddingProps extends BaseComponentProps {
  padding: Spacing;
  child: React.ReactNode;
}

export interface AlignProps extends BaseComponentProps {
  alignment?: AlignmentGeometry;
  widthFactor?: number;
  heightFactor?: number;
  child: React.ReactNode;
}

export interface CenterProps extends BaseComponentProps {
  widthFactor?: number;
  heightFactor?: number;
  child: React.ReactNode;
}

export interface SizedBoxProps extends BaseComponentProps {
  width?: Size;
  height?: Size;
  child?: React.ReactNode;
}

export interface StackProps extends BaseComponentProps {
  alignment?: AlignmentGeometry;
  fit?: StackFit;
  clipBehavior?: Clip;
  children?: React.ReactNode;
}

export interface PositionedProps extends BaseComponentProps {
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
  width?: number;
  height?: number;
  child: React.ReactNode;
}

export interface FittedBoxProps extends BaseComponentProps {
  fit?: BoxFit;
  alignment?: AlignmentGeometry;
  clipBehavior?: Clip;
  child: React.ReactNode;
}

export interface WrapProps extends BaseComponentProps {
  direction?: WrapDirection;
  alignment?: WrapAlignment;
  spacing?: Spacing;
  runSpacing?: Spacing;
  runAlignment?: WrapAlignment;
  crossAxisAlignment?: WrapCrossAlignment;
  verticalDirection?: VerticalDirection;
  children?: React.ReactNode;
}

export interface SpacerProps extends BaseComponentProps {
  flex?: number;
}

export interface ConstrainedBoxProps extends BaseComponentProps {
  constraints: BoxConstraints;
  child: React.ReactNode;
}
