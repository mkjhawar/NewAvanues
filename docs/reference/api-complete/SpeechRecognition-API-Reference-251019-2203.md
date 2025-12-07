# SpeechRecognition - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:03:53 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/SpeechRecognition`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the SpeechRecognition module.

## Files


### File: `build/generated/ksp/debug/java/com/augmentalis/speechrecognition/di/SpeechModule_ProvideSpeechConfigFactory.java`

**Package:** `com.augmentalis.speechrecognition.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.augmentalis.speechrecognition.SpeechConfig;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceos/speech/engines/vivoka/model/FirebaseRemoteConfigRepository_Factory.java`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka.model`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/hilt_aggregated_deps/_com_augmentalis_speechrecognition_di_SpeechModule.java`

**Package:** `hilt_aggregated_deps`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.hilt.processor.internal.aggregateddeps.AggregatedDeps;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/source/buildConfig/debug/com/augmentalis/speechrecognition/BuildConfig.java`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/bean/WhisperSegment.java`

**Package:** `io.github.ggerganov.whispercpp.bean`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/callbacks/GgmlAbortCallback.java`

**Package:** `io.github.ggerganov.whispercpp.callbacks`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/callbacks/WhisperEncoderBeginCallback.java`

**Package:** `io.github.ggerganov.whispercpp.callbacks`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.WhisperContext;
  - import io.github.ggerganov.whispercpp.model.WhisperState;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/callbacks/WhisperLogitsFilterCallback.java`

**Package:** `io.github.ggerganov.whispercpp.callbacks`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.model.WhisperTokenData;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/callbacks/WhisperNewSegmentCallback.java`

**Package:** `io.github.ggerganov.whispercpp.callbacks`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.WhisperContext;
  - import io.github.ggerganov.whispercpp.model.WhisperState;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/callbacks/WhisperProgressCallback.java`

**Package:** `io.github.ggerganov.whispercpp.callbacks`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.WhisperContext;
  - import io.github.ggerganov.whispercpp.model.WhisperState;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/ggml/GgmlTensor.java`

**Package:** `io.github.ggerganov.whispercpp.ggml`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/ggml/GgmlType.java`

**Package:** `io.github.ggerganov.whispercpp.ggml`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/model/EModel.java`

**Package:** `io.github.ggerganov.whispercpp.model`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/model/WhisperModel.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import io.github.ggerganov.whispercpp.ggml.GgmlTensor;
  - import io.github.ggerganov.whispercpp.model.EModel;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/model/WhisperModelLoader.java`

**Package:** `io.github.ggerganov.whispercpp.model`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Callback;
  - import com.sun.jna.Pointer;
  - import com.sun.jna.Structure;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/model/WhisperState.java`

**Package:** `io.github.ggerganov.whispercpp.model`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/model/WhisperTokenData.java`

**Package:** `io.github.ggerganov.whispercpp.model`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Structure;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/BeamSearchParams.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Structure;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/CBool.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.IntegerType;
  - import java.util.function.BooleanSupplier;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/GreedyParams.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Structure;
  - import java.util.Collections;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperAhead.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.*;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperAheads.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.*;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperContextParams.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.*;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperFilters.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperFullParams.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.*;
  - import io.github.ggerganov.whispercpp.callbacks.WhisperEncoderBeginCallback;
  - import io.github.ggerganov.whispercpp.callbacks.WhisperLogitsFilterCallback;
  - import io.github.ggerganov.whispercpp.callbacks.WhisperNewSegmentCallback;
  - import io.github.ggerganov.whispercpp.callbacks.WhisperProgressCallback;
  - import io.github.ggerganov.whispercpp.callbacks.GgmlAbortCallback;
  - import java.util.Arrays;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperHParams.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/params/WhisperSamplingStrategy.java`

**Package:** `io.github.ggerganov.whispercpp.params`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/WhisperConstants.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/WhisperContext.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.NativeLong;
  - import com.sun.jna.Structure;
  - import com.sun.jna.ptr.PointerByReference;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.ggml.GgmlType;
  - import io.github.ggerganov.whispercpp.WhisperModel;
  - import io.github.ggerganov.whispercpp.params.WhisperContextParams;
  - import java.util.List;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/WhisperCpp.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Native;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.bean.WhisperSegment;
  - import io.github.ggerganov.whispercpp.params.WhisperContextParams;
  - import io.github.ggerganov.whispercpp.params.WhisperFullParams;
  - import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
  - import java.io.File;
  - import java.io.FileNotFoundException;
  - import java.io.IOException;
  - import java.util.ArrayList;
  - ... and 1 more

---

