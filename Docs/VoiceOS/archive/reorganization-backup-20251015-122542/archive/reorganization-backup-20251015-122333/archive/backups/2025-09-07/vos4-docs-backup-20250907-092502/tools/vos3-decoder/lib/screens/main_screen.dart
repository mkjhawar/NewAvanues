// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'dart:ui';
import 'package:provider/provider.dart';
import 'package:vos3_decoder/providers/data_provider.dart';
import 'package:vos3_decoder/widgets/glass_panel.dart';
import 'package:vos3_decoder/widgets/category_list.dart';
import 'package:vos3_decoder/widgets/data_viewer.dart';
import 'package:vos3_decoder/widgets/toolbar.dart';
import 'package:vos3_decoder/theme/app_theme.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  String? selectedCategory;
  
  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Scaffold(
      backgroundColor: isDark ? AppTheme.systemBackgroundDark : AppTheme.systemBackground,
      body: Stack(
        children: [
          // Background gradient
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: isDark
                    ? [const Color(0xFF1C1C1E), const Color(0xFF000000)]
                    : [const Color(0xFFF2F2F7), const Color(0xFFFFFFFF)],
              ),
            ),
          ),
          
          // Main content
          Column(
            children: [
              // Top toolbar
              const ToolbarWidget(),
              
              // File info bar
              _buildFileInfoBar(context),
              
              // Main content area
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Left panel - Categories
                      SizedBox(
                        width: 320,
                        child: GlassPanel(
                          child: CategoryList(
                            selectedCategory: selectedCategory,
                            onCategorySelected: (category) {
                              setState(() {
                                selectedCategory = category;
                              });
                            },
                          ),
                        ),
                      ),
                      
                      const SizedBox(width: 16),
                      
                      // Right panel - Data viewer
                      Expanded(
                        child: GlassPanel(
                          child: DataViewer(
                            category: selectedCategory,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              
              // Status bar
              _buildStatusBar(context),
            ],
          ),
        ],
      ),
    );
  }
  
  Widget _buildFileInfoBar(BuildContext context) {
    final dataProvider = context.watch<DataProvider>();
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Container(
      height: 60,
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: GlassPanel(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Row(
          children: [
            // File name
            Icon(
              Icons.insert_drive_file,
              size: 20,
              color: isDark ? AppTheme.iosBlueDark : AppTheme.iosBlue,
            ),
            const SizedBox(width: 8),
            Text(
              dataProvider.fileName ?? 'No file loaded',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            
            const SizedBox(width: 24),
            
            // Status
            if (dataProvider.isValid != null) ...[
              Icon(
                dataProvider.isValid! ? Icons.check_circle : Icons.error,
                size: 20,
                color: dataProvider.isValid!
                    ? (isDark ? AppTheme.iosGreenDark : AppTheme.iosGreen)
                    : (isDark ? AppTheme.iosRedDark : AppTheme.iosRed),
              ),
              const SizedBox(width: 4),
              Text(
                dataProvider.isValid! ? 'Valid' : 'Invalid',
                style: TextStyle(
                  color: dataProvider.isValid!
                      ? (isDark ? AppTheme.iosGreenDark : AppTheme.iosGreen)
                      : (isDark ? AppTheme.iosRedDark : AppTheme.iosRed),
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
            
            const Spacer(),
            
            // Metadata
            if (dataProvider.fileData != null) ...[
              _buildInfoChip('Version', dataProvider.fileData!['version'] ?? 'Unknown'),
              const SizedBox(width: 12),
              _buildInfoChip('Size', _formatFileSize(dataProvider.fileSize)),
              const SizedBox(width: 12),
              _buildInfoChip('Encrypted', dataProvider.isEncrypted ? 'Yes' : 'No'),
            ],
          ],
        ),
      ),
    );
  }
  
  Widget _buildInfoChip(String label, String value) {
    return Row(
      children: [
        Text(
          '$label: ',
          style: TextStyle(
            fontSize: 12,
            color: Theme.of(context).textTheme.bodySmall?.color?.withOpacity(0.6),
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
  
  Widget _buildStatusBar(BuildContext context) {
    final dataProvider = context.watch<DataProvider>();
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Container(
      height: 32,
      decoration: BoxDecoration(
        color: isDark ? AppTheme.secondarySystemBackgroundDark : AppTheme.secondarySystemBackground,
        border: Border(
          top: BorderSide(
            color: isDark ? Colors.white10 : Colors.black12,
            width: 1,
          ),
        ),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          Text(
            'Status: ${dataProvider.status}',
            style: const TextStyle(fontSize: 12),
          ),
          
          const SizedBox(width: 24),
          
          if (dataProvider.isValid != null) ...[
            Text(
              'Validation: ',
              style: const TextStyle(fontSize: 12),
            ),
            Icon(
              dataProvider.isValid! ? Icons.check : Icons.close,
              size: 14,
              color: dataProvider.isValid!
                  ? (isDark ? AppTheme.iosGreenDark : AppTheme.iosGreen)
                  : (isDark ? AppTheme.iosRedDark : AppTheme.iosRed),
            ),
            const SizedBox(width: 4),
            Text(
              dataProvider.isValid! ? 'Passed' : 'Failed',
              style: TextStyle(
                fontSize: 12,
                color: dataProvider.isValid!
                    ? (isDark ? AppTheme.iosGreenDark : AppTheme.iosGreen)
                    : (isDark ? AppTheme.iosRedDark : AppTheme.iosRed),
              ),
            ),
          ],
          
          const Spacer(),
          
          Text(
            'Items: ${dataProvider.totalItems}',
            style: const TextStyle(fontSize: 12),
          ),
        ],
      ),
    );
  }
  
  String _formatFileSize(int? bytes) {
    if (bytes == null) return 'Unknown';
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}