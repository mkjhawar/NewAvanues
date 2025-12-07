# Protocol: Documentation Generation v1.0

**Purpose:** XML-structured patterns for generating documentation, reports, manuals, and instructions
**Status:** RECOMMENDED for all documentation tasks
**Priority:** HIGH
**Version:** 1.0
**Date:** 2025-11-14
**Source:** https://docs.claude.com/en/docs/build-with-claude/prompt-engineering/use-xml-tags#example-generating-financial-reports

---

## Overview

When generating documentation, reports, manuals, or instructions, use XML tags to structure inputs and outputs for maximum clarity, accuracy, and parseability.

**Golden Rule:** XML structure prevents misalignment, ensures consistent formatting, and enables automated post-processing.

---

## Core Benefits of XML-Structured Documentation

### 1. **Clarity**
Separate prompt components distinctly - data vs instructions vs examples

### 2. **Accuracy**
Reduce misinterpretation risks by explicitly labeling each section

### 3. **Flexibility**
Easy modification of specific sections without affecting others

### 4. **Parseability**
Simplified extraction of specific sections via automated tools

---

## Documentation Generation Patterns

### Pattern 1: Financial Reports ⭐

**Use for:** Quarterly reports, financial analysis, performance summaries

**Structure:**
```xml
<data>
{{SPREADSHEET_DATA}}
</data>

<instructions>
1. Include sections: Revenue Growth, Profit Margins, Cash Flow
2. Highlight strengths and areas for improvement
3. Use bullet points for readability
4. Include trend indicators (↑/↓)
</instructions>

<formatting_example>
{{Q1_REPORT}}
</formatting_example>

<output_format>
- Organized bullet-point sections
- Clear metric callouts with trends
- Separated action items
- Concise management outlook
</output_format>
```

**Example Output:**
```markdown
# Q2 2024 Financial Report

## Revenue Growth ↑
- Total revenue: $2.4M (↑ 18% YoY)
- Recurring revenue: $1.8M (↑ 25% YoY)
- New customer acquisition: 340 accounts

**Strengths:**
- Strong SaaS conversion rate (42%)
- Enterprise segment growth (↑ 35%)

**Areas for improvement:**
- SMB churn rate (8.5%, target: <5%)

## Profit Margins ↑
- Gross margin: 72% (↑ 3% from Q1)
- Operating margin: 28% (↑ 5% from Q1)

**Action items:**
- Optimize cloud infrastructure costs
- Negotiate vendor contracts

## Cash Flow →
- Operating cash flow: $680K (stable)
- Free cash flow: $520K (↑ 12%)

**Management Outlook:**
Maintain growth trajectory, focus on enterprise segment, reduce SMB churn.
```

---

### Pattern 2: Technical Documentation

**Use for:** API documentation, system manuals, user guides

**Structure:**
```xml
<source_code>
{{CODE_OR_SYSTEM}}
</source_code>

<target_audience>
{{AUDIENCE_DESCRIPTION}}
</target_audience>

<documentation_type>
{{API_REFERENCE | USER_GUIDE | SYSTEM_MANUAL | TROUBLESHOOTING}}
</documentation_type>

<instructions>
1. Use clear headings and subheadings
2. Include code examples where relevant
3. Add troubleshooting sections
4. Provide cross-references to related topics
5. Use consistent terminology
</instructions>

<style_guide>
- Tone: Professional, concise, helpful
- Code blocks: Syntax-highlighted with language tags
- Examples: Real-world use cases
- Warnings: Clearly marked with ⚠️
</style_guide>
```

**Example Output:**
```markdown
# Authentication API Reference

## Overview
The Authentication API provides secure OAuth2-based authentication for mobile and web clients.

## Authentication Flow

### 1. Authorization Request

**Endpoint:** `POST /oauth/authorize`

**Parameters:**
```json
{
  "client_id": "string (required)",
  "redirect_uri": "string (required)",
  "scope": "string (optional)",
  "state": "string (recommended)"
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/oauth/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "abc123",
    "redirect_uri": "https://myapp.com/callback",
    "scope": "read write",
    "state": "xyz789"
  }'
