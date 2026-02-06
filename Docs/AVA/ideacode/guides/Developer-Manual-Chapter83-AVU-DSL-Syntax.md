# Developer Manual - Chapter 83: AVU DSL Syntax

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

Layer 2 of the AVU three-layer architecture. The DSL format defines `.vos` (workflow) and `.avp` (plugin) files with a YAML-like header between `---` markers and an indentation-based body. This chapter covers the complete grammar, all directives, expression rules, and file format conventions.

---

## File Types

| Extension | Type | Purpose |
|-----------|------|---------|
| `.vos` | workflow | System workflows, automations |
| `.avp` | plugin | Distributable plugin packages |

---

## File Structure

Every AVU DSL file has two sections:

```
[Header]   ---  YAML-like key:value pairs  ---
[Body]     Top-level declarations: @workflow, @define, @on
```

### Minimal Example

```
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "Hello World"
  @log "Hello from AVU DSL!"
```

---

## Header Specification

The header section sits between `---` markers. Parsed by `AvuHeader.parse()` from AVUCodec. Core fields have dedicated properties; everything else goes into the generic `sections` map.

### Core Fields

| Field | Required | Description |
|-------|----------|-------------|
| `schema` | Yes | Format version, always `avu-2.2` |
| `version` | Yes | Document version (semver) |
| `type` | Yes | `workflow` or `plugin` |
| `project` | No | Project name |

### Section Fields

| Section | Description |
|---------|-------------|
| `metadata:` | Key-value pairs (name, plugin_id, min_vos_version, etc.) |
| `codes:` | Wire protocol codes used: `VCM: Voice Command (id:action:params)` |
| `permissions:` | Required permissions: `GESTURES`, `APPS`, `NOTIFICATIONS`, etc. |
| `triggers:` | Natural language trigger patterns: `open {app}`, `call {contact}` |

### Full Header Example

```yaml
---
schema: avu-2.2
version: 1.0.0
type: plugin
metadata:
  name: Smart Login
  plugin_id: com.augmentalis.smartlogin
  min_vos_version: 40100
  author: Augmentalis
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
  SCR: Screen Read (id:targetAvid:readMode)
permissions:
  GESTURES
  APPS
  SCREEN_READ
triggers:
  login
  login to {app}
  sign in to {app}
---
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

## Declarations

### @workflow

Names a workflow. Each `.vos` file typically has one `@workflow`.

```
@workflow "Daily Standup Opener"
  VCM(id: "cmd1", action: "open", target: "slack")
  @wait 2000
  VCM(id: "cmd2", action: "navigate", target: "standup-channel")
```

### @define

Declares a reusable function with named parameters.

```
@define login_app(app, username)
  VCM(id: "open", action: "open", target: $app)
  @wait 1500
  AAC(action: "SET_TEXT", target: "username_field", text: $username)
  AAC(action: "CLICK", target: "login_btn")
```

### @on

Declares a trigger handler. Captures `{variable}` patterns from natural language.

```
@on "login to {app}"
  login_app(app: $app, username: "default_user")

@on "scroll {direction} in {app}"
  VCM(id: "scroll", action: "scroll", direction: $direction, target: $app)
```

---

## Statements

### Code Invocation

Dispatches a 3-letter wire protocol code with named or positional arguments.

```
VCM(id: "cmd_01", action: "SCROLL_DOWN", target: "chrome")
AAC(action: "CLICK", target: "login_btn")
SCR(target: "status_bar", mode: "FULL")
```

### Function Call

Calls a `@define`d function.

```
login_app(app: "slack", username: "john")
scroll_and_wait(direction: "down", delay: 500)
```

### @if / @else

Conditional branching based on expressions.

```
@if $retryCount < 3
  VCM(id: "retry", action: "refresh")
  @set retryCount = $retryCount + 1
@else
  @log "Max retries reached"
  @emit "error" "too_many_retries"
```

### @wait

Two forms: delay (milliseconds) and conditional (with timeout).

```
@wait 1500
@wait screen.contains("Welcome") timeout 5000
```

### @repeat

Fixed-count loop.

```
@repeat 3
  VCM(id: "scroll", action: "SCROLL_DOWN")
  @wait 500
```

### @while

Condition-based loop.

```
@while not screen.contains("End of list")
  VCM(id: "scroll", action: "SCROLL_DOWN")
  @wait 300
```

### @sequence

Explicit sequential grouping.

```
@sequence
  AAC(action: "CLICK", target: "menu_btn")
  @wait 500
  AAC(action: "CLICK", target: "settings_option")
  @wait 500
  AAC(action: "CLICK", target: "dark_mode_toggle")
```

### @set

Variable assignment.

```
@set counter = 0
@set greeting = "Hello, " + $username
@set isReady = $count > 0 and $connected
```

### @log

Debug logging.

```
@log "Starting workflow"
@log "Counter is: " + $counter
```

### @return

Early exit from a `@define` function, optionally returning a value.

```
@define check_login()
  @if not $connected
    @return false
  AAC(action: "CLICK", target: "login_btn")
  @return true
