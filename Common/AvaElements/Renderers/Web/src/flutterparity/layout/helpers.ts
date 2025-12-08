/**
 * Flutter Parity Layout Helper Utilities
 * Helper functions for converting Flutter layout concepts to CSS
 *
 * @since 3.0.0-flutter-parity-web
 */

import {
  Spacing,
  Size,
  AlignmentGeometry,
  MainAxisAlignment,
  CrossAxisAlignment,
  BoxDecoration,
  BoxFit,
  WrapAlignment,
  BorderRadius,
  BoxShadow,
  Gradient,
} from './types';

// ============================================================================
// SPACING UTILITIES
// ============================================================================

/**
 * Convert Spacing to CSS padding string
 */
export function spacingToCSS(spacing?: Spacing): string {
  if (!spacing) return '0';

  const top = spacing.top ?? 0;
  const right = spacing.right ?? 0;
  const bottom = spacing.bottom ?? 0;
  const left = spacing.left ?? 0;

  if (top === right && right === bottom && bottom === left) {
    return `${top}px`;
  }
  if (top === bottom && right === left) {
    return `${top}px ${right}px`;
  }
  return `${top}px ${right}px ${bottom}px ${left}px`;
}

/**
 * Convert Spacing to single value (for gap property)
 */
export function spacingToValue(spacing?: Spacing): number {
  if (!spacing) return 0;
  return spacing.top ?? spacing.left ?? 0;
}

// ============================================================================
// SIZE UTILITIES
// ============================================================================

/**
 * Convert Size to CSS value
 */
export function sizeToCSS(size?: Size): string | number {
  if (!size) return 'auto';

  switch (size.type) {
    case 'dp':
      return size.value !== undefined ? `${size.value}px` : 'auto';
    case 'fill':
      return '100%';
    case 'wrapContent':
      return 'auto';
    default:
      return 'auto';
  }
}

/**
 * Check if size represents fill
 */
export function isFillSize(size?: Size): boolean {
  return size?.type === 'fill';
}

// ============================================================================
// ALIGNMENT UTILITIES
// ============================================================================

/**
 * Convert AlignmentGeometry to CSS justify-content value
 */
export function alignmentToJustifyContent(
  alignment?: AlignmentGeometry
): string {
  if (!alignment) return 'center';

  if (alignment.type === 'center') return 'center';

  const { x } = alignment;
  if (x === -1) return 'flex-start';
  if (x === 1) return 'flex-end';
  return 'center';
}

/**
 * Convert AlignmentGeometry to CSS align-items value
 */
export function alignmentToAlignItems(alignment?: AlignmentGeometry): string {
  if (!alignment) return 'center';

  if (alignment.type === 'center') return 'center';

  const { y } = alignment;
  if (y === -1) return 'flex-start';
  if (y === 1) return 'flex-end';
  return 'center';
}

/**
 * Convert AlignmentGeometry to CSS position values
 */
export function alignmentToPosition(
  alignment?: AlignmentGeometry,
  parentWidth?: number,
  parentHeight?: number,
  childWidth?: number,
  childHeight?: number
): { top?: string; left?: string; transform?: string } {
  if (!alignment) return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)' };

  if (alignment.type === 'center') {
    return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)' };
  }

  const { x, y } = alignment;

  // Convert -1..1 to percentage
  const leftPercent = ((x + 1) / 2) * 100;
  const topPercent = ((y + 1) / 2) * 100;

  return {
    top: `${topPercent}%`,
    left: `${leftPercent}%`,
    transform: `translate(-${leftPercent}%, -${topPercent}%)`,
  };
}

/**
 * Convert MainAxisAlignment to CSS justify-content
 */
