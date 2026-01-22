# TVM v0.22.0 Patches

This folder contains patched TVM Java files needed for compatibility with TVM v0.22.0 runtime.

## Module.java Patch

**Issue:** TVM v0.22.0 changed the FFI API - `ffi.ModuleLoadFromFile` now takes only 1 argument (path), but the original tvm4j code passed 2 arguments (path + format).

**Error:** `TypeError: Mismatched number of arguments... Expected 1 but got 2`

**Fix:** Modified `Module.java` to pass only the path argument.

## How to Rebuild tvm4j_core.jar

If you need to rebuild the JAR (e.g., after updating TVM version):

```bash
# 1. Copy patched file to TVM source
cp Module.java external/mlc-llm/3rdparty/tvm/jvm/core/src/main/java/org/apache/tvm/

# 2. Build the JAR
cd external/mlc-llm/3rdparty/tvm/jvm/core
mvn package -DskipTests -Dcheckstyle.skip=true

# 3. Copy to libs folder
cp target/tvm4j-core-0.0.1-SNAPSHOT.jar \
   ../../../../../../Universal/AVA/Features/LLM/libs/tvm4j_core.jar
```

## Files

| File | Description |
|------|-------------|
| `Module.java` | Patched for TVM v0.22.0 FFI (1-arg ModuleLoadFromFile) |

## Version Compatibility

| tvm4j_core.jar | TVM Runtime | Status |
|----------------|-------------|--------|
| Current (patched) | v0.22.0 | Working |
| Original | v0.22.0 | Broken (FFI mismatch) |
