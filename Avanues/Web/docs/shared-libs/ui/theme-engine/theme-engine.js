/**
 * Universal Theme Engine for WebAvanue OS
 * Manages theme settings, runtime switching, and persistence
 * Version: 1.0.0
 */

class ThemeEngine {
    constructor() {
        this.currentTheme = 'ocean';
        this.settings = this.getDefaultSettings();
        this.themes = this.loadThemes();
        this.cssVariables = new Map();
        this.observers = new Set();

        // Initialize
        this.loadSettingsFromStorage();
        this.applyTheme();
    }

    /**
     * Default universal settings
     */
    getDefaultSettings() {
        return {
            // Glass/Material settings
            glassMode: false,
            glassBlur: 40,
            glassOpacity: 0.7,
            glassBorderOpacity: 0.15,

            // Elevation/Shadows
            shadowIntensity: 1.0,
            elevationScale: 1.0,

            // Border radius
            radiusSm: 6,
            radiusMd: 10,
            radiusLg: 14,
            radiusXl: 18,

            // Motion/Animation
            motionSpeed: 1.0,
            motionEnabled: true,
            reducedMotion: false,

            // Typography
            fontScale: 1.0,
            fontWeight: 'normal',

            // Accessibility
            highContrast: false,
            focusIndicators: true,

            // Performance
            gpuAcceleration: true,
            animationQuality: 'high',

            // Voice
            voiceHintsEnabled: true,
            voiceHintsPosition: 'floating',
            voiceHintsOpacity: 0.8
        };
    }

    /**
     * Theme color definitions
     */
    loadThemes() {
        return {
            ocean: {
                name: 'Ocean Blue',
                primary: '#2563eb',
                primaryHover: '#1d4ed8',
                surface: '#1e293b',
                surfaceElevated: '#334155',
                surfaceHigh: '#475569',
                onSurface: '#f1f5f9',
                onSurfaceVariant: '#cbd5e1',
                accent: '#3b82f6',
                background: '#0f172a'
            },
            slate: {
                name: 'Slate Professional',
                primary: '#64748b',
                primaryHover: '#475569',
                surface: '#1e293b',
                surfaceElevated: '#334155',
                surfaceHigh: '#475569',
                onSurface: '#f1f5f9',
                onSurfaceVariant: '#cbd5e1',
                accent: '#94a3b8',
                background: '#0f172a'
            },
            teal: {
                name: 'Deep Teal',
                primary: '#0d9488',
                primaryHover: '#0f766e',
                surface: '#134e4a',
                surfaceElevated: '#115e59',
                surfaceHigh: '#0f766e',
                onSurface: '#ccfbf1',
                onSurfaceVariant: '#99f6e4',
                accent: '#14b8a6',
                background: '#042f2e'
            },
            navy: {
                name: 'Navy Corporate',
                primary: '#1e40af',
                primaryHover: '#1e3a8a',
                surface: '#172554',
                surfaceElevated: '#1e3a8a',
                surfaceHigh: '#1e40af',
                onSurface: '#dbeafe',
                onSurfaceVariant: '#bfdbfe',
                accent: '#3b82f6',
                background: '#0c1e3e'
            },
            charcoal: {
                name: 'Charcoal',
                primary: '#71717a',
                primaryHover: '#52525b',
                surface: '#27272a',
                surfaceElevated: '#3f3f46',
                surfaceHigh: '#52525b',
                onSurface: '#fafafa',
                onSurfaceVariant: '#d4d4d8',
                accent: '#a1a1aa',
                background: '#18181b'
            },
            taupe: {
                name: 'Warm Taupe',
                primary: '#78716c',
                primaryHover: '#57534e',
                surface: '#292524',
                surfaceElevated: '#44403c',
                surfaceHigh: '#57534e',
                onSurface: '#fafaf9',
                onSurfaceVariant: '#d6d3d1',
                accent: '#a8a29e',
                background: '#1c1917'
            },
            emerald: {
                name: 'Emerald Green',
                primary: '#10b981',
                primaryHover: '#059669',
                surface: '#1e3a2f',
                surfaceElevated: '#2d4a3e',
                surfaceHigh: '#3d5a4e',
                onSurface: '#d1fae5',
                onSurfaceVariant: '#a7f3d0',
                accent: '#34d399',
                background: '#0f291e'
            },
            purple: {
                name: 'Purple Majesty',
                primary: '#8b5cf6',
                primaryHover: '#7c3aed',
                surface: '#2d1b4e',
                surfaceElevated: '#3d2565',
                surfaceHigh: '#4d3075',
                onSurface: '#f3e8ff',
                onSurfaceVariant: '#ddd6fe',
                accent: '#a78bfa',
                background: '#1e1233'
            }
        };
    }

