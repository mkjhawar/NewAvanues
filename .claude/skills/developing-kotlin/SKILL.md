---
name: developing-kotlin
description: Develops applications using Kotlin language. Use for Kotlin syntax, coroutines, flows, sealed classes, data classes, extension functions, DSLs, and Kotlin idioms.
---

# Kotlin Development

## Language Features

| Feature | Usage |
|---------|-------|
| Data classes | `data class User(val id: String, val name: String)` |
| Sealed classes | `sealed class Result<T>` for exhaustive when |
| Extensions | `fun String.toSlug()` |
| Scope functions | `let`, `run`, `with`, `apply`, `also` |
| Null safety | `?.`, `?:`, `!!`, `requireNotNull()` |

## Coroutines

| Concept | Implementation |
|---------|----------------|
| Suspend | `suspend fun fetch()` |
| Launch | `scope.launch { }` for fire-and-forget |
| Async | `async { }.await()` for parallel |
| Flow | `flow { emit() }` for streams |
| StateFlow | `MutableStateFlow` for state |

## Best Practices

| Practice | Example |
|----------|---------|
| Immutability | `val` over `var` |
| Expression body | `fun sum(a: Int, b: Int) = a + b` |
| Named args | `User(id = "1", name = "John")` |
| Destructuring | `val (id, name) = user` |

## Idioms

| Pattern | Code |
|---------|------|
| Null check | `value?.let { use(it) } ?: default` |
| Filter/Map | `list.filter { }.map { }` |
| When | `when (x) { is Type -> ... }` |
| Lazy | `val x by lazy { compute() }` |

## Quality Gates

| Gate | Target |
|------|--------|
| Kotlin version | 2.0+ |
| Null safety | No `!!` except tests |
| Coroutine context | Explicit dispatchers |
