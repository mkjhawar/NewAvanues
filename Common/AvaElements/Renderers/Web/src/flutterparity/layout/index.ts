/**
 * Flutter Parity Layout Components - Main Export
 *
 * Complete implementation of Flutter's layout widgets for React/Web platform.
 * 14 core layout components with full TypeScript support.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/layout
 */

// ============================================================================
// TYPE EXPORTS
// ============================================================================

export type {
  // Core types
  Spacing,
  Size,
  BoxConstraints,
  BoxDecoration,
  Border,
  BorderSide,
  BorderRadius,
  BoxShadow,
  Gradient,
  LinearGradient,
  RadialGradient,
  AlignmentGeometry,

  // Component props
  BaseComponentProps,
  ContainerProps,
  FlexProps,
  RowProps,
  ColumnProps,
  ExpandedProps,
  FlexibleProps,
  PaddingProps,
  AlignProps,
  CenterProps,
  SizedBoxProps,
  StackProps,
  PositionedProps,
  FittedBoxProps,
  WrapProps,
  SpacerProps,
  ConstrainedBoxProps,
} from './types';

export {
  // Enums
  FlexDirection,
  MainAxisAlignment,
  MainAxisSize,
  CrossAxisAlignment,
  TextDirection,
  VerticalDirection,
  FlexFit,
  BoxFit,
  WrapDirection,
  WrapAlignment,
  WrapCrossAlignment,
  Clip,
  StackFit,

  // Constants
  Alignment,
} from './types';

// ============================================================================
// HELPER UTILITIES EXPORTS
// ============================================================================

export {
  // Spacing utilities
  spacingToCSS,
  spacingToValue,

  // Size utilities
  sizeToCSS,
  isFillSize,

  // Alignment utilities
  alignmentToJustifyContent,
  alignmentToAlignItems,
  alignmentToPosition,
  mainAxisAlignmentToCSS,
  crossAxisAlignmentToCSS,
  wrapAlignmentToCSS,

  // Box decoration utilities
  boxDecorationToCSS,

  // Box fit utilities
  boxFitToCSS,
  boxFitToStyles,

  // RTL utilities
  adjustSpacingForRTL,
  getFlexDirection,
} from './helpers';

// ============================================================================
// COMPONENT EXPORTS
// ============================================================================

// Flex layouts (3 components)
export { Flex, Row, Column } from './Flex';

// Container
export { Container } from './Container';

// Stack and positioning (2 components + 2 variants)
export {
  Stack,
  Positioned,
  PositionedFill,
  PositionedDirectional,
} from './Stack';

// Alignment (2 components)
export { Align, Center } from './Align';

// Constrained box
export { ConstrainedBox, ConstrainedBoxTight, ConstrainedBoxExpand, ConstrainedBoxLoose } from './ConstrainedBox';

// Flexible sizing (3 components)
export { Expanded, Flexible, Spacer } from './Flexible';

// Sizing (1 component + 3 variants)
export {
  SizedBox,
  SizedBoxExpand,
  SizedBoxShrink,
  SizedBoxSquare,
} from './SizedBox';

// Padding
export { Padding } from './Padding';

// Fitted box
export { FittedBox } from './FittedBox';

// Wrap layout
export { Wrap } from './Wrap';

// ============================================================================
// COMPONENT COUNT: 14 CORE LAYOUT COMPONENTS
// ============================================================================
// 1. Container
// 2. Row
// 3. Column
// 4. Flex
// 5. Stack
// 6. Positioned
// 7. Align
// 8. Center
// 9. Expanded
// 10. Flexible
// 11. SizedBox
// 12. Spacer
// 13. Padding
// 14. FittedBox
// 15. Wrap (bonus)
// ============================================================================

/**
 * Usage Examples:
 *
 * ```tsx
 * import { Row, Column, Container, Padding, Expanded, Spacer } from './layout';
 *
 * // Basic Row
 * <Row>
 *   <Text>Item 1</Text>
 *   <Text>Item 2</Text>
 * </Row>
 *
 * // Column with spacing
 * <Column mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
 *   <Text>Top</Text>
 *   <Text>Bottom</Text>
 * </Column>
 *
 * // Container with styling
 * <Container
 *   width={{ type: 'dp', value: 200 }}
 *   padding={{ top: 16, right: 16, bottom: 16, left: 16 }}
 *   decoration={{ color: '#blue', borderRadius: { topLeft: 8, topRight: 8, bottomLeft: 8, bottomRight: 8 } }}
 * >
 *   <Text>Styled content</Text>
 * </Container>
 *
 * // Expanded in Row
 * <Row>
 *   <Container width={{ type: 'dp', value: 100 }} />
 *   <Expanded flex={2}>
 *     <Text>Takes 2/3 of remaining space</Text>
 *   </Expanded>
 *   <Expanded flex={1}>
 *     <Text>Takes 1/3 of remaining space</Text>
 *   </Expanded>
 * </Row>
 *
 * // Stack with Positioned
 * <Stack>
 *   <Container width={{ type: 'fill' }} height={{ type: 'fill' }} decoration={{ color: '#gray' }} />
 *   <Positioned top={10} right={10}>
 *     <Text>Top Right</Text>
 *   </Positioned>
 *   <Positioned bottom={10} left={10}>
 *     <Text>Bottom Left</Text>
 *   </Positioned>
 * </Stack>
 * ```
 */
