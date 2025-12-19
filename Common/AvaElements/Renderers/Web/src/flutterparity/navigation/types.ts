/**
 * Navigation Component Types
 * Type definitions for navigation components
 */

export interface MenuItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  divider?: boolean;
  subItems?: MenuItem[];
  onClick?: () => void;
}

export interface Step {
  label: string;
  description?: string;
  icon?: React.ReactNode;
  completed?: boolean;
  error?: boolean;
}

export interface SidebarSection {
  id: string;
  label: string;
  icon?: React.ReactNode;
  items: SidebarItem[];
  collapsed?: boolean;
}

export interface SidebarItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  href?: string;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

export type MenuPlacement = 'bottom' | 'top' | 'left' | 'right';
export type Orientation = 'horizontal' | 'vertical';
export type ResponsiveColumns = number | { sm?: number; md?: number; lg?: number };