export function mainAxisAlignmentToCSS(alignment?: MainAxisAlignment): string {
  switch (alignment) {
    case MainAxisAlignment.Start:
      return 'flex-start';
    case MainAxisAlignment.End:
      return 'flex-end';
    case MainAxisAlignment.Center:
      return 'center';
    case MainAxisAlignment.SpaceBetween:
      return 'space-between';
    case MainAxisAlignment.SpaceAround:
      return 'space-around';
    case MainAxisAlignment.SpaceEvenly:
      return 'space-evenly';
    default:
      return 'flex-start';
  }
}

/**
 * Convert CrossAxisAlignment to CSS align-items
 */
export function crossAxisAlignmentToCSS(alignment?: CrossAxisAlignment): string {
  switch (alignment) {
    case CrossAxisAlignment.Start:
      return 'flex-start';
    case CrossAxisAlignment.End:
      return 'flex-end';
    case CrossAxisAlignment.Center:
      return 'center';
    case CrossAxisAlignment.Stretch:
      return 'stretch';
    case CrossAxisAlignment.Baseline:
      return 'baseline';
    default:
      return 'center';
  }
}

/**
 * Convert WrapAlignment to CSS justify-content
 */
export function wrapAlignmentToCSS(alignment?: WrapAlignment): string {
  switch (alignment) {
    case WrapAlignment.Start:
      return 'flex-start';
    case WrapAlignment.End:
      return 'flex-end';
    case WrapAlignment.Center:
      return 'center';
    case WrapAlignment.SpaceBetween:
      return 'space-between';
    case WrapAlignment.SpaceAround:
      return 'space-around';
    case WrapAlignment.SpaceEvenly:
      return 'space-evenly';
    default:
      return 'flex-start';
  }
}

// ============================================================================
// BOX DECORATION UTILITIES
// ============================================================================

/**
 * Convert BoxDecoration to CSS properties object
 */
export function boxDecorationToCSS(
  decoration?: BoxDecoration
): React.CSSProperties {
  if (!decoration) return {};

  const styles: React.CSSProperties = {};

  if (decoration.color) {
    styles.backgroundColor = decoration.color;
  }

  if (decoration.border) {
    const { top, right, bottom, left } = decoration.border;
    if (top) {
      styles.borderTop = `${top.width}px ${top.style || 'solid'} ${top.color}`;
    }
    if (right) {
      styles.borderRight = `${right.width}px ${right.style || 'solid'} ${right.color}`;
    }
    if (bottom) {
      styles.borderBottom = `${bottom.width}px ${bottom.style || 'solid'} ${bottom.color}`;
    }
    if (left) {
      styles.borderLeft = `${left.width}px ${left.style || 'solid'} ${left.color}`;
    }
  }

  if (decoration.borderRadius) {
    styles.borderRadius = borderRadiusToCSS(decoration.borderRadius);
  }

  if (decoration.boxShadow && decoration.boxShadow.length > 0) {
    styles.boxShadow = decoration.boxShadow.map(boxShadowToCSS).join(', ');
  }

  if (decoration.gradient) {
    styles.background = gradientToCSS(decoration.gradient);
  }

  return styles;
}

/**
 * Convert BorderRadius to CSS border-radius string
 */
function borderRadiusToCSS(radius: BorderRadius): string {
  const tl = radius.topLeft ?? 0;
  const tr = radius.topRight ?? 0;
  const br = radius.bottomRight ?? 0;
  const bl = radius.bottomLeft ?? 0;

  if (tl === tr && tr === br && br === bl) {
    return `${tl}px`;
  }
  return `${tl}px ${tr}px ${br}px ${bl}px`;
}

/**
 * Convert BoxShadow to CSS box-shadow string
 */
function boxShadowToCSS(shadow: BoxShadow): string {
  const { color, offset, blurRadius, spreadRadius = 0 } = shadow;
  return `${offset.x}px ${offset.y}px ${blurRadius}px ${spreadRadius}px ${color}`;
}

/**
 * Convert Gradient to CSS gradient string
 */
