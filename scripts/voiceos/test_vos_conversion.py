#!/usr/bin/env python3
"""
Unit tests for VOS conversion tools

Tests all three conversion scripts:
- convert_vos_to_compact.py
- translate_vos_commands.py
- generate_all_compact_json.py
"""

import unittest
import json
import os
import tempfile
import shutil
from pathlib import Path
from unittest.mock import patch, mock_open

# Import modules to test
import sys
sys.path.insert(0, os.path.dirname(__file__))

# We'll test the functions directly
from convert_vos_to_compact import convert_vos_command_to_compact, convert_vos_file_to_compact
from translate_vos_commands import translate_phrase, translate_command, TRANSLATIONS
from generate_all_compact_json import convert_vos_command_to_compact as gen_convert_command


class TestVOSConversion(unittest.TestCase):
    """Test VOS to compact JSON conversion"""

    def setUp(self):
        """Set up test fixtures"""
        self.sample_vos_command = {
            "action": "NAVIGATE_HOME",
            "cmd": "navigate home",
            "syn": ["go home", "return home", "home screen"]
        }

        self.sample_vos_file = {
            "schema": "vos-1.0",
            "version": "1.0.0",
            "file_info": {
                "filename": "test-commands.vos",
                "category": "test",
                "display_name": "Test Commands",
                "description": "Test command file",
                "command_count": 1
            },
            "locale": "en-US",
            "commands": [self.sample_vos_command]
        }

    def test_convert_single_command(self):
        """Test converting a single VOS command to compact format"""
        result = convert_vos_command_to_compact(self.sample_vos_command, "Navigation")

        # Verify array structure
        self.assertIsInstance(result, list)
        self.assertEqual(len(result), 4)

        # Verify array elements
        self.assertEqual(result[0], "navigate_home")  # action (lowercase)
        self.assertEqual(result[1], "navigate home")  # primary command
        self.assertEqual(result[2], ["go home", "return home", "home screen"])  # synonyms
        self.assertEqual(result[3], "Navigate Home (Navigation)")  # description

    def test_convert_command_action_lowercase(self):
        """Test that action IDs are converted to lowercase"""
        command = {
            "action": "TURN_ON_BLUETOOTH",
            "cmd": "turn on bluetooth",
            "syn": ["bluetooth on"]
        }

        result = convert_vos_command_to_compact(command, "Connectivity")
        self.assertEqual(result[0], "turn_on_bluetooth")

    def test_convert_command_description_format(self):
        """Test description format: '{Action Words} ({Category})'"""
        command = {
            "action": "SCROLL_UP",
            "cmd": "scroll up",
            "syn": ["swipe up"]
        }

        result = convert_vos_command_to_compact(command, "Scrolling")
        self.assertEqual(result[3], "Scroll Up (Scrolling)")

    def test_convert_vos_file(self):
        """Test converting entire VOS file"""
        # Create temporary VOS file
        with tempfile.NamedTemporaryFile(mode='w', suffix='.vos', delete=False) as f:
            json.dump(self.sample_vos_file, f)
            temp_file = f.name

        try:
            result = convert_vos_file_to_compact(Path(temp_file))

            # Verify result
            self.assertIsInstance(result, list)
            self.assertEqual(len(result), 1)
            self.assertEqual(result[0][0], "navigate_home")
        finally:
            os.unlink(temp_file)

    def test_convert_empty_synonyms(self):
        """Test command with empty synonym list"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "test command",
            "syn": []
        }

        result = convert_vos_command_to_compact(command, "Test")
        self.assertEqual(result[2], [])

    def test_convert_many_synonyms(self):
        """Test command with many synonyms"""
        synonyms = [f"synonym{i}" for i in range(20)]
        command = {
            "action": "TEST_ACTION",
            "cmd": "test command",
            "syn": synonyms
        }

        result = convert_vos_command_to_compact(command, "Test")
        self.assertEqual(result[2], synonyms)
        self.assertEqual(len(result[2]), 20)


class TestTranslation(unittest.TestCase):
    """Test VOS command translation"""

    def test_translate_simple_phrase_german(self):
        """Test translating simple phrase to German"""
        result = translate_phrase("turn on", "de")
        self.assertEqual(result, "einschalten")

    def test_translate_simple_phrase_spanish(self):
        """Test translating simple phrase to Spanish"""
        result = translate_phrase("open", "es")
        self.assertEqual(result, "abrir")

    def test_translate_simple_phrase_french(self):
        """Test translating simple phrase to French"""
        result = translate_phrase("close", "fr")
        self.assertEqual(result, "fermer")

    def test_translate_unknown_phrase(self):
        """Test translating unknown phrase (should return original)"""
        result = translate_phrase("unknown phrase", "de")
        self.assertEqual(result, "unknown phrase")

    def test_translate_compound_phrase(self):
        """Test translating compound phrase (word-by-word)"""
        result = translate_phrase("open menu", "de")
        # Should translate "open" → "öffnen" and "menu" → "Menü"
        self.assertIn("öffnen", result)
        self.assertIn("menü", result.lower())

    def test_translate_case_insensitive(self):
        """Test translation is case-insensitive"""
        result1 = translate_phrase("Open", "es")
        result2 = translate_phrase("open", "es")
        self.assertEqual(result1, result2)

    def test_translate_command_german(self):
        """Test translating entire command to German"""
        command = {
            "action": "NAVIGATE_HOME",
            "cmd": "navigate home",
            "syn": ["go home", "open home"]
        }

        result = translate_command(command, "de")

        # Verify structure
        self.assertEqual(result["action"], "NAVIGATE_HOME")  # Action unchanged
        self.assertIn("navigieren", result["cmd"].lower())  # Command translated
        self.assertIsInstance(result["syn"], list)  # Synonyms is list

    def test_translate_command_preserves_action(self):
        """Test that action ID is preserved during translation"""
        command = {
            "action": "TURN_ON_BLUETOOTH",
            "cmd": "turn on bluetooth",
            "syn": ["bluetooth on"]
        }

        result = translate_command(command, "es")
        self.assertEqual(result["action"], "TURN_ON_BLUETOOTH")

    def test_translation_dictionaries_exist(self):
        """Test that translation dictionaries exist for all languages"""
        self.assertIn("de", TRANSLATIONS)
        self.assertIn("es", TRANSLATIONS)
        self.assertIn("fr", TRANSLATIONS)

    def test_translation_dictionary_structure(self):
        """Test translation dictionary has required keys"""
        for lang in ["de", "es", "fr"]:
            self.assertIn("locale_code", TRANSLATIONS[lang])
            self.assertIn("locale_name", TRANSLATIONS[lang])
            self.assertIn("turn on", TRANSLATIONS[lang])
            self.assertIn("bluetooth", TRANSLATIONS[lang])


class TestCompactJSONGeneration(unittest.TestCase):
    """Test compact JSON generation"""

    def test_generate_command_format(self):
        """Test generated command has correct format"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "test command",
            "syn": ["synonym1", "synonym2"]
        }

        result = gen_convert_command(command, "Test Category")

        # Verify format: [action, cmd, [synonyms], description]
        self.assertEqual(len(result), 4)
        self.assertIsInstance(result[0], str)  # action
        self.assertIsInstance(result[1], str)  # cmd
        self.assertIsInstance(result[2], list)  # synonyms
        self.assertIsInstance(result[3], str)  # description

    def test_generate_preserves_synonyms(self):
        """Test that all synonyms are preserved"""
        synonyms = [f"syn{i}" for i in range(15)]
        command = {
            "action": "TEST_ACTION",
            "cmd": "test",
            "syn": synonyms
        }

        result = gen_convert_command(command, "Test")
        self.assertEqual(result[2], synonyms)


