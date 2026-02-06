# AVU DSL Evolution Plan

**Date:** 2026-02-06
**Module:** VoiceOSCore
**Status:** In Progress
**Branch:** `claude/060226-avu-dsl-evolution`

---

## Executive Summary

The AVU DSL Evolution extends the Avanues Universal Format from a wire-level IPC protocol into a full declarative scripting system. Three-layer architecture: Wire Protocol (existing AVUCodec) handles compact message encoding, a new DSL Format defines file-based workflows and plugins, and a new Runtime Interpreter executes DSL programs in a sandboxed environment.

Plugins are `.avp` text files - declarative, not compiled. App Store compliant (no DexClassLoader/dynamic code loading). All new code is KMP commonMain under `com.augmentalis.voiceoscore.dsl`.

---

## Three-Layer Architecture

```
+-----------------------------------------------------------+
| Layer 3: AVU Runtime (Interpreter)         <- NEW          |
|   Tree-walking evaluator, sandbox, IAvuDispatcher          |
|   Package: com.augmentalis.voiceoscore.dsl.interpreter     |
+-----------------------------------------------------------+
| Layer 2: AVU DSL (File Format v2.2)        <- NEW          |
|   Lexer, Parser, AST, .vos/.avp files                     |
|   Package: com.augmentalis.voiceoscore.dsl                 |
+-----------------------------------------------------------+
| Layer 1: AVU Wire Protocol (IPC v2.2)      <- EXISTING     |
|   CODE:field1:field2 encoding, AvuEscape, AvuCodeRegistry  |
|   Package: com.augmentalis.avucodec.core                   |
+-----------------------------------------------------------+
```

---

## EBNF Grammar

```ebnf
file            = header body ;
header          = "---" NEWLINE header_line* "---" NEWLINE ;
body            = declaration* ;

declaration     = workflow | function_def | trigger_handler ;
workflow        = "@workflow" STRING NEWLINE INDENT statement* DEDENT ;
function_def    = "@define" IDENT "(" params ")" NEWLINE INDENT statement* DEDENT ;
trigger_handler = "@on" STRING NEWLINE INDENT statement* DEDENT ;
params          = [ IDENT ( "," IDENT )* ] ;

statement       = code_invocation | function_call | if_else | wait_stmt
                | repeat_stmt | while_stmt | sequence | assignment
                | log_stmt | return_stmt | emit_stmt ;
code_invocation = CODE_NAME "(" named_args ")" ;
function_call   = IDENT "(" named_args ")" ;
named_args      = [ named_arg ( "," named_arg )* ] ;
named_arg       = [ IDENT ":" ] expression ;
if_else         = "@if" expression NEWLINE INDENT statement* DEDENT
                  [ "@else" NEWLINE INDENT statement* DEDENT ] ;
wait_stmt       = "@wait" expression [ "timeout" expression ] ;
repeat_stmt     = "@repeat" expression NEWLINE INDENT statement* DEDENT ;
while_stmt      = "@while" expression NEWLINE INDENT statement* DEDENT ;
sequence        = "@sequence" NEWLINE INDENT statement* DEDENT ;
assignment      = "@set" IDENT "=" expression ;
log_stmt        = "@log" expression ;
return_stmt     = "@return" [ expression ] ;
emit_stmt       = "@emit" STRING [ expression ] ;

expression      = or_expr ;
or_expr         = and_expr ( "or" and_expr )* ;
and_expr        = equality ( "and" equality )* ;
equality        = comparison ( ( "==" | "!=" ) comparison )* ;
comparison      = addition ( ( "<" | ">" | "<=" | ">=" ) addition )* ;
addition        = multiply ( ( "+" | "-" ) multiply )* ;
multiply        = unary ( ( "*" | "/" ) unary )* ;
unary           = ( "not" | "-" ) unary | postfix ;
postfix         = primary ( "." IDENT | "(" expr_list ")" )* ;
primary         = INT | FLOAT | STRING | BOOL | "$" IDENT | IDENT | "(" expression ")" ;

CODE_NAME       = UPPER UPPER UPPER ;
```

---

## AST Node Hierarchy

| Category | Types | Count |
|----------|-------|-------|
| Declarations | Workflow, FunctionDef, TriggerHandler | 3 |
| Statements | CodeInvocation, FunctionCall, WaitDelay, WaitCondition, IfElse, Repeat, While, Sequence, Assignment, Log, Return, Emit | 12 |
| Expressions | StringLiteral, IntLiteral, FloatLiteral, BooleanLiteral, VariableRef, Identifier, BinaryOp, UnaryOp, MemberAccess, CallExpression, Grouped | 11 |

