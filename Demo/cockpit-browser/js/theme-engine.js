/* ============================================================
   Cockpit Browser — theme-engine.js
   Theme switching: palette, material mode, appearance, presets.
   Manages data-attributes on <body> and CSS class for material mode.
   ============================================================ */

const PALETTES = ['hydra', 'sol', 'luna', 'terra'];
const MATERIALS = ['glass', 'water', 'cupertino', 'mountainview'];
const APPEARANCES = ['light', 'dark', 'auto'];

const PRESETS = {
  cupertino:      { palette: 'hydra', material: 'cupertino',    appearance: 'light' },
  mountainview:   { palette: 'hydra', material: 'mountainview', appearance: 'light' },
  mountainviewxr: { palette: 'luna',  material: 'mountainview', appearance: 'dark'  },
  metafacial:     { palette: 'luna',  material: 'glass',        appearance: 'dark'  },
  neumorphic:     { palette: 'terra', material: 'water',        appearance: 'light' },
  visionos:       { palette: 'luna',  material: 'glass',        appearance: 'light' },
  liquidui:       { palette: 'hydra', material: 'water',        appearance: 'dark'  },
};

const STORAGE_KEY = 'cockpit-theme';

function resolveAppearance(appearance) {
  if (appearance === 'auto') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return appearance;
}

function loadTheme() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (_) { /* ignore parse errors */ }
  return { palette: 'hydra', appearance: 'dark', material: 'glass', preset: null };
}

function saveTheme(theme) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(theme));
  } catch (_) { /* ignore quota errors */ }
}

function applyTheme(theme) {
  const body = document.body;
  const resolved = resolveAppearance(theme.appearance);

  /* data attributes for CSS custom property selectors */
  body.setAttribute('data-palette', theme.palette);
  body.setAttribute('data-appearance', resolved);

  /* material mode class */
  MATERIALS.forEach(m => body.classList.remove('mode-' + m));
  body.classList.add('mode-' + theme.material);

  /* brief transition flash */
  body.classList.add('theme-transitioning');
  setTimeout(() => body.classList.remove('theme-transitioning'), 350);

  saveTheme(theme);
}

function initThemeEngine(state) {
  /* load persisted or default theme */
  const persisted = loadTheme();
  Object.assign(state.theme, persisted);
  applyTheme(state.theme);

  /* auto-appearance media query listener */
  const mq = window.matchMedia('(prefers-color-scheme: dark)');
  mq.addEventListener('change', () => {
    if (state.theme.appearance === 'auto') {
      applyTheme(state.theme);
    }
  });
}

function setThemePalette(state, palette) {
  if (!PALETTES.includes(palette)) return;
  state.theme.palette = palette;
  state.theme.preset = null;
  applyTheme(state.theme);
}

function setThemeMaterial(state, material) {
  if (!MATERIALS.includes(material)) return;
  state.theme.material = material;
  state.theme.preset = null;
  applyTheme(state.theme);
}

function setThemeAppearance(state, appearance) {
  if (!APPEARANCES.includes(appearance)) return;
  state.theme.appearance = appearance;
  state.theme.preset = null;
  applyTheme(state.theme);
}

function applyPreset(state, presetName) {
  const preset = PRESETS[presetName];
  if (!preset) return;
  state.theme.palette = preset.palette;
  state.theme.material = preset.material;
  state.theme.appearance = preset.appearance;
  state.theme.preset = presetName;
  applyTheme(state.theme);
}

export {
  PALETTES,
  MATERIALS,
  APPEARANCES,
  PRESETS,
  initThemeEngine,
  applyTheme,
  setThemePalette,
  setThemeMaterial,
  setThemeAppearance,
  applyPreset,
};
