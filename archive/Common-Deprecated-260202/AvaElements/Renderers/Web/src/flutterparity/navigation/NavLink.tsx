/**
 * NavLink Component
 * Navigation link with active state support
 */

import React from 'react';

export interface NavLinkProps {
  href?: string;
  active?: boolean;
  disabled?: boolean;
  icon?: React.ReactNode;
  children: React.ReactNode;
  onClick?: (event: React.MouseEvent) => void;
  className?: string;
  style?: React.CSSProperties;
}

export const NavLink: React.FC<NavLinkProps> = ({
  href,
  active = false,
  disabled = false,
  icon,
  children,
  onClick,
  className = '',
  style = {},
}) => {
  const handleClick = (event: React.MouseEvent) => {
    if (disabled) {
      event.preventDefault();
      return;
    }
    onClick?.(event);
  };

  const baseStyle: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    padding: '8px 16px',
    textDecoration: 'none',
    color: active ? '#0066cc' : disabled ? '#999' : '#333',
    background: active ? '#e7f3ff' : 'transparent',
    border: 'none',
    borderRadius: '6px',
    fontSize: '14px',
    fontWeight: active ? 600 : 400,
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
    transition: 'all 0.2s ease',
    ...style,
  };

  const hoverStyle: React.CSSProperties = {
    background: active ? '#e7f3ff' : '#f5f5f5',
  };

  const [isHovered, setIsHovered] = React.useState(false);

  const Component = href ? 'a' : 'button';

  return (
    <Component
      href={href}
      onClick={handleClick}
      className={`nav-link ${active ? 'active' : ''} ${disabled ? 'disabled' : ''} ${className}`}
      style={isHovered && !disabled ? { ...baseStyle, ...hoverStyle } : baseStyle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      aria-current={active ? 'page' : undefined}
      aria-disabled={disabled}
    >
      {icon && <span className="nav-link-icon">{icon}</span>}
      <span className="nav-link-text">{children}</span>
    </Component>
  );
};
