# VoiceOS Code Review Checklist

**Version:** 1.0  
**Date:** 2025-11-13  
**Purpose:** Ensure code quality and consistency before merging

---

## 🎯 **How to Use This Checklist**

1. **Before submitting PR:** Author self-reviews using this checklist
2. **During code review:** Reviewer verifies all items
3. **Before merge:** All items must be ✅ or have documented exceptions

---

## ✅ **Mandatory Checks (Block Merge)**

### **1. Logging Standards** 🔴 **CRITICAL**

- [ ] **No direct `Log.*()` calls** (use ConditionalLogger or PIILoggingWrapper)
- [ ] **Voice commands use `PIILoggingWrapper`**
- [ ] **User text input uses `PIILoggingWrapper`**
- [ ] **Element text/descriptions use `PIILoggingWrapper`**
- [ ] **System state uses `ConditionalLogger`**
- [ ] **Package names use `ConditionalLogger`**
- [ ] **ConditionalLogger uses lambda syntax:** `{ "message" }`
- [ ] **PIILoggingWrapper uses string parameter:** `"message"`
- [ ] **No PII leaks in ConditionalLogger calls**

**Related:** [Logging Guidelines](LOGGING-GUIDELINES.md)

---

### **2. Resource Management** 🔴 **CRITICAL**

- [ ] **All AccessibilityNodeInfo instances recycled**
- [ ] **Use NodeRecyclingUtils for tree traversal**
- [ ] **try-finally blocks for node cleanup**
- [ ] **No resource leaks in exception paths**
- [ ] **Database connections properly closed**
- [ ] **Coroutines properly scoped and cancelled**

**Related:** [NodeRecyclingUtils](../modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/utils/NodeRecyclingUtils.kt)

---

### **3. Null Safety** 🔴 **CRITICAL**

- [ ] **No `!!` operator (use safe calls or requireNotNull with context)**
- [ ] **Database getInstance() checked for null**
- [ ] **Nullable return types handled properly**
- [ ] **lateinit vars initialized before use**
- [ ] **Safe navigation `?.` used appropriately**

---

### **4. Error Handling** 🟡 **HIGH PRIORITY**

- [ ] **All exceptions caught and logged**
- [ ] **Error messages include context and possible causes**
- [ ] **User-facing errors are meaningful**
- [ ] **Critical paths have fallback behavior**
- [ ] **No silent failures (always log errors)**

---

### **5. Testing** 🟡 **HIGH PRIORITY**

- [ ] **Unit tests added for new functionality**
- [ ] **Tests cover success and failure paths**
- [ ] **Edge cases tested (null, empty, boundary conditions)**
- [ ] **Tests are deterministic (no flaky tests)**
- [ ] **Test coverage ≥80% for new code**

---

### **6. Performance** 🟢 **MEDIUM PRIORITY**

- [ ] **No blocking operations on main thread**
- [ ] **Database queries use indices**
- [ ] **Large loops optimized**
- [ ] **Memory allocations minimized in hot paths**
- [ ] **No N+1 query problems**

---

### **7. Security & Privacy** 🔴 **CRITICAL**

- [ ] **No hardcoded credentials or API keys**
- [ ] **User data sanitized in logs (use PIILoggingWrapper)**
- [ ] **SQL injection prevented (use parameterized queries)**
- [ ] **File paths validated (no directory traversal)**
- [ ] **Permissions checked before sensitive operations**

---

### **8. Code Style** 🟢 **MEDIUM PRIORITY**

- [ ] **Follows Kotlin coding conventions**
- [ ] **KDoc comments for public APIs**
- [ ] **Method length <100 lines (ideally <50)**
- [ ] **Class length <500 lines (ideally <300)**
- [ ] **Meaningful variable names (no `a`, `b`, `temp`)**
- [ ] **No commented-out code**

---

### **9. Git Hygiene** 🟡 **HIGH PRIORITY**

- [ ] **Commit messages are descriptive**
- [ ] **No merge commits (rebase instead)**
- [ ] **No debug/test code left in**
- [ ] **No large binary files added**
- [ ] **Sensitive files not committed (.gitignore updated)**
- [ ] **Co-authorship attribution if applicable**

---

### **10. Documentation** 🟢 **MEDIUM PRIORITY**

- [ ] **Public APIs documented with KDoc**
- [ ] **Complex logic explained with comments**
- [ ] **TODO comments have issue numbers: `// TODO(VOS-123)`**
- [ ] **README updated if public API changed**
- [ ] **Architecture docs updated if design changed**

---

## 📋 **Context-Specific Checks**

### **For AccessibilityService Changes**

- [ ] **Event filtering preserves critical events**
- [ ] **Memory pressure handled appropriately**
- [ ] **Node recycling verified**
- [ ] **Permissions documented if added**

### **For Database Changes**

- [ ] **Migration path provided (if schema changed)**
- [ ] **Indices added for new queries**
- [ ] **Foreign keys have appropriate cascading**
- [ ] **Backwards compatibility considered**
- [ ] **Migration tested (upgrade and downgrade)**

### **For UI Changes**

