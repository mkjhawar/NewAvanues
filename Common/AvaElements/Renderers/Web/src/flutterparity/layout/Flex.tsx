/**
 * Flutter Parity Flex Component (Row/Column)
 * React implementation of Flutter's Flex, Row, and Column widgets
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import {
  FlexProps,
  RowProps,
  ColumnProps,
  FlexDirection,
  MainAxisAlignment,
  MainAxisSize,
  CrossAxisAlignment,
  TextDirection,
  VerticalDirection,
} from './types';
import {
  mainAxisAlignmentToCSS,
  crossAxisAlignmentToCSS,
  getFlexDirection,
} from './helpers';

/**
 * Flex - Generic flexible layout component
 *
 * Displays children in a one-dimensional array (horizontal or vertical).
 * Similar to CSS Flexbox.
 *
 * Web implementation: Uses CSS Flexbox
 * iOS equivalent: VStack/HStack in SwiftUI
 * Android equivalent: LinearLayout
 */
export const Flex: React.FC<FlexProps> = ({
  direction,
  mainAxisAlignment = MainAxisAlignment.Start,
  mainAxisSize = MainAxisSize.Max,
  crossAxisAlignment = CrossAxisAlignment.Center,
  verticalDirection = VerticalDirection.Down,
  textDirection = TextDirection.LTR,
  children,
  key,
  testID,
}) => {
  const isHorizontal = direction === FlexDirection.Horizontal;
  const isRTL = textDirection === TextDirection.RTL;
  const isReversed = !isHorizontal && verticalDirection === VerticalDirection.Up;

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isReversed
      ? 'column-reverse'
      : getFlexDirection(isHorizontal, isRTL),
    justifyContent: mainAxisAlignmentToCSS(mainAxisAlignment),
    alignItems: crossAxisAlignmentToCSS(crossAxisAlignment),
    // MainAxisSize handling
    ...(mainAxisSize === MainAxisSize.Max
      ? isHorizontal
        ? { width: '100%' }
        : { height: '100%' }
      : {}),
    // Width/height defaults
    ...(mainAxisSize === MainAxisSize.Min
      ? isHorizontal
        ? { width: 'fit-content' }
        : { height: 'fit-content' }
      : {}),
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {children}
    </div>
  );
};

/**
 * Row - Horizontal flex layout
 *
 * A widget that displays its children in a horizontal array.
 * Shorthand for Flex with direction = Horizontal.
 *
 * Web implementation: Flexbox with flex-direction: row
 * iOS equivalent: HStack in SwiftUI
 * Android equivalent: LinearLayout with horizontal orientation
 */
export const Row: React.FC<RowProps> = (props) => (
  <Flex direction={FlexDirection.Horizontal} {...props} />
);

/**
 * Column - Vertical flex layout
 *
 * A widget that displays its children in a vertical array.
 * Shorthand for Flex with direction = Vertical.
 *
 * Web implementation: Flexbox with flex-direction: column
 * iOS equivalent: VStack in SwiftUI
 * Android equivalent: LinearLayout with vertical orientation
 */
export const Column: React.FC<ColumnProps> = (props) => (
  <Flex direction={FlexDirection.Vertical} {...props} />
);
