/**
 * Animation Types for AvaElements Web Renderer
 * Common types and interfaces for animation components
 */

export type AnimationCurve =
  | 'linear'
  | 'easeIn'
  | 'easeOut'
  | 'easeInOut'
  | 'spring';

export interface BaseAnimationProps {
  /**
   * Duration of the animation in seconds
   * @default 0.3
   */
  duration?: number;

  /**
   * Delay before animation starts in seconds
   * @default 0
   */
  delay?: number;

  /**
   * Animation curve/easing function
   * @default 'easeInOut'
   */
  curve?: AnimationCurve;

  /**
   * Callback fired when animation completes
   */
  onAnimationComplete?: () => void;

  /**
   * Additional CSS class name
   */
  className?: string;
}

export interface SpringConfig {
  type: 'spring';
  stiffness?: number;
  damping?: number;
  mass?: number;
}

export interface TweenConfig {
  duration: number;
  delay: number;
  ease: string;
}

export type TransitionConfig = SpringConfig | TweenConfig;

/**
 * Helper to convert AnimationCurve to Framer Motion transition config
 */
export const getTransitionConfig = (
  curve: AnimationCurve,
  duration: number,
  delay: number
): TransitionConfig => {
  if (curve === 'spring') {
    return {
      type: 'spring',
      stiffness: 300,
      damping: 30,
      mass: 1,
    };
  }

  return {
    duration,
    delay,
    ease: curve,
  };
};

/**
 * Check if user prefers reduced motion
 */
export const prefersReducedMotion = (): boolean => {
  if (typeof window === 'undefined') return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
};
