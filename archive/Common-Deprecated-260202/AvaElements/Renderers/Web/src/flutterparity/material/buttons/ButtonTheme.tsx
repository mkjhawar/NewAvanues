/**
 * ButtonTheme - Flutter Parity Material Design
 *
 * Provides theming for all buttons in its subtree.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { createContext, useContext } from 'react';
import type { ButtonThemeData, ButtonThemeProps } from './types';

const ButtonThemeContext = createContext<ButtonThemeData | null>(null);

export const useButtonTheme = (): ButtonThemeData | null => {
  return useContext(ButtonThemeContext);
};

export const ButtonTheme: React.FC<ButtonThemeProps> = ({ data, children }) => {
  return (
    <ButtonThemeContext.Provider value={data}>
      {children}
    </ButtonThemeContext.Provider>
  );
};

export default ButtonTheme;
