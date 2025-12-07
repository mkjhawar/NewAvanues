// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io' show Platform;

class AppTheme {
  // iOS System Colors
  static const Color iosBlue = Color(0xFF007AFF);
  static const Color iosGreen = Color(0xFF34C759);
  static const Color iosIndigo = Color(0xFF5856D6);
  static const Color iosOrange = Color(0xFFFF9500);
  static const Color iosPink = Color(0xFFFF2D55);
  static const Color iosPurple = Color(0xFFAF52DE);
  static const Color iosRed = Color(0xFFFF3B30);
  static const Color iosTeal = Color(0xFF5AC8FA);
  static const Color iosYellow = Color(0xFFFFCC00);
  
  // iOS Dark Mode Colors
  static const Color iosBlueDark = Color(0xFF0A84FF);
  static const Color iosGreenDark = Color(0xFF32D74B);
  static const Color iosIndigoDark = Color(0xFF5E5CE6);
  static const Color iosOrangeDark = Color(0xFFFF9F0A);
  static const Color iosPinkDark = Color(0xFFFF375F);
  static const Color iosPurpleDark = Color(0xFFBF5AF2);
  static const Color iosRedDark = Color(0xFFFF453A);
  static const Color iosTealDark = Color(0xFF64D2FF);
  static const Color iosYellowDark = Color(0xFFFFD60A);
  
  // Background Colors
  static const Color systemBackground = Color(0xFFFFFFFF);
  static const Color systemBackgroundDark = Color(0xFF000000);
  static const Color secondarySystemBackground = Color(0xFFF2F2F7);
  static const Color secondarySystemBackgroundDark = Color(0xFF1C1C1E);
  static const Color tertiarySystemBackground = Color(0xFFFFFFFF);
  static const Color tertiarySystemBackgroundDark = Color(0xFF2C2C2E);
  
  // Glass Effect Colors
  static Color glassLight = Colors.white.withOpacity(0.7);
  static Color glassDark = const Color(0xFF1E1E1E).withOpacity(0.7);
  static Color glassBorder = Colors.white.withOpacity(0.18);
  
  static String? get fontFamily {
    if (Platform.isIOS || Platform.isMacOS) {
      return '.SF Pro Text';
    } else if (Platform.isWindows) {
      return 'Segoe UI';
    }
    return null; // Use default
  }
  
  static ThemeData lightTheme() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      primaryColor: iosBlue,
      scaffoldBackgroundColor: systemBackground,
      fontFamily: fontFamily,
      
      colorScheme: const ColorScheme.light(
        primary: iosBlue,
        secondary: iosIndigo,
        error: iosRed,
        background: systemBackground,
        surface: secondarySystemBackground,
        onPrimary: Colors.white,
        onSecondary: Colors.white,
        onError: Colors.white,
        onBackground: Colors.black,
        onSurface: Colors.black,
      ),
      
      appBarTheme: AppBarTheme(
        elevation: 0,
        backgroundColor: glassLight,
        foregroundColor: Colors.black,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        titleTextStyle: TextStyle(
          fontFamily: fontFamily,
          fontSize: 20,
          fontWeight: FontWeight.w600,
          color: Colors.black,
        ),
      ),
      
      cardTheme: CardTheme(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        color: secondarySystemBackground,
      ),
      
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size(44, 44),
          backgroundColor: iosBlue,
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: TextStyle(
            fontFamily: fontFamily,
            fontSize: 17,
            fontWeight: FontWeight.w400,
          ),
        ),
      ),
      
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF767680).withOpacity(0.12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: iosBlue, width: 1),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      ),
    );
  }
  
  static ThemeData darkTheme() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      primaryColor: iosBlueDark,
      scaffoldBackgroundColor: systemBackgroundDark,
      fontFamily: fontFamily,
      
      colorScheme: const ColorScheme.dark(
        primary: iosBlueDark,
        secondary: iosIndigoDark,
        error: iosRedDark,
        background: systemBackgroundDark,
        surface: secondarySystemBackgroundDark,
        onPrimary: Colors.white,
        onSecondary: Colors.white,
        onError: Colors.white,
        onBackground: Colors.white,
        onSurface: Colors.white,
      ),
      
      appBarTheme: AppBarTheme(
        elevation: 0,
        backgroundColor: glassDark,
        foregroundColor: Colors.white,
        systemOverlayStyle: SystemUiOverlayStyle.light,
        titleTextStyle: TextStyle(
          fontFamily: fontFamily,
          fontSize: 20,
          fontWeight: FontWeight.w600,
          color: Colors.white,
        ),
      ),
      
      cardTheme: CardTheme(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        color: secondarySystemBackgroundDark,
      ),
      
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size(44, 44),
          backgroundColor: iosBlueDark,
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: TextStyle(
            fontFamily: fontFamily,
            fontSize: 17,
            fontWeight: FontWeight.w400,
          ),
        ),
      ),
      
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF767680).withOpacity(0.24),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: iosBlueDark, width: 1),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      ),
    );
  }
}