class TestIntegration(unittest.TestCase):
    """Integration tests for complete workflow"""

    def setUp(self):
        """Set up temporary directory for integration tests"""
        self.temp_dir = tempfile.mkdtemp()

    def tearDown(self):
        """Clean up temporary directory"""
        shutil.rmtree(self.temp_dir)

    def test_end_to_end_conversion(self):
        """Test complete VOS to compact JSON conversion"""
        # Create sample VOS file
        vos_data = {
            "schema": "vos-1.0",
            "version": "1.0.0",
            "file_info": {
                "filename": "test-commands.vos",
                "category": "test",
                "display_name": "Test",
                "command_count": 2
            },
            "locale": "en-US",
            "commands": [
                {
                    "action": "ACTION_1",
                    "cmd": "command one",
                    "syn": ["cmd1", "first"]
                },
                {
                    "action": "ACTION_2",
                    "cmd": "command two",
                    "syn": ["cmd2", "second"]
                }
            ]
        }

        vos_path = Path(self.temp_dir) / "test.vos"
        with open(vos_path, 'w') as f:
            json.dump(vos_data, f)

        # Convert
        compact_commands = convert_vos_file_to_compact(vos_path)

        # Verify
        self.assertEqual(len(compact_commands), 2)
        self.assertEqual(compact_commands[0][0], "action_1")
        self.assertEqual(compact_commands[1][0], "action_2")

    def test_translation_preserves_structure(self):
        """Test that translation preserves command structure"""
        command = {
            "action": "NAVIGATE_HOME",
            "cmd": "navigate home",
            "syn": ["go home", "return home"]
        }

        # Translate to all languages
        for lang in ["de", "es", "fr"]:
            translated = translate_command(command, lang)

            # Verify structure preserved
            self.assertIn("action", translated)
            self.assertIn("cmd", translated)
            self.assertIn("syn", translated)
            self.assertEqual(len(translated["syn"]), 2)

    def test_compact_json_validity(self):
        """Test generated compact JSON is valid"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "test command",
            "syn": ["synonym1"]
        }

        compact = convert_vos_command_to_compact(command, "Test")

        # Should be JSON-serializable
        json_str = json.dumps(compact)
        parsed = json.loads(json_str)

        self.assertEqual(compact, parsed)


class TestEdgeCases(unittest.TestCase):
    """Test edge cases and error handling"""

    def test_special_characters_in_command(self):
        """Test commands with special characters"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "test's command",
            "syn": ["it's working", "don't stop"]
        }

        result = convert_vos_command_to_compact(command, "Test")
        self.assertEqual(result[1], "test's command")
        self.assertIn("it's working", result[2])

    def test_unicode_in_translation(self):
        """Test Unicode characters in translations"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "test",
            "syn": ["test"]
        }

        # German may have umlauts
        result = translate_command(command, "de")
        # Should not raise encoding errors
        json.dumps(result, ensure_ascii=False)

    def test_very_long_synonym_list(self):
        """Test command with very long synonym list"""
        synonyms = [f"synonym{i}" for i in range(100)]
        command = {
            "action": "TEST_ACTION",
            "cmd": "test",
            "syn": synonyms
        }

        result = convert_vos_command_to_compact(command, "Test")
        self.assertEqual(len(result[2]), 100)

    def test_empty_command_text(self):
        """Test handling of empty command text"""
        command = {
            "action": "TEST_ACTION",
            "cmd": "",
            "syn": ["synonym"]
        }

        result = convert_vos_command_to_compact(command, "Test")
        self.assertEqual(result[1], "")

    def test_action_with_numbers(self):
        """Test action with numbers (e.g., SET_VOLUME_5)"""
        command = {
            "action": "SET_VOLUME_5",
            "cmd": "volume five",
            "syn": ["set volume five"]
        }

        result = convert_vos_command_to_compact(command, "Volume")
        self.assertEqual(result[0], "set_volume_5")


class TestCommandValidation(unittest.TestCase):
    """Test validation of command structure"""

    def test_valid_compact_format(self):
        """Test validation of valid compact format"""
        compact_cmd = ["action", "cmd", ["syn1", "syn2"], "description"]

        # Should have exactly 4 elements
        self.assertEqual(len(compact_cmd), 4)

        # Element types
        self.assertIsInstance(compact_cmd[0], str)
        self.assertIsInstance(compact_cmd[1], str)
        self.assertIsInstance(compact_cmd[2], list)
        self.assertIsInstance(compact_cmd[3], str)

    def test_compact_json_structure(self):
        """Test complete compact JSON structure"""
        compact_json = {
            "version": "1.0",
            "locale": "en-US",
            "fallback": "en-US",
            "updated": "2025-11-13",
            "author": "VOS4 Team",
            "commands": [
                ["action1", "cmd1", ["syn1"], "desc1"],
                ["action2", "cmd2", ["syn2"], "desc2"]
            ]
        }

        # Verify required fields
        self.assertIn("version", compact_json)
        self.assertIn("locale", compact_json)
        self.assertIn("fallback", compact_json)
        self.assertIn("commands", compact_json)

        # Verify commands is array
        self.assertIsInstance(compact_json["commands"], list)


def run_tests():
    """Run all tests"""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # Add all test cases
    suite.addTests(loader.loadTestsFromTestCase(TestVOSConversion))
    suite.addTests(loader.loadTestsFromTestCase(TestTranslation))
    suite.addTests(loader.loadTestsFromTestCase(TestCompactJSONGeneration))
    suite.addTests(loader.loadTestsFromTestCase(TestIntegration))
    suite.addTests(loader.loadTestsFromTestCase(TestEdgeCases))
    suite.addTests(loader.loadTestsFromTestCase(TestCommandValidation))

    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    # Print summary
    print("\n" + "="*70)
    print("TEST SUMMARY")
    print("="*70)
    print(f"Tests run: {result.testsRun}")
    print(f"Successes: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print("="*70)

    return result.wasSuccessful()


if __name__ == "__main__":
    import sys
    success = run_tests()
    sys.exit(0 if success else 1)
