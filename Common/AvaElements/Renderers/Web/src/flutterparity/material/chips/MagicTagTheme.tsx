/**
 * ChipTheme Component - Flutter Parity Material Design
 *
 * Provides theming for all chip variants in its subtree.
 * Uses React Context for theme inheritance.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { createContext, useContext } from 'react';
import type { MagicTagStyle, MagicTagThemeProps } from './types';

const ChipThemeContext = createContext<MagicTagStyle | null>(null);

export const useMagicTagTheme = (): ChipThemeData | null => {
  return useContext(ChipThemeContext);
};

export const MagicTagTheme: React.FC<MagicTagThemeProps> = ({ data, children }) => {
  return (
    <ChipThemeContext.Provider value={data}>
      {children}
    </ChipThemeContext.Provider>
  );
};

export default ChipTheme;
