# Contextual Help Template
# Use this template for ALL IDEACODE commands
# Version: 8.5

## Standard Help Structure

Every command MUST include this `<help>` section at the top:

```markdown
<help>
╔════════════════════════════════════════════════════════════════╗
║  /{COMMAND} - {ONE_LINE_PURPOSE}                     ║
╚════════════════════════════════════════════════════════════════╝

📋 PURPOSE
{2-3 sentence description of what this command does. Focus on:
 - What problem it solves
 - When to use it
 - Key differentiator from similar commands}

📖 USAGE
  /{COMMAND} {required_arg} [{optional_arg}] [flags]

{IF command has arguments}
🎯 ARGUMENTS
  {arg_name}    {Description of argument}
                {Example value}
{END IF}

⚡ FLAGS & MODIFIERS
  --yolo        Auto-proceed through workflow (full automation)
  --stop        Disable workflow chaining (manual control)
  --mode <type> interactive|yolo|manual (default: interactive)
  --cot         Show Chain of Thought reasoning (educational)
  --tot         Use Tree of Thought (explore multiple paths)
  {command-specific flags...}

💡 EXAMPLES
  Basic usage:
    /{COMMAND} {basic_example}

  YOLO mode (full automation):
    /{COMMAND} {example} --yolo

  With reasoning (educational):
    /{COMMAND} {example} --cot

  {Additional 1-2 examples showing advanced usage}

{IF workflow command (specify, plan, tasks, implement)}
🔗 WORKFLOW CHAINING
  After completion → offers:
    → {next_step_1} (recommended)
    → {next_step_2} (alternative)

  Full chain: specify→plan→tasks→implement→test→commit

  Modes:
    --yolo:  Automatic progression through all steps
    default: Ask at each step (interactive)
    --stop:  No chaining (manual control)

  Example YOLO workflow:
    /{COMMAND} {example} --yolo
      → Auto-proceeds through entire chain
      → Pauses on errors (tests fail, review blockers)
{END IF}

🔧 RELATED COMMANDS
  /{related_1}    {Brief description}
  /{related_2}    {Brief description}
  /{related_3}    {Brief description}

📚 FULL DOCUMENTATION
  {Link to protocol if applicable: protocols/Protocol-{Name}-v1.0.md}
  /help → Category {number} ({Category Name})

{IF command is complex OR has simpler alternative}
💡 QUICK START
  New to IDEACODE? Try:
    /{simpler_alternative} (easier alternative)
    /wiz (guided interactive workflow)
{END IF}

{IF command has common pitfalls}
⚠️  COMMON PITFALLS
  • {pitfall_1}
  • {pitfall_2}
{END IF}

{IF command has useful tips}
💡 TIPS
  • {tip_1}
  • {tip_2}
{END IF}
</help>
```

## Help Detection Logic

**Add to the start of EVERY command (before main logic):**

```markdown
<!-- Help Detection - Check FIRST before any other logic -->
{IF user_input contains "?" OR user_input contains "help" OR user_input contains "--help"}
  {DISPLAY content from <help> section above}
  {EXIT - do NOT execute command logic}
{END IF}

<!-- If no help modifier detected, proceed with normal command execution -->
{normal command logic starts here...}
```

## Section Guidelines

### 📋 PURPOSE (Required)
- 2-3 sentences maximum
- Focus on **what** and **when**
- Differentiate from similar commands
- Use active voice

**Good:**
```
Creates detailed feature specification using IDEACODE workflow.
Interviews user about requirements, loads profile-specific standards,
generates spec.md with success criteria and technical constraints.
```

**Bad:**
```
This command is for making specifications for features that you
want to build in your project.
```

### 📖 USAGE (Required)
- Show exact syntax
- Use `<required>` and `[optional]`
- Include common flags
- Keep on one line if possible

**Format:**
```
/{command} <required_arg> [optional_arg] [flags]
```

### 🎯 ARGUMENTS (If Applicable)
- Only include if command takes arguments
- Show parameter name, description, example
- Indent example values

**Format:**
```
feature_description    What feature to build (required)
                      Example: "Add dark mode toggle"
```

### ⚡ FLAGS & MODIFIERS (Required)
- List ALL supported flags
- Include global flags (--yolo, --stop, --mode, --cot, --tot)
- Add command-specific flags
- Brief description for each

### 💡 EXAMPLES (Required - MOST IMPORTANT)
- Minimum 2, maximum 4 examples
- Start simple, progress to advanced
- Show different use cases
- Include expected behavior/output
- Examples should be copy-pasteable

**Priority order:**
1. Basic usage (no flags)
2. YOLO mode (if workflow command)
3. With reasoning (--cot or --tot)
4. Advanced/specific scenario

### 🔗 WORKFLOW CHAINING (Workflow Commands Only)
- Explain what happens after completion
- Show full workflow chain
- Explain three modes (yolo, interactive, manual)
- Give example YOLO workflow

### 🔧 RELATED COMMANDS (Required)
- 3-5 related commands
- Mix of: alternatives, next steps, complementary
- One-line description for each

### 📚 FULL DOCUMENTATION (Required)
- Link to protocol (if exists)
- Link to /help category

### 💡 QUICK START (Optional - For Complex Commands)
- Suggest simpler alternative
- Point to wizard mode
- When to use each

### ⚠️ COMMON PITFALLS (Optional)
- Known gotchas
- Frequent mistakes
- How to avoid them

### 💡 TIPS (Optional)
- Best practices
- Performance hints
- Quality improvements

## Visual Formatting Rules

### Use Emojis Consistently
- 📋 PURPOSE
- 📖 USAGE
- 🎯 ARGUMENTS
- ⚡ FLAGS & MODIFIERS
- 💡 EXAMPLES
- 🔗 WORKFLOW CHAINING
- 🔧 RELATED COMMANDS
- 📚 FULL DOCUMENTATION
- ⚠️ COMMON PITFALLS / WARNING
- 💡 TIPS / QUICK START
- ✅ Success/Recommended
- ❌ Error/Not Recommended

### Formatting Standards
- **Bold** for emphasis and key terms
- `Code formatting` for commands, flags, file paths
- Indent for sub-items and examples
- Use boxes (┌─┐ └─┘) for multi-line prompts
- Use whitespace generously for readability

### Box Drawing Characters
```
┌─────────────────────┐
│ Title or Header     │
│                     │
│ Content here        │
└─────────────────────┘
```

## Quality Checklist

Before finalizing help content:
- [ ] Help section is complete (<help>...</help>)
- [ ] PURPOSE is 2-3 sentences, clear and concise
- [ ] USAGE shows exact syntax
- [ ] All FLAGS are documented
- [ ] EXAMPLES are copy-pasteable and realistic
- [ ] RELATED COMMANDS includes 3-5 items
- [ ] FULL DOCUMENTATION links are correct
- [ ] Help detection logic is added (check for ?, help, --help)
- [ ] Consistent emoji usage
- [ ] No typos or formatting errors
- [ ] Length <60 lines (keep concise)

## Testing Help Content

Test with all three syntaxes:
```bash
/{command} ?
/{command} help
/{command} --help
```

All three MUST produce identical output.

---

**Version:** 8.5
**Last Updated:** 2025-11-23
**Usage:** Include this template when creating/updating IDEACODE commands
