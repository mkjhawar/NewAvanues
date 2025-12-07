// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

import 'package:flutter/foundation.dart';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:encrypt/encrypt.dart';
import 'package:vos3_decoder/models/vos3_data.dart';
import 'package:vos3_decoder/services/validator.dart';

class DataProvider extends ChangeNotifier {
  static const String _encryptionKey = 'VOS3DataExport2024SecureKey12345';
  static const String _encryptionIV = 'VOS3InitVector16';
  
  Map<String, dynamic>? _fileData;
  String? _fileName;
  int? _fileSize;
  bool _isEncrypted = false;
  bool? _isValid;
  String _status = 'Ready';
  
  final Map<String, List<dynamic>> _categorizedData = {};
  
  Map<String, dynamic>? get fileData => _fileData;
  String? get fileName => _fileName;
  int? get fileSize => _fileSize;
  bool get isEncrypted => _isEncrypted;
  bool? get isValid => _isValid;
  String get status => _status;
  Map<String, List<dynamic>> get categorizedData => _categorizedData;
  
  int get totalItems {
    int count = 0;
    _categorizedData.forEach((key, value) {
      count += value.length;
    });
    return count;
  }
  
  Future<void> loadFile(File file) async {
    try {
      _status = 'Loading file...';
      notifyListeners();
      
      _fileName = file.path.split('/').last;
      _fileSize = await file.length();
      
      final contents = await file.readAsString();
      final json = jsonDecode(contents);
      
      // Check if encrypted
      if (json['encodedData'] != null) {
        _isEncrypted = true;
        _status = 'Decrypting...';
        notifyListeners();
        
        final decrypted = _decryptData(json['encodedData']);
        _fileData = jsonDecode(decrypted);
      } else {
        _isEncrypted = false;
        _fileData = json;
      }
      
      // Parse and categorize data
      _parseData();
      
      // Validate
      _status = 'Validating...';
      notifyListeners();
      _isValid = Validator.validateData(_fileData!);
      
      _status = 'File loaded successfully';
      notifyListeners();
    } catch (e) {
      _status = 'Error: $e';
      _isValid = false;
      notifyListeners();
    }
  }
  
  void _parseData() {
    if (_fileData == null) return;
    
    _categorizedData.clear();
    
    // Parse compact JSON format
    if (_fileData!['p'] != null) {
      _categorizedData['User Preferences'] = _parsePreferences(_fileData!['p']);
    }
    
    if (_fileData!['h'] != null) {
      _categorizedData['Command History'] = _parseCommandHistory(_fileData!['h']);
    }
    
    if (_fileData!['c'] != null) {
      _categorizedData['Custom Commands'] = _fileData!['c'] as List;
    }
    
    if (_fileData!['g'] != null) {
      _categorizedData['Touch Gestures'] = _fileData!['g'] as List;
    }
    
    if (_fileData!['s'] != null) {
      _categorizedData['User Sequences'] = _fileData!['s'] as List;
    }
    
    // Add other categories...
    _categorizedData['Device Profiles'] = _fileData!['deviceProfiles'] ?? [];
    _categorizedData['Usage Statistics'] = _fileData!['usageStatistics'] ?? [];
    _categorizedData['Language Models'] = _fileData!['languageModels'] ?? [];
    _categorizedData['Retention Settings'] = _fileData!['retentionSettings'] != null ? [_fileData!['retentionSettings']] : [];
    _categorizedData['Analytics Settings'] = _fileData!['analyticsSettings'] != null ? [_fileData!['analyticsSettings']] : [];
    _categorizedData['Error Reports'] = _fileData!['errorReports'] ?? [];
    _categorizedData['Gesture Learning'] = _fileData!['gestureLearning'] ?? [];
  }
  
  List<Map<String, dynamic>> _parsePreferences(List<dynamic> prefs) {
    return prefs.map((pref) {
      final arr = pref as List;
      return {
        'key': arr[0],
        'value': arr[1],
        'type': arr[2],
        'module': arr[3],
      };
    }).toList();
  }
  