### File: `src/main/cpp/whisper-source/bindings/java/src/main/java/io/github/ggerganov/whispercpp/WhisperCppJnaLibrary.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.sun.jna.Library;
  - import com.sun.jna.Native;
  - import com.sun.jna.Pointer;
  - import io.github.ggerganov.whispercpp.model.WhisperModelLoader;
  - import io.github.ggerganov.whispercpp.model.WhisperTokenData;
  - import io.github.ggerganov.whispercpp.params.WhisperContextParams;
  - import io.github.ggerganov.whispercpp.params.WhisperFullParams;

---

### File: `src/main/cpp/whisper-source/bindings/java/src/test/java/io/github/ggerganov/whispercpp/WhisperCppTest.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**
  - class WhisperCppTest 

**Public Functions:**

**Imports:**
  - import static org.junit.jupiter.api.Assertions.*;
  - import io.github.ggerganov.whispercpp.bean.WhisperSegment;
  - import io.github.ggerganov.whispercpp.params.CBool;
  - import io.github.ggerganov.whispercpp.params.WhisperFullParams;
  - import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
  - import org.junit.jupiter.api.BeforeAll;
  - import org.junit.jupiter.api.Test;
  - import javax.sound.sampled.AudioInputStream;
  - import javax.sound.sampled.AudioSystem;
  - import java.io.File;
  - ... and 2 more

---

### File: `src/main/cpp/whisper-source/bindings/java/src/test/java/io/github/ggerganov/whispercpp/WhisperJnaLibraryTest.java`

**Package:** `io.github.ggerganov.whispercpp`

**Classes/Interfaces/Objects:**
  - class WhisperJnaLibraryTest 

**Public Functions:**

**Imports:**
  - import static org.junit.jupiter.api.Assertions.*;
  - import org.junit.jupiter.api.Test;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/androidTest/java/com/litongjava/whisper/android/java/ExampleInstrumentedTest.java`

**Package:** `com.litongjava.whisper.android.java`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import androidx.test.platform.app.InstrumentationRegistry;
  - import androidx.test.ext.junit.runners.AndroidJUnit4;
  - import org.junit.Test;
  - import org.junit.runner.RunWith;
  - import static org.junit.Assert.*;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/app/App.java`

**Package:** `com.litongjava.whisper.android.java.app`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.app.Application;
  - import com.blankj.utilcode.util.Utils;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/bean/WhisperSegment.java`

**Package:** `com.litongjava.whisper.android.java.bean`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/MainActivity.java`

**Package:** `com.litongjava.whisper.android.java`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.annotation.RequiresApi;
  - import androidx.appcompat.app.AppCompatActivity;
  - import android.content.Context;
  - import android.os.Build;
  - import android.os.Bundle;
  - import android.os.Handler;
  - import android.os.Looper;
  - import android.view.View;
  - import android.widget.TextView;
  - import com.blankj.utilcode.util.ThreadUtils;
  - ... and 14 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/services/WhisperService.java`

**Package:** `com.litongjava.whisper.android.java.services`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import android.os.Build;
  - import android.os.Handler;
  - import android.widget.TextView;
  - import android.widget.Toast;
  - import androidx.annotation.RequiresApi;
  - import com.blankj.utilcode.util.ToastUtils;
  - import com.blankj.utilcode.util.Utils;
  - import com.litongjava.android.utils.dialog.AlertDialogUtils;
  - import com.litongjava.jfinal.aop.Aop;
  - ... and 9 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/single/LocalWhisper.java`

**Package:** `com.litongjava.whisper.android.java.single`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.app.Application;
  - import android.os.Build;
  - import android.os.Handler;
  - import androidx.annotation.RequiresApi;
  - import com.blankj.utilcode.util.ToastUtils;
  - import com.blankj.utilcode.util.Utils;
  - import com.litongjava.jfinal.aop.Aop;
  - import com.litongjava.whisper.android.java.bean.WhisperSegment;
  - import com.litongjava.whisper.android.java.utils.AssetUtils;
  - import com.whispercpp.java.whisper.WhisperContext;
  - ... and 3 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/task/LoadModelTask.java`

**Package:** `com.litongjava.whisper.android.java.task`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import android.os.Build;
  - import android.os.Handler;
  - import android.widget.TextView;
  - import com.blankj.utilcode.util.ThreadUtils;
  - import com.litongjava.jfinal.aop.Aop;
  - import com.litongjava.whisper.android.java.services.WhisperService;
  - import java.io.File;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/task/TranscriptionTask.java`

**Package:** `com.litongjava.whisper.android.java.task`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import android.os.Build;
  - import android.widget.TextView;
  - import com.blankj.utilcode.util.ThreadUtils;
  - import com.litongjava.jfinal.aop.Aop;
  - import com.litongjava.whisper.android.java.services.WhisperService;
  - import java.io.File;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/utils/AssetUtils.java`

**Package:** `com.litongjava.whisper.android.java.utils`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import org.slf4j.Logger;
  - import org.slf4j.LoggerFactory;
  - import java.io.BufferedInputStream;
  - import java.io.BufferedOutputStream;
  - import java.io.File;
  - import java.io.FileNotFoundException;
  - import java.io.FileOutputStream;
  - import java.io.IOException;
  - import java.io.InputStream;
  - ... and 1 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/litongjava/whisper/android/java/utils/WaveEncoder.java`

