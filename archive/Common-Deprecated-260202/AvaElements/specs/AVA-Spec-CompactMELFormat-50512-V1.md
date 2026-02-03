# Compact MEL Format Specification

**Version:** 1.0.0 | **Date:** 2025-12-05 | **Status:** Draft

---

## Overview

Compact MEL combines the **Compact DSL syntax** with **MEL state/reducers** to create a minimal, iOS-compatible plugin format.

---

## Format Comparison

| Aspect | Verbose YAML | Compact MEL |
|--------|--------------|-------------|
| File Extension | `.yaml` | `.mel` or `.cmel` |
| Size Reduction | Baseline | -50% to -70% |
| Human Readable | Yes | Yes (learned) |
| iOS Compatible | Yes | Yes |
| Parser Complexity | YAML + custom | Custom only |

---

## Syntax

### 1. Header Block (YAML-like)

```
@mel/1.0
id:com.augmentalis.calculator
name:Calculator
tier:data

state:
  count:0
  display:"0"
  operator:null
```

### 2. Reducers Block

```
reducers:
  increment: count=$add($count,1)
  decrement: count=$sub($count,1)
  setOp(op): operator=$op; buffer=$display; display="0"
  calculate: display=$if($eq($operator,"+"),$add($buffer,$display),$display)
```

### 3. UI Block (Compact DSL)

```
ui:Col#main{@p(16);
  Text{text:$display;fontSize:48};
  Row{@gap(8);
    Btn{label:"+";@tap->setOp("+")};
    Btn{label:"-";@tap->setOp("-")};
    Btn{label:"=";@tap->calculate}
  }
}
```

---

## Complete Example: Counter (iOS Compatible)

```
@mel/1.0
id:com.augmentalis.counter
tier:data

state:
  count:0

reducers:
  increment: count=$add($count,1)
  decrement: count=$sub($count,1)
  reset: count=0
  addAmount(n): count=$add($count,$n)

ui:Col#main{@p(24),alignCenter;
  Text{text:"Counter";fontSize:32;fontWeight:"bold"};
  Box{@p(32),bg(#f0f0f0),r(16);
    Text{text:$str($count);fontSize:72;fontWeight:"bold";
      color:$if($lt($count,0),#cc0000,$if($gt($count,0),#00cc00,#666666))}
  };
  Row{@gap(12);
    Btn{label:"-";@tap->decrement;@f(80,80);fontSize:36};
    Btn{label:"Reset";@tap->reset;variant:"secondary"};
    Btn{label:"+";@tap->increment;@f(80,80);fontSize:36}
  }
}
```

---

## Complete Example: Calculator (iOS Compatible)

```
@mel/1.0
id:com.augmentalis.calculator
tier:data

state:
  display:"0"
  buffer:""
  operator:null

reducers:
  digit(d): display=$if($eq($display,"0"),$str($d),$cat($display,$str($d)))
  decimal: display=$if($has($display,"."),$display,$cat($display,"."))
  op(o): buffer=$display; operator=$o; display="0"
  calc: display=$str($switch($operator,
    "+":$add($num($buffer),$num($display)),
    "-":$sub($num($buffer),$num($display)),
    "*":$mul($num($buffer),$num($display)),
    "/":$div($num($buffer),$num($display)),
    $display
  )); buffer=""; operator=null
  clear: display="0"; buffer=""; operator=null
  back: display=$if($gt($len($display),1),$slice($display,0,-1),"0")

ui:Col{@p(16),gap(12);
  Box{@p(16),bg(#f5f5f5),r(8);
    Text{text:$display;fontSize:48;textAlign:"end";fontFamily:"mono"}
  };
  Div{};
  Row{@gap(8);Btn{label:"7";@tap->digit(7);flex:1};Btn{label:"8";@tap->digit(8);flex:1};Btn{label:"9";@tap->digit(9);flex:1};Btn{label:"/";@tap->op("/");variant:"secondary";flex:1}};
  Row{@gap(8);Btn{label:"4";@tap->digit(4);flex:1};Btn{label:"5";@tap->digit(5);flex:1};Btn{label:"6";@tap->digit(6);flex:1};Btn{label:"*";@tap->op("*");variant:"secondary";flex:1}};
  Row{@gap(8);Btn{label:"1";@tap->digit(1);flex:1};Btn{label:"2";@tap->digit(2);flex:1};Btn{label:"3";@tap->digit(3);flex:1};Btn{label:"-";@tap->op("-");variant:"secondary";flex:1}};
  Row{@gap(8);Btn{label:"C";@tap->clear;variant:"danger";flex:1};Btn{label:"0";@tap->digit(0);flex:1};Btn{label:".";@tap->decimal;flex:1};Btn{label:"+";@tap->op("+");variant:"secondary";flex:1}};
  Row{@gap(8);Btn{label:"=";@tap->calc;variant:"primary";flex:1}}
}
```

