# Protocol: No Speculation v1.0

**Purpose:** Prevent hallucinations by requiring fact-based responses
**Status:** MANDATORY - Zero Tolerance
**Priority:** CRITICAL
**Version:** 1.0
**Date:** 2025-11-14

---

## Overview

This protocol eliminates hallucinations by requiring AI to read actual code before answering questions about it. No speculation, no assumptions, no guessing.

**Golden Rule:** Read before answering. Always.

---

## 🚫 The Problem: Hallucination Cost

**Hallucination:** AI inventing facts about code it hasn't read

**Real-World Impact:**
- Developer implements wrong solution based on AI's speculation
- Hours wasted on incorrect approach
- Technical debt accumulated
- User trust destroyed

**Example Cost:**
```
AI speculates: "The authenticate() function uses JWT" (WRONG - it uses OAuth2)
Developer builds: JWT token refresh logic (unnecessary, 4 hours)
Reality check: Code already uses OAuth2 token refresh
Result: 4 hours wasted + need to undo changes
```

**Prevention Cost:**
```
AI reads first: 2 seconds to read AuthService.kt
AI answers correctly: "Uses OAuth2, not JWT"
Developer implements: Correct approach from start
Result: 0 hours wasted
```

**ROI: 2 seconds prevents 4 hours of waste**

---

## ✅ The Solution: Read-First Protocol

<hallucination_prevention protocol="mandatory">

### Rule 1: Read Before Answering (ABSOLUTE)

**BEFORE answering ANY question about code:**
1. ✅ Use `Read` tool on relevant file(s)
2. ✅ Verify assumptions against actual code
3. ✅ Base answer on facts, not memory

**NEVER:**
- ❌ Speculate about code you haven't read
- ❌ Assume file contents from memory
- ❌ Answer based on "typical" implementations
- ❌ Guess function signatures or behavior

### Rule 2: Explicit Uncertainty (REQUIRED)

**When you don't know:**
1. ✅ Say "I'm not sure - let me check the code"
2. ✅ Use Read/Grep tools to find answer
3. ✅ If still uncertain: "I don't see that in the codebase"
4. ✅ Suggest how to find it: "Try: grep -r 'functionName'"

**NEVER:**
- ❌ Use hedging language: "probably", "likely", "usually", "typically"
- ❌ Make educated guesses
- ❌ Invent plausible-sounding answers

### Rule 3: Reference Specifics (VERIFICATION)

**When answering about code:**
1. ✅ Include file path: `src/auth/AuthService.kt`
2. ✅ Include line numbers: `lines 42-58`
3. ✅ Quote relevant code snippets
4. ✅ Describe actual behavior, not assumed behavior

**Format:**
```
Based on [file]:[lines], the [component] does:
- [Specific behavior observed in code]
- [Specific behavior observed in code]

[Optional: Quote key lines if helpful]
```

</hallucination_prevention>

---

## 📋 Workflow Examples

### Example 1: User Asks About Function

<workflow>
  <user_question>
    "What does the authenticate() function do?"
  </user_question>

  <wrong_response>
    ❌ "The authenticate() function probably checks username and password
    against a database and returns a boolean indicating success."

    **Problems:**
    - "probably" = speculation
    - No file read performed
    - Assumes database check (might be wrong)
    - Assumes boolean return (might be wrong)
    - Based on typical patterns, not actual code
  </wrong_response>

  <acceptable_response>
    ⚠️  "Let me check the code..."

    [Uses Read tool on src/auth/AuthService.kt]

    "The authenticate() function uses OAuth2 validation."

    **Better, but:**
    - Lacks specificity
    - No line numbers
    - Minimal detail
    - Could be more helpful
  </acceptable_response>

  <perfect_response>
    ✅ "Let me read the actual implementation..."

    [Uses Read tool on src/auth/AuthService.kt]

    "Based on src/auth/AuthService.kt:42-58, the authenticate() function:

    **Parameters:**
    - username: String
    - password: String

    **Behavior:**
    - Validates credentials using OAuth2Provider.validate()
    - Returns User object with access token on success
    - Throws AuthException with error code on failure

    **Implementation notes:**
    - Does NOT check database directly (delegates to OAuth2Provider)
    - Token is cached in AuthCache for 1 hour
    - Supports MFA via optional mfaToken parameter

    **Key code (lines 45-47):**
    ```kotlin
    val user = oauth2Provider.validate(username, password)
    authCache.store(user.token, ttl = 3600)
    return user
    ```

    **Perfect because:**
    - Specific file and line references
    - Actual parameters documented
    - Behavior based on code, not assumptions
    - Implementation details included
    - Helpful code snippet provided
  </perfect_response>
