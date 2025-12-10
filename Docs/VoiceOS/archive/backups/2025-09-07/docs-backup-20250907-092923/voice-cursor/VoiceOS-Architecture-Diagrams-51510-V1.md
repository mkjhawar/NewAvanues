# VoiceCursor Architecture Diagrams
## Visual Technical Documentation

**Last Updated:** January 26, 2025  
**Version:** 3.0.0

---

## 1. System Architecture Overview

```mermaid
graph TB
    subgraph User Layer
        UI[User Interface]
        VC[Voice Commands]
        HM[Head Movement]
        GT[Gaze Tracking]
    end
    
    subgraph Application Layer
        VM[VoiceCursor Manager]
        CC[Command Controller]
        SM[State Manager]
        RM[Render Manager]
    end
    
    subgraph Service Layer
        OS[Overlay Service]
        AS[Accessibility Service]
        VS[Voice Service]
        IS[IMU Service]
    end
    
    subgraph Hardware Layer
        MIC[Microphone]
        IMU[IMU Sensors]
        CAM[Camera]
        DSP[Display]
    end
    
    UI --> VM
    VC --> CC
    HM --> IS
    GT --> VM
    
    VM --> SM
    CC --> SM
    SM --> RM
    RM --> OS
    
    OS --> AS
    AS --> DSP
    VS --> MIC
    IS --> IMU
    GT --> CAM
    
    style VM fill:#f9f,stroke:#333,stroke-width:4px
    style AS fill:#bbf,stroke:#333,stroke-width:2px
    style OS fill:#bbf,stroke:#333,stroke-width:2px
```

---

## 2. Component Interaction Flow

```mermaid
flowchart LR
    subgraph Input Processing
        A[Sensor Input] --> B[Filter & Smooth]
        B --> C[Position Calculate]
        
        D[Voice Input] --> E[Speech Recognition]
        E --> F[Command Parse]
        
        G[Gaze Input] --> H[Eye Tracking]
        H --> I[Dwell Detection]
    end
    
    subgraph Core Processing
        C --> J[State Update]
        F --> J
        I --> J
        J --> K[Event Dispatch]
        K --> L[Action Execute]
    end
    
    subgraph Output
        L --> M[Visual Feedback]
        L --> N[Haptic Feedback]
        L --> O[System Action]
        M --> P[Screen Render]
    end
    
    style J fill:#f96,stroke:#333,stroke-width:2px
    style K fill:#9f6,stroke:#333,stroke-width:2px
```

---

## 3. State Management Diagram

```mermaid
stateDiagram-v2
    [*] --> Initialization
    
    Initialization --> Ready: init complete
    Initialization --> Error: init failed
    
    Ready --> Active: start()
    Active --> Ready: stop()
    
    state Active {
        [*] --> Idle
        Idle --> Tracking: input detected
        
        Tracking --> Clicking: click command
        Clicking --> Idle: click complete
        
        Tracking --> Dragging: drag start
        Dragging --> Idle: drag end
        
        Tracking --> GazeMode: gaze active
        GazeMode --> AutoClick: dwell complete
        AutoClick --> Idle: click done
        
        Tracking --> Locked: lock command
        Locked --> Tracking: unlock
    }
    
    Active --> Paused: pause()
    Paused --> Active: resume()
    
    Active --> Error: critical error
    Error --> Initialization: reset()
    
    Ready --> [*]: dispose()
```

---

## 4. Data Flow Architecture

```mermaid
graph TD
    subgraph Sensor Data Flow
        S1[Accelerometer] --> SF[Sensor Fusion]
        S2[Gyroscope] --> SF
        S3[Magnetometer] --> SF
        SF --> KF[Kalman Filter]
        KF --> POS[Position Data]
    end
    
    subgraph Voice Data Flow
        MIC[Microphone] --> STT[Speech to Text]
        STT --> NLP[NLP Processing]
        NLP --> CMD[Command]
    end
    
    subgraph Gaze Data Flow
        CAM[Camera] --> FD[Face Detection]
        FD --> ED[Eye Detection]
        ED --> GP[Gaze Point]
        GP --> DW[Dwell Timer]
    end
    
    POS --> SM[State Manager]
    CMD --> SM
    DW --> SM
    
    SM --> RQ[Render Queue]
    SM --> AQ[Action Queue]
    
    RQ --> R[Renderer]
    AQ --> AS[Accessibility Service]
    
    R --> Display
    AS --> System
    
    style SM fill:#f9f,stroke:#333,stroke-width:4px
```

---

## 5. Thread & Concurrency Model

