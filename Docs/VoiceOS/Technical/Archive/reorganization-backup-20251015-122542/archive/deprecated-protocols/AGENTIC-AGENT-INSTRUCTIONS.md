# ⚠️ DEPRECATED - This file has been superseded

**Status:** DEPRECATED as of 2025-10-15
**New Location:** `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-AGENT-PROTOCOL.md`
**Reason:** Consolidated into VOS4-AGENT-PROTOCOL.md
**Archived By:** Documentation Consolidation Agent

This file is kept for historical reference only. DO NOT use for new development.

---

[Original content below]

<!--
filename: AGENTIC-AGENT-INSTRUCTIONS.md
created: 2025-01-25 00:00:00 PST
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: MANDATORY instructions for agentic agents using Task tool
last-modified: 2025-01-25 00:00:00 PST
version: 1.0.0
-->

# 🚨 MANDATORY: Agentic Agent Instructions

## Core Principle
**ALL agentic agents MUST follow these instructions when using the Task tool to spawn specialized agents.**

## When to Use Agentic Agents (MANDATORY)

### ALWAYS use agentic agents for:
1. **Complex multi-step research** requiring extensive file searching
2. **Cross-module analysis** spanning multiple directories
3. **Optimization opportunities** requiring deep code analysis
4. **Architecture decisions** needing comprehensive understanding
5. **Refactoring tasks** requiring complete feature mapping

### Required Agent Types by Task

#### 🔍 Research & Analysis Agent
**When to Deploy:**
- Searching for specific implementations across codebase
- Understanding module interactions
- Analyzing code patterns and dependencies
- Finding duplicate functionality
- Mapping feature implementations

**Agent Instructions:**
```
Deploy general-purpose agent with instructions to:
1. Search comprehensively across all relevant directories
2. Read and analyze all matching files
3. Create detailed mapping of findings
4. Identify patterns and relationships
5. Return structured analysis report
```

#### 🏗️ Architecture Review Agent
**When to Deploy:**
- Evaluating system architecture
- Planning module refactoring
- Assessing technical debt
- Reviewing SOLID compliance
- Analyzing performance bottlenecks

**Agent Instructions:**
```
Deploy general-purpose agent specialized in architecture to:
1. Analyze current architecture against VOS4 standards
2. Identify violations of direct implementation pattern
3. Find unnecessary abstractions and interfaces
4. Map module dependencies
5. Recommend architecture improvements
```

#### 📊 Optimization Hunter Agent
**When to Deploy:**
- Finding performance optimization opportunities
- Identifying memory reduction possibilities
- Discovering redundant code
- Locating inefficient algorithms
- Finding unnecessary object allocations

**Agent Instructions:**
```
Deploy general-purpose agent focused on optimization to:
1. Profile code for performance bottlenecks
2. Identify memory-intensive operations
3. Find redundant calculations
4. Locate inefficient data structures
5. Return prioritized optimization list with impact estimates
```

#### 🔄 Refactoring Specialist Agent
**When to Deploy:**
- Implementing SRP (Single Responsibility Principle)
- Breaking down large classes
- Extracting common functionality
- Eliminating code duplication
- Modernizing legacy code

**Agent Instructions:**
```
Deploy general-purpose agent for refactoring to:
1. Map all current functionality (100% feature preservation)
2. Identify responsibility violations
3. Plan refactoring without feature loss
4. Ensure backward compatibility
5. Return detailed refactoring plan
```

#### 📝 Documentation Mapper Agent
**When to Deploy:**
- Creating comprehensive documentation
- Mapping all module features
- Building architecture diagrams
- Documenting API surfaces
- Creating migration guides

**Agent Instructions:**
```
Deploy general-purpose agent for documentation to:
1. Map all classes, methods, and functions
2. Document public APIs
3. Create flow diagrams
4. Build dependency maps
5. Return complete documentation structure
```

## MANDATORY Agent Deployment Protocol

### Step 1: Task Assessment
```python
# Evaluate if task requires agentic approach
if task.requires_extensive_search or task.is_complex_analysis:
    use_agentic_agent = True
```