</workflow>

---

### Example 2: User Asks If Feature Exists

<workflow>
  <user_question>
    "Do we have a password reset feature?"
  </user_question>

  <wrong_response>
    ❌ "Most authentication systems include password reset, so yes,
    you probably have one. Check the forgot password link."

    **Problems:**
    - "Most systems" = speculation
    - "probably" = hedging
    - No actual code verification
    - Might send user on wild goose chase
  </wrong_response>

  <perfect_response>
    ✅ "Let me search the codebase..."

    [Uses Grep tool: pattern="password.*reset" or "resetPassword"]

    **Option A: Feature exists**
    "Yes, password reset is implemented in src/auth/PasswordResetService.kt.

    **Endpoints:**
    - POST /auth/reset-request (sends email)
    - POST /auth/reset-confirm (validates token, sets new password)

    **Flow:**
    1. User requests reset at resetRequest()
    2. Email sent with 6-digit code (30min expiry)
    3. User enters code at resetConfirm()
    4. New password set

    See PasswordResetService.kt:15-89 for implementation."

    **Option B: Feature missing**
    "I searched the codebase and don't see a password reset feature implemented.

    **Search performed:**
    - grep -r "password.*reset" (0 results)
    - grep -r "resetPassword" (0 results)
    - grep -r "forgot.*password" (0 results)

    Would you like me to implement one?"

    **Perfect because:**
    - Actual search performed
    - Definitive answer based on evidence
    - Helpful details if exists
    - Clear "no" if missing
    - No speculation
  </perfect_response>
</workflow>

---

### Example 3: User Asks About Architecture

<workflow>
  <user_question>
    "How does our authentication system work?"
  </user_question>

  <wrong_response>
    ❌ "Typically, authentication systems work like this:
    1. User submits credentials
    2. Server validates against database
    3. Session token issued
    4. Token validated on subsequent requests

    Your system probably follows this pattern."

    **Problems:**
    - Generic explanation (not specific to this codebase)
    - "Typically" and "probably" = speculation
    - Might be completely wrong for this system
    - No value add (user can Google generic explanations)
  </wrong_response>

  <perfect_response>
    ✅ "Let me examine your authentication implementation..."

    [Reads multiple files: AuthService.kt, OAuth2Provider.kt, AuthCache.kt]

    "Based on the code, your authentication architecture:

    **Components (src/auth/):**
    - AuthService.kt - Main entry point
    - OAuth2Provider.kt - OAuth2 integration
    - AuthCache.kt - Token caching
    - PasswordResetService.kt - Password recovery

    **Flow:**
    1. **Login Request** → AuthService.authenticate()
       - Validates via OAuth2Provider (not database)
       - Delegates to Google OAuth2 API
       - Returns User object with JWT access token

    2. **Token Storage** → AuthCache.store()
       - Caches token in Redis (1 hour TTL)
       - Key format: "auth:token:{userId}"

    3. **Subsequent Requests** → AuthMiddleware.validate()
       - Checks cache first (fast path)
       - Falls back to OAuth2 token introspection if cache miss
       - Refreshes token if expiring within 5 minutes

    **Security features:**
    - OAuth2 with PKCE (src/auth/OAuth2Provider.kt:45)
    - Token rotation on refresh (AuthCache.kt:67)
    - MFA support (AuthService.kt:89)

    **Key files:**
    - AuthService.kt:42-150 - Main logic
    - OAuth2Provider.kt:30-120 - OAuth integration
    - AuthMiddleware.kt:15-80 - Request validation

    **Perfect because:**
    - Architecture derived from actual code
    - Specific files and lines referenced
    - Explains actual implementation
    - Highlights security features
    - Provides roadmap for further exploration
  </perfect_response>
</workflow>

---

## 🔍 Detection: Speculation Keywords

**AI Self-Check - These words indicate speculation:**

