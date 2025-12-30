---
description: Switch AI model (per-repo) | /i.model
---

# Task: Switch AI Model

Per-repo model selection with AskUserQuestion interface.

---

## Execution

| Step | Action |
|------|--------|
| 1. Detect | Read `{repo}/.claude/settings.json` field `defaultModel` or current session |
| 2. Ask | AskUserQuestion with available models |
| 3. Save | Update `{repo}/.claude/settings.json` |
| 4. Apply | Notify: "Switched to {model}. Restart terminal for full effect." |

---

## Model Options (AskUserQuestion)

| Model | Description | Use For |
|-------|-------------|---------|
| Claude Sonnet 4.5 *(Recommended)* | Balance: capable + fast | Most development tasks |
| Claude Opus 4.5 | Most capable, slower | Complex reasoning, architecture |
| Claude Haiku | Fastest, cost-effective | Simple tasks, quick fixes |
| OpenAI o1 *(if available)* | Advanced reasoning | Complex problem solving |
| OpenAI GPT-4 Turbo *(if available)* | Strong capability | General development |
| OpenAI GPT-4o *(if available)* | Optimized | Balanced performance |
| Google Gemini Pro *(if available)* | Google flagship | Multimodal tasks |
| Other | Custom input | Manual entry |

**Settings:** multiSelect = false (single choice only)

---

## Storage

**Location:** `{repo}/.claude/settings.json`

```json
{
  "defaultModel": "sonnet|opus|haiku|o1|gpt-4-turbo|gpt-4o|gemini-pro|custom",
  "modelProvider": "anthropic|openai|google|custom"
}
```

---

## Question Format

```
Which AI model for this repository?

Current: Claude Sonnet 4.5

[Options with descriptions as table above]
```

---

## Behavior

**On `/i.model`:**
1. Show current model
2. AskUserQuestion with options
3. Save choice to settings.json
4. Notify user

**On terminal switch:**
- Read `defaultModel` from settings.json
- Apply if different from current
- Show in statusline: `IDEACODE v11.2.5 | Sonnet 4.5 | ...`

---

## Fallback

If selected model unavailable:
```
Warning: OpenAI o1 not available
Falling back to: Claude Sonnet 4.5
Update settings? (Y/n)
```

---

## Output

**Success:**
```
✓ Model: Sonnet 4.5 → Opus 4.5
✓ Saved: .claude/settings.json
✓ Repo will use Opus 4.5 by default

Restart terminal for full effect.
```

**Current:**
```
Model: Claude Sonnet 4.5
Repo: /Volumes/M-Drive/Coding/ideacode
Config: .claude/settings.json
```

---

## Examples

```bash
/i.model              # Interactive selector
# Select: Claude Opus 4.5
# → defaultModel = "opus", provider = "anthropic"

/i.model              # In different repo
# Select: Claude Haiku
# → That repo uses Haiku, this repo still uses Opus
```

---

**Version:** 1.0 | **Updated:** 2025-12-11

