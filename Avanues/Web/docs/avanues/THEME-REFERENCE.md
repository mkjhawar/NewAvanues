# Avanues Platform Theme Reference

Universal theme system for all Avanues platform applications.

## Integration

```javascript
import ThemeEngine from '@shared/ui/theme-engine';

const themeEngine = new ThemeEngine();
themeEngine.switchTheme('ocean');  // Platform default
```

## Platform Settings

```javascript
{
    glassMode: false,       // Solid for performance
    shadowIntensity: 1.0,   // Full shadows for clarity
    radiusSm: 6,
    radiusMd: 10,
    radiusLg: 14,
    motionSpeed: 1.0,
    gpuAcceleration: true
}
```

## References

- **Design Tokens:** `docs/shared-libs/ui/DESIGN-TOKENS.md`
- **Theme Engine:** `docs/shared-libs/ui/theme-engine/`
