/**
 * Theme converter from MagicUI to Material-UI
 */

import { createTheme, Theme as MuiTheme } from '@mui/material/styles';
import type {
  Theme,
  Color,
  ColorScheme,
  Typography as MagicTypography,
  Font
} from '../types';
import { colorToCss, fontWeightToCss } from '../types';

/**
 * Convert MagicUI Theme to Material-UI Theme
 */
export function convertTheme(magicTheme: Theme): MuiTheme {
  return createTheme({
    palette: convertColorScheme(magicTheme.colorScheme),
    typography: convertTypography(magicTheme.typography),
    shape: {
      borderRadius: magicTheme.shapes.medium
    },
    spacing: magicTheme.spacing.md,
    shadows: convertElevation(magicTheme.elevation)
  });
}

/**
 * Convert color scheme to Material-UI palette
 */
function convertColorScheme(colorScheme: ColorScheme) {
  const mode = colorScheme.mode === 'Light' ? 'light' : 'dark';

  return {
    mode,
    primary: {
      main: colorToCss(colorScheme.primary),
      light: colorToCss(colorScheme.primaryContainer),
      dark: colorToCss(darkenColor(colorScheme.primary, 0.2)),
      contrastText: colorToCss(colorScheme.onPrimary)
    },
    secondary: {
      main: colorToCss(colorScheme.secondary),
      light: colorToCss(colorScheme.secondaryContainer),
      dark: colorToCss(darkenColor(colorScheme.secondary, 0.2)),
      contrastText: colorToCss(colorScheme.onSecondary)
    },
    error: {
      main: colorToCss(colorScheme.error),
      light: colorToCss(colorScheme.errorContainer),
      dark: colorToCss(darkenColor(colorScheme.error, 0.2)),
      contrastText: colorToCss(colorScheme.onError)
    },
    background: {
      default: colorToCss(colorScheme.background),
      paper: colorToCss(colorScheme.surface)
    },
    text: {
      primary: colorToCss(colorScheme.onBackground),
      secondary: colorToCss(colorScheme.onSurfaceVariant)
    }
  };
}

/**
 * Convert typography to Material-UI typography
 */
function convertTypography(typography: MagicTypography) {
  return {
    fontFamily: typography.bodyMedium.family,
    h1: convertFont(typography.displayLarge),
    h2: convertFont(typography.displayMedium),
    h3: convertFont(typography.displaySmall),
    h4: convertFont(typography.headlineLarge),
    h5: convertFont(typography.headlineMedium),
    h6: convertFont(typography.headlineSmall),
    subtitle1: convertFont(typography.titleLarge),
    subtitle2: convertFont(typography.titleMedium),
    body1: convertFont(typography.bodyLarge),
    body2: convertFont(typography.bodyMedium),
    caption: convertFont(typography.bodySmall),
    button: convertFont(typography.labelLarge)
  };
}

/**
 * Convert Font to Material-UI typography variant
 */
function convertFont(font: Font) {
  return {
    fontFamily: font.family,
    fontSize: `${font.size}px`,
    fontWeight: fontWeightToCss(font.weight)
  };
}

/**
 * Convert elevation to Material-UI shadows
 */
function convertElevation(elevation: any): any {
  const baseShadow = 'rgba(0, 0, 0, 0.2)';

  return [
    'none', // 0
    `0px 1px ${elevation.level1}px ${baseShadow}`, // 1
    `0px 2px ${elevation.level2}px ${baseShadow}`, // 2
    `0px 3px ${elevation.level3}px ${baseShadow}`, // 3
    `0px 4px ${elevation.level4}px ${baseShadow}`, // 4
    `0px 5px ${elevation.level5}px ${baseShadow}`, // 5
    `0px 6px ${elevation.level5 * 1.2}px ${baseShadow}`, // 6
    `0px 7px ${elevation.level5 * 1.4}px ${baseShadow}`, // 7
    `0px 8px ${elevation.level5 * 1.6}px ${baseShadow}`, // 8
    `0px 9px ${elevation.level5 * 1.8}px ${baseShadow}`, // 9
    `0px 10px ${elevation.level5 * 2}px ${baseShadow}`, // 10
    `0px 11px ${elevation.level5 * 2.2}px ${baseShadow}`, // 11
    `0px 12px ${elevation.level5 * 2.4}px ${baseShadow}`, // 12
    `0px 13px ${elevation.level5 * 2.6}px ${baseShadow}`, // 13
    `0px 14px ${elevation.level5 * 2.8}px ${baseShadow}`, // 14
    `0px 15px ${elevation.level5 * 3}px ${baseShadow}`, // 15
    `0px 16px ${elevation.level5 * 3.2}px ${baseShadow}`, // 16
    `0px 17px ${elevation.level5 * 3.4}px ${baseShadow}`, // 17
    `0px 18px ${elevation.level5 * 3.6}px ${baseShadow}`, // 18
    `0px 19px ${elevation.level5 * 3.8}px ${baseShadow}`, // 19
    `0px 20px ${elevation.level5 * 4}px ${baseShadow}`, // 20
    `0px 21px ${elevation.level5 * 4.2}px ${baseShadow}`, // 21
    `0px 22px ${elevation.level5 * 4.4}px ${baseShadow}`, // 22
    `0px 23px ${elevation.level5 * 4.6}px ${baseShadow}`, // 23
    `0px 24px ${elevation.level5 * 4.8}px ${baseShadow}`  // 24
  ];
}

