# IMU System Architecture Diagram

## System Overview

```mermaid
graph TB
    %% Hardware Layer
    subgraph "Hardware Layer"
        GYRO[Gyroscope<br/>Angular Velocity]
        ACCEL[Accelerometer<br/>Linear Acceleration]
        MAG[Magnetometer<br/>Magnetic Field]
        GAME_ROT[Game Rotation Vector<br/>XR Optimized]
        ROT_VEC[Rotation Vector<br/>Fusion Sensor]
        ORIENT[Orientation<br/>Deprecated]
    end

    %% Android Sensor Framework
    subgraph "Android Sensor Framework"
        SENSOR_MGR[SensorManager<br/>System Service]
        SENSOR_EVENT[SensorEventListener<br/>Callback Interface]
    end

    %% IMU Core System
    subgraph "IMU Core System - DeviceManager"
        IMU_MGR[IMUManager<br/>Centralized Singleton]
        SENSOR_FUSION[EnhancedSensorFusion<br/>Complementary + Kalman Filter]
        MOTION_PRED[MotionPredictor<br/>16ms Latency Compensation]
        CALIB_MGR[CalibrationManager<br/>User Personalization]
        MATH_UTILS[IMUMathUtils<br/>Quaternion Mathematics]
    end

    %% Application Layer
    subgraph "Application Layer"
        CURSOR_ADAPTER[CursorIMUAdapter<br/>Simple Interface]
        CURSOR_INTEGRATION[CursorIMUIntegration<br/>Migration Layer]
        CURSOR_VIEW[VosCursorView<br/>UI Component]
        OTHER_APPS[Other VOS4 Apps<br/>Future Consumers]
    end

    %% Data Flow Connections
    GYRO --> SENSOR_MGR
    ACCEL --> SENSOR_MGR
    MAG --> SENSOR_MGR
    GAME_ROT --> SENSOR_MGR
    ROT_VEC --> SENSOR_MGR
    ORIENT --> SENSOR_MGR

    SENSOR_MGR --> SENSOR_EVENT
    SENSOR_EVENT --> IMU_MGR

    IMU_MGR --> SENSOR_FUSION
    SENSOR_FUSION --> MOTION_PRED
    MOTION_PRED --> CALIB_MGR
    
    MATH_UTILS --> SENSOR_FUSION
    MATH_UTILS --> MOTION_PRED
    MATH_UTILS --> CALIB_MGR

    IMU_MGR --> CURSOR_ADAPTER
    CURSOR_ADAPTER --> CURSOR_INTEGRATION
    CURSOR_INTEGRATION --> CURSOR_VIEW

    IMU_MGR --> OTHER_APPS

    %% Styling
    classDef hardware fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef android fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef imu fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef app fill:#fff3e0,stroke:#e65100,stroke-width:2px

    class GYRO,ACCEL,MAG,GAME_ROT,ROT_VEC,ORIENT hardware
    class SENSOR_MGR,SENSOR_EVENT android
    class IMU_MGR,SENSOR_FUSION,MOTION_PRED,CALIB_MGR,MATH_UTILS imu
    class CURSOR_ADAPTER,CURSOR_INTEGRATION,CURSOR_VIEW,OTHER_APPS app
```

## Data Processing Pipeline

```mermaid
flowchart LR
    %% Raw Data Input
    RAW[Raw Sensor Data<br/>120Hz Sample Rate]
    
    %% Processing Stages
    PRIORITY[Sensor Priority<br/>Selection]
    FUSION[Sensor Fusion<br/>α=0.98 Filter]
    KALMAN[Kalman Filter<br/>Noise Reduction]
    BIAS[Bias Compensation<br/>Drift Correction]
    PREDICT[Motion Prediction<br/>16ms Lookahead]
    CLASSIFY[Movement Classification<br/>GENTLE/RAPID/JITTER]
    ADAPTIVE[Adaptive Filtering<br/>Dynamic Smoothing]
    CALIB[User Calibration<br/>Personalization]
    
    %% Output
    OUTPUT[Position/Orientation<br/>Output to Apps]

    %% Flow
    RAW --> PRIORITY
    PRIORITY --> FUSION
    FUSION --> KALMAN
    KALMAN --> BIAS
    BIAS --> PREDICT
    PREDICT --> CLASSIFY
    CLASSIFY --> ADAPTIVE
    ADAPTIVE --> CALIB
    CALIB --> OUTPUT

    %% Styling
    classDef input fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef process fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef output fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px

    class RAW input
    class PRIORITY,FUSION,KALMAN,BIAS,PREDICT,CLASSIFY,ADAPTIVE,CALIB process
    class OUTPUT output
```

## Multi-Consumer Architecture

