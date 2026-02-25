/* ============================================================
   Cockpit Browser — app.js
   Main application controller and state management.
   Orchestrates dashboard, frames, layouts, command bar, themes.
   ============================================================ */

import {
  initThemeEngine,
  applyTheme,
  setThemePalette,
  setThemeMaterial,
  setThemeAppearance,
  applyPreset,
  PALETTES,
  MATERIALS,
  APPEARANCES,
  PRESETS,
} from './theme-engine.js';

import {
  createFrame,
  removeFrame,
  toggleMinimize,
  toggleMaximize,
  selectFrame,
  CONTENT_TYPES,
} from './frame-manager.js';

import { setLayout, renderLayout, LAYOUT_MODES } from './layout-engine.js';
import { renderCommandBar } from './command-bar.js';
import { renderDashboard } from './dashboard.js';
import { initPseudoSpatial, setPseudoSpatialEnabled } from './pseudo-spatial.js';
import {
  initDB,
  saveSession,
  loadSession,
  saveAppState,
  loadAppState,
} from './persistence.js';

/* ================================================================
   STATE
   ================================================================ */

const state = {
  view: 'dashboard',
  sessionName: 'Cockpit',
  sessionId: null,
  sessionCreatedAt: null,
  frames: [],
  selectedFrameId: null,
  layoutMode: 'grid',
  commandBarState: 'main',
  pseudoSpatial: true,  /* PseudoSpatial parallax enabled by default */
  theme: {
    palette: 'hydra',
    appearance: 'dark',
    material: 'glass',
    preset: null,
  },
};

/* ================================================================
   DOM REFERENCES
   ================================================================ */

let $topBarBack;
let $topBarTitle;
let $contentWrapper;
let $statusBar;
let $commandBar;
let $themePanelOverlay;
let $themePanel;

/* ================================================================
   PERSISTENCE — debounced auto-save
   ================================================================ */

let saveTimer = null;

/**
 * Schedule a debounced save of the current session state to IndexedDB.
 * Called after every render. The 500ms debounce prevents excessive writes
 * during rapid state changes (e.g. resizing, quick frame adds).
 */
function scheduleSave() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(async () => {
    if (state.view === 'session' && state.frames.length > 0) {
      const session = {
        id: state.sessionId || crypto.randomUUID(),
        name: state.sessionName,
        frames: state.frames.map(f => ({
          id: f.id,
          title: f.title,
          contentType: f.contentType,
          minimized: f.minimized,
          maximized: f.maximized,
        })),
        selectedFrameId: state.selectedFrameId,
        layoutMode: state.layoutMode,
        createdAt: state.sessionCreatedAt || new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      state.sessionId = session.id;
      state.sessionCreatedAt = session.createdAt;
      await saveSession(session);
      await saveAppState('currentSessionId', session.id);
    }
  }, 500);
}

/* ================================================================
   RENDER
   ================================================================ */

function render() {
  renderTopBar();
  renderContent();
  renderStatusBar();
  renderCommandBarUI();
  renderThemePanel();
  applyParallaxLayers();
  scheduleSave();
}

/**
 * Assign data-parallax-layer attributes to DOM elements for PseudoSpatial.
 * Layer 0 = background (deepest parallax), Layer 1 = mid-ground (frames),
 * Layer 2 = foreground (active frame content), Layer 3 = HUD (locked).
 */
function applyParallaxLayers() {
  /* Layer 3 (HUD, no movement): top bar, status bar, command bar */
  const topBar = document.querySelector('.top-bar');
  if (topBar) topBar.setAttribute('data-parallax-layer', '3');

  if ($statusBar) $statusBar.setAttribute('data-parallax-layer', '3');
  if ($commandBar) $commandBar.setAttribute('data-parallax-layer', '3');

  /* Layer 0 (background, deepest parallax): content wrapper background */
  if ($contentWrapper) $contentWrapper.setAttribute('data-parallax-layer', '0');

  /* Layer 1 (mid-ground): frame area containers, session-view */
  const frameAreas = document.querySelectorAll('.frame-area');
  frameAreas.forEach(function(el) {
    el.setAttribute('data-parallax-layer', '1');
  });

  /* Layer 1 also: dashboard tiles container */
  const dashboardGrid = document.querySelector('.dashboard-grid');
  if (dashboardGrid) dashboardGrid.setAttribute('data-parallax-layer', '1');

  /* Layer 2 (foreground): individual frame windows (active content) */
  const frameWindows = document.querySelectorAll('.frame-window');
  frameWindows.forEach(function(el) {
    el.setAttribute('data-parallax-layer', '2');
  });
}