    /**
     * Apply theme to document
     */
    applyTheme() {
        const theme = this.themes[this.currentTheme];
        const root = document.documentElement;

        // Apply colors
        root.style.setProperty('--primary', theme.primary);
        root.style.setProperty('--primary-hover', theme.primaryHover);
        root.style.setProperty('--surface', theme.surface);
        root.style.setProperty('--surface-elevated', theme.surfaceElevated);
        root.style.setProperty('--surface-high', theme.surfaceHigh);
        root.style.setProperty('--on-surface', theme.onSurface);
        root.style.setProperty('--on-surface-variant', theme.onSurfaceVariant);
        root.style.setProperty('--accent', theme.accent);
        root.style.setProperty('--background', theme.background);

        // Apply universal settings
        this.applySettings();

        // Notify observers
        this.notifyObservers('theme-changed', { theme: this.currentTheme });
    }

    /**
     * Apply universal settings
     */
    applySettings() {
        const root = document.documentElement;
        const s = this.settings;

        // Glass mode
        root.style.setProperty('--glass-blur', s.glassMode ? `${s.glassBlur}px` : '0px');
        root.style.setProperty('--glass-opacity', s.glassMode ? s.glassOpacity : 1);
        root.style.setProperty('--glass-border-opacity', s.glassMode ? s.glassBorderOpacity : 0.1);

        // Border radius
        root.style.setProperty('--radius-sm', `${s.radiusSm}px`);
        root.style.setProperty('--radius-md', `${s.radiusMd}px`);
        root.style.setProperty('--radius-lg', `${s.radiusLg}px`);
        root.style.setProperty('--radius-xl', `${s.radiusXl}px`);

        // Elevation/Shadows (scale multiplier)
        const shadowScale = s.shadowIntensity;
        root.style.setProperty('--elevation-1',
            `0 ${1 * shadowScale}px ${3 * shadowScale}px rgba(0, 0, 0, ${0.4 * shadowScale}), 0 ${1 * shadowScale}px ${2 * shadowScale}px rgba(0, 0, 0, ${0.3 * shadowScale})`
        );
        root.style.setProperty('--elevation-2',
            `0 ${3 * shadowScale}px ${6 * shadowScale}px rgba(0, 0, 0, ${0.4 * shadowScale}), 0 ${2 * shadowScale}px ${4 * shadowScale}px rgba(0, 0, 0, ${0.3 * shadowScale}), inset 0 1px 0 rgba(255, 255, 255, ${0.05 * shadowScale})`
        );
        root.style.setProperty('--elevation-3',
            `0 ${6 * shadowScale}px ${12 * shadowScale}px rgba(0, 0, 0, ${0.4 * shadowScale}), 0 ${3 * shadowScale}px ${6 * shadowScale}px rgba(0, 0, 0, ${0.3 * shadowScale}), inset 0 1px 0 rgba(255, 255, 255, ${0.08 * shadowScale})`
        );

        // Motion
        const speedFactor = s.motionSpeed;
        root.style.setProperty('--duration-quick', `${200 / speedFactor}ms`);
        root.style.setProperty('--duration-medium', `${300 / speedFactor}ms`);

        if (s.reducedMotion) {
            root.style.setProperty('--motion-emphasized', 'cubic-bezier(0, 0, 1, 1)');
            root.style.setProperty('--motion-standard', 'cubic-bezier(0, 0, 1, 1)');
        } else {
            root.style.setProperty('--motion-emphasized', 'cubic-bezier(0.2, 0, 0, 1)');
            root.style.setProperty('--motion-standard', 'cubic-bezier(0.4, 0, 0.2, 1)');
        }

        // Accessibility
        if (s.highContrast) {
            root.classList.add('high-contrast');
        } else {
            root.classList.remove('high-contrast');
        }

        // Performance
        if (s.gpuAcceleration) {
            root.style.setProperty('--depth-base', 'translateZ(0)');
            root.style.setProperty('--depth-raised', 'translateZ(2px)');
            root.style.setProperty('--depth-elevated', 'translateZ(4px)');
        } else {
            root.style.setProperty('--depth-base', 'translate(0)');
            root.style.setProperty('--depth-raised', 'translate(0)');
            root.style.setProperty('--depth-elevated', 'translate(0)');
        }

        // Notify observers
        this.notifyObservers('settings-changed', { settings: this.settings });
    }

