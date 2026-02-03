/**
 * BadgeTheme Component - Flutter Parity Material Design
 *
 * Provides theming for all badges in its subtree.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { createContext, useContext } from 'react';
import type { BadgeThemeData, BadgeThemeProps } from './types';

const BadgeThemeContext = createContext<BadgeThemeData | null>(null);

export const useBadgeTheme = (): BadgeThemeData | null => {
  return useContext(BadgeThemeContext);
};

export const BadgeTheme: React.FC<BadgeThemeProps> = ({ data, children }) => {
  return (
    <BadgeThemeContext.Provider value={data}>
      {children}
    </BadgeThemeContext.Provider>
  );
};

export default BadgeTheme;
