#!/bin/bash
# Fix common type conversion patterns across all Flutter mappers

for file in Flutter*.kt; do
  echo "Fixing $file..."
  
  # Fix Spacing -> Float conversions (extract the value)
  sed -i '' 's/component\.padding/component.padding?.let { extractSpacing(it) }/g' "$file"
  
  # Fix Color -> SwiftUIColor conversions  
  sed -i '' 's/component\.color?.let { SwiftUIColor\.system(\([^)]*\))/component.color?.let { color -> SwiftUIColor.rgb(color.red\/255f, color.green\/255f, color.blue\/255f, color.alpha\/255f)/g' "$file"
  
  # Fix duration divisions
  sed -i '' 's/component\.duration\.toDouble() \/ 1000\.0/component.duration.milliseconds \/ 1000.0/g' "$file"
  
  # Fix decoration.borderRadius access
  sed -i '' 's/component\.borderRadius/component.decoration?.borderRadius?.topLeft/g' "$file"
done

echo "Done!"