/* ---- Top Bar ---- */
function renderTopBar() {
  const onDashboard = state.view === 'dashboard';
  $topBarBack.className = 'top-bar-back' + (onDashboard ? '' : ' visible');
  $topBarTitle.textContent = onDashboard ? 'Avanues' : state.sessionName;
}

/* ---- Content Area ---- */
function renderContent() {
  $contentWrapper.innerHTML = '';

  if (state.view === 'dashboard') {
    const { element, actions } = renderDashboard();
    $contentWrapper.appendChild(element);
    bindDashboardActions(actions);
  } else {
    renderSessionView();
  }
}

function renderSessionView() {
  const sessionView = document.createElement('div');
  sessionView.className = 'session-view layout-' + state.layoutMode;

  const frameArea = renderLayout(state);
  sessionView.appendChild(frameArea);

  $contentWrapper.appendChild(sessionView);

  /* bind frame interactions */
  bindFrameActions(sessionView);
}

/* ---- Status Bar ---- */
function renderStatusBar() {
  if (state.view === 'dashboard') {
    $statusBar.classList.add('hidden');
  } else {
    $statusBar.classList.remove('hidden');
    const count = state.frames.length;
    const layoutLabel = LAYOUT_MODES[state.layoutMode]
      ? LAYOUT_MODES[state.layoutMode].label
      : state.layoutMode;
    $statusBar.textContent = count + ' frame' + (count !== 1 ? 's' : '') + ' \u00B7 ' + layoutLabel;
  }
}

/* ---- Command Bar ---- */
function renderCommandBarUI() {
  if (state.view === 'dashboard') {
    $commandBar.classList.add('hidden');
  } else {
    $commandBar.classList.remove('hidden');
    const actions = renderCommandBar($commandBar, state);
    bindCommandBarActions(actions);
  }
}

/* ---- Theme Panel ---- */
function renderThemePanel() {
  $themePanel.innerHTML = '';

  /* title */
  const title = document.createElement('div');
  title.className = 'theme-panel-title';
  title.textContent = 'Theme';
  $themePanel.appendChild(title);

  /* Presets */
  const presetSection = themeSection('Preset');
  const presetSelect = document.createElement('select');
  presetSelect.className = 'theme-select';
  presetSelect.setAttribute('data-avid', 'SEL-preset');
  presetSelect.setAttribute('aria-label', 'Theme preset');

  const customOpt = document.createElement('option');
  customOpt.value = '';
  customOpt.textContent = 'Custom';
  presetSelect.appendChild(customOpt);

  Object.keys(PRESETS).forEach(key => {
    const opt = document.createElement('option');
    opt.value = key;
    opt.textContent = key.charAt(0).toUpperCase() + key.slice(1);
    if (state.theme.preset === key) opt.selected = true;
    presetSelect.appendChild(opt);
  });

  if (!state.theme.preset) customOpt.selected = true;

  presetSelect.addEventListener('change', () => {
    const val = presetSelect.value;
    if (val) {
      applyPreset(state, val);
    }
    render();
  });

  presetSection.appendChild(presetSelect);
  $themePanel.appendChild(presetSection);

  /* Palette */
  const paletteSection = themeSection('Color Palette');
  const palettePicker = document.createElement('div');
  palettePicker.className = 'palette-picker';

  PALETTES.forEach(p => {
    const swatch = document.createElement('button');
    swatch.className = 'palette-swatch' + (state.theme.palette === p ? ' active' : '');
    swatch.setAttribute('data-p', p);
    swatch.setAttribute('data-avid', 'BTN-palette-' + p);
    swatch.setAttribute('aria-label', p.charAt(0).toUpperCase() + p.slice(1) + ' palette');
    swatch.textContent = p.substring(0, 3).toUpperCase();
    swatch.addEventListener('click', () => {
      setThemePalette(state, p);
      render();
    });
    palettePicker.appendChild(swatch);
  });

  paletteSection.appendChild(palettePicker);
  $themePanel.appendChild(paletteSection);

  /* Material Mode */
  const materialSection = themeSection('Material Mode');
  const materialPicker = document.createElement('div');
  materialPicker.className = 'material-picker';

  MATERIALS.forEach(m => {
    const btn = document.createElement('button');
    btn.className = 'material-btn' + (state.theme.material === m ? ' active' : '');
    btn.setAttribute('data-avid', 'BTN-material-' + m);
    btn.setAttribute('aria-label', m.charAt(0).toUpperCase() + m.slice(1) + ' mode');
    btn.textContent = m.charAt(0).toUpperCase() + m.slice(1);
    btn.addEventListener('click', () => {
      setThemeMaterial(state, m);
      render();
    });
    materialPicker.appendChild(btn);
  });

  materialSection.appendChild(materialPicker);
  $themePanel.appendChild(materialSection);

  /* Appearance */
  const appearanceSection = themeSection('Appearance');
  const appearanceToggle = document.createElement('div');
  appearanceToggle.className = 'appearance-toggle';

  APPEARANCES.forEach(a => {
    const btn = document.createElement('button');
    btn.className = 'appearance-btn' + (state.theme.appearance === a ? ' active' : '');
    btn.setAttribute('data-avid', 'BTN-appearance-' + a);
    btn.setAttribute('aria-label', a.charAt(0).toUpperCase() + a.slice(1) + ' appearance');
    btn.textContent = a.charAt(0).toUpperCase() + a.slice(1);
    btn.addEventListener('click', () => {
      setThemeAppearance(state, a);
      render();
    });
    appearanceToggle.appendChild(btn);
  });

  appearanceSection.appendChild(appearanceToggle);
  $themePanel.appendChild(appearanceSection);
}