**Package:** `com.litongjava.whisper.android.java.utils`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import java.io.ByteArrayOutputStream;
  - import java.io.File;
  - import java.io.FileInputStream;
  - import java.io.FileOutputStream;
  - import java.io.IOException;
  - import java.nio.ByteBuffer;
  - import java.nio.ByteOrder;
  - import java.nio.ShortBuffer;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/whispercpp/java/whisper/CpuInfo.java`

**Package:** `com.whispercpp.java.whisper`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.os.Build;
  - import android.util.Log;
  - import androidx.annotation.RequiresApi;
  - import java.io.BufferedReader;
  - import java.io.FileReader;
  - import java.io.IOException;
  - import java.util.ArrayList;
  - import java.util.HashMap;
  - import java.util.List;
  - import java.util.Map;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/whispercpp/java/whisper/WhisperContext.java`

**Package:** `com.whispercpp.java.whisper`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.res.AssetManager;
  - import android.os.Build;
  - import android.util.Log;
  - import androidx.annotation.RequiresApi;
  - import com.litongjava.whisper.android.java.bean.WhisperSegment;
  - import java.io.InputStream;
  - import java.util.ArrayList;
  - import java.util.List;
  - import java.util.concurrent.Callable;
  - import java.util.concurrent.ExecutionException;
  - ... and 2 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/whispercpp/java/whisper/WhisperCpuConfig.java`

**Package:** `com.whispercpp.java.whisper`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.os.Build;
  - import androidx.annotation.RequiresApi;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/whispercpp/java/whisper/WhisperLib.java`

**Package:** `com.whispercpp.java.whisper`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.res.AssetManager;
  - import android.os.Build;
  - import android.util.Log;
  - import androidx.annotation.RequiresApi;
  - import java.io.InputStream;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/main/java/com/whispercpp/java/whisper/WhisperUtils.java`

**Package:** `com.whispercpp.java.whisper`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.os.Build;
  - import android.util.Log;
  - import androidx.annotation.RequiresApi;
  - import java.io.File;
  - import java.nio.file.Path;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android.java/app/src/test/java/com/litongjava/whisper/android/java/ExampleUnitTest.java`

**Package:** `com.litongjava.whisper.android.java`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import org.junit.Test;
  - import static org.junit.Assert.*;

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/androidTest/java/com/whispercppdemo/ExampleInstrumentedTest.kt`

**Package:** `com.whispercppdemo`

**Classes/Interfaces/Objects:**
  - class ExampleInstrumentedTest 

**Public Functions:**

**Imports:**
  - import androidx.test.platform.app.InstrumentationRegistry
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - import org.junit.Assert.*

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/MainActivity.kt`

**Package:** `com.whispercppdemo`

**Classes/Interfaces/Objects:**
  - class MainActivity 

**Public Functions:**

**Imports:**
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import com.whispercppdemo.ui.main.MainScreen
  - import com.whispercppdemo.ui.main.MainScreenViewModel
  - import com.whispercppdemo.ui.theme.WhisperCppDemoTheme

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/media/RiffWaveHelper.kt`

**Package:** `com.whispercppdemo.media`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun decodeWaveFile(file: File): FloatArray 
  - fun encodeWaveFile(file: File, data: ShortArray) 

**Imports:**
  - import java.io.ByteArrayOutputStream
  - import java.io.File
  - import java.nio.ByteBuffer
  - import java.nio.ByteOrder

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/recorder/Recorder.kt`

**Package:** `com.whispercppdemo.recorder`

**Classes/Interfaces/Objects:**
  - class Recorder 

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.media.AudioFormat
  - import android.media.AudioRecord
  - import android.media.MediaRecorder
  - import com.whispercppdemo.media.encodeWaveFile
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.asCoroutineDispatcher
  - import kotlinx.coroutines.withContext
  - import java.io.File
  - import java.util.concurrent.Executors
  - ... and 1 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/ui/main/MainScreen.kt`

**Package:** `com.whispercppdemo.ui.main`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MainScreen(viewModel: MainScreenViewModel) 

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.text.selection.SelectionContainer
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.res.stringResource
  - import androidx.compose.ui.unit.dp
  - import com.google.accompanist.permissions.ExperimentalPermissionsApi
  - ... and 3 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/ui/main/MainScreenViewModel.kt`

**Package:** `com.whispercppdemo.ui.main`