    /**
     * Switch theme
     */
    switchTheme(themeName) {
        if (this.themes[themeName]) {
            this.currentTheme = themeName;
            this.applyTheme();
            this.saveToStorage();
            return true;
        }
        return false;
    }

    /**
     * Update setting
     */
    updateSetting(key, value) {
        if (this.settings.hasOwnProperty(key)) {
            this.settings[key] = value;
            this.applySettings();
            this.saveToStorage();
            return true;
        }
        return false;
    }

    /**
     * Update multiple settings at once
     */
    updateSettings(newSettings) {
        Object.assign(this.settings, newSettings);
        this.applySettings();
        this.saveToStorage();
    }

    /**
     * Toggle glass mode
     */
    toggleGlassMode() {
        this.settings.glassMode = !this.settings.glassMode;
        this.applySettings();
        this.saveToStorage();
        return this.settings.glassMode;
    }

    /**
     * Get current settings
     */
    getSettings() {
        return { ...this.settings };
    }

    /**
     * Get current theme
     */
    getCurrentTheme() {
        return this.currentTheme;
    }

    /**
     * Get all available themes
     */
    getThemes() {
        return { ...this.themes };
    }

    /**
     * Export theme configuration
     */
    exportConfig() {
        return {
            theme: this.currentTheme,
            settings: this.settings,
            version: '1.0.0',
            timestamp: new Date().toISOString()
        };
    }

    /**
     * Import theme configuration
     */
    importConfig(config) {
        try {
            if (config.theme && this.themes[config.theme]) {
                this.currentTheme = config.theme;
            }
            if (config.settings) {
                Object.assign(this.settings, config.settings);
            }
            this.applyTheme();
            this.saveToStorage();
            return true;
        } catch (error) {
            console.error('Failed to import config:', error);
            return false;
        }
    }

    /**
     * Save to localStorage
     */
    saveToStorage() {
        try {
            const config = this.exportConfig();
            localStorage.setItem('webavanue-theme-config', JSON.stringify(config));
        } catch (error) {
            console.error('Failed to save to storage:', error);
        }
    }

    /**
     * Load from localStorage
     */
    loadSettingsFromStorage() {
        try {
            const stored = localStorage.getItem('webavanue-theme-config');
            if (stored) {
                const config = JSON.parse(stored);
                this.importConfig(config);
            }
        } catch (error) {
            console.error('Failed to load from storage:', error);
        }
    }

    /**
     * Observer pattern for theme changes
     */
    subscribe(callback) {
        this.observers.add(callback);
        return () => this.observers.delete(callback);
    }

    notifyObservers(event, data) {
        this.observers.forEach(callback => {
            try {
                callback(event, data);
            } catch (error) {
                console.error('Observer callback error:', error);
            }
        });
    }

    /**
     * Reset to defaults
     */
    resetToDefaults() {
        this.settings = this.getDefaultSettings();
        this.currentTheme = 'ocean';
        this.applyTheme();
        this.saveToStorage();
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ThemeEngine;
}
