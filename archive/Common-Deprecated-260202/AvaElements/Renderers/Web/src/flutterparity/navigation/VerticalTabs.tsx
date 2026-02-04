/**
 * VerticalTabs Component
 * Vertical tab navigation for sidebar-style interfaces
 */

import React, { useState } from 'react';

export interface VerticalTab {
  id: string;
  label: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  content?: React.ReactNode;
}

export interface VerticalTabsProps {
  tabs: VerticalTab[];
  defaultTab?: string;
  onChange?: (tabId: string) => void;
  tabWidth?: number | string;
  className?: string;
}

export const VerticalTabs: React.FC<VerticalTabsProps> = ({
  tabs,
  defaultTab,
  onChange,
  tabWidth = 200,
  className = '',
}) => {
  const [activeTab, setActiveTab] = useState(defaultTab || tabs[0]?.id);

  const handleTabClick = (tabId: string, disabled?: boolean) => {
    if (disabled) return;
    setActiveTab(tabId);
    onChange?.(tabId);
  };

  const activeTabContent = tabs.find((tab) => tab.id === activeTab)?.content;
  const width = typeof tabWidth === 'number' ? `${tabWidth}px` : tabWidth;

  return (
    <div
      className={`vertical-tabs ${className}`}
      style={{
        display: 'flex',
        height: '100%',
      }}
    >
      {/* Tab List */}
      <div
        className="vertical-tabs-list"
        role="tablist"
        aria-orientation="vertical"
        style={{
          width,
          borderRight: '1px solid #e0e0e0',
          background: '#f8f9fa',
          padding: '8px',
        }}
      >
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;

          return (
            <div
              key={tab.id}
              role="tab"
              aria-selected={isActive}
              aria-disabled={tab.disabled}
              onClick={() => handleTabClick(tab.id, tab.disabled)}
              className={`vertical-tab ${isActive ? 'active' : ''} ${tab.disabled ? 'disabled' : ''}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '12px 16px',
                marginBottom: '4px',
                cursor: tab.disabled ? 'not-allowed' : 'pointer',
                opacity: tab.disabled ? 0.5 : 1,
                background: isActive ? '#ffffff' : 'transparent',
                border: isActive ? '1px solid #e0e0e0' : '1px solid transparent',
                borderRight: isActive ? '2px solid #2196f3' : '1px solid transparent',
                borderRadius: '6px',
                fontSize: '14px',
                fontWeight: isActive ? 600 : 400,
                color: isActive ? '#2196f3' : '#666',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => {
                if (!tab.disabled && !isActive) {
                  e.currentTarget.style.background = '#e9ecef';
                }
              }}
              onMouseLeave={(e) => {
                if (!isActive) {
                  e.currentTarget.style.background = 'transparent';
                }
              }}
            >
              {tab.icon && <span className="vertical-tab-icon">{tab.icon}</span>}
              <span className="vertical-tab-label">{tab.label}</span>
            </div>
          );
        })}
      </div>

      {/* Tab Content */}
      <div
        className="vertical-tabs-content"
        role="tabpanel"
        style={{
          flex: 1,
          padding: '24px',
          overflow: 'auto',
        }}
      >
        {activeTabContent}
      </div>
    </div>
  );
};
