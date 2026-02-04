/**
 * Animation Components for AvaElements Web Renderer
 * Flutter-parity animation components using Framer Motion
 */

export { AnimatedContainer } from './AnimatedContainer';
export type { AnimatedContainerProps } from './AnimatedContainer';

export { AnimatedOpacity } from './AnimatedOpacity';
export type { AnimatedOpacityProps } from './AnimatedOpacity';

export { AnimatedPositioned } from './AnimatedPositioned';
export type { AnimatedPositionedProps } from './AnimatedPositioned';

export { AnimatedDefaultTextStyle } from './AnimatedDefaultTextStyle';
export type { AnimatedDefaultTextStyleProps } from './AnimatedDefaultTextStyle';

export { AnimatedPadding } from './AnimatedPadding';
export type { AnimatedPaddingProps } from './AnimatedPadding';

export { AnimatedSize } from './AnimatedSize';
export type { AnimatedSizeProps } from './AnimatedSize';

export { AnimatedAlign } from './AnimatedAlign';
export type { AnimatedAlignProps } from './AnimatedAlign';

export { AnimatedScale } from './AnimatedScale';
export type { AnimatedScaleProps } from './AnimatedScale';

export type {
  AnimationCurve,
  BaseAnimationProps,
  SpringConfig,
  TweenConfig,
  TransitionConfig,
} from './types';

export { getTransitionConfig, prefersReducedMotion } from './types';
