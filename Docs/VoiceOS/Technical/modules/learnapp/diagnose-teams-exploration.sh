#!/bin/bash

# Teams App Exploration Diagnostics
# Created: 2025-10-30 01:10 PDT
# Purpose: Capture real data from Teams exploration for analysis

echo "=========================================="
echo "Teams App Exploration Diagnostics"
echo "=========================================="
echo ""

# Check if device connected
echo "1. Checking ADB connection..."
adb devices
echo ""

# Get database file location
DB_PATH="/data/data/com.augmentalis.voiceos/databases"

echo "2. Querying Teams elements from database..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/learnapp_database.db <<'EOF'
.headers on
.mode column

-- Count Teams elements
SELECT 'Total Teams Elements:' as Query, COUNT(*) as Count
FROM learned_apps
WHERE package_name = 'com.microsoft.teams';

-- Show Teams app info
SELECT package_name, exploration_status, total_screens, total_elements,
       datetime(last_updated_at/1000, 'unixepoch') as last_updated
FROM learned_apps
WHERE package_name = 'com.microsoft.teams';

EOF
"
echo ""

echo "3. Querying Teams screen states..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/learnapp_database.db <<'EOF'
.headers on
.mode column

-- Count screen states
SELECT 'Total Screen States:' as Query, COUNT(*) as Count
FROM screen_states
WHERE package_name = 'com.microsoft.teams';

-- Show screen hashes
SELECT substr(screen_hash, 1, 16) as screen_hash_short,
       package_name,
       element_count,
       datetime(discovered_at/1000, 'unixepoch') as discovered
FROM screen_states
WHERE package_name = 'com.microsoft.teams'
ORDER BY discovered_at;

EOF
"
echo ""

echo "4. Querying UUID Creator database for Teams elements..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/uuid_creator_database.db <<'EOF'
.headers on
.mode column

-- Count Teams UUIDs
SELECT 'Total Teams UUIDs:' as Query, COUNT(*) as Count
FROM uuid_elements
WHERE uuid LIKE 'com.microsoft.teams%';

-- Show first 20 Teams elements
SELECT substr(uuid, 1, 50) as uuid_short,
       name,
       type
FROM uuid_elements
WHERE uuid LIKE 'com.microsoft.teams%'
ORDER BY created_at
LIMIT 20;

EOF
"
echo ""

echo "5. Querying Teams aliases..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/uuid_creator_database.db <<'EOF'
.headers on
.mode column

-- Count Teams aliases
SELECT 'Total Teams Aliases:' as Query, COUNT(*) as Count
FROM uuid_aliases
WHERE uuid LIKE 'com.microsoft.teams%';

-- Show Teams aliases
SELECT alias,
       substr(uuid, 1, 50) as uuid_short
FROM uuid_aliases
WHERE uuid LIKE 'com.microsoft.teams%'
ORDER BY created_at
LIMIT 20;

EOF
"
echo ""

echo "6. Checking for exploration sessions..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/learnapp_database.db <<'EOF'
.headers on
.mode column

SELECT session_id,
       status,
       datetime(started_at/1000, 'unixepoch') as started,
       datetime(completed_at/1000, 'unixepoch') as completed
FROM exploration_sessions
WHERE package_name = 'com.microsoft.teams'
ORDER BY started_at DESC
LIMIT 5;

EOF
"
echo ""

echo "7. Checking navigation edges..."
echo "-------------------------------------------"
adb shell "run-as com.augmentalis.voiceos sqlite3 ${DB_PATH}/learnapp_database.db <<'EOF'
.headers on
.mode column

SELECT COUNT(*) as edge_count
FROM navigation_edges
WHERE package_name = 'com.microsoft.teams';

EOF
"
echo ""

echo "=========================================="
echo "Diagnostics Complete"
echo "=========================================="
echo ""
echo "Next Steps:"
echo "1. Review the output above"
echo "2. If Teams has NOT been explored yet, run: adb shell am start -n com.microsoft.teams/[main_activity]"
echo "3. Let LearnApp explore it"
echo "4. Run this script again to see the actual data"
echo ""
echo "To capture live logs during exploration:"
echo "  adb logcat -s ExplorationEngine:D ExplorationEngine-Visual:D LearnAppIntegration:D | tee teams_exploration_logs.txt"
echo ""
