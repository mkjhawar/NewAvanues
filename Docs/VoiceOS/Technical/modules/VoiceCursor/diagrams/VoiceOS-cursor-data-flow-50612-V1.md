# VoiceCursor Data Flow Diagrams
**Last Updated:** 2025-01-28

## 1. Complete Data Flow Pipeline

```mermaid
graph TB
    subgraph "Hardware Sensors"
        ACC[Accelerometer<br/>3-axis acceleration]
        GYRO[Gyroscope<br/>3-axis rotation]
        MAG[Magnetometer<br/>3-axis magnetic field]
    end
    
    subgraph "IMUManager"
        SF[Sensor Fusion<br/>Madgwick/Mahony]
        QO[Quaternion Output<br/>w,x,y,z]
    end
    
    subgraph "CursorAdapter - FIXED 2025-01-28"
        PO[Previous Orientation<br/>Storage]
        DC[Delta Calculation<br/>Q⁻¹ × Q]
        EC[Euler Conversion<br/>yaw, pitch, roll]
        TS[Tangent Scaling<br/>tan θ × screen × sensitivity]
        PU[Position Update<br/>cumulative]
    end
    
    subgraph "CursorFilter"
        DZ[Dead Zone<br/>< 0.001 rad]
        JF[Jitter Filter<br/>moving average]
        MS[Motion Smoothing<br/>interpolation]
    end
    
    subgraph "CursorView"
        REN[Renderer<br/>Canvas draw]
        ANI[Animations<br/>pulse, glow]
        GES[Gestures<br/>tap, swipe]
    end
    
    ACC --> SF
    GYRO --> SF
    MAG --> SF
    SF --> QO
    QO --> DC
    PO --> DC
    DC --> EC
    EC --> TS
    TS --> PU
    PU --> DZ
    DZ --> JF
    JF --> MS
    MS --> REN
    REN --> ANI
    ANI --> GES
    
    DC -.->|Store| PO
```

## 2. Coordinate Transformation Pipeline

```mermaid
graph LR
    subgraph "Input Space"
        Q1[Quaternion t-1<br/>Previous frame]
        Q2[Quaternion t<br/>Current frame]
    end
    
    subgraph "Delta Processing"
        INV[Q1 Inverse]
        MUL[Multiply<br/>Q1⁻¹ × Q2]
        DQ[Delta Quaternion]
    end
    
    subgraph "Angular Space"
        E[Euler Angles<br/>Δyaw, Δpitch]
        TAN[Tangent Function<br/>tan(Δθ)]
    end
    
    subgraph "Screen Space"
        SC[Scale by Screen<br/>× width, × height]
        SEN[Apply Sensitivity<br/>× 2.0, × 3.0]
        POS[Add to Position<br/>P += ΔP]
        CL[Clamp to Bounds<br/>[0,w] × [0,h]]
    end
    
    Q1 --> INV
    Q2 --> MUL
    INV --> MUL
    MUL --> DQ
    DQ --> E
    E --> TAN
    TAN --> SC
    SC --> SEN
    SEN --> POS
    POS --> CL
```

## 3. Initialization & Recovery Flow

```mermaid
stateDiagram-v2
    [*] --> Uninitialized
    
    Uninitialized --> Initializing: onCreate()
    
    Initializing --> CenterCursor: Set position
    CenterCursor --> WaitForSensor: width/2, height/2
    WaitForSensor --> CaptureBase: First data
    CaptureBase --> Tracking: Store base orientation
    
    Tracking --> Processing: Sensor data
    Processing --> Moving: Delta > threshold
    Processing --> Stationary: Delta < threshold
    
    Moving --> Tracking: Update position
    Stationary --> StuckDetection: 5 seconds
    
    StuckDetection --> AutoRecalibrate: Timeout
    AutoRecalibrate --> CenterCursor: Reset
    
    Tracking --> ManualRecalibrate: User request
    ManualRecalibrate --> CenterCursor: Reset
```

## 4. Bug Fix Visualization (Before vs After)