**Classes/Interfaces/Objects:**
  - class MainScreenViewModel(private val application

**Public Functions:**

**Imports:**
  - import android.app.Application
  - import android.content.Context
  - import android.media.MediaPlayer
  - import android.util.Log
  - import androidx.compose.runtime.getValue
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.setValue
  - import androidx.core.net.toUri
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.ViewModelProvider
  - ... and 11 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/ui/theme/Color.kt`

**Package:** `com.whispercppdemo.ui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.graphics.Color

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/ui/theme/Theme.kt`

**Package:** `com.whispercppdemo.ui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun WhisperCppDemoTheme(

**Imports:**
  - import android.app.Activity
  - import android.os.Build
  - import androidx.compose.foundation.isSystemInDarkTheme
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.dynamicDarkColorScheme
  - import androidx.compose.material3.dynamicLightColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.SideEffect
  - ... and 4 more

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/main/java/com/whispercppdemo/ui/theme/Type.kt`

**Package:** `com.whispercppdemo.ui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.Typography
  - import androidx.compose.ui.text.TextStyle
  - import androidx.compose.ui.text.font.FontFamily
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.sp

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/app/src/test/java/com/whispercppdemo/ExampleUnitTest.kt`

**Package:** `com.whispercppdemo`

**Classes/Interfaces/Objects:**
  - class ExampleUnitTest 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import org.junit.Assert.*

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/lib/src/main/java/com/whispercpp/whisper/LibWhisper.kt`

**Package:** `com.whispercpp.whisper`

**Classes/Interfaces/Objects:**
  - class WhisperContext private constructor(private var ptr

**Public Functions:**

**Imports:**
  - import android.content.res.AssetManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.io.File
  - import java.io.InputStream
  - import java.util.concurrent.Executors

---

### File: `src/main/cpp/whisper-source/examples/whisper.android/lib/src/main/java/com/whispercpp/whisper/WhisperCpuConfig.kt`

**Package:** `com.whispercpp.whisper`

**Classes/Interfaces/Objects:**
  - object WhisperCpuConfig 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.io.BufferedReader
  - import java.io.FileReader

---

### File: `src/main/java/com/augmentalis/speechrecognition/common/ServiceState.kt`

**Package:** `com.augmentalis.speechrecognition.common`

**Classes/Interfaces/Objects:**
  - enum class ServiceState 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/speechrecognition/di/SpeechModule.kt`

**Package:** `com.augmentalis.speechrecognition.di`

**Classes/Interfaces/Objects:**
  - object SpeechModule 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
  - import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository
  - import dagger.Module
  - import dagger.Provides
  - import dagger.hilt.InstallIn
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/speechrecognition/SpeechConfiguration.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - enum class SpeechEngine 
  - enum class SpeechMode 
  - data class SpeechConfig(

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository

---

### File: `src/main/java/com/augmentalis/speechrecognition/ui/WhisperModelDownloadUI.kt`

**Package:** `com.augmentalis.speechrecognition.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun WhisperModelDownloadDialog(
  - fun WhisperModelDownloadProgress(

**Imports:**
  - import androidx.compose.animation.AnimatedVisibility
  - import androidx.compose.animation.core.animateFloatAsState
  - import androidx.compose.animation.fadeIn
  - import androidx.compose.animation.fadeOut
  - import androidx.compose.foundation.BorderStroke
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - ... and 22 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/api/RecognitionResult.kt`

**Package:** `com.augmentalis.voiceos.speech.api`

**Classes/Interfaces/Objects:**
  - data class WordTimestamp(
  - data class RecognitionResult(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceos/speech/api/SpeechListeners.kt`

**Package:** `com.augmentalis.voiceos.speech.api`

**Classes/Interfaces/Objects:**
  - class SpeechListenerManager 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceos/speech/api/TTSIntegration.kt`

**Package:** `com.augmentalis.voiceos.speech.api`

**Classes/Interfaces/Objects:**
  - interface TTSIntegration 
  - enum class SpeechFeedbackEvent 
  - enum class SpeechPriority 
  - data class TTSSettings(
  - enum class FeedbackLevel 
  - data class TTSState(
  - class DefaultTTSIntegration 
  - object TTSIntegrationFactory 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.speech.tts.Voice
  - import com.augmentalis.voiceos.speech.engines.tts.TTSEngine
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.SharingStarted
  - import kotlinx.coroutines.flow.map
  - import kotlinx.coroutines.flow.stateIn
  - import kotlinx.coroutines.flow.flowOf
  - import kotlinx.coroutines.CoroutineScope
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/commands/StaticCommands.kt`

**Package:** `com.augmentalis.voiceos.speech.commands`

**Classes/Interfaces/Objects:**
  - object StaticCommands 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceos/speech/confidence/ConfidenceScorer.kt`

**Package:** `com.augmentalis.voiceos.speech.confidence`

**Classes/Interfaces/Objects:**
  - data class ConfidenceResult(
  - data class Alternate(
  - enum class ScoringMethod 
  - enum class ConfidenceLevel 
  - enum class RecognitionEngine 
  - class ConfidenceScorer 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.utils.SimilarityMatcher
  - import kotlin.math.exp

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidConfig.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidConfig(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidErrorHandler.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidErrorHandler(

**Public Functions:**

**Imports:**
  - import android.speech.SpeechRecognizer
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidIntent.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidIntent(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.speech.RecognizerIntent
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechMode

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidLanguage.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidLanguage 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidListener.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidListener(

**Public Functions:**

**Imports:**
  - import android.os.Bundle
  - import android.speech.RecognitionListener
  - import android.speech.SpeechRecognizer
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidRecognizer.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidRecognizer(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.speech.SpeechRecognizer
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicReference

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/android/AndroidSTTEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.android`

**Classes/Interfaces/Objects:**
  - class AndroidSTTEngine(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.engines.common.*
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/AudioStateManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class AudioStateManager(private val engineName

**Public Functions:**

**Imports:**
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/CommandCache.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class CommandCache 

**Public Functions:**

**Imports:**
  - import java.util.Collections
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/CommandProcessor.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class CommandProcessor 

**Public Functions:**

**Imports:**
  - import java.util.*
  - import java.util.concurrent.CopyOnWriteArrayList

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/ErrorRecoveryManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class ErrorRecoveryManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.net.ConnectivityManager
  - import android.net.NetworkCapabilities
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.io.File
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong
  - import kotlin.math.min
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/LearningSystem.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class LearningSystem(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
  - import com.augmentalis.datamanager.entities.EngineType
  - import kotlinx.coroutines.*
  - import java.util.concurrent.ConcurrentHashMap
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/PerformanceMonitor.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class PerformanceMonitor(private val engineName

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong
  - import kotlin.math.max

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/ResultProcessor.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class ResultProcessor(

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/SdkInitializationManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - object SdkInitializationManager 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.TimeoutException
  - import kotlin.coroutines.resume
  - import kotlin.coroutines.resumeWithException
  - import kotlin.coroutines.suspendCoroutine

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/ServiceState.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class ServiceState 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.OnStateChangeListener
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/SpeechError.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - data class SpeechError(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/SpeechErrorCodes.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - object SpeechErrorCodes 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/TimeoutManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class TimeoutManager(

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/UniversalInitializationManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class UniversalInitializationManager private constructor() 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicLong
  - import kotlin.random.Random

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/common/VoiceStateManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class VoiceStateManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicLong
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleAuth.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleAuth 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicBoolean

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleCloudEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleCloudEngine(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.TimeoutManager
  - import com.augmentalis.voiceos.speech.engines.common.*
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleConfig.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleConfig(private val initialConfig

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleErrorHandler.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleErrorHandler(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleNetwork.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleNetwork(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicInteger
  - import kotlin.math.min
  - import kotlin.math.pow

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleStreaming.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleStreaming(
  - class AudioRecorder(

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import java.util.concurrent.atomic.AtomicBoolean
  - import kotlin.math.max

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleTranscript.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.google`

**Classes/Interfaces/Objects:**
  - class GoogleTranscript(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.CommandCache
  - import com.augmentalis.voiceos.speech.engines.common.LearningSystem
  - import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
  - import com.augmentalis.datamanager.entities.EngineType
  - import com.augmentalis.voiceos.speech.engines.common.ResultProcessor
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/tts/TTSEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.tts`

**Classes/Interfaces/Objects:**
  - class TTSEngine(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.speech.tts.Voice
  - import com.augmentalis.devicemanager.accessibility.TTSManager
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import java.util.*

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/model/FileZipManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka.model`

**Classes/Interfaces/Objects:**
  - class FileZipManager 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.File
  - import java.io.FileOutputStream
  - import java.io.IOException
  - import java.util.zip.ZipEntry
  - import java.util.zip.ZipFile

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/model/FirebaseRemoteConfigRepository.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka.model`

**Classes/Interfaces/Objects:**
  - class FirebaseRemoteConfigRepository @Inject constructor(@ApplicationContext private val context
  - enum class FileError 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.speechrecognition.BuildConfig
  - import com.google.firebase.Firebase
  - import com.google.firebase.remoteconfig.FirebaseRemoteConfig
  - import com.google.firebase.remoteconfig.remoteConfig
  - import com.google.firebase.remoteconfig.remoteConfigSettings
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.tasks.await
  - import kotlinx.coroutines.withContext
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/model/VivokaLanguageRepository.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka.model`

**Classes/Interfaces/Objects:**
  - object VivokaLanguageRepository 
  - data class DownloadFiles(val files

**Public Functions:**

**Imports:**
  - import com.google.gson.Gson

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/model/VsdkConfigModels.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka.model`

**Classes/Interfaces/Objects:**
  - data class Root(
  - data class Csdk(
  - data class Log(
  - data class Cache(
  - data class Asr(
  - data class Recognizers(
  - data class Rec(
  - data class Model(
  - data class Slot(
  - data class Lexicon(
  - data class Paths(

**Public Functions:**

**Imports:**
  - import com.google.gson.annotations.SerializedName

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaAssets.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaAssets(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.vivoka.model.Model
  - import com.augmentalis.voiceos.speech.engines.vivoka.model.Root
  - import com.google.gson.Gson
  - import com.vivoka.vsdk.util.AssetsExtractor
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.File
  - import java.io.FileOutputStream
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaAudio.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaAudio(

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.SpeechErrorCodes
  - import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
  - import com.vivoka.vsdk.audio.Pipeline
  - import com.vivoka.vsdk.audio.producers.AudioRecorder
  - import kotlinx.coroutines.CoroutineScope
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaConfig.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaConfig(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import java.io.File

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaEngine(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
  - import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager
  - ... and 18 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaErrorMapper.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - object RecognizerError 
  - object VivokaErrorMapper 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.engines.common.SpeechError

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaInitializationManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaInitializationManager private constructor() 
  - class InitializationException(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager
  - import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationContext
  - import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationResult
  - import com.augmentalis.voiceos.speech.engines.common.SdkInitializationManager.InitializationState
  - import com.vivoka.vsdk.Vsdk
  - import kotlinx.coroutines.*
  - import java.io.File
  - import kotlin.coroutines.resume
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaInitializer.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaInitializer(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.vivoka.vsdk.Vsdk
  - import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.File

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaLearning.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaLearning(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.datamanager.entities.EngineType
  - import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
  - import com.augmentalis.voiceos.speech.engines.common.CommandCache
  - import com.augmentalis.voiceos.speech.engines.common.LearningSystem
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.isActive
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaModel.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaModel(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.SpeechErrorCodes
  - import com.vivoka.vsdk.asr.DynamicModel
  - import com.vivoka.vsdk.asr.csdk.recognizer.Recognizer
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.util.Collections

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaPerformance.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaPerformance(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import java.util.*
  - import kotlin.math.max

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaRecognizer.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaRecognizer(
  - data class RecognitionProcessingResult(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
  - import com.augmentalis.voiceos.speech.confidence.RecognitionEngine
  - import com.augmentalis.voiceos.speech.engines.common.ResultProcessor
  - import com.vivoka.vsdk.asr.recognizer.RecognizerResultType
  - import com.vivoka.vsdk.asr.utils.AsrResultParser
  - import kotlinx.coroutines.CoroutineScope
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaState.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

**Classes/Interfaces/Objects:**
  - class VivokaState 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.sync.Mutex

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskConfig.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskConfig 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskEngine(
  - data class VoskResult(val text

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.*
  - import com.augmentalis.voiceos.speech.utils.SimilarityMatcher
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskErrorHandler.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskErrorHandler(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskGrammar.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskGrammar(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.CommandCache
  - import com.google.gson.Gson
  - import org.vosk.Model
  - import org.vosk.Recognizer
  - import java.util.Collections

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskModel.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskModel(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.suspendCancellableCoroutine
  - import org.vosk.Model
  - import org.vosk.android.StorageService
  - import java.io.File
  - import java.io.IOException
  - import kotlin.coroutines.resume

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskRecognizer.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskRecognizer(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import org.vosk.Model
  - import org.vosk.Recognizer
  - import org.vosk.android.SpeechService

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskState.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskState(

**Public Functions:**

**Imports:**
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskStorage.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskStorage(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.datamanager.entities.EngineType
  - import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
  - import kotlinx.coroutines.*
  - import java.util.Collections

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperAndroid.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - class WhisperAndroid(private val context
  - data class TranscriptionConfig(
  - data class TranscriptionResult(
  - data class Segment(

**Public Functions:**
  - fun convertPCM16ToFloat(pcmData: ByteArray): FloatArray 
  - fun resampleAudio(input: FloatArray, fromRate: Int, toRate: Int): FloatArray 

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.whispercpp.whisper.WhisperContext
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.File
  - import java.io.FileInputStream
  - import java.nio.ByteBuffer
  - import java.nio.ByteOrder

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperConfig.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - data class WhisperEngineConfig(
  - data class ConfigValidationResult(
  - class WhisperConfig(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperEngine.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - class WhisperEngine(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.WordTimestamp
  - import com.augmentalis.voiceos.speech.engines.common.*
  - import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
  - ... and 10 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperErrorHandler.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - object WhisperErrorCode 
  - enum class ErrorSeverity 
  - enum class ErrorCategory 
  - data class WhisperError(
  - enum class ErrorRecoveryStrategy 
  - data class RecoveryResult(
  - class WhisperErrorHandler(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import java.util.concurrent.atomic.AtomicInteger
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperModel.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - enum class WhisperModelSize(
  - sealed class ModelState 
  - class WhisperModel(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.whisper.WhisperNative
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.io.File

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperModelManager.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - sealed class ModelDownloadState 
  - class WhisperModelManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import okhttp3.OkHttpClient
  - import okhttp3.Request
  - import okhttp3.ResponseBody
  - import java.io.File
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperNative.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - data class WhisperResult(
  - data class WhisperSegment(
  - data class WhisperInferenceParams(
  - class WhisperNative(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.api.WordTimestamp
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.util.concurrent.atomic.AtomicBoolean

---

### File: `src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperProcessor.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.whisper`

**Classes/Interfaces/Objects:**
  - enum class WhisperProcessingMode 
  - data class AudioBufferInfo(
  - class WhisperProcessor(

**Public Functions:**

**Imports:**
  - import android.media.AudioFormat
  - import android.media.AudioRecord
  - import android.media.MediaRecorder
  - import android.util.Log
  - import com.augmentalis.voiceos.speech.engines.common.AudioStateManager
  - import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock
  - import java.nio.ByteBuffer
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/help/SpeechRecognitionHelpMenu.kt`

**Package:** `com.augmentalis.voiceos.speech.help`

**Classes/Interfaces/Objects:**
  - class SpeechRecognitionHelpMenu(private val context
  - data class SpeechCommandGroup(
  - data class SpeechCommand(
  - class SpeechHelpCommandAdapter(

**Public Functions:**

**Imports:**
  - import android.app.AlertDialog
  - import android.content.Context
  - import android.graphics.Color
  - import android.graphics.drawable.ColorDrawable
  - import android.view.Gravity
  - import android.view.LayoutInflater
  - import android.view.View
  - import android.view.WindowManager
  - import android.widget.LinearLayout
  - import android.widget.ScrollView
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceos/speech/utils/SimilarityMatcher.kt`

**Package:** `com.augmentalis.voiceos.speech.utils`

**Classes/Interfaces/Objects:**
  - object SimilarityMatcher 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/whispercpp/whisper/LibWhisper.kt`

**Package:** `com.whispercpp.whisper`

**Classes/Interfaces/Objects:**
  - class WhisperContext private constructor(private var ptr

**Public Functions:**

**Imports:**
  - import android.content.res.AssetManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.io.File
  - import java.io.InputStream
  - import java.util.concurrent.Executors

---

### File: `src/main/java/com/whispercpp/whisper/WhisperCpuConfig.kt`

**Package:** `com.whispercpp.whisper`

**Classes/Interfaces/Objects:**
  - object WhisperCpuConfig 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.io.BufferedReader
  - import java.io.FileReader

---

### File: `src/test/java/com/augmentalis/speechrecognition/CompilationTest.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - class CompilationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.speechrecognition.mocks.AndroidSTTEngine
  - import com.augmentalis.speechrecognition.mocks.VoskEngine
  - import com.augmentalis.speechrecognition.mocks.WhisperEngine
  - import com.augmentalis.speechrecognition.test.TestConfig
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.engines.common.CommandCache
  - import com.vivoka.vsdk.Vsdk
  - ... and 6 more

---

### File: `src/test/java/com/augmentalis/speechrecognition/mocks/MockEngines.kt`

**Package:** `com.augmentalis.speechrecognition.mocks`

**Classes/Interfaces/Objects:**
  - class AndroidSTTEngine(private val context
  - class VoskEngine(private val context
  - class VivokaEngine(private val _context
  - class WhisperEngine(private val _context
  - class MockPerformanceMetrics 
  - data class PerformanceMetrics(
  - data class LearningStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.speech.RecognitionListener
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState.State
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
  - import kotlinx.coroutines.delay

---

### File: `src/test/java/com/augmentalis/speechrecognition/performance/SpeechRecognitionPerformanceTest.kt`

**Package:** `com.augmentalis.speechrecognition.performance`

**Classes/Interfaces/Objects:**
  - class SpeechRecognitionPerformanceTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.speechrecognition.*
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.test.*
  - import kotlinx.coroutines.Dispatchers
  - import org.junit.Before
  - import org.junit.Test
  - ... and 6 more

---

### File: `src/test/java/com/augmentalis/speechrecognition/RecognitionStatus.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - enum class RecognitionStatus 

**Public Functions:**

**Imports:**

---

### File: `src/test/java/com/augmentalis/speechrecognition/SmokeTest.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - class SmokeTest 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/speechrecognition/speechengines/AndroidSTTEngineIntegrationTest.kt`

**Package:** `com.augmentalis.speechrecognition.speechengines`

**Classes/Interfaces/Objects:**
  - class AndroidSTTEngineIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.os.Bundle
  - import android.speech.RecognitionListener
  - import android.speech.RecognizerIntent
  - import android.speech.SpeechRecognizer
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - ... and 26 more

---

### File: `src/test/java/com/augmentalis/speechrecognition/SpeechRecognitionCallback.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - interface SpeechRecognitionCallback 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.api.RecognitionResult

---

### File: `src/test/java/com/augmentalis/speechrecognition/SpeechRecognitionManager.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - class SpeechRecognitionManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import kotlinx.coroutines.*

---

### File: `src/test/java/com/augmentalis/speechrecognition/SpeechRecognitionManagerTest.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**
  - class SpeechRecognitionManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.speechrecognition.test.TestConfig
  - import com.augmentalis.speechrecognition.SpeechRecognitionManager
  - import com.augmentalis.speechrecognition.SpeechRecognitionCallback
  - import com.augmentalis.speechrecognition.RecognitionStatus
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - ... and 13 more

---

### File: `src/test/java/com/augmentalis/speechrecognition/test/TestConfig.kt`

**Package:** `com.augmentalis.speechrecognition.test`

**Classes/Interfaces/Objects:**
  - object TestConfig 

**Public Functions:**

**Imports:**
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode

---

### File: `src/test/java/com/augmentalis/speechrecognition/test/TestUtils.kt`

**Package:** `com.augmentalis.speechrecognition.test`

**Classes/Interfaces/Objects:**
  - object TestUtils 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.media.AudioFormat
  - import android.media.AudioRecord
  - import android.media.MediaRecorder
  - import android.os.Bundle
  - import android.util.Log
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
  - ... and 16 more

---

### File: `src/test/java/com/augmentalis/speechrecognition/TestTypeAliases.kt`

**Package:** `com.augmentalis.speechrecognition`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.engines.common.ServiceState

---

### File: `src/test/java/com/augmentalis/voiceos/speech/engines/common/SdkInitializationManagerTest.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.common`

**Classes/Interfaces/Objects:**
  - class SdkInitializationManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.platform.app.InstrumentationRegistry
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - import java.util.concurrent.CountDownLatch
  - import java.util.concurrent.TimeUnit
  - ... and 2 more

---

### File: `src/test/java/com/augmentalis/voiceos/speech/engines/vosk/VoskIntegrationTest.kt`

**Package:** `com.augmentalis.voiceos.speech.engines.vosk`

**Classes/Interfaces/Objects:**
  - class VoskIntegrationTest 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.speech.utils.SimilarityMatcher
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import com.augmentalis.voiceos.speech.confidence.RecognitionEngine
  - import org.junit.Test
  - import org.junit.Assert.*
  - import org.junit.Before

---

### File: `src/test/java/com/augmentalis/voiceos/speech/utils/SimilarityMatcherDemo.kt`

**Package:** `com.augmentalis.voiceos.speech.utils`

**Classes/Interfaces/Objects:**
  - object SimilarityMatcherDemo 

**Public Functions:**
  - fun main() 

**Imports:**

---

### File: `src/test/java/com/augmentalis/voiceos/speech/utils/SimilarityMatcherTest.kt`

**Package:** `com.augmentalis.voiceos.speech.utils`

**Classes/Interfaces/Objects:**
  - class SimilarityMatcherTest 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import org.junit.Assert.*
  - import org.junit.Before

---

### File: `src/test/java/com/vivoka/vsdk/VivokaMocks.kt`

**Package:** `com.vivoka.vsdk`

**Classes/Interfaces/Objects:**
  - object Vsdk 
  - class AsrEngine 

**Public Functions:**

**Imports:**
  - import android.content.Context

---

## Summary

**Total Files:** 152

**Module Structure:**
```
                  build
                    generated
                      ap_generated_sources
                        debug
                          out
                      ksp
                        debug
                          java
                            com
                              augmentalis
                                speechrecognition
                                  di
                                voiceos
                                  speech
                                    engines
                                      vivoka
                                        model
                            hilt_aggregated_deps
                          kotlin
                      res
                        pngs
                          debug
                        resValues
                          debug
                      source
                        buildConfig
                          debug
                            com
                              augmentalis
                                speechrecognition
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
                      annotation_processor_list
                        debug
                          javaPreCompileDebug
                      classes
                        debug
                          transformDebugClassesWithAsm
                            dirs
                              com
                                augmentalis
                                  speechrecognition
                                    common
                                    di
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
