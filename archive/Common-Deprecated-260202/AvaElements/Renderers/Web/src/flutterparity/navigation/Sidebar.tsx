/**
 * Sidebar Component
 * Collapsible sidebar navigation with sections
 */

import React, { useState } from 'react';
import { SidebarSection } from './types';

export interface SidebarProps {
  sections: SidebarSection[];
  collapsed?: boolean;
  width?: number | string;
  onSectionToggle?: (sectionId: string) => void;
  className?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({
  sections,
  collapsed = false,
  width = 280,
  onSectionToggle,
  className = '',
}) => {
  const [collapsedSections, setCollapsedSections] = useState<Set<string>>(
    new Set(sections.filter((s) => s.collapsed).map((s) => s.id))
  );

  const toggleSection = (sectionId: string) => {
    setCollapsedSections((prev) => {
      const next = new Set(prev);
      if (next.has(sectionId)) {
        next.delete(sectionId);
      } else {
        next.add(sectionId);
      }
      return next;
    });
    onSectionToggle?.(sectionId);
  };

  const sidebarWidth = typeof width === 'number' ? `${width}px` : width;

  return (
    <nav
      className={`sidebar ${collapsed ? 'collapsed' : ''} ${className}`}
      style={{
        width: collapsed ? '60px' : sidebarWidth,
        height: '100%',
        background: '#f8f9fa',
        borderRight: '1px solid #e0e0e0',
        overflow: 'auto',
        transition: 'width 0.3s ease',
      }}
    >
      <div style={{ padding: collapsed ? '8px 4px' : '16px' }}>
        {sections.map((section) => {
          const isSectionCollapsed = collapsedSections.has(section.id);

          return (
            <div key={section.id} className="sidebar-section" style={{ marginBottom: '24px' }}>
              <div
                className="sidebar-section-header"
                onClick={() => toggleSection(section.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: collapsed ? '8px 4px' : '8px 12px',
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '14px',
                  color: '#333',
                  borderRadius: '6px',
                  transition: 'background 0.2s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#e9ecef')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                {section.icon && (
                  <span className="sidebar-section-icon" style={{ flexShrink: 0 }}>
                    {section.icon}
                  </span>
                )}
                {!collapsed && (
                  <>
                    <span style={{ flex: 1 }}>{section.label}</span>
                    <span
                      style={{
                        transition: 'transform 0.2s',
                        transform: isSectionCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
                      }}
                    >
                      ▼
                    </span>
                  </>
                )}
              </div>

              {!isSectionCollapsed && (
                <div
                  className="sidebar-section-items"
                  style={{
                    marginTop: '4px',
                    marginLeft: collapsed ? 0 : '12px',
                  }}
                >
                  {section.items.map((item) => (
                    <div
                      key={item.id}
                      className={`sidebar-item ${item.active ? 'active' : ''}`}
                      onClick={item.onClick}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        padding: collapsed ? '8px 4px' : '8px 12px',
                        cursor: item.disabled ? 'not-allowed' : 'pointer',
                        opacity: item.disabled ? 0.5 : 1,
                        background: item.active ? '#e7f3ff' : 'transparent',
                        borderRadius: '6px',
                        fontSize: '14px',
                        color: item.active ? '#0066cc' : '#666',
                        transition: 'all 0.2s',
                      }}
                      onMouseEnter={(e) => {
                        if (!item.disabled && !item.active) {
                          e.currentTarget.style.background = '#f1f3f5';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!item.active) {
                          e.currentTarget.style.background = 'transparent';
                        }
                      }}
                    >
                      {item.icon && (
                        <span className="sidebar-item-icon" style={{ flexShrink: 0 }}>
                          {item.icon}
                        </span>
                      )}
                      {!collapsed && <span className="sidebar-item-label">{item.label}</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </nav>
  );
};