```

**Response:**
```json
{
  "authorization_code": "def456",
  "expires_in": 600
}
```

### 2. Token Exchange

**Endpoint:** `POST /oauth/token`

**Parameters:**
```json
{
  "grant_type": "authorization_code",
  "code": "string (required)",
  "client_id": "string (required)",
  "client_secret": "string (required)",
  "redirect_uri": "string (required)"
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "authorization_code",
    "code": "def456",
    "client_id": "abc123",
    "client_secret": "secret",
    "redirect_uri": "https://myapp.com/callback"
  }'
```

**Response:**
```json
{
  "access_token": "ghi789",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "jkl012"
}
```

## Troubleshooting

### Error: "Invalid redirect_uri"

⚠️ **Cause:** The redirect_uri doesn't match the registered URI for your client_id.

**Solution:**
1. Verify redirect_uri in your developer console
2. Ensure exact match (including trailing slash)
3. Check for URL encoding issues

### Error: "Authorization code expired"

⚠️ **Cause:** Authorization codes expire after 10 minutes.

**Solution:**
Initiate a new authorization flow. Do not reuse expired codes.

## Related Topics
- [Token Refresh](./token-refresh.md)
- [Security Best Practices](./security.md)
- [Rate Limiting](./rate-limits.md)
```

---

### Pattern 3: Legal/Contract Analysis

**Use for:** Contract reviews, legal summaries, compliance reports

**Structure:**
```xml
<agreement>
{{CONTRACT_TEXT}}
</agreement>

<standard_contract>
{{STANDARD_CONTRACT_FOR_COMPARISON}}
</standard_contract>

<instructions>
1. Analyze key clauses: Indemnification, Limitation of liability, IP ownership
2. Note unusual or non-standard terms
3. Compare to standard contract template
4. Summarize findings in <findings> tags
5. List recommendations in <recommendations> tags
</instructions>

<output_structure>
<findings>
  For each clause:
  - Issue assessment
  - Risk evaluation
  - Deviation from standard
</findings>

<recommendations>
  - Actionable negotiation points
  - Risk mitigation strategies
</recommendations>
</output_structure>
```

**Example Output:**
```markdown
# Contract Analysis: Vendor Agreement

## Findings

### 1. Indemnification Clause (Section 8.2)

**Issue:** Vendor indemnification is capped at contract value ($50K).

**Risk:** HIGH - Our potential exposure from third-party IP claims could exceed $50K significantly.

**Deviation:** Standard contract has uncapped indemnification for IP claims.

---

### 2. Limitation of Liability (Section 9.1)

**Issue:** Liability limited to 12 months of fees paid.

**Risk:** MEDIUM - For a $50K annual contract, max liability is $50K, but our damages from data breach could reach $500K+.

**Deviation:** Standard contract excludes data breach from liability caps.

---

### 3. IP Ownership (Section 12)

**Issue:** Vendor retains all IP rights to custom integrations built for us.

**Risk:** HIGH - We cannot reuse or modify integrations without vendor permission.

**Deviation:** Standard contract grants us ownership of custom work product.

---

### 4. Auto-Renewal Clause (Section 3.2)

**Issue:** Contract auto-renews with 90-day notice period required.

**Risk:** LOW - Standard practice, but easy to miss deadline.

**Deviation:** None - matches standard contract.

---

## Recommendations

### Critical (Must Address)

1. **Uncap IP Indemnification**
   - Request: Remove $50K cap for IP infringement claims
   - Rationale: Third-party claims often exceed contract value
   - Fallback: Minimum $500K cap for IP claims

2. **Exclude Data Breach from Liability Cap**
   - Request: Add carveout for data breach/security incidents
   - Rationale: Regulatory fines and notification costs can be substantial
   - Fallback: Separate cap of 3x annual fees for security incidents

3. **Secure IP Rights for Custom Work**
   - Request: We own all custom integrations built for our use case
   - Rationale: Business continuity if we change vendors
   - Fallback: Perpetual, irrevocable license with right to modify

### Important (Should Address)

4. **Shorten Auto-Renewal Notice**
   - Request: 60-day notice period instead of 90 days
   - Rationale: More manageable internal approval timeline

5. **Add Termination for Convenience**
   - Request: Either party can terminate with 60-day notice after Year 1
   - Rationale: Flexibility if business needs change

## Risk Summary

**Overall Risk Rating:** MEDIUM-HIGH

**Primary Concerns:**
- IP indemnification cap too low
- Liability cap excludes critical scenarios
- Vendor retains IP we need for business operations

**Negotiation Priority:**
1. IP ownership/licensing (CRITICAL)
2. Indemnification cap (CRITICAL)
3. Liability exclusions (IMPORTANT)
4. Auto-renewal terms (NICE TO HAVE)
```