function themeSection(label) {
  const section = document.createElement('div');
  section.className = 'theme-section';
  const lbl = document.createElement('div');
  lbl.className = 'theme-section-label';
  lbl.textContent = label;
  section.appendChild(lbl);
  return section;
}

/* ================================================================
   EVENT BINDING
   ================================================================ */

function bindDashboardActions(actions) {
  actions.forEach(({ el, action, payload }) => {
    const handler = (e) => {
      if (e.type === 'keydown' && e.key !== 'Enter' && e.key !== ' ') return;
      if (e.type === 'keydown') e.preventDefault();

      switch (action) {
        case 'open_module':
          openModuleSession(payload);
          break;
        case 'open_recent':
          openRecentSession(payload);
          break;
        case 'open_template':
          openTemplate(payload);
          break;
      }
    };
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });
}

function bindCommandBarActions(actions) {
  actions.forEach(({ el, action, payload }) => {
    const handler = (e) => {
      if (e.type === 'keydown' && e.key !== 'Enter' && e.key !== ' ') return;
      if (e.type === 'keydown') e.preventDefault();

      switch (action) {
        case 'set_bar_state':
          state.commandBarState = payload;
          render();
          break;
        case 'add_frame':
          createFrame(state, payload);
          state.commandBarState = 'main';
          render();
          break;
        case 'set_layout':
          setLayout(state, payload);
          state.commandBarState = 'main';
          render();
          break;
        case 'toggle_minimize':
          toggleMinimize(state, payload);
          render();
          break;
        case 'toggle_maximize':
          toggleMaximize(state, payload);
          render();
          break;
        case 'remove_frame':
          removeFrame(state, payload);
          state.commandBarState = 'main';
          render();
          break;
      }
    };
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });
}

function bindFrameActions(container) {
  /* delegate click on frame windows for selection */
  container.addEventListener('click', (e) => {
    /* traffic light buttons */
    const dot = e.target.closest('.traffic-dot');
    if (dot) {
      const frameId = dot.getAttribute('data-frame');
      const action = dot.getAttribute('data-action');
      if (action === 'close') {
        removeFrame(state, frameId);
        state.commandBarState = 'main';
      } else if (action === 'minimize') {
        toggleMinimize(state, frameId);
      } else if (action === 'maximize') {
        toggleMaximize(state, frameId);
      }
      render();
      return;
    }

    /* tab strip items (fullscreen mode) */
    const tab = e.target.closest('.tab-strip-item');
    if (tab) {
      const frameId = tab.getAttribute('data-frame');
      if (frameId) {
        selectFrame(state, frameId);
        render();
      }
      return;
    }

    /* workflow step items */
    const step = e.target.closest('.workflow-step');
    if (step) {
      const frameId = step.getAttribute('data-frame');
      if (frameId) {
        selectFrame(state, frameId);
        render();
      }
      return;
    }

    /* carousel navigation arrows */
    const carouselNav = e.target.closest('.carousel-nav');
    if (carouselNav) {
      const frameId = carouselNav.getAttribute('data-frame');
      if (frameId) {
        selectFrame(state, frameId);
        render();
      }
      return;
    }

    /* frame window selection */
    const frameEl = e.target.closest('.frame-window');
    if (frameEl) {
      selectFrame(state, frameEl.id);
      render();
    }
  });
}

