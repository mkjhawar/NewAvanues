/**
 * RadioListTile Component - Flutter Parity Material Design
 *
 * A list tile with an integrated radio button, following Material Design 3 specifications.
 * Combines a ListTile with a Radio control for grouped radio behavior.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useCallback } from 'react';
import type { RadioListTileProps, ListTileControlAffinity } from './types';

export const RadioListTile: React.FC<RadioListTileProps> = ({
  id,
  title,
  subtitle,
  secondary,
  value,
  groupValue,
  enabled = true,
  controlAffinity = 'trailing' as ListTileControlAffinity,
  activeColor,
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
  const isSelected = value === groupValue;

  const handleClick = useCallback(() => {
    if (!enabled || !onChanged) return;
    onChanged(value);
  }, [enabled, value, onChanged]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleClick();
    }
  }, [handleClick]);

  const accessibilityLabel = contentDescription ||
    (typeof title === 'string' ? title : 'Radio list tile');
  const accessibilityState = isSelected ? 'selected' : 'not selected';

  const renderRadio = () => (
    <div
      className="radio-control"
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
      <div
        className="radio-outer"
        style={{
          width: 20,
          height: 20,
          borderRadius: '50%',
          border: `2px solid ${isSelected ? (activeColor || '#1976d2') : 'rgba(0, 0, 0, 0.54)'}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transition: 'border-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
          backgroundColor: 'transparent',
          cursor: enabled ? 'pointer' : 'default',
        }}
      >
        {isSelected && (
          <div
            className="radio-inner"
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              backgroundColor: activeColor || '#1976d2',
              transition: 'transform 150ms cubic-bezier(0.4, 0, 0.2, 1)',
              transform: 'scale(1)',
            }}
          />
        )}
      </div>
      <input
        type="radio"
        checked={isSelected}
        value={value}
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
      className={`radio-list-tile ${className}`}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="radio"
      aria-checked={isSelected}
      aria-disabled={!enabled}
      aria-label={`${accessibilityLabel}, ${accessibilityState}`}
      tabIndex={enabled ? 0 : -1}
      autoFocus={autofocus}
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: contentPadding || '8px 16px',
        cursor: enabled ? 'pointer' : 'default',
        backgroundColor: selected || isSelected
          ? (selectedTileColor || 'rgba(0, 0, 0, 0.08)')
          : (tileColor || 'transparent'),
        opacity: enabled ? 1 : 0.6,
        borderRadius: shape || 0,
        transition: 'background-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
        userSelect: 'none',
        minHeight: dense ? 48 : (isThreeLine ? 88 : 56),
        ...style,
      }}
    >
      {controlAffinity === 'leading' && renderRadio()}
      {renderContent()}
      {controlAffinity === 'trailing' && renderRadio()}
    </div>
  );
};

export default RadioListTile;
