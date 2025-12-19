/**
 * Web Bridge for Shared Utilities
 *
 * Converts shared cross-platform types (from Kotlin common utilities)
 * to TypeScript/CSS/React equivalents.
 *
 * This bridge eliminates the need for duplicate conversion logic in Web components
 * and ensures consistency with other platform renderers.
 *
 * Agent 3: Desktop & Web Renderer - Shared Utilities Integration
 * @since 3.0.0-shared-utilities
 */

import { CSSProperties } from 'react';

// ═══════════════════════════════════════════════════════════════
// Type Definitions (mirroring Kotlin common types)
// ═══════════════════════════════════════════════════════════════

/**
 * Universal color representation (ARGB components, 0.0 - 1.0)
 */
export interface UniversalColor {
  alpha: number;
  red: number;
  green: number;
  blue: number;
}

/**
 * Edge insets for padding/margin
 */
export interface EdgeInsets {
  start: number;
  top: number;
  end: number;
  bottom: number;
}

/**
 * Corner radius configuration
 */
export interface CornerRadius {
  topStart: number;
  topEnd: number;
  bottomStart: number;
  bottomEnd: number;
}

/**
 * Border configuration
 */
export interface Border {
  width: number;
  color: number; // ARGB int
  style: 'solid' | 'dashed' | 'dotted' | 'none';
}

/**
 * Shadow configuration
 */
export interface Shadow {
  color: number; // ARGB int
  blurRadius: number;
  spreadRadius: number;
  offsetX: number;
  offsetY: number;
}

/**
 * Size with width and height
 */
export interface Size {
  width: number;
  height: number;
}

/**
 * Layout direction
 */
export type LayoutDirection = 'ltr' | 'rtl';

/**
 * Alignment types
 */
export type HorizontalAlignment = 'start' | 'end' | 'center';
export type VerticalAlignment = 'top' | 'bottom' | 'center';

export type MainAxisAlignment = 'start' | 'end' | 'center' | 'spaceBetween' | 'spaceAround' | 'spaceEvenly';
export type CrossAxisAlignment = 'start' | 'end' | 'center' | 'stretch' | 'baseline';
export type WrapAlignment = 'start' | 'end' | 'center' | 'spaceBetween' | 'spaceAround' | 'spaceEvenly';

// ═══════════════════════════════════════════════════════════════
// Color Utilities
// ═══════════════════════════════════════════════════════════════

/**
 * Convert UniversalColor to CSS rgba string
 */
export function universalColorToCss(color: UniversalColor): string {
  const r = Math.round(color.red * 255);
  const g = Math.round(color.green * 255);
  const b = Math.round(color.blue * 255);
  return `rgba(${r}, ${g}, ${b}, ${color.alpha})`;
}

/**
 * Convert ARGB integer to CSS rgba string
 */
export function argbToCss(argb: number): string {
  const a = ((argb >> 24) & 0xFF) / 255;
  const r = (argb >> 16) & 0xFF;
  const g = (argb >> 8) & 0xFF;
  const b = argb & 0xFF;
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}

/**
 * Convert hex string to UniversalColor
 */
export function hexToUniversalColor(hex: string): UniversalColor {
  const clean = hex.replace('#', '');

  if (clean.length === 3) {
    // #RGB
    const r = parseInt(clean[0] + clean[0], 16) / 255;
    const g = parseInt(clean[1] + clean[1], 16) / 255;
    const b = parseInt(clean[2] + clean[2], 16) / 255;
    return { alpha: 1, red: r, green: g, blue: b };
  } else if (clean.length === 6) {
    // #RRGGBB
    const r = parseInt(clean.substring(0, 2), 16) / 255;
    const g = parseInt(clean.substring(2, 4), 16) / 255;
    const b = parseInt(clean.substring(4, 6), 16) / 255;
    return { alpha: 1, red: r, green: g, blue: b };
  } else if (clean.length === 8) {
    // #AARRGGBB
    const a = parseInt(clean.substring(0, 2), 16) / 255;
    const r = parseInt(clean.substring(2, 4), 16) / 255;
    const g = parseInt(clean.substring(4, 6), 16) / 255;
    const b = parseInt(clean.substring(6, 8), 16) / 255;
    return { alpha: a, red: r, green: g, blue: b };
  }

  return { alpha: 1, red: 0, green: 0, blue: 0 };
}

/**
 * Calculate luminance (0.0 - 1.0)
 */
export function luminance(color: UniversalColor): number {
  return 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue;
}

/**
 * Lighten a color by a factor (0.0 - 1.0)
 */
export function lightenColor(color: UniversalColor, factor: number): UniversalColor {
  const hsl = rgbToHsl(color);
  const newL = Math.min(1, hsl.l + (1 - hsl.l) * Math.max(0, Math.min(1, factor)));
  return hslToRgb({ ...hsl, l: newL });
}