  List<Map<String, dynamic>> _parseCommandHistory(List<dynamic> history) {
    return history.map((entry) {
      final arr = entry as List;
      return {
        'originalText': arr[0],
        'processedCommand': arr[1],
        'confidence': arr[2],
        'timestamp': arr[3],
        'language': arr[4],
        'engineUsed': arr[5],
        'success': arr[6],
        'executionTimeMs': arr[7],
        'usageCount': arr.length > 8 ? arr[8] : 1,
      };
    }).toList();
  }
  
  String _decryptData(String encryptedData) {
    try {
      final key = Key.fromUtf8(_encryptionKey);
      final iv = IV.fromUtf8(_encryptionIV);
      final encrypter = Encrypter(AES(key));
      
      final encrypted = Encrypted.fromBase64(encryptedData);
      final decrypted = encrypter.decrypt(encrypted, iv: iv);
      
      return decrypted;
    } catch (e) {
      // Fallback to base64 decode
      final bytes = base64Decode(encryptedData);
      return utf8.decode(bytes);
    }
  }
  
  Future<void> saveFile(String path) async {
    try {
      _status = 'Saving file...';
      notifyListeners();
      
      // Convert categorized data back to compact format
      final compactData = _convertToCompactFormat();
      
      // Encrypt if needed
      String finalData;
      if (_isEncrypted) {
        final jsonStr = jsonEncode(compactData);
        final encrypted = _encryptData(jsonStr);
        final checksum = _calculateChecksum(jsonStr);
        
        final wrapper = {
          'version': '1.0.0',
          'exportDate': DateTime.now().millisecondsSinceEpoch,
          'deviceId': 'decoder-tool',
          'dataChecksum': checksum,
          'encodedData': encrypted,
        };
        finalData = jsonEncode(wrapper);
      } else {
        finalData = const JsonEncoder.withIndent('  ').convert(compactData);
      }
      
      final file = File(path);
      await file.writeAsString(finalData);
      
      _status = 'File saved successfully';
      notifyListeners();
    } catch (e) {
      _status = 'Error saving: $e';
      notifyListeners();
    }
  }
  
  Map<String, dynamic> _convertToCompactFormat() {
    final compact = <String, dynamic>{};
    
    if (_categorizedData['User Preferences'] != null) {
      compact['p'] = _categorizedData['User Preferences']!.map((pref) {
        return [pref['key'], pref['value'], pref['type'], pref['module']];
      }).toList();
    }
    
    // Add other categories...
    
    return compact;
  }
  
  String _encryptData(String data) {
    final key = Key.fromUtf8(_encryptionKey);
    final iv = IV.fromUtf8(_encryptionIV);
    final encrypter = Encrypter(AES(key));
    
    final encrypted = encrypter.encrypt(data, iv: iv);
    return encrypted.base64;
  }
  
  String _calculateChecksum(String data) {
    final bytes = utf8.encode(data);
    final digest = sha256.convert(bytes);
    return digest.toString();
  }
  
  void updateItem(String category, int index, Map<String, dynamic> newData) {
    if (_categorizedData[category] != null) {
      _categorizedData[category]![index] = newData;
      notifyListeners();
    }
  }
  
  void deleteItem(String category, int index) {
    if (_categorizedData[category] != null) {
      _categorizedData[category]!.removeAt(index);
      notifyListeners();
    }
  }
  
  void addItem(String category, Map<String, dynamic> newItem) {
    _categorizedData[category] ??= [];
    _categorizedData[category]!.add(newItem);
    notifyListeners();
  }
  
  void validateData() {
    if (_fileData != null) {
      _isValid = Validator.validateData(_fileData!);
      _status = _isValid! ? 'Validation passed' : 'Validation failed';
      notifyListeners();
    }
  }
  
  void clearData() {
    _fileData = null;
    _fileName = null;
    _fileSize = null;
    _isEncrypted = false;
    _isValid = null;
    _status = 'Ready';
    _categorizedData.clear();
    notifyListeners();
  }
}