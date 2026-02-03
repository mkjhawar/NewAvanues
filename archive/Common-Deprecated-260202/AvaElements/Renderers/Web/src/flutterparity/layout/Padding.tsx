/**
 * Flutter Parity Padding Component
 * React implementation of Flutter's Padding widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { PaddingProps } from './types';
import { spacingToCSS } from './helpers';

/**
 * Padding - Insets child by given padding
 *
 * A widget that insets its child by the given padding. When passing padding
 * to a widget, the widget insets (or pads) its child's bounds by that amount.
 *
 * Web implementation: div with CSS padding
 * iOS equivalent: padding() modifier in SwiftUI
 * Android equivalent: View with padding attributes
 *
 * Features:
 * - All-sides padding: Spacing.all(16)
 * - Symmetric padding: Spacing.symmetric(horizontal: 16, vertical: 8)
 * - Custom per-side: Spacing.only(top: 8, left: 16, ...)
 * - RTL-aware when using start/end properties
 *
 * Common patterns:
 * 1. Equal padding: Padding(padding: Spacing.all(16), child: ...)
 * 2. Horizontal only: Padding(padding: Spacing.horizontal(16), child: ...)
 * 3. Vertical only: Padding(padding: Spacing.vertical(8), child: ...)
 * 4. Custom: Padding(padding: Spacing.only(top: 8, left: 16), child: ...)
 */
export const Padding: React.FC<PaddingProps> = ({
  padding,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    padding: spacingToCSS(padding),
    boxSizing: 'border-box',
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};
