# Zen MCP Configuration Guide for Claude Code CLI

**Date**: 2025-10-18 18:55 PDT
**Status**: Configuration Complete (requires API key)
**Purpose**: Enable multi-model consultation in Claude Code via MCP protocol

---

## Quick Links
- [MCP Documentation](https://modelcontextprotocol.io/)
- [OpenRouter](https://openrouter.ai/) - Get API key here
- [VOS4 Settings](../.claude/settings.local.json)
- [MCP Configuration](../.mcp.json)

---

## What is MCP (Model Context Protocol)?

**MCP** allows Claude Code to communicate with external services and other AI models through a standardized protocol.

**Benefits for VOS4**:
- Consult multiple AI models for complex decisions
- Access specialized knowledge (e.g., latest Android APIs)
- Cost optimization (use local models via Ollama)
- Consensus building (multiple models agree = higher confidence)

---

## Configuration Files Created

### 1. `.mcp.json` (Project-Level MCP Configuration)

**Location**: `/vos4/.mcp.json`

```json
{
  "mcpServers": {
    "everything": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-everything"
      ],
      "env": {
        "OPENROUTER_API_KEY": "${OPENROUTER_API_KEY}"
      }
    }
  }
}
```

**What it does**:
- Configures MCP server "everything" (provides access to multiple models)
- Uses `npx` to run server (auto-installs if needed)
- Reads `OPENROUTER_API_KEY` from environment variable

---

## Setup Instructions

### Step 1: Get OpenRouter API Key

1. **Go to OpenRouter**: https://openrouter.ai/
2. **Sign up** (free account available)
3. **Get API Key**:
   - Go to "Keys" section
   - Click "Create Key"
   - Copy the key (starts with `sk-or-...`)

**Pricing** (as of 2025-10):
- Free tier available
- Pay-as-you-go pricing
- Example costs:
  - GPT-4.5-turbo: ~$0.01/1K tokens
  - Gemini 2.5 Pro: ~$0.015/1K tokens
  - Claude Sonnet 4.5: ~$0.003/1K tokens (input), ~$0.015/1K tokens (output)

### Step 2: Set Environment Variable

**Option A: Session-Only (temporary)**
```bash
export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"
```

**Option B: Permanent (recommended)**

Add to your shell configuration:

```bash
# For zsh (macOS default)
echo 'export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"' >> ~/.zshrc
source ~/.zshrc

# For bash
echo 'export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"' >> ~/.bashrc
source ~/.bashrc
```

### Step 3: Enable MCP Server in Claude Code

The `.mcp.json` file has been created. Now you need to approve it:

```bash
# Restart Claude Code (close and reopen terminal)
# OR reload your session

# On first use, Claude Code will prompt:
# "Allow MCP server 'everything'?"
# → Click "Allow" or "Always allow"
```

**Alternatively**, add to settings:

Edit `/vos4/.claude/settings.local.json` and add:

```json
{
  "enableAllProjectMcpServers": true,
  "permissions": {
    ...existing permissions...
  }
}
```

### Step 4: Verify Configuration

```bash
# Check if MCP server is recognized
# (Next time you start Claude Code, you should see MCP server loaded)

# Test with a simple query:
# In Claude Code session:
"What models are available via MCP?"
```

---

## Available Models via OpenRouter

### Claude Models (Best for VOS4 work)
- `anthropic/claude-sonnet-4.5` - Current model (balanced)
- `anthropic/claude-opus-4` - Most capable (expensive)
- `anthropic/claude-haiku-4` - Fast and cheap

### OpenAI Models
- `openai/gpt-5` - Cutting-edge (expensive)
- `openai/gpt-4.5-turbo` - Fast, good quality
- `openai/gpt-4o` - Optimized version

### Google Models
- `google/gemini-2.5-pro` - Excellent for code
- `google/gemini-2.5-flash` - Fast, cheaper
- `google/gemini-pro-1.5` - Proven model

### Meta Models (Cheaper)
- `meta-llama/llama-3.2-90b` - Large, capable
- `meta-llama/llama-3.1-8b` - Fast, cheap

---

## Usage Examples

### Example 1: Architectural Decision

```markdown
User: "Should we consolidate VOS4 databases? Consult with GPT-5 and Gemini 2.5 Pro"

Claude Code:
1. Uses MCP to query GPT-5 with context
2. Uses MCP to query Gemini 2.5 Pro with context
3. Synthesizes both responses
4. Provides recommendation with rationale from both models

Response:
"After consulting GPT-5 and Gemini 2.5 Pro:

**GPT-5 says**: [recommendation + reasoning]
**Gemini 2.5 Pro says**: [recommendation + reasoning]

**Synthesis**: Both models agree that [recommendation].
**Reasoning**: [combined rationale]
**Trade-offs**: [from both perspectives]"
```

### Example 2: Latest Technology Advice

```markdown
User: "What's the best way to handle Android gesture injection in 2025? Check with multiple models"

Claude Code via MCP:
- Queries multiple models about latest Android APIs
- Gets perspectives from models with more recent training data
- Synthesizes recommendations

Response includes:
- Latest API recommendations
- Best practices from 2025
- Code examples from multiple sources
```

### Example 3: Code Review

```markdown
User: "Review this coroutine implementation, consult with GPT-4.5 for Kotlin best practices"

Claude Code:
1. Sends code to GPT-4.5 via MCP
2. Gets Kotlin-specific feedback
3. Combines with own analysis

Response:
"My analysis: [Claude's review]

GPT-4.5 additional insights: [External model review]

**Combined recommendations**: [Synthesized feedback]"
```

---

## Cost Optimization Strategies

### Strategy 1: Tiered Consultation

```json
{
  "consultation_strategy": {
    "critical_architecture": "openai/gpt-5",
    "code_review": "google/gemini-2.5-pro",
    "quick_questions": "anthropic/claude-haiku-4",
    "bulk_processing": "meta-llama/llama-3.1-8b"
  }
}
```

**Estimated costs**:
- Critical decision (1 consultation): ~$0.10
- Code review (5 consultations/week): ~$2.50/month
- Quick questions (20 consultations/month): ~$1.00/month

**Total estimated cost**: ~$5-10/month for moderate use

### Strategy 2: Local Models (Ollama) - FREE

**Install Ollama**:
```bash
# macOS
brew install ollama

# Start Ollama service
ollama serve

# Pull models
ollama pull llama3.1:8b        # General purpose
ollama pull codellama:13b      # Code-focused
ollama pull mistral:7b         # Fast, efficient
```

**Configure Ollama MCP** (add to `.mcp.json`):
```json
{
  "mcpServers": {
    "everything": { ...existing... },
    "ollama": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-ollama"
      ],
      "env": {
        "OLLAMA_HOST": "http://localhost:11434"
      }
    }
  }
}
```

**Use local models for**:
- 70% of consultations (FREE)
- Non-critical decisions
- Code generation
- Documentation review

**Reserve OpenRouter for**:
- 30% of consultations
- Critical architecture decisions
- Latest technology advice
- Consensus building

---

## When to Use MCP Consultation

### ✅ Use MCP Consultation For:

1. **Complex Architectural Decisions**
   ```
   "Consult with GPT-5 and Gemini about database architecture"
   ```
   - Multiple perspectives valuable
   - High-stakes decision
   - Want consensus

2. **Latest Technology Advice**
   ```
   "Check with Gemini 2.5 Pro about latest Android 15 gesture APIs"
   ```
   - Need recent training data
   - Latest best practices
   - New API patterns

3. **Specialized Domain Knowledge**
   ```
   "Consult CodeLlama about optimal Kotlin coroutine patterns"
   ```
   - Domain-specific model
   - Specialized expertise
   - Code generation

4. **Consensus Building**
   ```
   "Get opinions from 3 different models on this performance issue"
   ```
   - Confidence building
   - Multiple approaches
   - Risk mitigation

### ❌ Don't Use MCP For:

1. **Simple Implementation Tasks**
   - Local subagents sufficient
   - No external expertise needed
   - Cost not justified

2. **Well-Understood Domains**
   - VOS4 subagents have context
   - No need for external perspective
   - Faster locally

3. **Fast Iteration**
   - External API latency
   - Not cost-effective
   - Local execution faster

4. **Cost-Sensitive Work**
   - Use local subagents
   - Use Ollama if needed
   - Save OpenRouter for critical use

---

## Integration with VOS4 Subagents

### Hybrid Approach: VOS4 Subagents + MCP

**Workflow**:
```
User: "Design voice gesture recognition architecture"
↓
@vos4-orchestrator:
  1. Routes to @vos4-architecture-reviewer (local)
  2. Local architecture review completed
  3. For high-stakes decision, consults via MCP:
     - GPT-5 for latest Android patterns
     - Gemini 2.5 Pro for code architecture
  4. Synthesizes local + external perspectives
  5. Provides comprehensive recommendation
```

**Benefits**:
- Local subagents have VOS4 context
- External models have latest knowledge
- Best of both worlds
- Cost-controlled (only when needed)

---

## Troubleshooting

### Issue: "MCP server not found"

**Solution**:
```bash
# Verify npx is available
which npx

# Test MCP server manually
npx -y @modelcontextprotocol/server-everything --help

# Check .mcp.json syntax
cat /vos4/.mcp.json | python3 -m json.tool
```

### Issue: "OpenRouter API key invalid"

**Solution**:
```bash
# Verify environment variable
echo $OPENROUTER_API_KEY

# Test API key
curl -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  https://openrouter.ai/api/v1/auth/key

# Re-export if needed
export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"
```

### Issue: "MCP server not approved"

**Solution**:
Edit `/vos4/.claude/settings.local.json`:
```json
{
  "enableAllProjectMcpServers": true,
  ...
}
```

Or approve individually when prompted.

---

## Security Considerations

### API Key Protection

**✅ DO:**
- Store API key in environment variable
- Use `${OPENROUTER_API_KEY}` in .mcp.json
- Add `.env` to `.gitignore` if using .env files
- Rotate keys periodically

**❌ DON'T:**
- Hard-code API keys in .mcp.json
- Commit API keys to git
- Share API keys in documentation
- Use personal keys for team projects

### Rate Limiting

OpenRouter has rate limits:
- Free tier: 20 requests/minute
- Paid tier: Higher limits

**Best practices**:
- Cache responses when appropriate
- Don't spam consultations
- Use local models for bulk work

---

## Monitoring Costs

### Track Usage

OpenRouter dashboard shows:
- Requests per model
- Cost per request
- Total monthly spend

**Set budgets**:
```
OpenRouter Dashboard → Settings → Budget Alerts
→ Set monthly limit (e.g., $10/month)
→ Get email alert at 80% usage
```

### VOS4 Cost Tracking

Create monthly tracking file:

```bash
# /vos4/docs/Active/MCP-Usage-YYMM.md

## MCP Usage - October 2025

**Total Consultations**: 15
**Cost Breakdown**:
- GPT-5: 3 consultations @ $0.30 = $0.90
- Gemini 2.5 Pro: 8 consultations @ $0.15 = $1.20
- Claude Haiku: 4 consultations @ $0.05 = $0.20

**Total**: $2.30

**Budget**: $10/month
**Remaining**: $7.70
```

---

## Next Steps

### Immediate (Complete Configuration)

1. **Get OpenRouter API key**: https://openrouter.ai/
2. **Set environment variable**:
   ```bash
   export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"
   ```
3. **Restart Claude Code** (reload session)
4. **Approve MCP server** when prompted

### Short-term (Test & Validate)

1. **Test consultation**:
   ```
   "What models are available via MCP?"
   ```

2. **Try complex query**:
   ```
   "Consult with GPT-5 about Android gesture injection best practices"
   ```

3. **Measure value**:
   - Did external consultation provide value?
   - Was cost justified?
   - Should we use more/less?

### Long-term (Optimize)

1. **Install Ollama** for free local models
2. **Develop consultation patterns** (when to use which model)
3. **Track ROI** (cost vs value)
4. **Refine strategy** based on usage

---

## Reference Documentation

**MCP Protocol**:
- Official docs: https://modelcontextprotocol.io/
- Server implementations: https://github.com/modelcontextprotocol

**OpenRouter**:
- Homepage: https://openrouter.ai/
- API docs: https://openrouter.ai/docs
- Pricing: https://openrouter.ai/models

**Ollama (Local Models)**:
- Homepage: https://ollama.ai/
- Installation: https://ollama.ai/download
- Model library: https://ollama.ai/library

---

## Files Created

**Configuration**:
- `/vos4/.mcp.json` - MCP server configuration

**Documentation**:
- `/vos4/docs/Active/Zen-MCP-Configuration-Guide-251018-1855.md` (this file)

**Next**: Get OpenRouter API key → Set env variable → Test consultation

---

## Status

**Configuration**: ✅ Complete
**API Key**: ⏳ Waiting for user
**Testing**: 📋 Pending (after API key)
**Integration**: ✅ Ready (works with VOS4 subagents)

---

**Quick Setup Summary**:

```bash
# 1. Get API key from https://openrouter.ai/

# 2. Set environment variable
echo 'export OPENROUTER_API_KEY="sk-or-YOUR-KEY-HERE"' >> ~/.zshrc
source ~/.zshrc

# 3. Restart Claude Code

# 4. Approve MCP server when prompted

# 5. Test
# "What models are available via MCP?"
```

That's it! Multi-model consultation ready to use.
