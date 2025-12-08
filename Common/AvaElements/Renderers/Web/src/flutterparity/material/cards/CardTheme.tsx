/**
 * CardTheme Component - Flutter Parity Material Design
 *
 * Provides theming for all cards in its subtree.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { createContext, useContext } from 'react';
import type { CardThemeData, CardThemeProps } from './types';

const CardThemeContext = createContext<CardThemeData | null>(null);

export const useCardTheme = (): CardThemeData | null => {
  return useContext(CardThemeContext);
};

export const CardTheme: React.FC<CardThemeProps> = ({ data, children }) => {
  return (
    <CardThemeContext.Provider value={data}>
      {children}
    </CardThemeContext.Provider>
  );
};

export default CardTheme;
