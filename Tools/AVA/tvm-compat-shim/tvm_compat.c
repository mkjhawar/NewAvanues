/**
 * TVM API Compatibility Shim (v2)
 *
 * Provides TVM symbols by dynamically looking up implementations in the runtime.
 * Required because Android loads JNI libraries with RTLD_LOCAL, making symbols
 * invisible to subsequently dlopen()'d libraries.
 *
 * This shim provides:
 * 1. Old TVM C API -> New FFI API translation
 * 2. Dynamic lookup of TVM backend functions
 *
 * Link this into model .ads shared libraries to resolve all TVM symbols.
 *
 * Author: Manoj Jhawar
 * Date: 2025-12-04
 */

#include <stdint.h>
#include <string.h>
#include <dlfcn.h>
#include <android/log.h>

#define TAG "TVMShim"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* Thread-local error message storage */
static __thread char tvm_last_error[1024] = {0};

/* Function pointer cache for dynamic lookups */
static void* cached_funcs[32] = {0};
static int cache_initialized = 0;

/* Helper to look up a function dynamically */
static void* lookup_func(const char* name) {
    void* func = dlsym(RTLD_DEFAULT, name);
    if (!func) {
        LOGE("Failed to lookup %s: %s", name, dlerror());
    }
    return func;
}

/* Initialize function cache on first call */
static void init_cache(void) {
    if (cache_initialized) return;
    cache_initialized = 1;
    LOGI("TVM compatibility shim v2 initialized");
}

/*
 * ========================================================================
 *  NEW FFI API Forwarders (TVM v0.22.0)
 *  These are the primary API used by the runtime
 * ========================================================================
 */

typedef int (*TVMFFIFunctionCall_t)(void*, void*, int*, int, void*, int*);
typedef void (*TVMFFIErrorSetRaisedFromCStr_t)(const char*);
typedef int (*TVMFFIEnvModRegisterSystemLibSymbol_t)(const char*, void*);

/* TVMFFIFunctionCall - forward to runtime */
int TVMFFIFunctionCall(void* func, void* arg_values, int* type_codes,
                       int num_args, void* ret_val, int* ret_type_code) {
    init_cache();
    static TVMFFIFunctionCall_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMFFIFunctionCall_t)lookup_func("TVMFFIFunctionCall");
        if (!real_func) return -1;
    }
    return real_func(func, arg_values, type_codes, num_args, ret_val, ret_type_code);
}

/* TVMFFIErrorSetRaisedFromCStr - forward to runtime */
void TVMFFIErrorSetRaisedFromCStr(const char* msg) {
    init_cache();
    static TVMFFIErrorSetRaisedFromCStr_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMFFIErrorSetRaisedFromCStr_t)lookup_func("TVMFFIErrorSetRaisedFromCStr");
    }
    if (real_func) real_func(msg);
}

/* TVMFFIEnvModRegisterSystemLibSymbol - forward to runtime */
int TVMFFIEnvModRegisterSystemLibSymbol(const char* name, void* ptr) {
    init_cache();
    static TVMFFIEnvModRegisterSystemLibSymbol_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMFFIEnvModRegisterSystemLibSymbol_t)lookup_func("TVMFFIEnvModRegisterSystemLibSymbol");
        if (!real_func) return -1;
    }
    return real_func(name, ptr);
}

/*
 * ========================================================================
 *  OLD TVM C API Shims (for models compiled with older MLC-LLM)
 * ========================================================================
 */

/* TVMFuncCall - Old C API wrapper */
int TVMFuncCall(void* func, void* arg_values, int* type_codes,
                int num_args, void* ret_val, int* ret_type_code) {
    return TVMFFIFunctionCall(func, arg_values, type_codes, num_args, ret_val, ret_type_code);
}

/* TVMAPISetLastError - Old C API wrapper */
void TVMAPISetLastError(const char* msg) {
    if (msg) {
        strncpy(tvm_last_error, msg, sizeof(tvm_last_error) - 1);
        tvm_last_error[sizeof(tvm_last_error) - 1] = '\0';
    }
    TVMFFIErrorSetRaisedFromCStr(msg);
}

/* TVMBackendRegisterSystemLibSymbol - Old C API wrapper */
int TVMBackendRegisterSystemLibSymbol(const char* name, void* ptr) {
    return TVMFFIEnvModRegisterSystemLibSymbol(name, ptr);
}

/* TVMGetLastError - Old C API */
const char* TVMGetLastError(void) {
    return tvm_last_error;
}

/*
 * ========================================================================
 *  TVM Backend Functions (still used in v0.22.0)
 *  These are called by compiled model code
 * ========================================================================
 */