```mermaid
graph LR
    subgraph Main Thread
        UI[UI Updates]
        RENDER[View Rendering]
        EVENT[Event Handling]
    end
    
    subgraph Sensor Thread
        SENSOR[Sensor Reading]
        FILTER[Data Filtering]
        CALC[Position Calc]
    end
    
    subgraph Voice Thread
        AUDIO[Audio Capture]
        RECOG[Recognition]
        PARSE[Parse Commands]
    end
    
    subgraph Worker Thread
        GAZE[Gaze Processing]
        ML[ML Inference]
        CACHE[Cache Management]
    end
    
    SENSOR --> Q1[Position Queue]
    PARSE --> Q2[Command Queue]
    GAZE --> Q3[Gaze Queue]
    
    Q1 --> STATE[Thread-Safe State]
    Q2 --> STATE
    Q3 --> STATE
    
    STATE --> UI
    STATE --> RENDER
    STATE --> EVENT
    
    style STATE fill:#ff9,stroke:#333,stroke-width:3px
```

---

## 6. Service Lifecycle

```mermaid
sequenceDiagram
    participant App
    participant OverlayService
    participant AccessibilityService
    participant CursorView
    participant System
    
    App->>OverlayService: startForegroundService()
    OverlayService->>OverlayService: onCreate()
    OverlayService->>System: createNotification()
    OverlayService->>CursorView: initialize()
    CursorView->>CursorView: setupRendering()
    
    App->>AccessibilityService: enable in settings
    AccessibilityService->>AccessibilityService: onServiceConnected()
    AccessibilityService->>System: registerForEvents()
    
    OverlayService->>CursorView: addToWindowManager()
    CursorView->>AccessibilityService: registerCallbacks()
    
    loop User Interaction
        CursorView->>AccessibilityService: requestGesture()
        AccessibilityService->>System: dispatchGesture()
        System-->>AccessibilityService: gestureComplete()
        AccessibilityService-->>CursorView: callback()
    end
    
    App->>OverlayService: stopService()
    OverlayService->>CursorView: cleanup()
    CursorView->>AccessibilityService: unregister()
    OverlayService->>System: removeNotification()
```

---

## 7. Sensor Processing Pipeline

```mermaid
flowchart TD
    subgraph Raw Sensor Data
        ACC[Accelerometer<br/>100Hz]
        GYRO[Gyroscope<br/>100Hz]
        MAG[Magnetometer<br/>50Hz]
    end
    
    subgraph Preprocessing
        CAL[Calibration<br/>Offset Correction]
        NORM[Normalization<br/>Scale to Units]
        TIME[Timestamp<br/>Alignment]
    end
    
    subgraph Sensor Fusion
        COMP[Complementary<br/>Filter]
        KALM[Kalman<br/>Filter]
        MADG[Madgwick<br/>Algorithm]
    end
    
    subgraph Noise Reduction
        LOW[Low-Pass<br/>Filter α=0.8]
        AVG[Moving<br/>Average n=5]
        DEAD[Deadzone<br/>Filter ±5px]
    end
    
    subgraph Output
        QUAT[Quaternion<br/>Orientation]
        EULER[Euler<br/>Angles]
        POS[Screen<br/>Position]
    end
    
    ACC --> CAL
    GYRO --> CAL
    MAG --> CAL
    
    CAL --> NORM
    NORM --> TIME
    
    TIME --> COMP
    TIME --> KALM
    TIME --> MADG
    
    COMP --> LOW
    KALM --> LOW
    MADG --> LOW
    
    LOW --> AVG
    AVG --> DEAD
    
    DEAD --> QUAT
    QUAT --> EULER
    EULER --> POS
    
    style KALM fill:#9f9,stroke:#333,stroke-width:2px
    style POS fill:#f99,stroke:#333,stroke-width:2px
```

---

## 8. Rendering Pipeline

```mermaid
graph TD
    subgraph Frame Start
        VSYNC[VSync Signal]
        TIME[Frame Timer]
    end
    
    subgraph State Collection
        POS[Position State]
        CONFIG[Cursor Config]
        ANIM[Animation State]
    end
    
    subgraph Rendering Stages
        CLEAR[Clear Buffer]
        BG[Draw Background]
        CURSOR[Draw Cursor]
        EFFECTS[Draw Effects]
        OVERLAY[Draw Overlays]
    end
    
    subgraph Post Processing
        AA[Anti-aliasing]
        BLEND[Alpha Blending]
        COMP[Compositing]
    end
    
    subgraph Frame End
        SWAP[Buffer Swap]
        PRESENT[Present Frame]
    end
    
    VSYNC --> TIME
    TIME --> POS
    TIME --> CONFIG
    TIME --> ANIM
    
    POS --> CLEAR
    CONFIG --> CLEAR
    ANIM --> CLEAR
    
    CLEAR --> BG
    BG --> CURSOR
    CURSOR --> EFFECTS
    EFFECTS --> OVERLAY
    
    OVERLAY --> AA
    AA --> BLEND
    BLEND --> COMP
    
    COMP --> SWAP
    SWAP --> PRESENT
    
    style CURSOR fill:#ff9,stroke:#333,stroke-width:2px
    style PRESENT fill:#9ff,stroke:#333,stroke-width:2px
```

