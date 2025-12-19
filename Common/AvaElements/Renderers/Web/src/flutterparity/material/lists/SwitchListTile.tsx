/**
 * SwitchListTile Component - Flutter Parity Material Design
 *
 * A list tile with an integrated switch toggle, following Material Design 3 specifications.
 * Combines a ListTile with a Switch control.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useCallback } from 'react';
import type { SwitchListTileProps, ListTileControlAffinity } from './types';

export const SwitchListTile: React.FC<SwitchListTileProps> = ({
  id,
  title,
  subtitle,
  secondary,
  value = false,
  enabled = true,
  controlAffinity = 'trailing' as ListTileControlAffinity,
  activeColor,
  activeTrackColor,
  inactiveThumbColor,
  inactiveTrackColor,
  tileColor,
  selectedTileColor,
  dense = false,
  isThreeLine = false,
  contentPadding,
  selected = false,
  autofocus = false,
  shape,
  contentDescription,
  className = '',
  style,
  onChanged,
}) => {
  const handleClick = useCallback(() => {
    if (!enabled || !onChanged) return;
    onChanged(!value);
  }, [enabled, value, onChanged]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleClick();
    }
  }, [handleClick]);

  const accessibilityLabel = contentDescription ||
    (typeof title === 'string' ? title : 'Switch list tile');
  const accessibilityState = value ? 'on' : 'off';

  const renderSwitch = () => (
    <div
      className="switch-control"
      style={{
        width: 52,
        height: 32,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: controlAffinity === 'leading' ? 16 : 0,
        marginLeft: controlAffinity === 'trailing' ? 16 : 0,
      }}
    >
      <div
        className="switch-track"
        style={{
          width: 52,
          height: 32,
          borderRadius: 16,
          backgroundColor: value
            ? (activeTrackColor || 'rgba(25, 118, 210, 0.5)')
            : (inactiveTrackColor || 'rgba(0, 0, 0, 0.38)'),
          position: 'relative',
          transition: 'background-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
          cursor: enabled ? 'pointer' : 'default',
          opacity: enabled ? 1 : 0.5,
        }}
      >
        <div
          className="switch-thumb"
          style={{
            width: 20,
            height: 20,
            borderRadius: '50%',
            backgroundColor: value
              ? (activeColor || '#1976d2')
              : (inactiveThumbColor || '#fafafa'),
            position: 'absolute',
            top: '50%',
            left: value ? 'calc(100% - 26px)' : 6,
            transform: 'translateY(-50%)',
            transition: 'left 150ms cubic-bezier(0.4, 0, 0.2, 1), background-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
          }}
        />
      </div>
      <input
        type="checkbox"
        checked={value}
        onChange={() => {}} // Controlled by parent click
        disabled={!enabled}
        tabIndex={-1}
        aria-hidden="true"
        style={{ display: 'none' }}
      />
    </div>
  );

  const renderContent = () => (
    <div
      className="list-tile-content"
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        minHeight: dense ? 48 : (isThreeLine ? 88 : 56),
      }}
    >
      <div
        className="list-tile-title"
        style={{
          fontSize: 16,
          fontWeight: 500,
          lineHeight: '24px',
        }}
      >
        {title}
      </div>
      {subtitle && (
        <div
          className="list-tile-subtitle"
          style={{
            fontSize: 14,
            lineHeight: '20px',
            opacity: 0.7,
            marginTop: 2,
          }}
        >
          {subtitle}
        </div>
      )}
      {secondary && (
        <div
          className="list-tile-secondary"
          style={{
            fontSize: 14,
            lineHeight: '20px',
            opacity: 0.7,
            marginTop: 2,
          }}
        >
          {secondary}
        </div>
      )}
    </div>
  );

  return (
    <div
      id={id}
      className={`switch-list-tile ${className}`}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="switch"
      aria-checked={value}
      aria-disabled={!enabled}
      aria-label={`${accessibilityLabel}, ${accessibilityState}`}
      tabIndex={enabled ? 0 : -1}
      autoFocus={autofocus}
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: contentPadding || '8px 16px',
        cursor: enabled ? 'pointer' : 'default',
        backgroundColor: selected ? (selectedTileColor || 'rgba(0, 0, 0, 0.08)') : (tileColor || 'transparent'),
        opacity: enabled ? 1 : 0.6,
        borderRadius: shape || 0,
        transition: 'background-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
        userSelect: 'none',
        minHeight: dense ? 48 : (isThreeLine ? 88 : 56),
        ...style,
      }}
    >
      {controlAffinity === 'leading' && renderSwitch()}
      {renderContent()}
      {controlAffinity === 'trailing' && renderSwitch()}
    </div>
  );
};

export default SwitchListTile;
