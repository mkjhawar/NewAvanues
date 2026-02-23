/* ============================================================
   Cockpit Browser — layout-engine.js
   Layout mode renderer for 4 modes: GRID, FULLSCREEN, SPLIT, ROW.
   Builds the frame-area DOM tree according to the active layout.
   ============================================================ */

import { buildFrameElement } from './frame-manager.js';

const LAYOUT_MODES = {
  grid:       { label: 'Grid',       icon: '\u25A6', desc: 'Auto-fit grid' },
  fullscreen: { label: 'Fullscreen', icon: '\u2B1C', desc: 'Single frame' },
  split:      { label: 'Split',      icon: '\u25E7', desc: '60/40 split' },
  row:        { label: 'Row',        icon: '\u2261', desc: 'Horizontal row' },
};

function setLayout(state, mode) {
  if (!LAYOUT_MODES[mode]) return;
  state.layoutMode = mode;
}

/**
 * Render the frame-area content based on layout mode.
 * Returns the DOM element to insert into session-view.
 */
function renderLayout(state) {
  const { frames, selectedFrameId, layoutMode } = state;
  const visibleFrames = frames; /* show all frames; minimized ones collapse via CSS */
  const container = document.createElement('div');
  container.className = 'frame-area';

  if (visibleFrames.length === 0) {
    const empty = document.createElement('div');
    empty.style.cssText = 'display:flex;align-items:center;justify-content:center;flex:1;color:var(--av-text-tertiary);font-size:14px;';
    empty.textContent = 'No frames. Use the command bar to add one.';
    container.appendChild(empty);
    return container;
  }

  switch (layoutMode) {

    case 'grid':
      renderGrid(container, visibleFrames, selectedFrameId);
      break;

    case 'fullscreen':
      renderFullscreen(container, visibleFrames, selectedFrameId);
      break;

    case 'split':
      renderSplit(container, visibleFrames, selectedFrameId);
      break;

    case 'row':
      renderRow(container, visibleFrames, selectedFrameId);
      break;

    default:
      renderGrid(container, visibleFrames, selectedFrameId);
  }

  return container;
}

/* ---- GRID ---- */
function renderGrid(container, frames, selectedId) {
  frames.forEach(frame => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    container.appendChild(el);
  });
}

/* ---- FULLSCREEN ---- */
function renderFullscreen(container, frames, selectedId) {
  /* tab strip at top */
  const tabStrip = document.createElement('div');
  tabStrip.className = 'tab-strip';
  tabStrip.setAttribute('role', 'tablist');
  tabStrip.setAttribute('aria-label', 'Frame tabs');

  frames.forEach(frame => {
    const tab = document.createElement('button');
    tab.className = 'tab-strip-item' + (frame.id === selectedId ? ' active' : '');
    tab.setAttribute('role', 'tab');
    tab.setAttribute('aria-selected', frame.id === selectedId ? 'true' : 'false');
    tab.setAttribute('aria-label', frame.title);
    tab.setAttribute('data-avid', 'TAB-' + frame.id);
    tab.setAttribute('data-frame', frame.id);
    tab.setAttribute('data-action', 'select');

    const icon = document.createElement('span');
    icon.className = 'tab-icon';
    icon.textContent = getContentTypeIcon(frame.contentType);
    icon.setAttribute('aria-hidden', 'true');

    const label = document.createElement('span');
    label.textContent = frame.title;

    tab.appendChild(icon);
    tab.appendChild(label);
    tabStrip.appendChild(tab);
  });

  container.appendChild(tabStrip);

  /* selected frame fills remaining space */
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  if (selected) {
    const wrapper = document.createElement('div');
    wrapper.className = 'fullscreen-frame-container';
    const el = buildFrameElement(selected, true);
    wrapper.appendChild(el);
    container.appendChild(wrapper);
  }
}

/* ---- SPLIT ---- */
function renderSplit(container, frames, selectedId) {
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  const others = frames.filter(f => f.id !== selected.id);

  const primary = document.createElement('div');
  primary.className = 'split-primary';
  const primaryEl = buildFrameElement(selected, true);
  primary.appendChild(primaryEl);

  const secondary = document.createElement('div');
  secondary.className = 'split-secondary';
  if (others.length === 0) {
    const hint = document.createElement('div');
    hint.style.cssText = 'display:flex;align-items:center;justify-content:center;flex:1;color:var(--av-text-tertiary);font-size:13px;';
    hint.textContent = 'Add more frames to see split view';
    secondary.appendChild(hint);
  } else {
    others.forEach(frame => {
      const el = buildFrameElement(frame, false);
      secondary.appendChild(el);
    });
  }

  container.appendChild(primary);
  container.appendChild(secondary);
}

/* ---- ROW ---- */
function renderRow(container, frames, selectedId) {
  frames.forEach(frame => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    container.appendChild(el);
  });
}

/* helper */
function getContentTypeIcon(contentType) {
  const map = {
    web: '\uD83C\uDF10', pdf: '\uD83D\uDCC4', image: '\uD83D\uDDBC\uFE0F',
    video: '\uD83C\uDFA5', note: '\uD83D\uDCDD', terminal: '\u2328\uFE0F',
    whiteboard: '\uD83D\uDDBC\uFE0F', camera: '\uD83D\uDCF7', media: '\uD83C\uDFB5',
    photo: '\uD83D\uDCF8',
  };
  return map[contentType] || '\uD83D\uDCC1';
}

export {
  LAYOUT_MODES,
  setLayout,
  renderLayout,
};