---

## 9. Voice Command Processing

```mermaid
flowchart LR
    subgraph Audio Input
        MIC[Microphone] --> VAD[Voice Activity<br/>Detection]
        VAD --> BUF[Audio Buffer]
    end
    
    subgraph Speech Recognition
        BUF --> ASR[Automatic Speech<br/>Recognition]
        ASR --> TRANS[Transcription]
    end
    
    subgraph NLP Processing
        TRANS --> TOKEN[Tokenization]
        TOKEN --> INTENT[Intent<br/>Classification]
        INTENT --> ENTITY[Entity<br/>Extraction]
    end
    
    subgraph Command Mapping
        ENTITY --> MATCH[Command<br/>Matching]
        MATCH --> VAL[Validation]
        VAL --> EXEC[Execution]
    end
    
    subgraph Feedback
        EXEC --> VIS[Visual<br/>Feedback]
        EXEC --> AUD[Audio<br/>Feedback]
        EXEC --> HAP[Haptic<br/>Feedback]
    end
    
    style ASR fill:#9f9,stroke:#333,stroke-width:2px
    style MATCH fill:#ff9,stroke:#333,stroke-width:2px
```

---

## 10. Gaze Tracking System

```mermaid
stateDiagram-v2
    [*] --> Calibration: First Use
    
    Calibration --> Idle: Calibrated
    
    state GazeTracking {
        Idle --> FaceDetected: Face Found
        FaceDetected --> EyesDetected: Eyes Located
        EyesDetected --> PupilTracked: Pupils Found
        
        PupilTracked --> GazeCalculated: Vector Computed
        GazeCalculated --> ScreenMapped: Position Mapped
        
        ScreenMapped --> DwellStarted: Position Stable
        DwellStarted --> DwellProgress: Timer Running
        DwellProgress --> ClickTriggered: Threshold Met
        
        ClickTriggered --> Idle: Reset
        
        FaceDetected --> Idle: Face Lost
        EyesDetected --> FaceDetected: Eyes Lost
        PupilTracked --> EyesDetected: Pupils Lost
    }
    
    GazeTracking --> Calibration: Recalibrate
    GazeTracking --> [*]: Disable
```

---

## 11. Memory Management

```mermaid
graph TD
    subgraph Object Pools
        BP[Bitmap Pool<br/>Max: 10]
        PP[Path Pool<br/>Max: 20]
        MP[Matrix Pool<br/>Max: 15]
    end
    
    subgraph Cache Management
        IC[Image Cache<br/>LRU: 50MB]
        RC[Resource Cache<br/>LRU: 20MB]
        SC[State Cache<br/>Ring: 100]
    end
    
    subgraph Lifecycle
        CREATE[Create/Acquire]
        USE[Active Use]
        RELEASE[Release/Recycle]
    end
    
    subgraph GC Optimization
        YOUNG[Young Generation]
        OLD[Old Generation]
        PERM[Permanent]
    end
    
    BP --> CREATE
    PP --> CREATE
    MP --> CREATE
    
    CREATE --> USE
    USE --> RELEASE
    
    RELEASE --> BP
    RELEASE --> PP
    RELEASE --> MP
    
    IC --> YOUNG
    RC --> OLD
    SC --> YOUNG
    
    YOUNG --> GC[Garbage Collector]
    OLD --> GC
    PERM --> GC
    
    style GC fill:#f99,stroke:#333,stroke-width:2px
```

---

## 12. Performance Monitoring

```mermaid
graph LR
    subgraph Metrics Collection
        FPS[Frame Rate]
        LAT[Latency]
        MEM[Memory Usage]
        CPU[CPU Usage]
        BAT[Battery Drain]
    end
    
    subgraph Analysis
        AVG[Average]
        P95[95th Percentile]
        P99[99th Percentile]
        MAX[Maximum]
    end
    
    subgraph Optimization
        PROF[Profiling]
        TRACE[Tracing]
        BENCH[Benchmarking]
    end
    
    subgraph Reporting
        LOG[Logging]
        DASH[Dashboard]
        ALERT[Alerts]
    end
    
    FPS --> AVG
    LAT --> P95
    MEM --> MAX
    CPU --> AVG
    BAT --> AVG
    
    AVG --> PROF
    P95 --> TRACE
    P99 --> TRACE
    MAX --> BENCH
    
    PROF --> LOG
    TRACE --> DASH
    BENCH --> ALERT
    
    style P95 fill:#ff9,stroke:#333,stroke-width:2px
    style DASH fill:#9ff,stroke:#333,stroke-width:2px
```

