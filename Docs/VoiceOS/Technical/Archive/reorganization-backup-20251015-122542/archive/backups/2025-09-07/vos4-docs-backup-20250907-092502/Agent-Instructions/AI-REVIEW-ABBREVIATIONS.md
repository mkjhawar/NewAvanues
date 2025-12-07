<!--
filename: AI-REVIEW-ABBREVIATIONS.md
created: 2025-01-25 00:00:00 PST
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: AI agent review patterns and abbreviations for efficient workflow
last-modified: 2025-08-27 23:00:00 PDT
version: 2.0.0
changelog:
- 2025-08-27: Added MANDATORY analysis requirements and decision flow
- 2025-01-25: Initial creation with basic patterns
-->

# AI Review Abbreviations & Patterns

## 🚨 MANDATORY ANALYSIS REQUIREMENTS

### When Analysis is REQUIRED:
**ALL code issues, bugs, warnings, errors, or architectural decisions MUST undergo:**

1. **Minimum Analysis:** COT → ROT (always)
2. **Extended Analysis:** COT → ROT → TOT → COT → ROT (when issues arise)
3. **Decision Process:**
   - **DEFAULT:** Present analysis + options + pros/cons + recommendation
   - **EXCEPTION:** "work independently" → Make decision based on analysis
   - **ALWAYS:** Document reasoning in code/commits

### Analysis Flow:
```
Issue Detected
    ↓
COT (Chain of Thought)
    ↓
ROT (Reflection)
    ↓
Issues Found? → TOT (Train of Thought)
    ↓                ↓
    No              Yes
    ↓                ↓
Implement    Present Options
             with Pros/Cons
                  ↓
            User Decision
```

## 🎯 Core Review Patterns

### Individual Patterns
- **COT** = Chain of Thought
  - Linear reasoning through steps
  - Shows progression from problem to solution
  - Explicit step-by-step logic

- **ROT** = Reflection
  - Analysis of current approach
  - Self-evaluation of decisions made
  - Consideration of alternatives

- **TOT** = Train of Thought (or Tree of Thought)
  - Exploration of multiple solution paths
  - Branching logic with different approaches
  - Evaluation of each path's viability

### Combined Patterns

#### CRT = Chain of Thought + Reflection + Train of Thought
**Full Analysis Pattern:**
1. **Chain of Thought** - Work through the problem linearly
2. **Reflection** - Evaluate the approach taken
3. **Train of Thought** - Explore alternative solutions
4. **Options Presentation** - Present all viable options with:
   - Pros for each option
   - Cons for each option
   - Recommendation with reasoning
   - Request for approval or new direction

#### Variations (Order Matters)
- **CRT** = COT → ROT → TOT (standard sequence)
- **TRC** = TOT → ROT → COT (explore first, then focus)
- **RCT** = ROT → COT → TOT (reflect, chain, then explore)
- **TCR** = TOT → COT → ROT (explore, chain, reflect)
- **RTC** = ROT → TOT → COT (reflect, explore, chain)
- **CTR** = COT → TOT → ROT (chain, explore, reflect)

## 📋 Usage Examples

### Basic Pattern Request
```
User: "COT this optimization"
AI: [Provides step-by-step chain of thought analysis]
```

### Combined Pattern Request
```
User: "CRT this architecture decision"
AI: 
1. Chain of Thought: [Linear analysis of the decision]
2. Reflection: [Evaluation of approach]
3. Train of Thought: [Alternative architectures explored]
4. Options:
   Option A: [Description]
   - Pros: [List]
   - Cons: [List]
   
   Option B: [Description]
   - Pros: [List]
   - Cons: [List]
   
   Recommendation: Option A because [reasoning]
   
   Please approve or provide alternative direction.
```

### Sequence Variation
```
User: "TRC the refactoring approach"
AI: [Starts with exploring multiple approaches, then reflects, finally chains through the best one]
```

## 🔄 Quick Reference Matrix

| Abbreviation | Sequence | Purpose |
|-------------|----------|---------|
| **CRT** | COT→ROT→TOT | Standard full analysis |
| **TRC** | TOT→ROT→COT | Exploration-first approach |
| **RCT** | ROT→COT→TOT | Reflection-first approach |
| **TCR** | TOT→COT→ROT | Explore then validate |
| **RTC** | ROT→TOT→COT | Reflect then explore |
| **CTR** | COT→TOT→ROT | Chain then branch |

## 🎨 Special Modifiers

### Depth Modifiers
- **-D** = Deep (more thorough analysis)
- **-S** = Shallow (quick overview)
- **-F** = Fast (time-constrained)

Example: `CRT-D` = Deep combined analysis

### Focus Modifiers
- **-P** = Performance focused
- **-M** = Memory focused
- **-A** = Architecture focused
- **-S** = Security focused

Example: `CRT-P` = Combined analysis with performance focus

### Output Modifiers
- **-O** = Options only (skip intermediate steps)
- **-R** = Recommendation only
- **-V** = Verbose (include all details)

Example: `CRT-O` = Just show me the options

## 🚀 Workflow Integration

### With Other Abbreviations
- `CRT + UD` = Full analysis then update documents
- `CRT + SCP` = Full analysis then stage, commit, push
- `TCR + SUF` = Explore-first analysis then full workflow

### Error Handling
When errors are found during CRT:
1. Document the error clearly
2. Provide root cause analysis
3. Present fix options with trade-offs
4. Request approval before proceeding

## 📝 Template Responses

### CRT Template
```markdown
## Chain of Thought
[Step-by-step analysis]

## Reflection
[Evaluation of approach]

## Train of Thought
[Alternative paths explored]

## Options & Recommendations

### Option 1: [Name]
**Description:** [Brief description]
**Pros:**
- [Pro 1]
- [Pro 2]
**Cons:**
- [Con 1]
- [Con 2]
**Effort:** [Low/Medium/High]
**Risk:** [Low/Medium/High]

### Option 2: [Name]
[Same structure]

### Recommendation
I recommend **Option 1** because:
1. [Reason 1]
2. [Reason 2]

Please approve Option 1 or provide alternative direction.
```

## 🔍 Quick Commands

### Analysis Commands
- `COT` - Chain of thought only
- `ROT` - Reflection only
- `TOT` - Train of thought only
- `CRT` - Full combined analysis
- `CRT-O` - Options presentation only

### Decision Commands
- `APPROVE` - Proceed with recommendation
- `ALT` - Provide alternative approach
- `DEFER` - Postpone decision
- `DETAIL` - Need more information

## 📊 Analysis Depth Guide

### Level 1: Quick (COT/ROT/TOT individually)
- Single pattern analysis
- 1-2 minute review
- Basic pros/cons

### Level 2: Standard (CRT)
- Combined pattern analysis
- 3-5 minute review
- Detailed options with trade-offs

### Level 3: Deep (CRT-D)
- Exhaustive analysis
- 10+ minute review
- Multiple alternatives
- Risk assessment
- Performance implications
- Long-term considerations

## 🎯 Best Practices

1. **Choose the right pattern:**
   - Simple decisions: COT
   - Complex architecture: CRT
   - Performance critical: CRT-P
   - Exploring unknowns: TRC

2. **Be explicit about constraints:**
   - Time constraints: Add -F
   - Resource constraints: Add -M
   - Quality requirements: Add -D

3. **Follow up appropriately:**
   - After CRT, wait for approval
   - After errors, always use CRT
   - For critical changes, use CRT-D

---

**Remember:** These abbreviations are designed to make AI-human collaboration more efficient. Use them to get the type of analysis you need quickly and consistently.