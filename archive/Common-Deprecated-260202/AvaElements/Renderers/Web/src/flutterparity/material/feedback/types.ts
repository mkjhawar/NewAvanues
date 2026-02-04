/**
 * Shared types for Feedback components
 */

export interface BasePanelProps {
  title?: string;
  children: React.ReactNode;
  icon?: React.ReactNode;
  dismissible?: boolean;
  onDismiss?: () => void;
  className?: string;
}

export interface PopupProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
  title?: string;
  showBackdrop?: boolean;
  closeOnBackdropClick?: boolean;
  className?: string;
}

export interface DisclosureProps {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  icon?: React.ReactNode;
  className?: string;
  onToggle?: (isOpen: boolean) => void;
}

export interface CalloutProps {
  children: React.ReactNode;
  variant?: 'info' | 'warning' | 'error' | 'success' | 'neutral';
  icon?: React.ReactNode;
  title?: string;
  className?: string;
}

export interface FullPageLoadingProps {
  message?: string;
  spinner?: React.ReactNode;
  className?: string;
}

export interface AnimatedCheckProps {
  size?: number;
  color?: string;
  strokeWidth?: number;
  delay?: number;
  className?: string;
}

export interface AnimatedErrorProps {
  size?: number;
  color?: string;
  strokeWidth?: number;
  delay?: number;
  className?: string;
}