| Keyword | Why It's Bad | Replace With |
|---------|--------------|--------------|
| "probably" | Admits uncertainty | Read code, then state facts |
| "likely" | Hedging without evidence | Verify, then confirm |
| "usually" | Generic, not specific | Check this codebase |
| "typically" | Based on patterns, not code | Read actual implementation |
| "should" | Assumption about behavior | Verify what it DOES do |
| "might" | Guessing | Check if it DOES |
| "I think" | Opinion without verification | Read, then know |
| "I believe" | Speculation | Verify, then state |
| "generally" | Not specific to this code | Check actual code |
| "often" | Pattern-based guess | Verify this instance |
| "sometimes" | Vague, unverified | Check when it does/doesn't |
| "could be" | Possibility without proof | Check what it IS |

**Automated Detection:**
```regex
/(probably|likely|usually|typically|should|might|I think|I believe|generally|often|sometimes|could be)/gi
```

**Self-correction:**
```
IF response contains speculation keywords THEN
  STOP
  Read relevant files
  Replace speculation with facts
  Verify all claims
  Rewrite response
END IF
```

---

## ✅ Verification Checklist

**Before sending ANY answer about code:**

- [ ] **Did I read the relevant file(s)?**
  - If NO: Stop and read now
  - If YES: Continue

- [ ] **Did I reference specific locations?**
  - File path: ✅ / ❌
  - Line numbers: ✅ / ❌
  - Function names: ✅ / ❌

- [ ] **Is my answer based on facts or assumptions?**
  - Facts from code: ✅
  - Assumptions/speculation: ❌

- [ ] **Did I use speculation keywords?**
  - "probably", "likely", "usually", "typically": ❌
  - Definitive statements: ✅

- [ ] **Can I quote the actual code?**
  - If NO: My answer is probably speculation
  - If YES: Good sign (include quote if helpful)

- [ ] **What if I'm wrong?**
  - If speculation: Major impact (hours wasted)
  - If fact-based: Minimal risk (code is evidence)

**If ANY checkbox fails:** Stop, read code, rewrite answer.

---

## 🚨 Enforcement

**Zero Tolerance Policy:**

<enforcement>
  **Violation:** Answering about code without reading it
  **Severity:** CRITICAL
  **Action:** Immediate correction required

  **When violation detected:**
  1. Stop immediately
  2. Acknowledge: "I should have read the code first"
  3. Read relevant files now
  4. Provide corrected, fact-based answer
  5. Apologize for speculation

  **Example:**
  ```
  User: "I notice you said 'probably' - did you actually read the code?"

  AI: "You're right - I should have read the code first. Let me do that now..."

  [Reads actual code]

  AI: "My apologies for speculating. Based on the actual code at
  src/auth/AuthService.kt:42-58, here's what it ACTUALLY does: [facts]"
  ```
</enforcement>

**Quality Gate:**
- Every code-related answer MUST pass Read-First verification
- No exceptions for "simple" questions
- No exceptions for "obvious" patterns
- Reading takes 2 seconds, speculation costs hours

---

## 🎯 Success Metrics

**Goal:** Zero hallucinations about code

**Measurement:**
- Speculation keywords per 100 responses: <1
- File reads before code questions: 100%
- Line number references: >80%
- User corrections for inaccuracy: <5%

**Dashboard:**
```
Hallucination Prevention Metrics:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Read-First Compliance:    100% ✅
Speculation Keywords:     0/100 ✅
Specific References:      95% ✅
User Trust Score:         98% ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Status: EXCELLENT
```

---

## 🛡️ Advanced Hallucination Reduction Techniques

**Source:** https://docs.claude.com/en/docs/test-and-evaluate/strengthen-guardrails/reduce-hallucinations

### Technique 1: Permission to Express Uncertainty ✅

**Already Implemented:** Rule 2 (Explicit Uncertainty)

**Enhancement:**
Include in ALL complex prompts:
```
If you're unsure about any aspect or if the code lacks necessary
information, say "I don't have enough information to confidently
assess this."
```

**Impact:** "drastically reduces false information"

---

### Technique 2: Direct Quotation Extraction ✅

**For lengthy documents (>20K tokens):**

**Process:**
1. Extract word-for-word quotes FIRST
2. Number the quotes
3. Base subsequent analysis on numbered quotes only