- [ ] **Accessibility attributes set**
- [ ] **Dark mode tested**
- [ ] **Different screen sizes tested**
- [ ] **Orientation changes handled**
- [ ] **No hardcoded strings (use resources)**

### **For Scraping Changes**

- [ ] **Hash-based deduplication used**
- [ ] **Max depth enforced**
- [ ] **UUIDs generated for elements**
- [ ] **Semantic inference applied**
- [ ] **Memory pressure checked**

---

## 🚫 **Anti-Patterns (Reject PR)**

### **Logging Anti-Patterns**

```kotlin
// ❌ REJECT: Direct Log call
Log.d(TAG, "Processing")

// ❌ REJECT: User data in ConditionalLogger
ConditionalLogger.d(TAG) { "Voice command: $userInput" }

// ❌ REJECT: Not using lambda syntax
ConditionalLogger.d(TAG, "message")  // Should be { "message" }
```

### **Resource Management Anti-Patterns**

```kotlin
// ❌ REJECT: No recycling
val child = node.getChild(0)
processNode(child)
// Missing: child.recycle()

// ❌ REJECT: Recycling after exception
try {
    val result = processNode(child)
    child.recycle()  // Won't run if exception thrown!
} catch (e: Exception) { ... }

// ✅ ACCEPT: try-finally recycling
try {
    processNode(child)
} finally {
    child.recycle()  // Always runs
}
```

### **Null Safety Anti-Patterns**

```kotlin
// ❌ REJECT: Force unwrap
val db = VoiceOSAppDatabase.getInstance(context)!!

// ❌ REJECT: Unsafe lateinit
lateinit var database: Database
fun doWork() {
    database.query()  // Might crash if not initialized
}

// ✅ ACCEPT: Safe initialization check
lateinit var database: Database
fun doWork() {
    if (!::database.isInitialized) {
        Log.e(TAG, "Database not initialized")
        return
    }
    database.query()
}
```

---

## 📊 **Review Outcomes**

### **✅ APPROVE**
All mandatory checks pass. Minor issues documented as follow-up tasks.

### **💬 COMMENT**
Non-blocking suggestions for improvement. Author can address now or later.

### **🔄 REQUEST CHANGES**
One or more critical issues found. Must be fixed before merge.

---

## 🎯 **Special Scenarios**

### **Emergency Hotfixes**

For critical production bugs:
- [ ] **Bug severity justifies fast-track** (P0 only)
- [ ] **Fix verified manually**
- [ ] **Tests can be added post-merge** (but must be added)
- [ ] **Full review performed after merge**

### **Refactoring PRs**

For large refactoring changes:
- [ ] **Functional equivalence verified**
- [ ] **No behavior changes (pure refactoring)**
- [ ] **Tests pass without modification**
- [ ] **Performance impact measured**

### **Documentation-Only PRs**

For docs changes:
- [ ] **No code changes**
- [ ] **Spelling and grammar checked**
- [ ] **Links verified**
- [ ] **Examples tested**

---

## 📝 **Reviewer Notes Template**

```markdown
## Review Summary

**Reviewer:** @username
**Date:** 2025-11-13
**Outcome:** ✅ APPROVE / 💬 COMMENT / 🔄 REQUEST CHANGES

### Checks Performed
- [x] Logging standards
- [x] Resource management
- [x] Null safety
- [x] Testing
- [ ] Performance (not applicable)

### Issues Found
1. **[CRITICAL]** Direct Log.d() calls in VoiceCommandProcessor.kt:142
   - **Fix:** Replace with PIILoggingWrapper
   - **Status:** 🔄 Must fix

2. **[MINOR]** Method name could be more descriptive
   - **Suggestion:** rename `process()` to `processVoiceCommand()`
   - **Status:** 💬 Optional

### Recommendations
- Consider extracting helper method for repeated code in lines 100-150
- Add integration test for error scenarios

### Test Results
- ✅ Unit tests: 42/42 passing
- ✅ Integration tests: 8/8 passing
- ✅ Coverage: 87% (target: 80%)

### Performance Impact
- No significant performance impact expected
- Memory usage: +5KB (acceptable)
```

---

## 🔗 **Related Resources**

- [Logging Guidelines](LOGGING-GUIDELINES.md) - Detailed logging standards
- [NodeRecyclingUtils](../modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/utils/NodeRecyclingUtils.kt) - Resource management utilities
- [Code Review Report](CODE-REVIEW-2025-11-13.md) - Historical code review
- [Refactoring Summary](REFACTORING-SUMMARY-2025-11-13.md) - Ongoing refactoring work

---

## 🔄 **Continuous Improvement**

This checklist is a living document. If you find:
- Missing items that should be checked
- Items that are no longer relevant
- Better ways to check things

Please submit a PR to update this checklist!

---

**Version History:**
- **v1.0** (2025-11-13) - Initial checklist (Phase 2 refactoring)

**Status:** ✅ ACTIVE - Use for all code reviews

---

**Maintained by:** VOS4 Development Team  
**Last Updated:** 2025-11-13

**Co-authored-by:** factory-droid[bot] <138933559+factory-droid[bot]@users.noreply.github.com>