---

### Pattern 4: User Manuals

**Use for:** Product manuals, installation guides, how-to documentation

**Structure:**
```xml
<product_information>
{{PRODUCT_SPECS_AND_FEATURES}}
</product_information>

<target_users>
{{USER_SKILL_LEVEL}}
</target_users>

<manual_sections>
1. Getting Started
2. Installation
3. Configuration
4. Common Tasks
5. Troubleshooting
6. FAQ
</manual_sections>

<instructions>
- Use step-by-step numbered lists
- Include screenshots/diagrams where helpful
- Provide both quick start and detailed guides
- Add safety warnings where applicable
- Use consistent terminology
</instructions>

<tone>
Friendly, helpful, assumes beginner skill level
</tone>
```

**Example Output:**
```markdown
# VoiceOS User Manual

## Getting Started

Welcome to VoiceOS! This guide will help you set up and use your voice-first operating system.

**What you'll need:**
- Android device (Android 10+)
- 2GB free storage
- Internet connection (for initial setup)

**Estimated setup time:** 10 minutes

---

## Installation

### Step 1: Download VoiceOS

1. Open Google Play Store
2. Search for "VoiceOS"
3. Tap **Install**
4. Wait for download to complete (~200MB)

### Step 2: Initial Setup

1. Open VoiceOS app
2. Grant permissions when prompted:
   - ✅ Microphone (required for voice commands)
   - ✅ Storage (required for voice models)
   - ✅ Accessibility (required for system control)

3. Complete voice training:
   - Read 5 sample phrases
   - Takes ~2 minutes
   - Improves recognition accuracy

4. Choose your activation phrase:
   - Default: "Hey VoiceOS"
   - Custom: Tap to set your own (3-10 syllables recommended)

### Step 3: First Voice Command

**Try it now:**
1. Say your activation phrase: "Hey VoiceOS"
2. Wait for the listening tone (♪)
3. Say: "What can you do?"
4. VoiceOS will explain available commands

---

## Configuration

### Changing Voice Settings

**To adjust recognition sensitivity:**
1. Say: "Hey VoiceOS, open settings"
2. Say: "Voice sensitivity"
3. Choose: Low | Medium | High
   - **Low:** Requires clear pronunciation
   - **Medium:** ✅ Recommended for most users
   - **High:** More forgiving but may trigger accidentally

### Adding Custom Commands

**To create a custom voice command:**
1. Say: "Hey VoiceOS, create custom command"
2. Say the command phrase: e.g., "Start my day"
3. Choose action:
   - Launch app
   - Run routine
   - Send message
   - Custom automation

**Example:**
```
You: "Hey VoiceOS, create custom command"
VoiceOS: "What should the command phrase be?"
You: "Start my day"
VoiceOS: "What should happen when you say 'Start my day'?"
You: "Run routine"
VoiceOS: "Which routine?"
You: "Morning routine"
VoiceOS: "Done! Say 'Start my day' to run your morning routine."
```

---

## Common Tasks

### Task 1: Sending a Message

1. Say: "Hey VoiceOS, send a message"
2. Say recipient name: "to John"
3. Speak your message: "I'll be there in 10 minutes"
4. Confirm: Say "send" or "cancel"

### Task 2: Setting a Reminder

1. Say: "Hey VoiceOS, remind me to..."
2. State the task: "call mom"
3. State the time: "at 5 PM today"
4. VoiceOS confirms: "Reminder set for 5 PM today: Call mom"

### Task 3: Controlling Smart Home

1. Say: "Hey VoiceOS, turn on..."
2. State device: "living room lights"
3. VoiceOS executes and confirms

**Tip:** Say "Hey VoiceOS, what devices are connected?" to see all available devices.

---

## Troubleshooting

### Problem: VoiceOS doesn't respond to activation phrase

**Possible causes:**
- Background noise too loud
- Microphone blocked or muffled
- Activation phrase not trained properly

**Solutions:**
1. ✅ Move to quieter environment
2. ✅ Check microphone not covered
3. ✅ Retrain voice model: Settings → Voice Training → Retrain

---

### Problem: Commands not recognized correctly

**Possible causes:**
- Speaking too fast
- Accent/pronunciation differs from training
- Background noise interference

**Solutions:**
1. ✅ Speak clearly and at moderate pace
2. ✅ Retrain voice model with more samples
3. ✅ Increase recognition sensitivity: Settings → Voice Sensitivity → High
4. ✅ Use push-to-talk mode: Settings → Activation → Push-to-Talk

---

### Problem: Battery drain

**Possible causes:**
- Always-listening mode enabled
- High sensitivity setting
- Many background automations

**Solutions:**
1. ✅ Switch to push-to-talk mode (saves ~40% battery)
2. ✅ Lower sensitivity to Medium
3. ✅ Review automations: Settings → Automations → Disable unused

---

## FAQ

**Q: Can I use VoiceOS offline?**
A: Yes! Core features work offline. Internet required for web searches, smart home control, and cloud services.

**Q: How do I change the voice assistant's voice?**
A: Settings → Voice Output → Choose from 8 voices (4 male, 4 female)

**Q: Can I use VoiceOS in multiple languages?**
A: Currently supports English, Spanish, French, German. More languages coming soon.

**Q: Is my voice data stored?**
A: Voice processing happens on-device. No audio sent to cloud unless you use cloud-enabled features (e.g., web search). See Privacy Policy for details.

**Q: How do I update VoiceOS?**
A: Updates download automatically. You'll get a voice notification: "Update available. Say 'install update' to update now."

---

## Safety Information

⚠️ **Do not use VoiceOS while:**
- Driving (unless in hands-free mode)
- Operating heavy machinery
- In emergency situations requiring immediate action

⚠️ **Accessibility note:**
VoiceOS requires Accessibility permissions to control your device. Never grant Accessibility permissions to untrusted apps.

⚠️ **Privacy:**
Review privacy settings before enabling cloud features. Voice data may be processed by third-party services.

---

## Support

**Need help?**
- Say: "Hey VoiceOS, help"
- Email: support@voiceos.com
- Community: community.voiceos.com

**Found a bug?**
- Say: "Hey VoiceOS, report a bug"
- Or visit: bugs.voiceos.com
```

