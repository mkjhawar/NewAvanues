#!/usr/bin/env python3
"""Clean up old empty directories after migration."""

import os
import shutil
from pathlib import Path

BASE_DIR = Path("/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/Chat")

# Directories to remove
old_dirs = [
    "src/main/kotlin/com/augmentalis/ava",
    "src/test/kotlin/com/augmentalis/ava",
    "src/androidTest/kotlin/com/augmentalis/ava",
    "src/commonMain/kotlin/com/augmentalis/ava",
    "src/androidMain/kotlin/com/augmentalis/ava",
]

for dir_path in old_dirs:
    full_path = BASE_DIR / dir_path
    if full_path.exists():
        try:
            shutil.rmtree(full_path)
            print(f"Removed: {dir_path}")
        except Exception as e:
            print(f"Failed to remove {dir_path}: {e}")

print("\nCleanup complete!")
