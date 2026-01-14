#!/usr/bin/env python3
"""
Rename documentation files to IDEACODE naming convention.

Convention: App-Module-Description-YDDMMHH-V#.md
- App: VoiceOS, AVA, Avanues, etc.
- YDDMMHH: Y=last digit of year, DD=day, MM=month, HH=hour
- V#: Version number

Standard files to skip: README.md, CHANGELOG.md, LICENSE.md, TODO.md, BACKLOG.md
"""

import os
import re
from pathlib import Path
from datetime import datetime
import sys

# Files to skip renaming
SKIP_FILES = {
    'README.md', 'CHANGELOG.md', 'LICENSE.md', 'TODO.md', 'BACKLOG.md',
    'CLAUDE.md', 'FOLDER-REGISTRY.md', 'INDEX.md', '.DS_Store'
}

# App prefixes based on folder
APP_MAP = {
    'VoiceOS': 'VoiceOS',
    'AVA': 'AVA',
    'Avanues': 'Avanues',
    'AvaConnect': 'AvaConnect',
    'WebAvanue': 'WebAvanue',
    'Common': 'Common',
    'Project': 'Project',
}

def extract_date_from_name(filename):
    """Extract date from various formats in filename."""
    # Pattern: YYYYMMDD (e.g., 20251127)
    match = re.search(r'(\d{4})(\d{2})(\d{2})', filename)
    if match:
        year, month, day = match.groups()
        return f"{year[-1]}{day}{month}"  # Convert to YDDMM format

    # Pattern: YYMMDD (e.g., 251127)
    match = re.search(r'(\d{2})(\d{2})(\d{2})', filename)
    if match:
        yy, mm_or_dd, dd_or_mm = match.groups()
        # Assume YYMMDD format (year-month-day)
        if int(mm_or_dd) <= 12:  # Likely month
            return f"{yy[-1]}{dd_or_mm}{mm_or_dd}"  # YDDMM
        else:  # Likely YYDDMM (year-day-month)
            return f"{yy[-1]}{mm_or_dd}{dd_or_mm}"

    return None

def extract_version(filename):
    """Extract version number from filename."""
    match = re.search(r'-V(\d+)\.md$', filename, re.IGNORECASE)
    if match:
        return int(match.group(1))
    return 1  # Default to V1

def clean_description(filename, app_prefix):
    """Clean filename to get description part."""
    # Remove extension
    name = filename.replace('.md', '')

    # Remove app prefix if present
    for prefix in [app_prefix, app_prefix.upper()]:
        if name.startswith(prefix + '-'):
            name = name[len(prefix) + 1:]
        elif name.startswith(prefix):
            name = name[len(prefix):]

    # Remove date patterns
    name = re.sub(r'-?\d{6,8}(-\d{4,6})?', '', name)

    # Remove version
    name = re.sub(r'-?V\d+$', '', name, flags=re.IGNORECASE)

    # Remove time patterns like -0013, -0141
    name = re.sub(r'-\d{4}$', '', name)

    # Clean up multiple dashes
    name = re.sub(r'-+', '-', name)
    name = name.strip('-')

    return name if name else 'Document'

def already_follows_convention(filename, app):
    """Check if file already follows the convention."""
    # Pattern: App-...-YDDMM(HH)?-V#.md
    pattern = rf'^{app}-.*-\d{{5,7}}-V\d+\.md$'
    return bool(re.match(pattern, filename, re.IGNORECASE))

def get_new_filename(filepath, app):
    """Generate new filename following convention."""
    filename = os.path.basename(filepath)

    # Skip standard files
    if filename in SKIP_FILES:
        return None

    # Skip if already follows convention
    if already_follows_convention(filename, app):
        return None

    # Skip Living Docs (LD-*.md) - they have their own convention
    if filename.startswith('LD-'):
        return None

    # Extract components
    date_str = extract_date_from_name(filename)
    version = extract_version(filename)
    description = clean_description(filename, app)

    # Use file modification time if no date found
    if not date_str:
        mtime = os.path.getmtime(filepath)
        dt = datetime.fromtimestamp(mtime)
        date_str = f"{dt.year % 10}{dt.day:02d}{dt.month:02d}"

    # Build new filename
    new_name = f"{app}-{description}-{date_str}-V{version}.md"

    # Clean up
    new_name = re.sub(r'-+', '-', new_name)

    return new_name

def preview_renames(base_dir, dry_run=True):
    """Preview or execute renames."""
    docs_dir = Path(base_dir) / 'Docs'

    renames = []

    for app_folder in docs_dir.iterdir():
        if not app_folder.is_dir():
            continue

        app = app_folder.name
        if app not in APP_MAP:
            continue

        app_prefix = APP_MAP[app]

        for md_file in app_folder.rglob('*.md'):
            new_name = get_new_filename(str(md_file), app_prefix)

            if new_name and new_name != md_file.name:
                new_path = md_file.parent / new_name
                renames.append((md_file, new_path))

    print(f"Found {len(renames)} files to rename")

    if dry_run:
        for old, new in renames[:50]:  # Show first 50
            print(f"  {old.name}")
            print(f"    -> {new.name}")
            print()
    else:
        for old, new in renames:
            try:
                old.rename(new)
                print(f"Renamed: {old.name} -> {new.name}")
            except Exception as e:
                print(f"Error renaming {old}: {e}")

    return renames

if __name__ == '__main__':
    base_dir = '/Volumes/M-Drive/Coding/NewAvanues'

    dry_run = '--execute' not in sys.argv

    if dry_run:
        print("DRY RUN - Preview mode (use --execute to rename)")
    else:
        print("EXECUTING renames...")

    preview_renames(base_dir, dry_run)