/**
 * Darken a color by a factor (0.0 - 1.0)
 */
export function darkenColor(color: UniversalColor, factor: number): UniversalColor {
  const hsl = rgbToHsl(color);
  const newL = hsl.l * (1 - Math.max(0, Math.min(1, factor)));
  return hslToRgb({ ...hsl, l: newL });
}

/**
 * Get contrasting foreground color (black or white)
 */
export function contrastingForeground(background: UniversalColor): UniversalColor {
  const lum = luminance(background);
  return lum > 0.5
    ? { alpha: 1, red: 0, green: 0, blue: 0 } // Black
    : { alpha: 1, red: 1, green: 1, blue: 1 }; // White
}

/**
 * RGB to HSL conversion
 */
function rgbToHsl(color: UniversalColor): { h: number; s: number; l: number; alpha: number } {
  const { red: r, green: g, blue: b, alpha } = color;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;

  if (max === min) {
    return { h: 0, s: 0, l, alpha };
  }

  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

  let h = 0;
  if (max === r) {
    h = ((g - b) / d + (g < b ? 6 : 0)) * 60;
  } else if (max === g) {
    h = ((b - r) / d + 2) * 60;
  } else {
    h = ((r - g) / d + 4) * 60;
  }

  return { h, s, l, alpha };
}

/**
 * HSL to RGB conversion
 */
function hslToRgb(hsl: { h: number; s: number; l: number; alpha: number }): UniversalColor {
  const { h, s, l, alpha } = hsl;

  if (s === 0) {
    return { alpha, red: l, green: l, blue: l };
  }

  const hueToRgb = (p: number, q: number, t: number): number => {
    let t1 = t;
    if (t1 < 0) t1 += 1;
    if (t1 > 1) t1 -= 1;
    if (t1 < 1 / 6) return p + (q - p) * 6 * t1;
    if (t1 < 1 / 2) return q;
    if (t1 < 2 / 3) return p + (q - p) * (2 / 3 - t1) * 6;
    return p;
  };

  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const hNorm = h / 360;

  return {
    alpha,
    red: hueToRgb(p, q, hNorm + 1 / 3),
    green: hueToRgb(p, q, hNorm),
    blue: hueToRgb(p, q, hNorm - 1 / 3),
  };
}

// ═══════════════════════════════════════════════════════════════
// Spacing Utilities
// ═══════════════════════════════════════════════════════════════

/**
 * Convert EdgeInsets to CSS padding
 */
export function edgeInsetsToPadding(insets: EdgeInsets, layoutDirection: LayoutDirection = 'ltr'): CSSProperties {
  const { start, top, end, bottom } = insets;

  if (layoutDirection === 'rtl') {
    return {
      paddingRight: `${start}px`,
      paddingTop: `${top}px`,
      paddingLeft: `${end}px`,
      paddingBottom: `${bottom}px`,
    };
  } else {
    return {
      paddingLeft: `${start}px`,
      paddingTop: `${top}px`,
      paddingRight: `${end}px`,
      paddingBottom: `${bottom}px`,
    };
  }
}

/**
 * Convert EdgeInsets to CSS margin
 */
export function edgeInsetsToMargin(insets: EdgeInsets, layoutDirection: LayoutDirection = 'ltr'): CSSProperties {
  const { start, top, end, bottom } = insets;

  if (layoutDirection === 'rtl') {
    return {
      marginRight: `${start}px`,
      marginTop: `${top}px`,
      marginLeft: `${end}px`,
      marginBottom: `${bottom}px`,
    };
  } else {
    return {
      marginLeft: `${start}px`,
      marginTop: `${top}px`,
      marginRight: `${end}px`,
      marginBottom: `${bottom}px`,
    };
  }
}

/**
 * Convert CornerRadius to CSS border-radius
 */
export function cornerRadiusToCss(radius: CornerRadius, layoutDirection: LayoutDirection = 'ltr'): CSSProperties {
  const { topStart, topEnd, bottomStart, bottomEnd } = radius;

  if (layoutDirection === 'rtl') {
    return {
      borderTopLeftRadius: `${topEnd}px`,
      borderTopRightRadius: `${topStart}px`,
      borderBottomLeftRadius: `${bottomEnd}px`,
      borderBottomRightRadius: `${bottomStart}px`,
    };
  } else {
    return {
      borderTopLeftRadius: `${topStart}px`,
      borderTopRightRadius: `${topEnd}px`,
      borderBottomLeftRadius: `${bottomStart}px`,
      borderBottomRightRadius: `${bottomEnd}px`,
    };
  }
}

/**
 * Convert Border to CSS border
 */
export function borderToCss(border: Border): CSSProperties {
  if (border.width === 0) return {};

  return {
    borderWidth: `${border.width}px`,
    borderStyle: border.style,
    borderColor: argbToCss(border.color),
  };
}