```mermaid
graph TB
    subgraph "BEFORE - Broken"
        B1[Orientation Data]
        B2[Direct Euler<br/>absolute angles]
        B3[Linear Scale<br/>angle × screen × 1.2]
        B4[Kill Movement<br/>× 0.1 😱]
        B5[Result: 2 pixels<br/>X=0, Y=0 stuck]
        
        B1 --> B2
        B2 --> B3
        B3 --> B4
        B4 --> B5
    end
    
    subgraph "AFTER - Fixed ✅"
        A1[Orientation Data]
        A2[Delta Processing<br/>frame-to-frame]
        A3[Tangent Scale<br/>tan(Δθ) × screen]
        A4[Proper Sensitivity<br/>× 2.0, × 3.0]
        A5[Result: 336 pixels<br/>Smooth movement]
        
        A1 --> A2
        A2 --> A3
        A3 --> A4
        A4 --> A5
    end
    
    style B5 fill:#f99
    style A5 fill:#9f9
```

## 5. State Machine

```mermaid
stateDiagram-v2
    [*] --> Idle
    
    Idle --> Active: Start tracking
    Active --> Locked: Lock gesture
    Locked --> Active: Unlock gesture
    
    Active --> GazeTracking: Enable gaze
    GazeTracking --> GazeClick: 1.5s dwell
    GazeClick --> Active: Click dispatched
    
    Active --> MenuOpen: Menu request
    MenuOpen --> Active: Menu closed
    
    Active --> Stuck: No movement 5s
    Stuck --> Recalibrating: Auto-recover
    Recalibrating --> Active: Centered
    
    Active --> Hidden: Hide cursor
    Hidden --> Active: Show cursor
    
    Active --> [*]: Destroy view
```

## 6. Performance Flow

```mermaid
gantt
    title Frame Processing Timeline (16ms budget)
    dateFormat X
    axisFormat %L
    
    section Sensor
    Read sensors       :done, sensor, 0, 2
    
    section Math
    Quaternion inverse :done, math1, 2, 1
    Delta calculation  :done, math2, 3, 1
    Euler conversion   :done, math3, 4, 1
    Tangent scaling    :done, math4, 5, 1
    
    section Filter
    Dead zone check    :done, filter1, 6, 1
    Jitter filter      :done, filter2, 7, 1
    
    section Update
    Position update    :done, update, 8, 1
    
    section Render
    Draw cursor        :done, render1, 9, 5
    Draw effects       :done, render2, 14, 2
    
    section Margin
    Frame margin       :crit, margin, 16, 0
```

## 7. Module Integration

```mermaid
graph TD
    subgraph "VoiceCursor Module"
        VC[CursorView]
        CA[CursorAdapter]
        CF[CursorFilter]
    end
    
    subgraph "DeviceManager Module"
        IM[IMUManager]
        IMU[IMUMathUtils]
    end
    
    subgraph "VoiceAccessibility Module"
        AS[AccessibilityService]
        CD[ClickDispatcher]
    end
    
    subgraph "VoiceDataManager Module"
        SP[SharedPreferences]
        CS[CursorSettings]
    end
    
    IM -->|OrientationData| CA
    CA -->|Position| CF
    CF -->|Filtered| VC
    VC -->|Click Event| AS
    AS -->|Dispatch| CD
    
    CS -->|Config| VC
    VC -->|Save| SP
    
    IMU -->|Math Functions| CA
```

## 8. Error Recovery Paths

```mermaid
flowchart TD
    START[Normal Operation]
    
    START --> CHK1{Quaternion Valid?}
    CHK1 -->|No| SKIP[Skip Frame]
    CHK1 -->|Yes| PROC[Process Delta]
    
    PROC --> CHK2{Movement Detected?}
    CHK2 -->|Yes| UPD[Update Position]
    CHK2 -->|No| TMR[Check Timer]
    
    TMR --> CHK3{Stuck > 5s?}
    CHK3 -->|Yes| RECAL[Auto Recalibrate]
    CHK3 -->|No| WAIT[Continue Waiting]
    
    UPD --> CHK4{In Bounds?}
    CHK4 -->|No| CLAMP[Clamp to Screen]
    CHK4 -->|Yes| RENDER[Render Cursor]
    
    SKIP --> START
    RECAL --> START
    WAIT --> START
    CLAMP --> RENDER
    RENDER --> START
```

---
**Diagrams Version:** 1.0.0  
**Created:** 2025-01-28  
**Format:** Mermaid Markdown