typedef int (*TVMBackendGetFuncFromEnv_t)(void*, const char*, void*);
typedef void* (*TVMBackendAllocWorkspace_t)(int, int, uint64_t, int, int);
typedef int (*TVMBackendFreeWorkspace_t)(int, int, void*);
typedef int (*TVMBackendParallelLaunch_t)(void*, void*, int);
typedef int (*TVMBackendParallelBarrier_t)(int, void*);
typedef int (*TVMBackendRunOnce_t)(void**, int (*)(void*), void*, int);

/* TVMBackendGetFuncFromEnv - forward to runtime */
int TVMBackendGetFuncFromEnv(void* mod_node, const char* func_name, void* out) {
    init_cache();
    static TVMBackendGetFuncFromEnv_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendGetFuncFromEnv_t)lookup_func("TVMBackendGetFuncFromEnv");
        if (!real_func) {
            LOGE("TVMBackendGetFuncFromEnv not found in runtime!");
            return -1;
        }
    }
    return real_func(mod_node, func_name, out);
}

/* TVMBackendAllocWorkspace - forward to runtime */
void* TVMBackendAllocWorkspace(int device_type, int device_id, uint64_t nbytes,
                                int dtype_code_hint, int dtype_bits_hint) {
    init_cache();
    static TVMBackendAllocWorkspace_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendAllocWorkspace_t)lookup_func("TVMBackendAllocWorkspace");
        if (!real_func) return NULL;
    }
    return real_func(device_type, device_id, nbytes, dtype_code_hint, dtype_bits_hint);
}

/* TVMBackendFreeWorkspace - forward to runtime */
int TVMBackendFreeWorkspace(int device_type, int device_id, void* ptr) {
    init_cache();
    static TVMBackendFreeWorkspace_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendFreeWorkspace_t)lookup_func("TVMBackendFreeWorkspace");
        if (!real_func) return -1;
    }
    return real_func(device_type, device_id, ptr);
}

/* TVMBackendParallelLaunch - forward to runtime */
int TVMBackendParallelLaunch(void* flambda, void* cdata, int num_task) {
    init_cache();
    static TVMBackendParallelLaunch_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendParallelLaunch_t)lookup_func("TVMBackendParallelLaunch");
        if (!real_func) return -1;
    }
    return real_func(flambda, cdata, num_task);
}

/* TVMBackendParallelBarrier - forward to runtime */
int TVMBackendParallelBarrier(int task_id, void* penv) {
    init_cache();
    static TVMBackendParallelBarrier_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendParallelBarrier_t)lookup_func("TVMBackendParallelBarrier");
        if (!real_func) return -1;
    }
    return real_func(task_id, penv);
}

/* TVMBackendRunOnce - forward to runtime */
int TVMBackendRunOnce(void** handle, int (*f)(void*), void* cdata, int nbytes) {
    init_cache();
    static TVMBackendRunOnce_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendRunOnce_t)lookup_func("TVMBackendRunOnce");
        if (!real_func) return -1;
    }
    return real_func(handle, f, cdata, nbytes);
}

/*
 * ========================================================================
 *  Additional TVM Backend Functions (used by MLC-LLM models)
 * ========================================================================
 */

typedef int (*TVMBackendAnyListSetPackedArg_t)(void*, int, void*, int);
typedef void (*TVMBackendAnyListResetItem_t)(void*, int);
typedef int (*TVMBackendAnyListMoveFromPackedReturn_t)(void*, void*, void*, int*);

/* TVMBackendAnyListSetPackedArg - forward to runtime */
int TVMBackendAnyListSetPackedArg(void* any_list, int index, void* value, int type_code) {
    init_cache();
    static TVMBackendAnyListSetPackedArg_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendAnyListSetPackedArg_t)lookup_func("TVMBackendAnyListSetPackedArg");
        if (!real_func) return -1;
    }
    return real_func(any_list, index, value, type_code);
}

/* TVMBackendAnyListResetItem - forward to runtime */
void TVMBackendAnyListResetItem(void* any_list, int index) {
    init_cache();
    static TVMBackendAnyListResetItem_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendAnyListResetItem_t)lookup_func("TVMBackendAnyListResetItem");
    }
    if (real_func) real_func(any_list, index);
}

/* TVMBackendAnyListMoveFromPackedReturn - forward to runtime */
int TVMBackendAnyListMoveFromPackedReturn(void* any_list, void* ret_value,
                                           void* ret_tcode, int* moved) {
    init_cache();
    static TVMBackendAnyListMoveFromPackedReturn_t real_func = NULL;
    if (!real_func) {
        real_func = (TVMBackendAnyListMoveFromPackedReturn_t)lookup_func("TVMBackendAnyListMoveFromPackedReturn");
        if (!real_func) return -1;
    }
    return real_func(any_list, ret_value, ret_tcode, moved);
}
