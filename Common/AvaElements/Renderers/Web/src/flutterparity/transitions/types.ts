/**
 * Common types for transition components
 */

export type TransitionDirection = 'left' | 'right' | 'up' | 'down';

export type TransitionMode = 'sync' | 'wait';

export type EasingFunction =
  | 'linear'
  | 'easeIn'
  | 'easeOut'
  | 'easeInOut'
  | 'circIn'
  | 'circOut'
  | 'circInOut'
  | 'backIn'
  | 'backOut'
  | 'backInOut'
  | number[];

export interface BaseTransitionProps {
  children: React.ReactNode;
  duration?: number;
  delay?: number;
  easing?: EasingFunction;
  className?: string;
}

export interface VisibilityTransitionProps extends BaseTransitionProps {
  show?: boolean;
}

/**
 * Check if user prefers reduced motion
 */
export const prefersReducedMotion = (): boolean => {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
};

/**
 * Get adjusted duration based on reduced motion preference
 */
export const getAdjustedDuration = (duration: number): number => {
  return prefersReducedMotion() ? 0 : duration;
};

/**
 * Convert easing string to Framer Motion easing array
 */
export const getEasing = (easing?: EasingFunction): number[] | undefined => {
  if (Array.isArray(easing)) return easing;
  if (!easing || easing === 'linear') return undefined;

  const easings: Record<string, number[]> = {
    easeIn: [0.4, 0, 1, 1],
    easeOut: [0, 0, 0.2, 1],
    easeInOut: [0.4, 0, 0.2, 1],
    circIn: [0.6, 0.04, 0.98, 0.335],
    circOut: [0.075, 0.82, 0.165, 1],
    circInOut: [0.785, 0.135, 0.15, 0.86],
    backIn: [0.6, -0.28, 0.735, 0.045],
    backOut: [0.175, 0.885, 0.32, 1.275],
    backInOut: [0.68, -0.55, 0.265, 1.55],
  };

  return easings[easing];
};
