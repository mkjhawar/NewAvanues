/**
 * MagicUI TypeScript type definitions for Web Renderer
 */

/**
 * Color representation
 */
export interface Color {
  red: number;    // 0-255
  green: number;  // 0-255
  blue: number;   // 0-255
  alpha: number;  // 0-1
}

/**
 * Font weight enum
 */
export enum FontWeight {
  Thin = 'Thin',
  ExtraLight = 'ExtraLight',
  Light = 'Light',
  Normal = 'Normal',
  Medium = 'Medium',
  SemiBold = 'SemiBold',
  Bold = 'Bold',
  ExtraBold = 'ExtraBold',
  Black = 'Black'
}

/**
 * Font definition
 */
export interface Font {
  family: string;
  size: number;    // in sp/px
  weight: FontWeight;
}

/**
 * Color mode
 */
export enum ColorMode {
  Light = 'Light',
  Dark = 'Dark'
}

/**
 * Theme platform
 */
export enum ThemePlatform {
  Material3_Expressive = 'Material3_Expressive',
  iOS26_LiquidGlass = 'iOS26_LiquidGlass',
  Windows11_Fluent2 = 'Windows11_Fluent2',
  VisionOS2_SpatialGlass = 'VisionOS2_SpatialGlass'
}

/**
 * Color scheme
 */
export interface ColorScheme {
  mode: ColorMode;
  primary: Color;
  onPrimary: Color;
  primaryContainer: Color;
  onPrimaryContainer: Color;
  secondary: Color;
  onSecondary: Color;
  secondaryContainer: Color;
  onSecondaryContainer: Color;
  tertiary: Color;
  onTertiary: Color;
  tertiaryContainer: Color;
  onTertiaryContainer: Color;
  error: Color;
  onError: Color;
  errorContainer: Color;
  onErrorContainer: Color;
  surface: Color;
  onSurface: Color;
  surfaceVariant: Color;
  onSurfaceVariant: Color;
  background: Color;
  onBackground: Color;
  outline: Color;
  outlineVariant: Color;
}

/**
 * Typography
 */
export interface Typography {
  displayLarge: Font;
  displayMedium: Font;
  displaySmall: Font;
  headlineLarge: Font;
  headlineMedium: Font;
  headlineSmall: Font;
  titleLarge: Font;
  titleMedium: Font;
  titleSmall: Font;
  bodyLarge: Font;
  bodyMedium: Font;
  bodySmall: Font;
  labelLarge: Font;
  labelMedium: Font;
  labelSmall: Font;
}

/**
 * Shapes
 */
export interface Shapes {
  extraSmall: number;
  small: number;
  medium: number;
  large: number;
  extraLarge: number;
}

/**
 * Spacing
 */
export interface Spacing {
  xs: number;
  sm: number;
  md: number;
  lg: number;
  xl: number;
  xxl: number;
}

/**
 * Elevation
 */
export interface Elevation {
  level0: number;
  level1: number;
  level2: number;
  level3: number;
  level4: number;
  level5: number;
}

/**
 * Water effect level (Apple Liquid Glass-inspired)
 */
export enum WaterLevel {
  Regular = 'regular',
  Clear = 'clear',
  Identity = 'identity'
}

/**
 * Water effect configuration for AvanueWaterUI
 *
 * Liquid Glass-inspired dynamic material effects.
 * CSS implementation uses backdrop-filter + SVG displacement filters.
 */
export interface WaterEffect {
  level: WaterLevel;
  blur: number;                    // px — backdrop-filter blur radius
  refractionStrength: number;      // px — SVG displacement magnitude
  overlayOpacity: number;          // 0-1 — background rgba alpha
  highlightColor: string;          // rgba() — specular highlight tint
  causticColor: string;            // rgba() — animated shimmer color
  borderWidth: number;             // px — hairline border width
  borderTopColor: string;          // rgba() — gradient border top
  borderBottomColor: string;       // rgba() — gradient border bottom
  interactive: boolean;            // Enable press scale + shimmer
}

/**
 * Water theme scheme (per-variant colors for water effects)
 */
export interface WaterScheme {
  highlightColor: Color;
  causticColor: Color;
  refractionTint: Color;
  depthShadowColor: Color;
  surfaceTint: Color;
  borderTint: Color;
  enableRefraction: boolean;
  enableCaustics: boolean;
  enableSpecular: boolean;
}

/**
 * Theme definition
 */