/**
 * Darken a color by a factor (0-1)
 */
function darkenColor(color: Color, factor: number): Color {
  return {
    red: Math.round(color.red * (1 - factor)),
    green: Math.round(color.green * (1 - factor)),
    blue: Math.round(color.blue * (1 - factor)),
    alpha: color.alpha
  };
}

/**
 * Lighten a color by a factor (0-1)
 */
function lightenColor(color: Color, factor: number): Color {
  return {
    red: Math.round(color.red + (255 - color.red) * factor),
    green: Math.round(color.green + (255 - color.green) * factor),
    blue: Math.round(color.blue + (255 - color.blue) * factor),
    alpha: color.alpha
  };
}

/**
 * Create default Material 3 theme
 */
export function createDefaultTheme(): Theme {
  return {
    name: 'Material 3 Light',
    platform: 'Material3_Expressive' as any,
    colorScheme: {
      mode: 'Light' as any,
      primary: { red: 103, green: 80, blue: 164, alpha: 1 },
      onPrimary: { red: 255, green: 255, blue: 255, alpha: 1 },
      primaryContainer: { red: 234, green: 221, blue: 255, alpha: 1 },
      onPrimaryContainer: { red: 33, green: 0, blue: 94, alpha: 1 },
      secondary: { red: 98, green: 91, blue: 113, alpha: 1 },
      onSecondary: { red: 255, green: 255, blue: 255, alpha: 1 },
      secondaryContainer: { red: 232, green: 222, blue: 248, alpha: 1 },
      onSecondaryContainer: { red: 30, green: 25, blue: 43, alpha: 1 },
      tertiary: { red: 125, green: 82, blue: 96, alpha: 1 },
      onTertiary: { red: 255, green: 255, blue: 255, alpha: 1 },
      tertiaryContainer: { red: 255, green: 216, blue: 228, alpha: 1 },
      onTertiaryContainer: { red: 55, green: 11, blue: 30, alpha: 1 },
      error: { red: 186, green: 26, blue: 26, alpha: 1 },
      onError: { red: 255, green: 255, blue: 255, alpha: 1 },
      errorContainer: { red: 249, green: 222, blue: 220, alpha: 1 },
      onErrorContainer: { red: 65, green: 0, blue: 2, alpha: 1 },
      surface: { red: 254, green: 247, blue: 255, alpha: 1 },
      onSurface: { red: 28, green: 27, blue: 31, alpha: 1 },
      surfaceVariant: { red: 231, green: 224, blue: 236, alpha: 1 },
      onSurfaceVariant: { red: 73, green: 69, blue: 79, alpha: 1 },
      background: { red: 254, green: 247, blue: 255, alpha: 1 },
      onBackground: { red: 28, green: 27, blue: 31, alpha: 1 },
      outline: { red: 121, green: 116, blue: 126, alpha: 1 },
      outlineVariant: { red: 202, green: 196, blue: 208, alpha: 1 }
    },
    typography: {
      displayLarge: { family: 'Roboto', size: 57, weight: 'Normal' as any },
      displayMedium: { family: 'Roboto', size: 45, weight: 'Normal' as any },
      displaySmall: { family: 'Roboto', size: 36, weight: 'Normal' as any },
      headlineLarge: { family: 'Roboto', size: 32, weight: 'Normal' as any },
      headlineMedium: { family: 'Roboto', size: 28, weight: 'Normal' as any },
      headlineSmall: { family: 'Roboto', size: 24, weight: 'Normal' as any },
      titleLarge: { family: 'Roboto', size: 22, weight: 'Medium' as any },
      titleMedium: { family: 'Roboto', size: 16, weight: 'Medium' as any },
      titleSmall: { family: 'Roboto', size: 14, weight: 'Medium' as any },
      bodyLarge: { family: 'Roboto', size: 16, weight: 'Normal' as any },
      bodyMedium: { family: 'Roboto', size: 14, weight: 'Normal' as any },
      bodySmall: { family: 'Roboto', size: 12, weight: 'Normal' as any },
      labelLarge: { family: 'Roboto', size: 14, weight: 'Medium' as any },
      labelMedium: { family: 'Roboto', size: 12, weight: 'Medium' as any },
      labelSmall: { family: 'Roboto', size: 11, weight: 'Medium' as any }
    },
    shapes: {
      extraSmall: 4,
      small: 8,
      medium: 12,
      large: 16,
      extraLarge: 28
    },
    spacing: {
      xs: 4,
      sm: 8,
      md: 16,
      lg: 24,
      xl: 32,
      xxl: 48
    },
    elevation: {
      level0: 0,
      level1: 1,
      level2: 3,
      level3: 6,
      level4: 8,
      level5: 12
    }
  };
}