```mermaid
graph TD
    %% Single IMU Source
    IMU_SOURCE[IMUManager Singleton<br/>Single Sensor Connection]
    
    %% Consumer Management
    CONSUMER_MGR[Consumer Registry<br/>Automatic Tracking]
    
    %% Multiple Consumers
    CURSOR_APP[Cursor Application<br/>Head Tracking]
    NAVIGATION_APP[Navigation App<br/>Spatial Awareness]
    GESTURE_APP[Gesture Recognition<br/>Hand Tracking]
    XR_APP[XR Application<br/>6DOF Tracking]
    
    %% Lifecycle Management
    LIFECYCLE[Lifecycle Manager<br/>Auto Cleanup]

    %% Connections
    IMU_SOURCE --> CONSUMER_MGR
    CONSUMER_MGR --> CURSOR_APP
    CONSUMER_MGR --> NAVIGATION_APP
    CONSUMER_MGR --> GESTURE_APP
    CONSUMER_MGR --> XR_APP
    
    CURSOR_APP --> LIFECYCLE
    NAVIGATION_APP --> LIFECYCLE
    GESTURE_APP --> LIFECYCLE
    XR_APP --> LIFECYCLE
    LIFECYCLE --> CONSUMER_MGR

    %% Styling
    classDef source fill:#e1f5fe,stroke:#01579b,stroke-width:3px
    classDef mgmt fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef consumer fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px

    class IMU_SOURCE source
    class CONSUMER_MGR,LIFECYCLE mgmt
    class CURSOR_APP,NAVIGATION_APP,GESTURE_APP,XR_APP consumer
```

## Sensor Priority and Fallback Strategy

```mermaid
flowchart TD
    START[IMU System Initialization]
    
    %% Priority 1
    CHECK_GAME[Check for<br/>TYPE_GAME_ROTATION_VECTOR<br/>🎯 XR Optimized]
    GAME_AVAILABLE{Available?}
    USE_GAME[✅ Use Game Rotation Vector<br/>Best for XR/AR]
    
    %% Priority 2
    CHECK_ROT[Check for<br/>TYPE_ROTATION_VECTOR<br/>🔄 Standard Fusion]
    ROT_AVAILABLE{Available?}
    USE_ROT[✅ Use Rotation Vector<br/>Good General Purpose]
    
    %% Priority 3 (Fallback)
    CHECK_ORIENT[Check for<br/>TYPE_ORIENTATION<br/>⚠️ Deprecated]
    ORIENT_AVAILABLE{Available?}
    USE_ORIENT[⚠️ Use Orientation<br/>Legacy Compatibility]
    
    %% Failure State
    NO_SENSORS[❌ No Compatible Sensors<br/>Disable IMU Features]
    
    %% Final States
    INIT_FUSION[Initialize Sensor Fusion<br/>Based on Selected Sensor]
    START_TRACKING[🚀 Begin IMU Tracking]

    %% Flow
    START --> CHECK_GAME
    CHECK_GAME --> GAME_AVAILABLE
    GAME_AVAILABLE -->|Yes| USE_GAME
    GAME_AVAILABLE -->|No| CHECK_ROT
    
    CHECK_ROT --> ROT_AVAILABLE
    ROT_AVAILABLE -->|Yes| USE_ROT
    ROT_AVAILABLE -->|No| CHECK_ORIENT
    
    CHECK_ORIENT --> ORIENT_AVAILABLE
    ORIENT_AVAILABLE -->|Yes| USE_ORIENT
    ORIENT_AVAILABLE -->|No| NO_SENSORS
    
    USE_GAME --> INIT_FUSION
    USE_ROT --> INIT_FUSION
    USE_ORIENT --> INIT_FUSION
    INIT_FUSION --> START_TRACKING

    %% Styling
    classDef start fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef check fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef decision fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef success fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef warning fill:#fff8e1,stroke:#f57f17,stroke-width:2px
    classDef error fill:#ffebee,stroke:#c62828,stroke-width:2px

    class START,INIT_FUSION,START_TRACKING start
    class CHECK_GAME,CHECK_ROT,CHECK_ORIENT check
    class GAME_AVAILABLE,ROT_AVAILABLE,ORIENT_AVAILABLE decision
    class USE_GAME,USE_ROT success
    class USE_ORIENT warning
    class NO_SENSORS error
```

## Component Responsibilities

| Component | Primary Responsibility | Key Features |
|-----------|----------------------|--------------|
| **IMUManager** | Centralized sensor management | Singleton pattern, multi-consumer support, lifecycle management |
| **EnhancedSensorFusion** | Data fusion and filtering | Complementary filter (α=0.98), Kalman smoothing, bias compensation |
| **MotionPredictor** | Latency compensation | 16ms prediction, movement classification, adaptive filtering |
| **CalibrationManager** | User personalization | Neutral position calibration, sensitivity adjustment, range detection |
| **IMUMathUtils** | Mathematical operations | Quaternion math, Vector3 operations, SLERP interpolation |
| **CursorIMUAdapter** | Application interface | Simple API, Flow-based updates, legacy compatibility |

## Performance Characteristics

### Sensor Sampling Rates
- **Game Rotation Vector**: Up to 200Hz (XR optimized)
- **Rotation Vector**: Up to 100Hz (standard rate)
- **Orientation**: Up to 60Hz (legacy fallback)

### Processing Pipeline Latency
- **Sensor to Fusion**: <2ms
- **Fusion to Prediction**: <3ms  
- **Prediction to Output**: <1ms
- **Total Latency**: <6ms (target <10ms)

### Memory Usage
- **Base IMU System**: ~8MB
- **Per Consumer**: ~1MB additional
- **Sensor Buffers**: ~512KB total

---

*This architecture provides centralized, efficient, and scalable IMU data management for all VOS4 applications while maintaining optimal performance and user experience.*