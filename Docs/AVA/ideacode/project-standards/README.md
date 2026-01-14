# Project-Specific Standards

This directory contains project-specific programming standards that extend or override global IDEACODE standards.

## Usage

1. Create standard files: `custom-naming.md`, `team-conventions.md`, etc.
2. Create `REGISTRY.json` to map file extensions/contexts to standards
3. Standards here take precedence over global standards

## Example REGISTRY.json

```json
{
  "version": "1.0",
  "updated": "2025-11-11",
  "extensions": {
    ".kt": ["team-kotlin-style.md"]
  },
  "contexts": {
    "naming": ["custom-naming.md"]
  },
  "always_load": []
}
```

See: `/Volumes/M-Drive/Coding/ideacode/examples/project-standards/` for examples.
