# 🚀 Build Error Fixer (BEF) Shortcuts

## Quick Commands

### Standard Fix
```
BEF: [PASTE BUILD OUTPUT]
```

### Module-Specific Fix
```
BEF CommandManager: [PASTE BUILD OUTPUT]
```

### Multiple Modules
```
BEF CommandManager+LocalizationManager: [PASTE BUILD OUTPUT]
```

## What BEF Does Automatically

🔴 **Fixes errors FIRST** (compilation failures, unresolved references)  
🟡 **Then handles warnings** (deprecated APIs, unused parameters)  
✅ **Applies appropriate fixes**: @Suppress for intentional stubs, version checks for deprecated APIs  
✅ **Updates documentation**: Module changelog + status report with proper naming  
✅ **Performs COT+ROT analysis**: Quality assessment and reflection  
✅ **Commits and pushes**: Clean commit messages without AI references  
✅ **Preserves architecture**: Maintains stub parameters for future integrations  

## File Outputs

**Documentation Created:**
- `BUILD-ERROR-FIX-[MODULE]-[DATE].md` (status report)
- Updated module changelog with version bump
- COT+ROT analysis in response

**Code Changes:**
- Fixed compilation errors (PRIORITY #1)
- Fixed build failures
- Suppressed intentional warnings
- Version-aware deprecated API handling

## Full Template Location
📁 `/docs/CLAUDE-BUILD-ERROR-FIXER-TEMPLATE.md`

---
**Last Updated:** 2025-09-06  
**Usage:** Just type `BEF:` followed by your build output!  
**Remember:** BEF handles both **ERRORS** and **WARNINGS** - errors get priority!