---

### Pattern 5: Project Status Reports

**Use for:** Sprint reviews, milestone reports, project updates

**Structure:**
```xml
<project_data>
{{TASK_STATUS_METRICS_TIMELINE}}
</project_data>

<stakeholders>
{{AUDIENCE_TECHNICAL_LEVEL}}
</stakeholders>

<report_period>
{{START_DATE}} to {{END_DATE}}
</report_period>

<instructions>
1. Executive summary (2-3 sentences)
2. Key accomplishments (bullet points)
3. Metrics and KPIs
4. Blockers and risks
5. Next sprint goals
6. Use status indicators: ✅ Done, 🔄 In Progress, ⏸️ Blocked, 🔴 At Risk
</instructions>

<formatting>
- Brief, scannable sections
- Quantified metrics
- Visual status indicators
- Action items clearly marked
</formatting>
```

**Example Output:**
```markdown
# Sprint 23 Status Report
**Period:** Nov 1-14, 2024
**Team:** VoiceOS Core (8 engineers)

## Executive Summary

Sprint 23 completed 23/25 story points with OAuth2 authentication shipped to production. Offline mode implementation blocked by Android API limitations; investigating workarounds. On track for Q4 launch.

---

## Key Accomplishments ✅

### Shipped to Production
- ✅ OAuth2 authentication with PKCE flow
  - 100% test coverage
  - Security audit passed
  - Performance: <200ms token exchange

- ✅ Voice command registry system
  - 32 commands registered
  - 95% recognition accuracy
  - Voice training UI completed

- ✅ Accessibility improvements
  - WCAG 2.1 AA compliance achieved
  - Screen reader support added
  - High-contrast mode implemented

### Completed (Staging)
- ✅ Smart home integration API
  - 15 device types supported
  - Rate limiting implemented
  - Documentation published

---

## Metrics & KPIs

### Velocity
- **Planned:** 25 story points
- **Completed:** 23 story points
- **Velocity:** 92% (↓ 8% from Sprint 22)
- **Carryover:** 2 story points (offline mode)

### Quality
- **Test Coverage:** 91% (↑ 3%)
- **Bugs Found:** 12 (↓ 25% from Sprint 22)
- **Bugs Fixed:** 15 (↑ 20%)
- **Critical Bugs:** 0 ✅

### Performance
- **App Launch Time:** 1.2s (target: <1.5s) ✅
- **Voice Recognition Latency:** 180ms (target: <200ms) ✅
- **Battery Impact:** 3.5%/hour (target: <5%) ✅

---

## Blockers & Risks 🔴

### Active Blockers

**1. Offline Voice Recognition** ⏸️ BLOCKED
- **Issue:** Android Speech API requires internet for high accuracy
- **Impact:** Cannot ship offline mode as planned
- **Options:**
  1. Use TensorFlow Lite model (accuracy: 75% vs 95% online)
  2. Delay offline mode to Sprint 25 (wait for Android 15 API)
  3. Hybrid: Basic commands offline, complex commands require internet
- **Decision needed:** By Nov 16
- **Owner:** Sarah Chen

### Risks

**2. Third-Party API Rate Limits** 🔴 HIGH RISK
- **Issue:** Smart home provider API has 100 req/hour limit
- **Impact:** Multi-device users may hit limits
- **Mitigation:** Implementing request batching (Sprint 24)
- **Probability:** 70%
- **Owner:** Mike Torres

**3. App Store Review Delay** ⚠️ MEDIUM RISK
- **Issue:** Accessibility permissions may trigger extended review
- **Impact:** Could delay launch by 1-2 weeks
- **Mitigation:** Proactive communication with Apple reviewer
- **Probability:** 40%
- **Owner:** Emma Liu

---

## Next Sprint Goals (Sprint 24: Nov 15-28)

### Committed
1. 🔄 Resolve offline mode blocker (TFLite evaluation)
2. 🔄 Smart home API rate limit mitigation
3. 🔄 Widget support for iOS/Android
4. 🔄 Multi-language support (Spanish, French)

### Stretch Goals
5. Performance optimization (target: <1s app launch)
6. Voice training v2 (adaptive learning)

**Planned Velocity:** 26 story points

---

## Action Items

**Before Nov 16:**
- [ ] Sarah: Offline mode decision (TFLite vs delay vs hybrid)
- [ ] Mike: Complete rate limit batching PoC
- [ ] Emma: Submit iOS build for pre-review feedback

**Before Nov 20:**
- [ ] All: Code freeze for Q4 release candidate
- [ ] QA: Begin regression testing

---

## Team Notes

**Kudos:**
- 🎉 Sarah for OAuth2 security audit pass on first attempt
- 🎉 Team for 91% test coverage milestone

**Concerns:**
- Sprint velocity dropped 8% due to offline mode complexity
- Need to reprioritize Sprint 24 if offline mode delayed

**Morale:** High - excited for Q4 launch! 🚀
```