---

## iOS Tier 1 Function Reference

### Shorthand Syntax

| Shorthand | Full Form | Description |
|-----------|-----------|-------------|
| `$add(a,b)` | `$math.add` | Addition |
| `$sub(a,b)` | `$math.subtract` | Subtraction |
| `$mul(a,b)` | `$math.multiply` | Multiplication |
| `$div(a,b)` | `$math.divide` | Division |
| `$num(s)` | `$math.parse` | Parse string to number |
| `$abs(n)` | `$math.abs` | Absolute value |
| `$round(n)` | `$math.round` | Round |
| `$floor(n)` | `$math.floor` | Floor |
| `$ceil(n)` | `$math.ceil` | Ceiling |
| `$if(c,t,f)` | `$logic.if` | Conditional |
| `$eq(a,b)` | `$logic.equals` | Equals |
| `$ne(a,b)` | `$logic.notEquals` | Not equals |
| `$gt(a,b)` | `$logic.gt` | Greater than |
| `$lt(a,b)` | `$logic.lt` | Less than |
| `$gte(a,b)` | `$logic.gte` | Greater or equal |
| `$lte(a,b)` | `$logic.lte` | Less or equal |
| `$and(a,b)` | `$logic.and` | Logical AND |
| `$or(a,b)` | `$logic.or` | Logical OR |
| `$not(a)` | `$logic.not` | Logical NOT |
| `$cat(a,b)` | `$string.concat` | Concatenate |
| `$str(v)` | `$string.toString` | To string |
| `$len(s)` | `$string.length` | String length |
| `$slice(s,i,j)` | `$string.substring` | Substring |
| `$has(s,v)` | `$string.contains` | Contains |
| `$upper(s)` | `$string.upper` | Uppercase |
| `$lower(s)` | `$string.lower` | Lowercase |
| `$trim(s)` | `$string.trim` | Trim whitespace |
| `$switch(v,...)` | Pattern match | Switch/case expression |

### State References

| Syntax | Meaning |
|--------|---------|
| `$count` | Read `state.count` |
| `$state.count` | Full path (optional) |
| `$display` | Read `state.display` |

### Reducer Assignments

```
# Simple assignment
count=0

# Expression assignment
count=$add($count,1)

# Multiple assignments (semicolon separated)
display="0"; buffer=""; operator=null

# Conditional assignment
color=$if($lt($count,0),#cc0000,#00cc00)
```

---

## iOS Logic Patterns

### Pattern 1: Simple State Toggle
```
reducers:
  toggle: active=$not($active)
```

### Pattern 2: Increment/Decrement with Bounds
```
reducers:
  increment: count=$if($lt($count,$max),$add($count,1),$count)
  decrement: count=$if($gt($count,$min),$sub($count,1),$count)
```

### Pattern 3: Multi-Case Switch
```
reducers:
  calculate: result=$switch($operator,
    "+":$add($a,$b),
    "-":$sub($a,$b),
    "*":$mul($a,$b),
    "/":$div($a,$b),
    0
  )
```

### Pattern 4: Conditional Styling
```
ui:Text{
  text:$str($value);
  color:$if($lt($value,0),#FF0000,$if($gt($value,0),#00FF00,#888888))
}
```