### Step 2: Agent Configuration
```python
agent_config = {
    "subagent_type": "general-purpose",
    "description": "Brief 3-5 word description",
    "prompt": """
    MANDATORY: Follow VOS4 standards from /Agent-Instructions/MASTER-STANDARDS.md
    
    Task: [Specific detailed task]
    
    Requirements:
    1. Search comprehensively
    2. Analyze thoroughly  
    3. Return structured results
    4. Include specific examples
    5. Provide actionable recommendations
    
    Output Format:
    - Summary of findings
    - Detailed analysis
    - Specific file references (path:line_number)
    - Recommendations with priority
    """
}
```

### Step 3: Result Processing
```python
# Process agent results
results = agent.execute()
# Always validate results against VOS4 standards
# Present findings to user with clear recommendations
```

## Example Agent Deployments

### Example 1: Finding All Command Processors
```
Task tool deployment:
- subagent_type: "general-purpose"
- description: "Find command processors"
- prompt: "Search entire VOS4 codebase for all command processing implementations. Include:
  1. All files containing command processing logic
  2. Different approaches used (direct, delegated, etc.)
  3. Module locations
  4. Duplication analysis
  Return structured report with file:line references"
```

### Example 2: Architecture Compliance Check
```
Task tool deployment:
- subagent_type: "general-purpose"  
- description: "Check architecture compliance"
- prompt: "Analyze VoiceAccessibility module for VOS4 architecture compliance:
  1. Find all interface usage (should be zero)
  2. Check namespace compliance (com.augmentalis.*)
  3. Verify direct implementation pattern
  4. Identify abstraction violations
  Return violations with specific locations and fix recommendations"
```

### Example 3: Performance Optimization Hunt
```
Task tool deployment:
- subagent_type: "general-purpose"
- description: "Find optimization opportunities"
- prompt: "Analyze CommandManager for performance optimizations:
  1. Find all HashMap/ConcurrentHashMap usage (replace with ArrayMap)
  2. Identify blocking I/O operations
  3. Locate unnecessary object allocations
  4. Find redundant calculations
  Return prioritized list with effort estimates and impact analysis"
```

## MANDATORY Rules for Agentic Agents

### ALWAYS:
- ✅ Deploy agents for complex multi-file searches
- ✅ Use agents for comprehensive analysis tasks
- ✅ Provide detailed, specific prompts
- ✅ Include VOS4 standards reference in prompt
- ✅ Request structured output format
- ✅ Specify file:line_number references

### NEVER:
- ❌ Use agents for simple single-file reads
- ❌ Deploy without specific search criteria
- ❌ Accept vague or incomplete results
- ❌ Skip validation of agent findings
- ❌ Proceed without user approval of findings

## Quality Assurance Checklist

Before deploying agent:
- [ ] Task requires multi-file analysis?
- [ ] Clear search criteria defined?
- [ ] Output format specified?
- [ ] VOS4 standards included?
- [ ] Success criteria defined?

After agent returns:
- [ ] Results comprehensive?
- [ ] File references specific?
- [ ] Findings validated?
- [ ] Recommendations clear?
- [ ] User approval obtained?

## Integration with Review Patterns

### Using CRT with Agentic Agents:
1. **Deploy agent** for comprehensive analysis
2. **Apply COT** to agent findings
3. **Use ROT** to evaluate approach
4. **Apply TOT** for alternative solutions
5. **Present CRT** analysis to user

### Example Combined Flow:
```
User: "Find all duplicate command processing logic"

Step 1: Deploy agentic agent for search
Step 2: Agent returns 5 instances of duplication
Step 3: Apply CRT analysis:
  - COT: Trace through each duplication
  - ROT: Evaluate impact of duplication
  - TOT: Explore consolidation strategies
  - Present options with pros/cons
```

## Escalation Protocol

### When to escalate beyond single agent:
1. Task spans multiple specialized domains
2. Results require expert validation
3. Findings contradict VOS4 standards
4. Implementation requires multiple expertise areas

### Multi-Agent Coordination:
```
Primary Agent: Research and discovery
→ Architecture Agent: Validate design
→ Performance Agent: Assess impact
→ Security Agent: Review implications
→ Final consolidated report
```

---

**REMEMBER:** Agentic agents are MANDATORY for complex research and analysis. Deploy them proactively for comprehensive understanding.

**Author:** Manoj Jhawar
**Enforcement:** Tasks requiring multi-file analysis without agent deployment = incomplete work