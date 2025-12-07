#!/bin/bash

# Fix all CommandDefinition issues in definitions file
cd "/Volumes/M Drive/Coding/Warp/VOS4/managers/CommandsMGR/src/main/java/com/ai/definitions"

# Replace all occurrences of CommandCategory enum with string
sed -i '' 's/CommandCategory\.NAVIGATION/"NAVIGATION"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.CURSOR/"CURSOR"/g' CommandDefinitions.kt  
sed -i '' 's/CommandCategory\.SCROLL/"SCROLL"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.VOLUME/"VOLUME"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.DICTATION/"DICTATION"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.SYSTEM/"SYSTEM"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.APP/"APP"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.TEXT/"TEXT"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.VOICE/"VOICE"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.MEDIA/"MEDIA"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.GESTURE/"GESTURE"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.CUSTOM/"CUSTOM"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.ACCESSIBILITY/"ACCESSIBILITY"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.INPUT/"INPUT"/g' CommandDefinitions.kt
sed -i '' 's/CommandCategory\.APP_CONTROL/"APP_CONTROL"/g' CommandDefinitions.kt

echo "Fixed CommandCategory references"