/**
 * Convert Shadow to CSS box-shadow
 */
export function shadowToCss(shadow: Shadow): string {
  const color = argbToCss(shadow.color);
  return `${shadow.offsetX}px ${shadow.offsetY}px ${shadow.blurRadius}px ${shadow.spreadRadius}px ${color}`;
}

// ═══════════════════════════════════════════════════════════════
// Alignment Utilities
// ═══════════════════════════════════════════════════════════════

/**
 * Convert HorizontalAlignment to CSS justify-content
 */
export function horizontalAlignmentToJustify(alignment: HorizontalAlignment, layoutDirection: LayoutDirection = 'ltr'): string {
  if (alignment === 'start') {
    return layoutDirection === 'rtl' ? 'flex-end' : 'flex-start';
  } else if (alignment === 'end') {
    return layoutDirection === 'rtl' ? 'flex-start' : 'flex-end';
  } else {
    return 'center';
  }
}

/**
 * Convert VerticalAlignment to CSS align-items
 */
export function verticalAlignmentToAlign(alignment: VerticalAlignment): string {
  if (alignment === 'top') return 'flex-start';
  if (alignment === 'bottom') return 'flex-end';
  return 'center';
}

/**
 * Convert MainAxisAlignment to CSS justify-content
 */
export function mainAxisAlignmentToJustify(alignment: MainAxisAlignment, layoutDirection: LayoutDirection = 'ltr'): string {
  switch (alignment) {
    case 'start':
      return layoutDirection === 'rtl' ? 'flex-end' : 'flex-start';
    case 'end':
      return layoutDirection === 'rtl' ? 'flex-start' : 'flex-end';
    case 'center':
      return 'center';
    case 'spaceBetween':
      return 'space-between';
    case 'spaceAround':
      return 'space-around';
    case 'spaceEvenly':
      return 'space-evenly';
  }
}

/**
 * Convert CrossAxisAlignment to CSS align-items
 */
export function crossAxisAlignmentToAlign(alignment: CrossAxisAlignment): string {
  switch (alignment) {
    case 'start':
      return 'flex-start';
    case 'end':
      return 'flex-end';
    case 'center':
      return 'center';
    case 'stretch':
      return 'stretch';
    case 'baseline':
      return 'baseline';
  }
}

/**
 * Convert WrapAlignment to CSS justify-content
 */
export function wrapAlignmentToJustify(alignment: WrapAlignment, layoutDirection: LayoutDirection = 'ltr'): string {
  switch (alignment) {
    case 'start':
      return layoutDirection === 'rtl' ? 'flex-end' : 'flex-start';
    case 'end':
      return layoutDirection === 'rtl' ? 'flex-start' : 'flex-end';
    case 'center':
      return 'center';
    case 'spaceBetween':
      return 'space-between';
    case 'spaceAround':
      return 'space-around';
    case 'spaceEvenly':
      return 'space-evenly';
  }
}

// ═══════════════════════════════════════════════════════════════
// Convenience Helpers
// ═══════════════════════════════════════════════════════════════

/**
 * Create EdgeInsets from various inputs
 */
export const EdgeInsetsUtils = {
  all: (value: number): EdgeInsets => ({
    start: value,
    top: value,
    end: value,
    bottom: value,
  }),

  symmetric: (horizontal: number = 0, vertical: number = 0): EdgeInsets => ({
    start: horizontal,
    top: vertical,
    end: horizontal,
    bottom: vertical,
  }),

  only: (params: { start?: number; top?: number; end?: number; bottom?: number }): EdgeInsets => ({
    start: params.start || 0,
    top: params.top || 0,
    end: params.end || 0,
    bottom: params.bottom || 0,
  }),
};

/**
 * Create CornerRadius from various inputs
 */
export const CornerRadiusUtils = {
  all: (radius: number): CornerRadius => ({
    topStart: radius,
    topEnd: radius,
    bottomStart: radius,
    bottomEnd: radius,
  }),

  only: (params: { topStart?: number; topEnd?: number; bottomStart?: number; bottomEnd?: number }): CornerRadius => ({
    topStart: params.topStart || 0,
    topEnd: params.topEnd || 0,
    bottomStart: params.bottomStart || 0,
    bottomEnd: params.bottomEnd || 0,
  }),
};

/**
 * Standard spacing scale (4px base unit - Material Design)
 */
export const SpacingScale = {
  none: 0,
  xxs: 2,   // 0.5 * 4
  xs: 4,    // 1 * 4
  sm: 8,    // 2 * 4
  md: 12,   // 3 * 4
  lg: 16,   // 4 * 4
  xl: 24,   // 6 * 4
  xxl: 32,  // 8 * 4
  xxxl: 48, // 12 * 4
};
