/**
 * MenuBar Component
 * Horizontal menu bar for desktop navigation
 */

import React, { useState } from 'react';
import { MenuItem } from './types';

export interface MenuBarProps {
  items: MenuItem[];
  onItemClick?: (item: MenuItem) => void;
  className?: string;
  style?: React.CSSProperties;
}

export const MenuBar: React.FC<MenuBarProps> = ({
  items,
  onItemClick,
  className = '',
  style = {},
}) => {
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);
  const [hoveredItemId, setHoveredItemId] = useState<string | null>(null);

  const handleItemClick = (item: MenuItem, event: React.MouseEvent) => {
    if (item.disabled) return;

    if (item.subItems && item.subItems.length > 0) {
      event.stopPropagation();
      setActiveMenuId(activeMenuId === item.id ? null : item.id);
    } else {
      onItemClick?.(item);
      item.onClick?.();
      setActiveMenuId(null);
    }
  };

  const handleSubItemClick = (subItem: MenuItem) => {
    if (subItem.disabled) return;
    onItemClick?.(subItem);
    subItem.onClick?.();
    setActiveMenuId(null);
  };

  return (
    <nav
      className={`menu-bar ${className}`}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '4px',
        background: '#ffffff',
        borderBottom: '1px solid #e0e0e0',
        padding: '8px 16px',
        ...style,
      }}
    >
      {items.map((item) => {
        const isActive = activeMenuId === item.id;
        const isHovered = hoveredItemId === item.id;
        const hasSubItems = item.subItems && item.subItems.length > 0;

        return (
          <div
            key={item.id}
            className="menu-bar-item-container"
            style={{ position: 'relative' }}
            onMouseEnter={() => setHoveredItemId(item.id)}
            onMouseLeave={() => setHoveredItemId(null)}
          >
            <div
              className={`menu-bar-item ${isActive ? 'active' : ''}`}
              onClick={(e) => handleItemClick(item, e)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 12px',
                cursor: item.disabled ? 'not-allowed' : 'pointer',
                opacity: item.disabled ? 0.5 : 1,
                background: isActive || isHovered ? '#f5f5f5' : 'transparent',
                borderRadius: '6px',
                fontSize: '14px',
                fontWeight: 500,
                color: '#333',
                transition: 'all 0.2s ease',
              }}
            >
              {item.icon && <span className="menu-bar-item-icon">{item.icon}</span>}
              <span className="menu-bar-item-label">{item.label}</span>
              {hasSubItems && <span style={{ fontSize: '10px' }}>▼</span>}
            </div>

            {/* Dropdown for subItems */}
            {isActive && hasSubItems && (
              <div
                className="menu-bar-dropdown"
                style={{
                  position: 'absolute',
                  top: '100%',
                  left: 0,
                  marginTop: '4px',
                  background: 'white',
                  border: '1px solid #e0e0e0',
                  borderRadius: '8px',
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                  minWidth: '200px',
                  padding: '8px 0',
                  zIndex: 1000,
                }}
              >
                {item.subItems!.map((subItem) => (
                  <div
                    key={subItem.id}
                    className="menu-bar-dropdown-item"
                    onClick={() => handleSubItemClick(subItem)}
                    style={{
                      padding: '10px 16px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      cursor: subItem.disabled ? 'not-allowed' : 'pointer',
                      opacity: subItem.disabled ? 0.5 : 1,
                      transition: 'background 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      if (!subItem.disabled) {
                        e.currentTarget.style.background = '#f5f5f5';
                      }
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'transparent';
                    }}
                  >
                    {subItem.icon && <span className="menu-bar-dropdown-item-icon">{subItem.icon}</span>}
                    <span className="menu-bar-dropdown-item-label">{subItem.label}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </nav>
  );
};