export interface Theme {
  name: string;
  platform: ThemePlatform;
  colorScheme: ColorScheme;
  typography: Typography;
  shapes: Shapes;
  spacing: Spacing;
  elevation: Elevation;
  water?: WaterScheme;
}

/**
 * Component props base
 */
export interface ComponentProps {
  id?: string;
  className?: string;
  style?: React.CSSProperties;
  onClick?: () => void;
}

/**
 * Button variant
 */
export enum ButtonVariant {
  Filled = 'Filled',
  Outlined = 'Outlined',
  Text = 'Text',
  Elevated = 'Elevated'
}

/**
 * Button props
 */
export interface ButtonProps extends ComponentProps {
  text: string;
  variant?: ButtonVariant;
  disabled?: boolean;
  fullWidth?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
}

/**
 * TextField props
 */
export interface TextFieldProps extends ComponentProps {
  label?: string;
  placeholder?: string;
  value?: string;
  onChange?: (value: string) => void;
  isPassword?: boolean;
  disabled?: boolean;
  error?: boolean;
  helperText?: string;
  multiline?: boolean;
  rows?: number;
}

/**
 * Checkbox props
 */
export interface CheckboxProps extends ComponentProps {
  label?: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
}

/**
 * Switch props
 */
export interface SwitchProps extends ComponentProps {
  label?: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
}

/**
 * Card props
 */
export interface CardProps extends ComponentProps {
  title?: string;
  subtitle?: string;
  elevation?: number;
  children?: React.ReactNode;
}

/**
 * Dialog props
 */
export interface DialogProps extends ComponentProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children?: React.ReactNode;
  actions?: React.ReactNode;
  fullWidth?: boolean;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

/**
 * List item data
 */
export interface ListItemData {
  id: string;
  primaryText: string;
  secondaryText?: string;
  icon?: React.ReactNode;
  onClick?: () => void;
}

/**
 * ListView props
 */
export interface ListViewProps extends ComponentProps {
  items: ListItemData[];
  onItemClick?: (item: ListItemData) => void;
}

/**
 * Image props
 */
export interface ImageProps extends ComponentProps {
  source: string;
  alt?: string;
  width?: number | string;
  height?: number | string;
  borderRadius?: number;
}

/**
 * Text props
 */
export interface TextProps extends ComponentProps {
  children: React.ReactNode;
  variant?: 'displayLarge' | 'displayMedium' | 'displaySmall' |
            'headlineLarge' | 'headlineMedium' | 'headlineSmall' |
            'titleLarge' | 'titleMedium' | 'titleSmall' |
            'bodyLarge' | 'bodyMedium' | 'bodySmall' |
            'labelLarge' | 'labelMedium' | 'labelSmall';
  color?: Color;
}

/**
 * ColorPicker props
 */
export interface ColorPickerProps extends ComponentProps {
  value: Color;
  onChange: (color: Color) => void;
  label?: string;
}

/**
 * Arrangement types
 */
export enum Arrangement {
  Start = 'Start',
  Center = 'Center',
  End = 'End',
  SpaceBetween = 'SpaceBetween',
  SpaceAround = 'SpaceAround',
  SpaceEvenly = 'SpaceEvenly'
}

/**
 * Alignment types
 */
export enum Alignment {
  Start = 'Start',
  Center = 'Center',
  End = 'End',
  Stretch = 'Stretch'
}

/**
 * Column props
 */
export interface ColumnProps extends ComponentProps {
  children: React.ReactNode;
  horizontalAlignment?: Alignment;
  verticalArrangement?: Arrangement;
  fillMaxWidth?: boolean;
  fillMaxHeight?: boolean;
  padding?: number;
  spacing?: number;
}

/**
 * Row props
 */
export interface RowProps extends ComponentProps {
  children: React.ReactNode;
  verticalAlignment?: Alignment;
  horizontalArrangement?: Arrangement;
  fillMaxWidth?: boolean;
  fillMaxHeight?: boolean;
  padding?: number;
  spacing?: number;
}

/**
 * Box props
 */
export interface BoxProps extends ComponentProps {
  children: React.ReactNode;
  width?: number | string;
  height?: number | string;
  backgroundColor?: Color;
  borderRadius?: number;
  padding?: number;
  elevation?: number;
}

/**
 * Scrollable column props
 */
export interface ScrollableColumnProps extends ComponentProps {
  children: React.ReactNode;
  fillMaxWidth?: boolean;
  fillMaxHeight?: boolean;
  padding?: number;
}