---

## XML Tagging Best Practices

### 1. Consistency

**DO:**
- ✅ Use identical tag names throughout
- ✅ Reference tags when discussing content: "Using the contract in `<contract>` tags..."
- ✅ Maintain naming conventions across similar documents

**Example:**
```xml
<data>{{CONTENT}}</data>
<instructions>...</instructions>
<formatting_example>...</formatting_example>

<!-- Later in prompt: -->
"Analyze the data in <data> tags according to <instructions>."
```

### 2. Nesting

**DO:**
- ✅ Use hierarchical structure for organization
- ✅ Nest related content under parent tags
- ✅ Keep nesting depth reasonable (≤4 levels)

**Example:**
```xml
<report>
  <metadata>
    <period>Q2 2024</period>
    <author>Finance Team</author>
  </metadata>

  <sections>
    <revenue>
      <summary>...</summary>
      <details>...</details>
    </revenue>

    <expenses>
      <summary>...</summary>
      <details>...</details>
    </expenses>
  </sections>

  <recommendations>
    <critical>...</critical>
    <important>...</important>
  </recommendations>
</report>
```

### 3. Combination with Other Techniques

**Multishot Prompting:**
```xml
<examples>
  <example type="good">
    <input>{{INPUT}}</input>
    <output>{{OUTPUT}}</output>
  </example>

  <example type="bad">
    <input>{{INPUT}}</input>
    <output>{{OUTPUT}}</output>
    <why_bad>{{EXPLANATION}}</why_bad>
  </example>
</examples>
```

