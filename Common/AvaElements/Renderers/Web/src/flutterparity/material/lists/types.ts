/**
 * Shared types for List Tile components - Flutter Parity Material Design
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';

/**
 * Control affinity - position of control relative to title
 */
export enum ListTileControlAffinity {
  /** Control appears before the title (left in LTR) */
  Leading = 'leading',

  /** Control appears after the title (right in LTR) */
  Trailing = 'trailing',

  /** Control follows platform conventions */
  Platform = 'platform'
}

/**
 * Cross-axis alignment for children
 */
export enum CrossAxisAlignment {
  /** Align children to start */
  Start = 'start',

  /** Center children */
  Center = 'center',

  /** Align children to end */
  End = 'end',

  /** Stretch children to fill width */
  Stretch = 'stretch'
}

/**
 * Horizontal alignment for expanded content
 */
export enum Alignment {
  /** Align to start (left in LTR) */
  Start = 'start',

  /** Center horizontally */
  Center = 'center',

  /** Align to end (right in LTR) */
  End = 'end'
}

/**
 * Base props shared by all list tile components
 */
export interface BaseListTileProps {
  /** Unique identifier */
  id?: string;

  /** Main title text */
  title: React.ReactNode;

  /** Optional subtitle text */
  subtitle?: React.ReactNode;

  /** Optional secondary widget */
  secondary?: React.ReactNode;

  /** Whether the tile is enabled for interaction */
  enabled?: boolean;

  /** Background color of the tile */
  tileColor?: string;

  /** Background color when selected */
  selectedTileColor?: string;

  /** Whether to use dense vertical layout */
  dense?: boolean;

  /** Whether this is a three-line list tile */
  isThreeLine?: boolean;

  /** Custom padding for the tile */
  contentPadding?: string;

  /** Whether the tile is in selected state */
  selected?: boolean;

  /** Whether to autofocus this tile */
  autofocus?: boolean;

  /** Custom shape for the tile */
  shape?: string;

  /** Accessibility description */
  contentDescription?: string;

  /** Additional CSS class name */
  className?: string;

  /** Inline styles */
  style?: React.CSSProperties;
}

/**
 * Props for ExpansionTile component
 */
export interface ExpansionTileProps extends Omit<BaseListTileProps, 'secondary'> {
  /** Leading widget (typically icon or avatar) */
  leading?: React.ReactNode;

  /** Optional custom trailing widget (overrides default expand icon) */
  trailing?: React.ReactNode;

  /** Child components shown when expanded */
  children: React.ReactNode;

  /** Whether tile starts in expanded state */
  initiallyExpanded?: boolean;

  /** Whether to maintain child state when collapsed */
  maintainState?: boolean;

  /** Cross-axis alignment of children when expanded */
  expandedCrossAxisAlignment?: CrossAxisAlignment;

  /** Horizontal alignment of expanded content */
  expandedAlignment?: Alignment;

  /** Padding applied to children container */
  childrenPadding?: string;

  /** Background color override */
  backgroundColor?: string;

  /** Background color when collapsed */
  collapsedBackgroundColor?: string;

  /** Text color override */
  textColor?: string;

  /** Text color when collapsed */
  collapsedTextColor?: string;

  /** Icon color override */
  iconColor?: string;

  /** Icon color when collapsed */
  collapsedIconColor?: string;

  /** Callback invoked when expansion state changes */
  onExpansionChanged?: (expanded: boolean) => void;
}

/**
 * Props for CheckboxListTile component
 */
export interface CheckboxListTileProps extends BaseListTileProps {
  /** Checkbox state (true = checked, false = unchecked, null = indeterminate) */
  value: boolean | null;

  /** Whether to allow indeterminate state */
  tristate?: boolean;

  /** Position of checkbox relative to title */
  controlAffinity?: ListTileControlAffinity;

  /** Color when checkbox is checked */
  activeColor?: string;

  /** Color of checkmark */
  checkColor?: string;

  /** Callback invoked when checkbox value changes */
  onChanged?: (value: boolean | null) => void;
}

/**
 * Props for SwitchListTile component
 */
export interface SwitchListTileProps extends BaseListTileProps {
  /** Switch state (true = on, false = off) */
  value: boolean;

  /** Position of switch relative to title */
  controlAffinity?: ListTileControlAffinity;

  /** Color when switch is on */
  activeColor?: string;

  /** Track color when switch is on */
  activeTrackColor?: string;

  /** Thumb color when switch is off */
  inactiveThumbColor?: string;

  /** Track color when switch is off */
  inactiveTrackColor?: string;

  /** Callback invoked when switch value changes */
  onChanged?: (value: boolean) => void;
}

/**
 * Props for RadioListTile component
 */
export interface RadioListTileProps extends BaseListTileProps {
  /** This radio button's value */
  value: string;

  /** Currently selected value in the radio group */
  groupValue: string | null;

  /** Position of radio relative to title */
  controlAffinity?: ListTileControlAffinity;

  /** Color when radio is selected */
  activeColor?: string;

  /** Callback invoked when radio value changes */
  onChanged?: (value: string) => void;
}
