/**
 * CheckboxListTile Component - Flutter Parity Material Design
 *
 * A list tile with an integrated checkbox, following Material Design 3 specifications.
 * Combines a ListTile with a Checkbox control.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useCallback } from 'react';
import type { CheckboxListTileProps, ListTileControlAffinity } from './types';

export const CheckboxListTile: React.FC<CheckboxListTileProps> = ({
  id,
  title,
  subtitle,
  secondary,
  value = false,
  tristate = false,
  enabled = true,
  controlAffinity = 'leading' as ListTileControlAffinity,
  activeColor,
  checkColor,
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

    if (tristate) {
      // Cycle through: false -> true -> null -> false
      if (value === false) {
        onChanged(true);
      } else if (value === true) {
        onChanged(null);
      } else {
        onChanged(false);
      }
    } else {
      onChanged(!value);
    }
  }, [enabled, value, tristate, onChanged]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleClick();
    }
  }, [handleClick]);

  const getCheckboxState = (): string => {
    if (value === null) return 'indeterminate';
    return value ? 'checked' : 'unchecked';
  };

  const accessibilityLabel = contentDescription ||
    (typeof title === 'string' ? title : 'Checkbox list tile');

  const renderCheckbox = () => (
    <div
      className="checkbox-control"
      style={{
        width: 40,
        height: 40,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: controlAffinity === 'leading' ? 16 : 0,
        marginLeft: controlAffinity === 'trailing' ? 16 : 0,
      }}
    >
      <input
        type="checkbox"
        checked={value === true}
        ref={(input) => {
          if (input && value === null) {
            input.indeterminate = true;
          }
        }}
        onChange={() => {}} // Controlled by parent click
        disabled={!enabled}
        tabIndex={-1}
        aria-hidden="true"
        style={{
          width: 18,
          height: 18,
          cursor: enabled ? 'pointer' : 'default',
          accentColor: activeColor || '#1976d2',
          pointerEvents: 'none',
        }}
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
      className={`checkbox-list-tile ${className}`}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="checkbox"
      aria-checked={value === null ? 'mixed' : value}
      aria-disabled={!enabled}
      aria-label={`${accessibilityLabel}, ${getCheckboxState()}`}
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
      {controlAffinity === 'leading' && renderCheckbox()}
      {renderContent()}
      {controlAffinity === 'trailing' && renderCheckbox()}
    </div>
  );
};

export default CheckboxListTile;