/**
 * WebSocket IPC message types
 */
export interface IPCMessage {
  type: 'UI_TRANSFER' | 'CODE_TRANSFER' | 'THEME_TRANSFER';
  payload: unknown;
  timestamp: number;
}

/**
 * UI transfer payload
 */
export interface UITransferPayload {
  dsl: string;
  preview?: string;
  metadata?: Record<string, unknown>;
}

/**
 * Helper function to convert Color to CSS string
 */
export function colorToCss(color: Color): string {
  return `rgba(${color.red}, ${color.green}, ${color.blue}, ${color.alpha})`;
}

/**
 * Helper function to convert hex to Color
 */
export function hexToColor(hex: string): Color {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) {
    throw new Error(`Invalid hex color: ${hex}`);
  }

  return {
    red: parseInt(result[1], 16),
    green: parseInt(result[2], 16),
    blue: parseInt(result[3], 16),
    alpha: 1.0
  };
}

/**
 * Helper function to convert Color to hex
 */
export function colorToHex(color: Color): string {
  const toHex = (n: number) => {
    const hex = n.toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  };

  return `#${toHex(color.red)}${toHex(color.green)}${toHex(color.blue)}`;
}

/**
 * Font weight CSS mapping
 */
export function fontWeightToCss(weight: FontWeight): number {
  const weightMap: Record<FontWeight, number> = {
    [FontWeight.Thin]: 100,
    [FontWeight.ExtraLight]: 200,
    [FontWeight.Light]: 300,
    [FontWeight.Normal]: 400,
    [FontWeight.Medium]: 500,
    [FontWeight.SemiBold]: 600,
    [FontWeight.Bold]: 700,
    [FontWeight.ExtraBold]: 800,
    [FontWeight.Black]: 900
  };

  return weightMap[weight];
}

// Sprint 1 Component Types

/**
 * Icon props
 */
export interface IconProps extends ComponentProps {
  name: string;
  size?: 'small' | 'medium' | 'large';
  color?: string;
}

/**
 * ScrollView props
 */
export interface ScrollViewProps extends ComponentProps {
  children: React.ReactNode;
  orientation?: 'vertical' | 'horizontal';
  maxHeight?: number | string;
  maxWidth?: number | string;
}

/**
 * Radio option
 */
export interface RadioOption {
  value: string;
  label: string;
  disabled?: boolean;
}

/**
 * Radio props
 */
export interface RadioProps extends ComponentProps {
  label?: string;
  options: RadioOption[];
  value?: string;
  onChange?: (value: string) => void;
  orientation?: 'vertical' | 'horizontal';
  disabled?: boolean;
}

/**
 * Slider props
 */
export interface SliderProps extends ComponentProps {
  label?: string;
  value?: number;
  min?: number;
  max?: number;
  step?: number;
  onChange?: (value: number) => void;
  disabled?: boolean;
  showValue?: boolean;
  valueLabelDisplay?: 'auto' | 'on' | 'off';
}

/**
 * ProgressBar props
 */
export interface ProgressBarProps extends ComponentProps {
  value?: number;
  variant?: 'determinate' | 'indeterminate';
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  showLabel?: boolean;
  label?: string;
}

/**
 * Spinner props
 */
export interface SpinnerProps extends ComponentProps {
  size?: number | string;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  thickness?: number;
  centered?: boolean;
}

/**
 * Toast props
 */
export interface ToastProps extends ComponentProps {
  message: string;
  open?: boolean;
  duration?: number;
  severity?: 'success' | 'info' | 'warning' | 'error';
  position?: {
    vertical: 'top' | 'bottom';
    horizontal: 'left' | 'center' | 'right';
  };
  onClose?: () => void;
}

/**
 * Alert props
 */
export interface AlertProps extends ComponentProps {
  message: string;
  title?: string;
  severity?: 'success' | 'info' | 'warning' | 'error';
  variant?: 'filled' | 'outlined' | 'standard';
  icon?: React.ReactNode | false;
  onClose?: () => void;
}

/**
 * Avatar props
 */
export interface AvatarProps extends ComponentProps {
  initials?: string;
  src?: string;
  alt?: string;
  size?: number;
  variant?: 'circular' | 'rounded' | 'square';
}

/**
 * Container props
 */
export interface ContainerProps extends ComponentProps {
  children: React.ReactNode;
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
  disableGutters?: boolean;
}
