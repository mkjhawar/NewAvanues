#!/bin/bash

# WebAvanue Database Encryption Verification Script
# Verifies that SQLCipher encryption is properly implemented

set -e

echo "======================================"
echo "WebAvanue Encryption Verification"
echo "======================================"
echo ""

PROJECT_ROOT="/Volumes/M-Drive/Coding/NewAvanues-WebAvanue"
MODULE_PATH="$PROJECT_ROOT/Modules/WebAvanue"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check counter
CHECKS_PASSED=0
CHECKS_FAILED=0

check_file() {
    local file=$1
    local description=$2

    if [ -f "$file" ]; then
        echo -e "${GREEN}✅${NC} $description"
        echo "   → $file"
        ((CHECKS_PASSED++))
    else
        echo -e "${RED}❌${NC} $description"
        echo "   → Missing: $file"
        ((CHECKS_FAILED++))
    fi
}

check_content() {
    local file=$1
    local pattern=$2
    local description=$3

    if [ -f "$file" ] && grep -q "$pattern" "$file"; then
        echo -e "${GREEN}✅${NC} $description"
        ((CHECKS_PASSED++))
    else
        echo -e "${RED}❌${NC} $description"
        echo "   → Pattern not found in $file"
        ((CHECKS_FAILED++))
    fi
}

echo "1. Checking Implementation Files..."
echo "-----------------------------------"

check_file "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "EncryptionManager.kt exists"

check_file "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DatabaseDriver.kt" \
    "DatabaseDriver.kt exists"

check_file "$MODULE_PATH/coredata/build.gradle.kts" \
    "build.gradle.kts exists"

echo ""
echo "2. Checking Dependencies..."
echo "-----------------------------------"

check_content "$MODULE_PATH/coredata/build.gradle.kts" \
    "sqlcipher-android:4.5.4" \
    "SQLCipher dependency added"

check_content "$MODULE_PATH/coredata/build.gradle.kts" \
    "androidx.sqlite:sqlite-ktx" \
    "SQLite KTX dependency added"

echo ""
echo "3. Checking Implementation Details..."
echo "-----------------------------------"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "AndroidKeyStore" \
    "Android Keystore integration"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "AES_KEY_SIZE = 256" \
    "AES-256 key size"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "PASSPHRASE_LENGTH = 32" \
    "256-bit passphrase"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DatabaseDriver.kt" \
    "SupportFactory" \
    "SQLCipher SupportFactory integration"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DatabaseDriver.kt" \
    "browser_encrypted.db" \
    "Encrypted database file name"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DatabaseDriver.kt" \
    "migratePlaintextToEncrypted" \
    "Migration function implemented"

echo ""
echo "4. Checking Test Files..."
echo "-----------------------------------"

check_file "$MODULE_PATH/coredata/src/androidUnitTest/kotlin/com/augmentalis/webavanue/security/EncryptionManagerTest.kt" \
    "EncryptionManagerTest.kt exists"

check_file "$MODULE_PATH/coredata/src/androidUnitTest/kotlin/com/augmentalis/webavanue/security/EncryptedDatabaseTest.kt" \
    "EncryptedDatabaseTest.kt exists"

echo ""
echo "5. Checking Documentation..."
echo "-----------------------------------"

check_file "$MODULE_PATH/ENCRYPTION.md" \
    "ENCRYPTION.md documentation"

check_file "$MODULE_PATH/SECURITY_IMPLEMENTATION.md" \
    "SECURITY_IMPLEMENTATION.md"

check_content "$MODULE_PATH/ENCRYPTION.md" \
    "CWE-311" \
    "CWE-311 vulnerability documented"

check_content "$MODULE_PATH/ENCRYPTION.md" \
    "AES-256" \
    "Encryption algorithm documented"

check_content "$MODULE_PATH/ENCRYPTION.md" \
    "Key Rotation" \
    "Key rotation documented"

echo ""
echo "6. Checking Security Features..."
echo "-----------------------------------"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "SecureRandom" \
    "Secure random number generation"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "GCM" \
    "GCM authenticated encryption"

check_content "$MODULE_PATH/coredata/src/androidMain/kotlin/com/augmentalis/webavanue/security/EncryptionManager.kt" \
    "rotateEncryptionKey" \
    "Key rotation support"

echo ""
echo "======================================"
echo "Verification Summary"
echo "======================================"
echo -e "${GREEN}Checks Passed: $CHECKS_PASSED${NC}"
echo -e "${RED}Checks Failed: $CHECKS_FAILED${NC}"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ ALL CHECKS PASSED!${NC}"
    echo ""
    echo "Database encryption is properly implemented."
    echo ""
    echo "Next Steps:"
    echo "1. Run unit tests: ./gradlew :Modules:WebAvanue:coredata:testDebugUnitTest"
    echo "2. Build app: ./gradlew :android:apps:webavanue:app:assembleDebug"
    echo "3. Install on device: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "4. Test manually on device"
    echo "5. Verify encrypted database: adb pull /data/data/com.augmentalis.Avanues.web/databases/browser_encrypted.db"
    exit 0
else
    echo -e "${RED}❌ VERIFICATION FAILED${NC}"
    echo ""
    echo "Please review the failed checks above and ensure all files are properly created."
    exit 1
fi