```

### @emit

Emits a named event with optional data payload.

```
@emit "workflow_started"
@emit "login_complete" $username
```

---

## Expressions

### Operator Precedence (low to high)

| Level | Operators | Associativity |
|-------|-----------|---------------|
| 1 | `or` | Left |
| 2 | `and` | Left |
| 3 | `==` `!=` | Left |
| 4 | `<` `>` `<=` `>=` | Left |
| 5 | `+` `-` | Left |
| 6 | `*` `/` | Left |
| 7 | `not` `-` (negate) | Right (unary) |
| 8 | `.member` `(args)` | Left (postfix) |

### Literals

```
42                # IntLiteral
3.14              # FloatLiteral
"hello world"     # StringLiteral (double quotes)
'hello world'     # StringLiteral (single quotes)
true              # BooleanLiteral
false             # BooleanLiteral
```

### Variable References

```
$username         # reads variable 'username' from scope
$retryCount       # reads variable 'retryCount'
```

### Member Access and Method Calls

```
screen.contains("text")      # member access + call
$result.length               # member access
len($items)                  # built-in function call
```

### Composite Expressions

```
$count > 0 and $connected == true
not screen.contains("error") or $retryCount >= 3
($a + $b) * 2
"Hello, " + $name
```

---

## Indentation Rules

- The body of `@workflow`, `@define`, `@on`, `@if`, `@else`, `@repeat`, `@while`, `@sequence` must be indented deeper than the directive
- Consistent indentation within a block (spaces or tabs; tabs count as 4 spaces)
- Blank lines and comment-only lines are ignored for indentation purposes
- The lexer emits synthetic `INDENT`/`DEDENT` tokens, keeping the parser grammar regular

### Indentation Example

```
@workflow "Nested Example"         # indent level 0
  @if $condition                   # indent level 1 (body of workflow)
    @repeat 3                      # indent level 2 (body of @if)
      VCM(id: "x", action: "y")   # indent level 3 (body of @repeat)
      @wait 500                    # indent level 3
    @log "done repeating"          # indent level 2 (still in @if)
  @else                            # indent level 1 (sibling of @if)
    @log "condition was false"     # indent level 2 (body of @else)
```

---

## Comments

Line comments start with `#`. No block comments.

```
# This is a comment
@workflow "Example"
  VCM(id: "cmd1", action: "tap")  # inline comment
```

---

## String Escapes

| Escape | Character |
|--------|-----------|
| `\\` | Backslash |
| `\"` | Double quote |
| `\'` | Single quote |
| `\n` | Newline |
| `\r` | Carriage return |
| `\t` | Tab |

---

## Complete Plugin Example

```
---
schema: avu-2.2
version: 1.0.0
type: plugin
metadata:
  name: Smart Login
  plugin_id: com.augmentalis.smartlogin
  min_vos_version: 40100
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
permissions:
  GESTURES
  APPS
triggers:
  login
  login to {app}
---

@define do_login(app, username, password)
  VCM(id: "open", action: "launch", target: $app)
  @wait 2000
  @if screen.contains("username")
    AAC(action: "SET_TEXT", target: "username_field", text: $username)
    AAC(action: "SET_TEXT", target: "password_field", text: $password)
    AAC(action: "CLICK", target: "submit_btn")
    @wait screen.contains("Welcome") timeout 5000
    @log "Login successful for " + $app
  @else
    @log "Login screen not found"
    @emit "login_failed" $app

@on "login"
  do_login(app: "default", username: "user", password: "pass")

@on "login to {app}"
  do_login(app: $app, username: "user", password: "pass")
```

---

## Parser Pipeline

```
Source Text
    |
    v
[AvuDslLexer]  -->  List<Token>  (HEADER_LINE, INDENT, DEDENT, CODE_NAME, ...)
    |
    v
[AvuDslParser]  -->  AvuDslFile(header, declarations)
    |                    |
    |                    +-- AvuDslHeader (schema, version, type, codes, permissions, triggers)
    |                    +-- List<Declaration> (Workflow, FunctionDef, TriggerHandler)
    v
[Future: AvuInterpreter]  -->  Execution with sandbox
```

### Usage

```kotlin
import com.augmentalis.voiceoscore.dsl.lexer.AvuDslLexer
import com.augmentalis.voiceoscore.dsl.parser.AvuDslParser

val source = File("workflow.vos").readText()
val tokens = AvuDslLexer(source).tokenize()
val result = AvuDslParser(tokens).parse()

if (result.hasErrors) {
    result.errors.forEach { println(it) }
} else {
    val ast = result.file
    println("Parsed ${ast.declarations.size} declarations")
}
```

---

## Best Practices

1. One `@workflow` per `.vos` file; multiple `@define` and `@on` are fine
2. Use `@define` to factor out repeated step sequences
3. Always declare `codes:` in the header for documentation
4. Use `@emit` for inter-plugin communication instead of direct code invocations
5. Keep nesting depth under 4 levels for readability
6. Use descriptive trigger patterns in `@on` handlers

---

## Related Documents

- [Ch37: Universal Format v2.0](Developer-Manual-Chapter37-Universal-Format-v2.0.md)
- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)
- [Ch82: AVU Wire Protocol](Developer-Manual-Chapter82-AVU-Wire-Protocol.md)
- [Ch84: AVU Code Registry](Developer-Manual-Chapter84-AVU-Code-Registry.md)
- [Ch85: AVU Runtime Interpreter](Developer-Manual-Chapter85-AVU-Runtime-Interpreter.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
