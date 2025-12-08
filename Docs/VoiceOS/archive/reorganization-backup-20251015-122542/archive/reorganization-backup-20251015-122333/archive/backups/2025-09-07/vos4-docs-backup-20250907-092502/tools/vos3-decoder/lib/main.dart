// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'dart:io' show Platform;
import 'package:vos3_decoder/providers/data_provider.dart';
import 'package:vos3_decoder/screens/main_screen.dart';
import 'package:vos3_decoder/theme/app_theme.dart';

void main() {
  runApp(const VOS3DecoderApp());
}

class VOS3DecoderApp extends StatelessWidget {
  const VOS3DecoderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => DataProvider()),
      ],
      child: MaterialApp(
        title: 'VOS3 Data Decoder',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme(),
        darkTheme: AppTheme.darkTheme(),
        themeMode: ThemeMode.system,
        home: const MainScreen(),
      ),
    );
  }
}