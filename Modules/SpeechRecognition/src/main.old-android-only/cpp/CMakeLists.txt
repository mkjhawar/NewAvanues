# CMakeLists.txt for Whisper JNI Integration
# VOS4 SpeechRecognition Library
# Created: 2025-01-28

cmake_minimum_required(VERSION 3.22.1)
project("whisper-jni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Android log library
find_library(log-lib log)

# Add whisper.cpp source files
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/whisper)

# Check if whisper.cpp exists
if(NOT EXISTS ${WHISPER_DIR}/whisper.cpp)
    message(WARNING "whisper.cpp not found. Please clone whisper.cpp repository.")
endif()

# Whisper source files
set(WHISPER_SOURCES
    ${WHISPER_DIR}/whisper.cpp
    ${WHISPER_DIR}/ggml.c
    ${WHISPER_DIR}/ggml-alloc.c
    ${WHISPER_DIR}/ggml-backend.cpp
    ${WHISPER_DIR}/ggml-quants.c
)

# Create whisper library
add_library(whisper STATIC ${WHISPER_SOURCES})

# Configure whisper library
target_include_directories(whisper PUBLIC ${WHISPER_DIR})
target_compile_definitions(whisper PUBLIC
    GGML_USE_CPU
    _GNU_SOURCE
    GGML_VERSION="1.5.4"
    GGML_COMMIT="latest"
)

# Enable NEON on ARM
if(${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_definitions(whisper PUBLIC GGML_USE_NEON)
    if(${ANDROID_ABI} STREQUAL "arm64-v8a")
        target_compile_definitions(whisper PUBLIC GGML_USE_FP16_VA)
    endif()
endif()

# Optimization flags
target_compile_options(whisper PRIVATE
    -O3
    -ffast-math
    -funroll-loops
    -ftree-vectorize
    -fno-finite-math-only
)

# Create JNI wrapper library
add_library(whisper-jni SHARED whisper_jni.cpp)

# Link libraries
target_link_libraries(whisper-jni
    whisper
    ${log-lib}
    android
)

# Include directories
target_include_directories(whisper-jni PRIVATE
    ${WHISPER_DIR}
)

# Set library properties
set_target_properties(whisper-jni PROPERTIES
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
)