/* ================================================================
   SESSION MANAGEMENT
   ================================================================ */

function openModuleSession(mod) {
  state.view = 'session';
  state.sessionName = mod.name;
  state.sessionId = null;
  state.sessionCreatedAt = null;
  state.frames = [];
  state.selectedFrameId = null;
  state.layoutMode = 'grid';
  state.commandBarState = 'main';
  createFrame(state, mod.contentType);
  render();
}

function openRecentSession(session) {
  state.view = 'session';
  state.sessionName = session.title;
  state.sessionId = null;
  state.sessionCreatedAt = null;
  state.frames = [];
  state.selectedFrameId = null;
  state.layoutMode = 'grid';
  state.commandBarState = 'main';

  /* create placeholder frames */
  const types = ['web', 'note', 'pdf', 'video', 'image'];
  for (let i = 0; i < session.frames; i++) {
    createFrame(state, types[i % types.length]);
  }
  render();
}

function openTemplate(tmpl) {
  state.view = 'session';
  state.sessionName = tmpl.name;
  state.sessionId = null;
  state.sessionCreatedAt = null;
  state.frames = [];
  state.selectedFrameId = null;
  state.layoutMode = tmpl.frames.length <= 2 ? 'split' : 'grid';
  state.commandBarState = 'main';

  tmpl.frames.forEach(type => createFrame(state, type));
  render();
}

function goToDashboard() {
  state.view = 'dashboard';
  state.sessionName = 'Cockpit';
  state.sessionId = null;
  state.sessionCreatedAt = null;
  state.frames = [];
  state.selectedFrameId = null;
  state.commandBarState = 'main';

  /* clear the active session pointer so reload goes to dashboard */
  saveAppState('currentSessionId', null).catch(() => {});

  render();
}

/* ================================================================
   INIT
   ================================================================ */

async function init() {
  /* cache DOM refs */
  $topBarBack = document.getElementById('topBarBack');
  $topBarTitle = document.getElementById('topBarTitle');
  $contentWrapper = document.getElementById('contentWrapper');
  $statusBar = document.getElementById('statusBar');
  $commandBar = document.getElementById('commandBar');
  $themePanelOverlay = document.getElementById('themePanelOverlay');
  $themePanel = document.getElementById('themePanel');

  /* init theme engine */
  initThemeEngine(state);

  /* init PseudoSpatial parallax */
  initPseudoSpatial(state.pseudoSpatial);

  /* ---- Restore session from IndexedDB ---- */
  try {
    await initDB();
    const entry = await loadAppState('currentSessionId');
    const lastSessionId = entry ? entry.value : null;
    if (lastSessionId) {
      const session = await loadSession(lastSessionId);
      if (session && session.frames && session.frames.length > 0) {
        state.view = 'session';
        state.sessionId = session.id;
        state.sessionName = session.name;
        state.frames = session.frames;
        state.selectedFrameId = session.selectedFrameId;
        state.layoutMode = session.layoutMode || 'grid';
        state.sessionCreatedAt = session.createdAt;
      }
    }
  } catch (e) {
    console.warn('[Cockpit] IndexedDB restore failed:', e);
  }

  /* top bar back button */
  $topBarBack.addEventListener('click', goToDashboard);
  $topBarBack.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      goToDashboard();
    }
  });

  /* theme panel toggle */
  const $themeBtn = document.getElementById('themeToggleBtn');
  $themeBtn.addEventListener('click', () => {
    $themePanelOverlay.classList.toggle('visible');
  });

  /* close theme panel on overlay click */
  $themePanelOverlay.addEventListener('click', (e) => {
    if (e.target === $themePanelOverlay) {
      $themePanelOverlay.classList.remove('visible');
    }
  });

  /* close theme panel on Escape */
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && $themePanelOverlay.classList.contains('visible')) {
      $themePanelOverlay.classList.remove('visible');
    }
  });

  /* initial render */
  render();
}

/* Boot */
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
