/**
 * SubMenu Component
 * Nested submenu support for complex navigation hierarchies
 */

import React, { useState } from 'react';
import { MenuItem } from './types';

export interface SubMenuProps {
  label: string;
  icon?: React.ReactNode;
  items: MenuItem[];
  level?: number;
  onItemClick?: (item: MenuItem) => void;
  className?: string;
}

export const SubMenu: React.FC<SubMenuProps> = ({
  label,
  icon,
  items,
  level = 0,
  onItemClick,
  className = '',
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  const handleToggle = () => {
    setIsExpanded(!isExpanded);
  };

  const handleItemClick = (item: MenuItem) => {
    if (item.disabled) return;
    onItemClick?.(item);
    item.onClick?.();
  };

  const indent = level * 16;

  return (
    <div className={`submenu ${className}`}>
      <div
        className={`submenu-trigger ${isExpanded ? 'expanded' : ''}`}
        onClick={handleToggle}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '8px 12px',
          paddingLeft: `${12 + indent}px`,
          cursor: 'pointer',
          background: isExpanded ? '#f5f5f5' : 'transparent',
          borderRadius: '6px',
          fontSize: '14px',
          fontWeight: 500,
          color: '#333',
          transition: 'all 0.2s ease',
        }}
        onMouseEnter={(e) => {
          if (!isExpanded) {
            e.currentTarget.style.background = '#fafafa';
          }
        }}
        onMouseLeave={(e) => {
          if (!isExpanded) {
            e.currentTarget.style.background = 'transparent';
          }
        }}
      >
        <span
          style={{
            fontSize: '10px',
            transition: 'transform 0.2s',
            transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
          }}
        >
          ▶
        </span>
        {icon && <span className="submenu-icon">{icon}</span>}
        <span className="submenu-label">{label}</span>
      </div>

      {isExpanded && (
        <div
          className="submenu-items"
          style={{
            marginTop: '4px',
            paddingLeft: `${indent}px`,
          }}
        >
          {items.map((item) => (
            <div key={item.id}>
              {item.divider ? (
                <div
                  style={{
                    height: '1px',
                    background: '#e0e0e0',
                    margin: '8px 16px',
                  }}
                />
              ) : item.subItems && item.subItems.length > 0 ? (
                <SubMenu
                  label={item.label}
                  icon={item.icon}
                  items={item.subItems}
                  level={level + 1}
                  onItemClick={onItemClick}
                />
              ) : (
                <div
                  className="submenu-item"
                  onClick={() => handleItemClick(item)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '8px 12px',
                    paddingLeft: `${28 + indent}px`,
                    cursor: item.disabled ? 'not-allowed' : 'pointer',
                    opacity: item.disabled ? 0.5 : 1,
                    borderRadius: '6px',
                    fontSize: '14px',
                    color: '#666',
                    transition: 'all 0.2s ease',
                  }}
                  onMouseEnter={(e) => {
                    if (!item.disabled) {
                      e.currentTarget.style.background = '#f5f5f5';
                      e.currentTarget.style.color = '#333';
                    }
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = '#666';
                  }}
                >
                  {item.icon && <span className="submenu-item-icon">{item.icon}</span>}
                  <span className="submenu-item-label">{item.label}</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
