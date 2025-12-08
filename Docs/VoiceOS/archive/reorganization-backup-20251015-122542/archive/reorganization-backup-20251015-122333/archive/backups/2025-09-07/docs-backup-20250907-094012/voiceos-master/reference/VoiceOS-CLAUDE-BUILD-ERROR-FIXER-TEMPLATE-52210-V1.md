# Claude Build Error Fixer (BEF) Instructions Template

## 🚀 Quick Shortcuts

### Standard Fix Command
```
BEF: [PASTE BUILD OUTPUT]
```

### Module-Specific Fix Command  
```
BEF [MODULE-NAME]: [PASTE BUILD OUTPUT]
```

### Extended Command (Original)
```
Using the same instructions as before, see the following warning and errors on compile we need to address: 

[PASTE BUILD OUTPUT HERE]

start with the errors first then the warnings, remember to do a cot+rot afterwards
```

## Command Shortcuts Explained

- **BEF** = Build Error Fixer
- Automatically follows the complete process:
  - Errors first, then warnings
  - COT+ROT analysis
  - Documentation updates
  - Proper file naming with module details
  - Stage, commit, and push

## Detailed Process (for reference)

### 1. Analysis Phase
- Create comprehensive todo list breaking down all errors and warnings
- Prioritize: **Errors first** (critical path), then warnings
- Use specialized agents in parallel when beneficial

### 2. Implementation Pattern
- **Errors**: Fix with functional changes (replace missing fields, fix references)
- **Deprecated APIs**: Add version-aware checks or @Suppress("DEPRECATION")
- **Unused Parameters**: 
  - If intentional stubs: Add @Suppress("UNUSED_PARAMETER")
  - If genuinely unused: Remove or replace with underscore
- **Unused Variables**: Add @Suppress("UNUSED_VARIABLE") if intentional

### 3. Documentation Requirements
**MANDATORY**: Update documentation for every change
- Module changelog (version bump)
- Status report for the session
- Architecture docs if structural changes

### 4. Quality Assurance
- **COT+ROT Analysis**: Chain of Thought + Reflection on Thought
  - Evaluate decisions made
  - Consider alternatives
  - Assess quality of implementation
- **Build Verification**: Confirm warnings/errors are resolved

### 5. Version-Aware Pattern for Deprecated APIs
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.API_LEVEL) {
    // Use modern API
} else {
    @Suppress("DEPRECATION")
    // Use legacy API for backward compatibility
}
```

### 6. Suppression Patterns
```kotlin
// For parameters
fun method(@Suppress("UNUSED_PARAMETER") param: Type): ReturnType

// For variables  
@Suppress("UNUSED_VARIABLE")
val variable = value

// For deprecated APIs
@Suppress("DEPRECATION")
val result = deprecatedMethod()
```

### 7. Architecture Preservation Rules
- **Never remove stub parameters** that are placeholders for future features
- **Document architectural intentions** with comments and suppressions
- **Maintain 100% functional equivalency** unless explicitly requested otherwise
- **Preserve backward compatibility** to minimum supported Android API level

### 8. Commit Strategy
- Update all documentation FIRST
- Group related changes logically
- Use clear, descriptive commit messages
- No AI tool references in commit messages

## Expected Deliverables

### Files That Will Be Modified
- Source files with the actual fixes
- Module changelog with version bump  
- Status report documenting the session

### Documentation Naming Conventions
**Status Reports**: Follow existing VOS4 patterns with module details
- **Pattern**: `BUILD-ERROR-FIX-[MODULE-DETAILS]-[YYYY-MM-DD].md`
- **Single Module**: `BUILD-ERROR-FIX-CommandManager-2025-09-06.md`
- **Multiple Modules**: `BUILD-ERROR-FIX-CommandManager-LocalizationManager-2025-09-06.md`
- **Project-Wide**: `BUILD-ERROR-FIX-AllModules-2025-09-06.md`
- **Round/Session**: `BUILD-ERROR-FIX-CommandManager-Round3-2025-09-06.md`

**Additional Reports**: Follow VOS4 file naming standards
- `[MODULE-NAME]-Warning-Analysis-[YYYY-MM-DD].md`
- `[MODULE-NAME]-Deprecation-Fix-Report-[YYYY-MM-DD].md`
- `[MODULE-NAME]-Build-Status-[YYYY-MM-DD].md`

### Reports To Generate
1. **Session Status Report**: Use naming convention above based on affected modules
2. **COT+ROT Analysis**: Embedded in response
3. **Updated Module Changelog**: Version bump with detailed changes

## Success Criteria
- ✅ All compilation errors resolved
- ✅ All warnings either fixed or intentionally suppressed
- ✅ Clean build with BUILD SUCCESSFUL
- ✅ Documentation updated
- ✅ Functional equivalency maintained
- ✅ Architecture preserved for future enhancements

## Example Usage

1. **Run build** and copy warning/error output
2. **Paste into prompt** with template command above
3. **Claude will:**
   - Create todo list
   - Fix errors first
   - Address warnings systematically  
   - Update documentation
   - Perform COT+ROT analysis
   - Stage, commit, and push changes
   - Provide comprehensive status report

## Template Variations

### For Specific Modules
```
Fix build warnings in [MODULE_NAME] module. Same process as before: errors first, then warnings, include COT+ROT analysis.

[PASTE BUILD OUTPUT]
```

### For Quick Fixes
```
Quick warning cleanup needed:

[PASTE BUILD OUTPUT]

Standard process: prioritize errors, suppress intentional unused parameters, update docs, COT+ROT.
```

This template ensures consistent, high-quality build warning resolution with proper documentation and architectural preservation.