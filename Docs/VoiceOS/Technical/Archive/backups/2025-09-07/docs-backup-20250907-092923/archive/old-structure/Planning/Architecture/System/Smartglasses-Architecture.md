# Smartglasses - Architecture
**Module:** Smartglasses
**Author:** Manoj Jhawar
**Created:** 240820
**Last Updated:** 240820

## Changelog
- 240820: Initial creation with new structure proposal
- 240820: Reorganized from devices/ subfolder to brand-based structure

## Current Structure (OLD - To Be Refactored)
```
smartglasses/
└── devices/
    ├── VuzixDevice.kt
    ├── RealWearDevice.kt
    ├── XrealDevice.kt
    ├── TCLDevice.kt
    └── GenericDevice.kt
```

## Proposed Structure (NEW - Brand-Based)
```
smartglasses/
├── core/
│   ├── SmartGlassesDevice.kt (base interface)
│   ├── DeviceManager.kt
│   └── DeviceDetector.kt
├── vuzix/
│   ├── VuzixBlade.kt
│   ├── VuzixShield.kt
│   └── VuzixCommon.kt
├── realwear/
│   ├── RealWearNavigator500.kt
│   ├── RealWearNavigator520.kt
│   └── RealWearCommon.kt
├── xreal/
│   ├── XrealAir.kt
│   ├── XrealAir2.kt
│   └── XrealCommon.kt
├── rokid/
│   ├── RokidMax.kt
│   └── RokidCommon.kt
├── tcl/
│   ├── TCLNxtWear.kt
│   └── TCLCommon.kt
└── generic/
    └── GenericGlasses.kt
```

## Benefits of New Structure
1. **Scalability**: Easy to add new models for each brand
2. **Organization**: Clear separation by manufacturer
3. **Reusability**: Common code per brand in *Common.kt files
4. **Maintainability**: Model-specific customizations isolated
5. **SOLID Compliance**: Better separation of concerns

## Migration Plan
1. Create brand-specific directories
2. Split existing device files into model-specific files
3. Extract common brand code into *Common.kt files
4. Update all references
5. Update namespace to com.ai.vos.smartglasses.*