### Pattern 5: Dynamic Labels
```
ui:Btn{
  label:$if($loading,"Loading...",$if($success,"Done!","Submit"));
  enabled:$not($loading)
}
```

### Pattern 6: Array Operations
```
state:
  items:[]

reducers:
  addItem(item): items=$push($items,$item)
  removeItem(i): items=$splice($items,$i,1)
  clearAll: items=[]
```

### Pattern 7: Validated Input
```
reducers:
  setEmail(v):
    email=$v;
    emailValid=$has($v,"@")

ui:Field{
  value:$email;
  @change->setEmail;
  error:$if($not($emailValid),"Invalid email",null)
}
```

---

## Modifier Reference

| Modifier | Arguments | Example |
|----------|-----------|---------|
| `@p(n)` | Padding (1 or 4 values) | `@p(16)`, `@p(16,8,16,8)` |
| `@m(n)` | Margin | `@m(8)` |
| `@gap(n)` | Gap between children | `@gap(12)` |
| `@f(w,h)` | Frame (width, height) | `@f(100,50)` |
| `@bg(c)` | Background color | `@bg(#FFFFFF)` |
| `@r(n)` | Border radius | `@r(8)` |
| `@op(n)` | Opacity (0-1) | `@op(0.5)` |
| `@tap->` | Tap handler | `@tap->increment` |
| `@change->` | Change handler | `@change->setValue` |
| `@submit->` | Submit handler | `@submit->handleSubmit` |

---

## Parser Implementation

The Compact MEL parser needs to:

1. **Parse Header** - Extract metadata, tier, state
2. **Parse Reducers** - Build reducer map with expressions
3. **Parse UI** - Convert compact DSL to UINode tree
4. **Resolve Shorthands** - Expand `$add` to `$math.add`
5. **Validate Tier** - Ensure all functions are tier-compatible

### Entry Points

```kotlin
// Load from string
val runtime = CompactMELParser.parse(content, platform)

// Load from file
val runtime = PluginLoader().loadCompactMEL(path, platform)
```

---

## Migration from Verbose YAML

| Verbose YAML | Compact MEL |
|--------------|-------------|
| `$math.add($state.count, 1)` | `$add($count,1)` |
| `$logic.if($logic.equals(...))` | `$if($eq(...))` |
| `$string.concat("", $state.x)` | `$str($x)` |
| `next_state:` block | Inline assignments |
| `style:` nested block | Inline or `@modifier` |
| `children:` array | Direct nesting |

---

## File Size Comparison

### Counter Example

| Format | Size | Reduction |
|--------|------|-----------|
| Verbose YAML | 1,672 bytes | Baseline |
| Compact MEL | 486 bytes | **71%** |

### Calculator Example

| Format | Size | Reduction |
|--------|------|-----------|
| Verbose YAML | 2,890 bytes | Baseline |
| Compact MEL | 892 bytes | **69%** |

---

## Appendix: Grammar (EBNF)

```ebnf
CompactMEL ::= Header State Reducers UI

Header ::= '@mel/' Version EOL Metadata*
Metadata ::= Key ':' Value EOL

State ::= 'state:' EOL StateEntry+
StateEntry ::= Indent Key ':' Literal EOL

Reducers ::= 'reducers:' EOL ReducerEntry+
ReducerEntry ::= Indent Name ParamList? ':' Assignment+ EOL
ParamList ::= '(' Name (',' Name)* ')'
Assignment ::= Key '=' Expression (';' Assignment)*

UI ::= 'ui:' Component
Component ::= TypeAlias ID? '{' Body '}'
Body ::= (Property | Modifier | Callback | Component)*
Property ::= Key ':' (Literal | Expression) ';'?
Modifier ::= '@' ModName '(' Args ')' ','?
Callback ::= '@' Event '->' Handler ';'?

Expression ::= '$' FunctionCall | '$' VarRef | Literal
FunctionCall ::= Name '(' (Expression (',' Expression)*)? ')'
VarRef ::= Name ('.' Name)*
```

---

**Author:** IDEACODE v10.2
**License:** Proprietary - Augmentalis ES