---

## 13. Error Handling Flow

```mermaid
flowchart TD
    subgraph Error Detection
        TRY[Try Operation]
        CATCH[Catch Exception]
        TYPE{Error Type?}
    end
    
    subgraph Error Classification
        RECOV[Recoverable]
        PARTIAL[Partial Recovery]
        FATAL[Fatal Error]
    end
    
    subgraph Recovery Strategy
        RETRY[Retry Operation]
        FALLBACK[Use Fallback]
        DEGRADE[Degrade Gracefully]
        RESTART[Restart Service]
    end
    
    subgraph User Notification
        SILENT[Log Only]
        TOAST[Toast Message]
        DIALOG[Error Dialog]
        CRASH[Crash Report]
    end
    
    TRY --> CATCH
    CATCH --> TYPE
    
    TYPE -->|Permission| RECOV
    TYPE -->|Network| PARTIAL
    TYPE -->|Memory| FATAL
    TYPE -->|Sensor| PARTIAL
    
    RECOV --> RETRY
    PARTIAL --> FALLBACK
    FATAL --> RESTART
    
    RETRY --> SILENT
    FALLBACK --> TOAST
    DEGRADE --> TOAST
    RESTART --> DIALOG
    
    FATAL --> CRASH
    
    style TYPE fill:#f99,stroke:#333,stroke-width:2px
    style FALLBACK fill:#9f9,stroke:#333,stroke-width:2px
```

---

## 14. Security Architecture

```mermaid
graph TB
    subgraph Permission Layer
        OVERLAY[Overlay Permission]
        ACCESS[Accessibility Permission]
        MICRO[Microphone Permission]
        CAMERA[Camera Permission]
    end
    
    subgraph Validation Layer
        PCHECK[Permission Check]
        SVALID[Signature Validation]
        TVALID[Token Validation]
    end
    
    subgraph Isolation Layer
        SANDBOX[Process Sandbox]
        CRYPTO[Encryption]
        SECURE[Secure Storage]
    end
    
    subgraph Audit Layer
        LOG[Activity Logging]
        MONITOR[Behavior Monitor]
        REPORT[Security Reports]
    end
    
    OVERLAY --> PCHECK
    ACCESS --> PCHECK
    MICRO --> PCHECK
    CAMERA --> PCHECK
    
    PCHECK --> SVALID
    SVALID --> TVALID
    
    TVALID --> SANDBOX
    SANDBOX --> CRYPTO
    CRYPTO --> SECURE
    
    SECURE --> LOG
    LOG --> MONITOR
    MONITOR --> REPORT
    
    style PCHECK fill:#f99,stroke:#333,stroke-width:2px
    style CRYPTO fill:#9f9,stroke:#333,stroke-width:2px
```

---

## 15. Module Dependencies

```mermaid
graph TD
    subgraph Application Modules
        APP[Main App]
        VC[VoiceCursor]
        SR[SpeechRecognition]
    end
    
    subgraph Library Modules
        DM[DeviceManager]
        VUI[VoiceUIElements]
        DB[Database/ObjectBox]
    end
    
    subgraph Android Framework
        COMP[Jetpack Compose]
        ACC[Accessibility API]
        SENS[Sensor API]
    end
    
    subgraph External Libraries
        COROUT[Kotlinx Coroutines]
        FLOW[Kotlin Flow]
        MAT3[Material3]
    end
    
    APP --> VC
    APP --> SR
    VC --> DM
    VC --> VUI
    SR --> DB
    
    VC --> COMP
    VC --> ACC
    VC --> SENS
    
    VC --> COROUT
    VC --> FLOW
    VC --> MAT3
    
    DM --> SENS
    VUI --> COMP
    
    style VC fill:#ff9,stroke:#333,stroke-width:4px
    style DM fill:#9ff,stroke:#333,stroke-width:2px
    style ACC fill:#f9f,stroke:#333,stroke-width:2px
```

---

## Conclusion

These architecture diagrams provide a comprehensive visual understanding of the VoiceCursor system. Each diagram focuses on a specific aspect of the architecture, from high-level system overview to detailed component interactions. Use these diagrams as reference when implementing, debugging, or extending the VoiceCursor functionality.

---

**Legend:**
- 🟨 Yellow boxes: Critical components
- 🟦 Blue boxes: Service components  
- 🟩 Green boxes: Optimized/efficient components
- 🟥 Red boxes: Performance-critical or error-prone areas
- Thick borders: Primary flow paths
- Thin borders: Secondary flow paths