---

## File Format

| Extension | Type | Purpose |
|-----------|------|---------|
| `.vos` | workflow | System workflows, automations |
| `.avp` | plugin | Distributable plugin packages |

### Header Specification

```yaml
---
schema: avu-2.2
version: 1.0.0
type: workflow | plugin
metadata:
  name: Plugin Name
  plugin_id: com.augmentalis.xxx
  min_vos_version: 40100
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
permissions:
  GESTURES
  APPS
triggers:
  login
  call {contact}
---
```

---

## Plugin Lifecycle

```
DISCOVERY -> VALIDATION -> PERMISSION GRANT -> REGISTRATION -> ACTIVATION -> DEACTIVATION
```

| Phase | Description |
|-------|-------------|
| DISCOVERY | .avp files found in plugin directories or marketplace |
| VALIDATION | Parse header, verify schema, validate codes against registry |
| PERMISSION GRANT | User prompted for declared permissions |
| REGISTRATION | Register codes (namespaced), triggers in DynamicCommandRegistry |
| ACTIVATION | Interpreter started, @on handlers active |
| DEACTIVATION | Handlers detached, codes unregistered, resources released |

---

## Sandbox Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| maxExecutionTimeMs | 10,000 | Maximum wall-clock execution time |
| maxSteps | 1,000 | Maximum interpreter steps |
| maxLoopIterations | 100 | Maximum iterations per loop |
| maxNestingDepth | 10 | Maximum call stack depth |
| maxVariables | 100 | Maximum variables in scope |

---

## Migration: MacroDSL.kt to AVU DSL

| MacroDSL.kt | AVU DSL |
|-------------|---------|
| `macro("Name") { }` | `@workflow "Name"` |
| `step { tap("btn") }` | `AAC(action: "CLICK", target: "btn")` |
| `delay(1000)` | `@wait 1000` |
| `conditional { then { } otherwise { } }` | `@if condition ... @else ...` |
| `loop(5) { }` | `@repeat 5` |
| `loopWhile { condition = ...; maxIterations = 100 }` | `@while condition` |
| `waitFor { condition = ...; timeout = 5000 }` | `@wait condition timeout 5000` |
| `variable("name", value)` | `@set name = value` |

---

## Implementation Phases

| Phase | Priority | Description |
|-------|----------|-------------|
| 1. Parser & AST | CRITICAL | Lexer, parser, AST in `commonMain/dsl/` |
| 2. Interpreter | CRITICAL | Tree-walking evaluator with sandbox |
| 3. Code Registry Extension | HIGH | Namespace support, CodePermissionMap |
| 4. Plugin Loader | HIGH | PluginLoader, PluginRegistry, PluginSandbox |
| 5. Migration Utilities | MEDIUM | MacroDslMigrator |
| 6. Platform Dispatchers | MEDIUM | iOS & Desktop IAvuDispatcher implementations |
| 7. UI & Tooling | LOW | DSL editor, plugin marketplace |

---

## Existing Code Relationships

| Component | Action | Reason |
|-----------|--------|--------|
| `AvuEscape.kt` | **KEEP** | Foundation for field encoding |
| `AvuCodeRegistry.kt` | **EXTEND** | Add namespace support |
| `AvuHeader.kt` | **EXTENDED** | Generic sections map for permissions/triggers |
| `AvuProtocol.kt` | **KEEP** | Wire-level encoder/decoder |
| `MacroDSL.kt` | **SUPERSEDE** | Replaced by AVU DSL text format |
| `PluginManager.kt` | **DEPRECATE** | DexClassLoader replaced by .avp text plugins |
| `HandlerRegistry.kt` | **KEEP** | Dispatcher delegates to handlers |
| `DynamicCommandRegistry.kt` | **KEEP** | Plugin triggers register into it |

---

## Related Documents

- Ch37: Universal Format v2.0
- Ch51: 3Letter JSON Schema
- Ch67: Avanues Plugin Development (SUPERSEDED by Ch86)
- Ch68: Workflow Engine Architecture (SUPERSEDED by Ch85)
- Ch76: RPC Module Architecture
- Ch80: AVU Codec v2.2 (Foundation)
- Ch81-87: New AVU DSL Developer Manual chapters

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| V1 | 2026-02-06 | Initial comprehensive plan |