function gradientToCSS(gradient: Gradient): string {
  if (gradient.type === 'linear') {
    const angle = calculateGradientAngle(gradient.begin, gradient.end);
    const colorStops = gradient.colors
      .map((color, index) => {
        const stop = gradient.stops?.[index];
        return stop !== undefined ? `${color} ${stop * 100}%` : color;
      })
      .join(', ');
    return `linear-gradient(${angle}deg, ${colorStops})`;
  } else {
    // Radial gradient
    const colorStops = gradient.colors
      .map((color, index) => {
        const stop = gradient.stops?.[index];
        return stop !== undefined ? `${color} ${stop * 100}%` : color;
      })
      .join(', ');
    return `radial-gradient(circle at center, ${colorStops})`;
  }
}

/**
 * Calculate gradient angle from begin/end alignment
 */
function calculateGradientAngle(
  begin: AlignmentGeometry,
  end: AlignmentGeometry
): number {
  // Simplified: top-to-bottom = 180deg, left-to-right = 90deg
  const beginX = begin.type === 'center' ? 0 : begin.x;
  const beginY = begin.type === 'center' ? 0 : begin.y;
  const endX = end.type === 'center' ? 0 : end.x;
  const endY = end.type === 'center' ? 0 : end.y;

  const deltaX = endX - beginX;
  const deltaY = endY - beginY;

  const angle = (Math.atan2(deltaY, deltaX) * 180) / Math.PI + 90;
  return angle;
}

// ============================================================================
// BOX FIT UTILITIES
// ============================================================================

/**
 * Convert BoxFit to CSS object-fit value
 */
export function boxFitToCSS(fit?: BoxFit): string {
  switch (fit) {
    case BoxFit.Fill:
      return 'fill';
    case BoxFit.Contain:
      return 'contain';
    case BoxFit.Cover:
      return 'cover';
    case BoxFit.FitWidth:
    case BoxFit.FitHeight:
      return 'scale-down';
    case BoxFit.None:
      return 'none';
    case BoxFit.ScaleDown:
      return 'scale-down';
    default:
      return 'contain';
  }
}

/**
 * Get styles for BoxFit implementation
 */
export function boxFitToStyles(
  fit?: BoxFit,
  alignment?: AlignmentGeometry
): React.CSSProperties {
  const styles: React.CSSProperties = {
    display: 'flex',
    alignItems: alignmentToAlignItems(alignment),
    justifyContent: alignmentToJustifyContent(alignment),
  };

  switch (fit) {
    case BoxFit.Fill:
      styles.width = '100%';
      styles.height = '100%';
      break;
    case BoxFit.Contain:
      styles.maxWidth = '100%';
      styles.maxHeight = '100%';
      break;
    case BoxFit.Cover:
      styles.minWidth = '100%';
      styles.minHeight = '100%';
      break;
    case BoxFit.FitWidth:
      styles.width = '100%';
      styles.height = 'auto';
      break;
    case BoxFit.FitHeight:
      styles.height = '100%';
      styles.width = 'auto';
      break;
    case BoxFit.None:
    case BoxFit.ScaleDown:
      // Natural size, up to container limits
      styles.maxWidth = '100%';
      styles.maxHeight = '100%';
      break;
  }

  return styles;
}

// ============================================================================
// RTL SUPPORT UTILITIES
// ============================================================================

/**
 * Adjust spacing for RTL
 */
export function adjustSpacingForRTL(spacing: Spacing, isRTL: boolean): Spacing {
  if (!isRTL) return spacing;

  return {
    top: spacing.top,
    right: spacing.left,
    bottom: spacing.bottom,
    left: spacing.right,
  };
}

/**
 * Get flex-direction considering RTL
 */
export function getFlexDirection(
  horizontal: boolean,
  isRTL: boolean
): 'row' | 'row-reverse' | 'column' | 'column-reverse' {
  if (horizontal) {
    return isRTL ? 'row-reverse' : 'row';
  }
  return 'column';
}
