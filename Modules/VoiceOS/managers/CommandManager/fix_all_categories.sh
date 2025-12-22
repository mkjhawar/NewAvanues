#!/bin/bash

# Fix all CommandCategory type issues by using strings everywhere
cd "/Volumes/M Drive/Coding/Warp/VOS4/managers/CommandsMGR/src/main/java/com/ai"

# Fix context manager
sed -i '' 's/CommandCategory\.NAVIGATION/"NAVIGATION"/g' context/ContextManager.kt
sed -i '' 's/CommandCategory\.TEXT/"TEXT"/g' context/ContextManager.kt
sed -i '' 's/CommandCategory\.MEDIA/"MEDIA"/g' context/ContextManager.kt
sed -i '' 's/CommandCategory\.SYSTEM/"SYSTEM"/g' context/ContextManager.kt
sed -i '' 's/CommandCategory\.APP/"APP"/g' context/ContextManager.kt

# Fix registry
sed -i '' 's/CommandCategory\.NAVIGATION/"NAVIGATION"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.TEXT/"TEXT"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.MEDIA/"MEDIA"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.SYSTEM/"SYSTEM"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.APP/"APP"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.CURSOR/"CURSOR"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.SCROLL/"SCROLL"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.VOLUME/"VOLUME"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.DICTATION/"DICTATION"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.VOICE/"VOICE"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.GESTURE/"GESTURE"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.CUSTOM/"CUSTOM"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.ACCESSIBILITY/"ACCESSIBILITY"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.INPUT/"INPUT"/g' registry/CommandRegistry.kt
sed -i '' 's/CommandCategory\.APP_CONTROL/"APP_CONTROL"/g' registry/CommandRegistry.kt

# Fix validator
sed -i '' 's/CommandCategory\.NAVIGATION/"NAVIGATION"/g' validation/CommandValidator.kt
sed -i '' 's/CommandCategory\.TEXT/"TEXT"/g' validation/CommandValidator.kt
sed -i '' 's/CommandCategory\.MEDIA/"MEDIA"/g' validation/CommandValidator.kt
sed -i '' 's/CommandCategory\.SYSTEM/"SYSTEM"/g' validation/CommandValidator.kt
sed -i '' 's/CommandCategory\.APP/"APP"/g' validation/CommandValidator.kt
sed -i '' 's/CommandCategory\.VOICE/"VOICE"/g' validation/CommandValidator.kt

# Fix CommandCategory type declarations
sed -i '' 's/: CommandCategory/: String/g' context/ContextManager.kt
sed -i '' 's/: CommandCategory/: String/g' registry/CommandRegistry.kt
sed -i '' 's/: CommandCategory/: String/g' validation/CommandValidator.kt

# Fix Map types
sed -i '' 's/Map<CommandCategory,/Map<String,/g' context/ContextManager.kt
sed -i '' 's/Map<CommandCategory,/Map<String,/g' registry/CommandRegistry.kt
sed -i '' 's/Map<CommandCategory,/Map<String,/g' validation/CommandValidator.kt

echo "Fixed all CommandCategory references to use strings"