**Chain of Thought:**
```xml
<thinking>
  <step_1>Analyze the problem</step_1>
  <step_2>Generate hypotheses</step_2>
  <step_3>Evaluate evidence</step_3>
</thinking>

<answer>
  Final response based on thinking above
</answer>
```

**Prefilling:**
```xml
<output_format>
Start response with: "# Financial Report"
</output_format>

<!-- Claude's response will be prefilled with the heading -->
```

---

## Integration with IDEACODE Workflows

### When to Use Documentation Generation Protocol

**Triggers:**
- Creating user manuals or guides
- Generating reports (financial, project status, analysis)
- Writing technical documentation (API docs, system manuals)
- Producing legal/compliance documents
- Creating training materials

**MCP Tools:**
- `ideacode_specify` - Use XML structure for spec.md
- `ideacode_archive` - Use XML for DELTA.md format
- `ideacode_issue` - Use XML for issue analysis reports
- All documentation generation tasks

**Example Integration:**
```typescript
// ideacode MCP tool using documentation protocol
const generateReport = async (data: ReportData) => {
  const prompt = `
<project_data>
${JSON.stringify(data.metrics, null, 2)}
</project_data>

<report_type>Sprint Status Report</report_type>

<stakeholders>
Engineering team, Product managers, Executives
</stakeholders>

<instructions>
Generate sprint status report following Protocol-Documentation-Generation-v1.0.md
Pattern 5: Project Status Reports
</instructions>
  `;

  return await claude.generate(prompt);
};
```

---

## Verification Checklist

**Before generating documentation, ensure:**

- [ ] **XML Structure Defined**
  - [ ] All input sections tagged appropriately
  - [ ] Output format specified in tags
  - [ ] Instructions clearly marked
  - [ ] Examples provided where helpful

- [ ] **Consistency**
  - [ ] Tag names used consistently
  - [ ] Referenced correctly in instructions
  - [ ] Matching open/close tags

- [ ] **Nesting**
  - [ ] Hierarchical organization logical
  - [ ] Depth reasonable (≤4 levels)
  - [ ] Parent-child relationships clear

- [ ] **Combination Techniques**
  - [ ] Examples in `<examples>` tags if needed
  - [ ] Thinking in `<thinking>` tags if complex
  - [ ] Output format prefilled if specific structure required

- [ ] **Parseability**
  - [ ] Output can be extracted programmatically
  - [ ] Standard XML formatting used
  - [ ] No malformed tags

---

## Common Pitfalls

### ❌ WRONG: Unstructured Input

```
Generate a financial report from this data: [large spreadsheet dump]
Include revenue, expenses, and recommendations.
```

**Problem:** Claude may misalign sections, miss requirements, or use inconsistent formatting.

---

### ✅ CORRECT: XML-Structured Input

```xml
<data>
[spreadsheet data]
</data>

<instructions>
1. Sections: Revenue Growth, Profit Margins, Cash Flow
2. Highlight strengths and areas for improvement
3. Use bullet points
4. Include trend indicators (↑/↓)
</instructions>

<formatting_example>
# Q1 Report
## Revenue Growth ↑
- Metric: Value (% change)
...
</formatting_example>
```

**Result:** Precise, consistent, parseable output.

---

## Why This Works

**From Anthropic Documentation:**
> "XML tags provide clarity, accuracy, flexibility, and parseability—especially critical for complex documentation tasks where structure determines output quality."

**Benefits:**
1. **Clarity:** Sections are unambiguous
2. **Accuracy:** Reduced misinterpretation
3. **Flexibility:** Easy to modify specific sections
4. **Parseability:** Automated post-processing possible

**IDEACODE Integration:**
All 29 MCP tools can use this protocol for generating structured documentation, ensuring consistency across all generated artifacts.

---

**Version:** 1.0
**Status:** Recommended for all documentation generation
**Last Updated:** 2025-11-14
**Owner:** IDEACODE Framework
**Source:** Anthropic Claude Best Practices

---

**Remember: XML structure transforms documentation generation from variable quality to consistently professional output.**