**Example:**
```
Step 1: Extract relevant code sections
Quote 1 (lines 42-45): [exact code]
Quote 2 (lines 78-82): [exact code]

Step 2: Analyze based on quotes
Based on Quote 1, the authentication flow...
Based on Quote 2, the token refresh logic...
```

**Benefit:** "grounds responses in actual text, reducing hallucinations"

**When to use:** Files >1,000 lines, specs >20K tokens

---

### Technique 3: Citation and Verification ✅

**Method A: Require Citations (Already Implemented)**
```
For each claim:
1. Provide supporting quote from code
2. Include file path and line numbers
3. If no quote exists, retract claim
```

**Method B: Post-Generation Verification (NEW)**
```
After generating analysis:
1. Review each assertion
2. Find direct quotes supporting it
3. If quote not found, remove assertion
4. Re-verify final output
```

**Enforcement:** If no supporting quote exists, claim MUST be retracted.

---

### Technique 4: Chain-of-Thought Verification ✅

**Pattern:**
```xml
<thinking>
Before answering, reason step-by-step:
1. What does the code actually say? [read first]
2. What can I directly observe? [facts only]
3. What would I need to verify assumptions? [identify gaps]
4. Is my reasoning sound? [logic check]
</thinking>

<answer>
[Fact-based response with citations]
</answer>
```

**Benefit:** Exposes faulty logic before it reaches the user

**Integration:** Combine with Protocol-Extended-Thinking-v1.0.md

---

### Technique 5: Best-of-N Comparison ✅

**For critical analyses:**

**Process:**
1. Run same prompt 2-3 times independently
2. Compare outputs for consistency
3. Inconsistencies signal potential hallucinations
4. Investigate discrepancies by reading source

**Example:**
```
Run 1: "AuthService uses OAuth2"
Run 2: "AuthService uses OAuth2"
Run 3: "AuthService uses JWT"  ← Inconsistency detected!

Action: Read AuthService.kt to verify (turns out: OAuth2 is correct)
```

**When to use:** High-stakes decisions, complex analyses

---

### Technique 6: Information Restriction ✅

**Explicit boundaries:**
```xml
<restrictions>
Use ONLY information from:
- Files in /src/auth/
- Specification in /specs/authentication.md

DO NOT use:
- General knowledge about authentication
- Assumptions about "typical" implementations
- Code patterns from other projects

If information not in specified sources, say "not found in provided files"
</restrictions>
```

**Benefit:** Prevents contamination from general knowledge

---

## 📚 Related Protocols

- **Protocol-Test-Driven-Development.md** - Verify test code before explaining
- **Protocol-Research-Methodology-v1.0.md** - Research requires evidence
- **Protocol-Tool-Reflection-v1.0.md** - Verify tool outputs before proceeding
- **Protocol-Zero-Tolerance-Pre-Code.md** - Quality standards
- **Protocol-Extended-Thinking-v1.0.md** - Chain-of-thought verification

---

## 💡 Why This Works

**Claude 4.5 Best Practices:**
> "Never speculate about code you haven't opened. Read relevant files BEFORE answering questions about the codebase."
> — Anthropic, Claude 4 Prompt Engineering Best Practices

> "These approaches significantly reduce hallucinations but don't eliminate them entirely. Validation is essential for high-stakes decisions."
> — Anthropic, Reduce Hallucinations Guide

**The Multi-Layered Approach:**
1. **Read-First Protocol:** Eliminates speculation (base layer)
2. **Permission to Admit Uncertainty:** Enables honest "I don't know"
3. **Direct Quotation:** Grounds responses in actual text
4. **Citation Requirement:** Makes answers auditable
5. **Chain-of-Thought:** Exposes faulty logic
6. **Best-of-N:** Detects inconsistencies
7. **Information Restriction:** Prevents knowledge contamination

**Combined Impact:**
- Hallucination rate: <1% (target: 0%)
- User trust: 98%+
- ROI: 1,000:1 (seconds spent vs hours saved)

---

**Version:** 1.0
**Status:** Mandatory for all code-related questions
**Last Updated:** 2025-11-14
**Next Review:** When hallucination rate > 1%
**Owner:** IDEACODE Framework

---

**Remember: Read before answering. Always. No exceptions.**
