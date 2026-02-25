/* ============================================================
   Cockpit Browser — layout-engine.js
   Layout mode renderer for 14 modes matching KMP Cockpit LayoutMode.
   Builds the frame-area DOM tree according to the active layout.
   ============================================================ */

import { buildFrameElement } from './frame-manager.js';

const LAYOUT_MODES = {
  grid:         { label: 'Grid',         icon: '\u25A6', desc: 'Auto-fit grid' },
  fullscreen:   { label: 'Fullscreen',   icon: '\u2B1C', desc: 'Single frame' },
  split:        { label: 'Split Left',   icon: '\u25E7', desc: '60/40 split left' },
  split_right:  { label: 'Split Right',  icon: '\u25E9', desc: '60/40 split right' },
  row:          { label: 'Row',          icon: '\u2261', desc: 'Horizontal row' },
  freeform:     { label: 'Freeform',     icon: '\u2B50', desc: 'Free position' },
  cockpit:      { label: 'Flight Deck',  icon: '\u2708', desc: '6-slot instrument panel' },
  t_panel:      { label: 'T-Panel',      icon: '\u2534', desc: 'Top primary + bottom row' },
  mosaic:       { label: 'Mosaic',       icon: '\u25A8', desc: 'Primary + tiled' },
  workflow:     { label: 'Workflow',      icon: '\u2630', desc: 'Step sidebar + content' },
  carousel:     { label: 'Carousel',     icon: '\u27F3', desc: '3D card swipe' },
  spatial_dice: { label: 'Dice-5',       icon: '\u2685', desc: '4 corners + center' },
  gallery:      { label: 'Gallery',      icon: '\u25A3', desc: 'Media masonry' },
  triptych:     { label: 'Triptych',     icon: '\u2637', desc: '3-panel book spread' },
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

    case 'split_right':
      renderSplitRight(container, visibleFrames, selectedFrameId);
      break;

    case 'row':
      renderRow(container, visibleFrames, selectedFrameId);
      break;

    case 'freeform':
      renderFreeform(container, visibleFrames, selectedFrameId);
      break;

    case 'cockpit':
      renderCockpit(container, visibleFrames, selectedFrameId);
      break;

    case 't_panel':
      renderTPanel(container, visibleFrames, selectedFrameId);
      break;

    case 'mosaic':
      renderMosaic(container, visibleFrames, selectedFrameId);
      break;

    case 'workflow':
      renderWorkflow(container, visibleFrames, selectedFrameId, state);
      break;

    case 'carousel':
      renderCarousel(container, visibleFrames, selectedFrameId);
      break;

    case 'spatial_dice':
      renderSpatialDice(container, visibleFrames, selectedFrameId);
      break;

    case 'gallery':
      renderGallery(container, visibleFrames, selectedFrameId);
      break;

    case 'triptych':
      renderTriptych(container, visibleFrames, selectedFrameId);
      break;

    default:
      renderGrid(container, visibleFrames, selectedFrameId);
  }

  return container;
}

/* ==================================================================
   GRID
   ================================================================== */
function renderGrid(container, frames, selectedId) {
  frames.forEach(frame => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    container.appendChild(el);
  });
}

/* ==================================================================
   FULLSCREEN
   ================================================================== */
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

/* ==================================================================
   SPLIT (left primary)
   ================================================================== */
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

/* ==================================================================
   SPLIT RIGHT (right primary — mirror of split)
   ================================================================== */
function renderSplitRight(container, frames, selectedId) {
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  const others = frames.filter(f => f.id !== selected.id);

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

  const primary = document.createElement('div');
  primary.className = 'split-primary';
  const primaryEl = buildFrameElement(selected, true);
  primary.appendChild(primaryEl);

  /* secondary first, primary second — CSS flex-direction: row gives left-to-right */
  container.appendChild(secondary);
  container.appendChild(primary);
}

/* ==================================================================
   ROW
   ================================================================== */
function renderRow(container, frames, selectedId) {
  frames.forEach(frame => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    container.appendChild(el);
  });
}

/* ==================================================================
   FREEFORM — absolute positioned, draggable frames
   ================================================================== */
function renderFreeform(container, frames, selectedId) {
  frames.forEach((frame, index) => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    el.style.position = 'absolute';
    el.style.top = (20 + index * 30) + 'px';
    el.style.left = (20 + index * 30) + 'px';
    el.style.width = '45%';
    el.style.minHeight = '220px';
    el.style.zIndex = String(frame.id === selectedId ? 100 : 10 + index);
    el.style.cursor = 'grab';
    el.style.transition = 'box-shadow 0.2s ease';

    /* drag behavior */
    let isDragging = false;
    let startX = 0;
    let startY = 0;
    let origLeft = 0;
    let origTop = 0;

    const header = el.querySelector('.frame-header');
    if (header) {
      header.addEventListener('mousedown', (e) => {
        /* skip if clicking traffic light buttons */
        if (e.target.closest('.traffic-dot')) return;
        isDragging = true;
        startX = e.clientX;
        startY = e.clientY;
        origLeft = parseInt(el.style.left, 10) || 0;
        origTop = parseInt(el.style.top, 10) || 0;
        el.style.cursor = 'grabbing';
        el.style.zIndex = '200';
        e.preventDefault();
      });
    }

    document.addEventListener('mousemove', (e) => {
      if (!isDragging) return;
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      el.style.left = (origLeft + dx) + 'px';
      el.style.top = (origTop + dy) + 'px';
    });

    document.addEventListener('mouseup', () => {
      if (!isDragging) return;
      isDragging = false;
      el.style.cursor = 'grab';
      el.style.zIndex = frame.id === selectedId ? '100' : String(10 + index);
    });

    container.appendChild(el);
  });
}

/* ==================================================================
   COCKPIT (Flight Deck) — instrument panel layout
   1 frame  = fullscreen
   2 frames = side by side
   3-4      = 2 top + rest bottom
   5-6      = 1 status strip + 2 main + rest bottom
   ================================================================== */
function renderCockpit(container, frames, selectedId) {
  const count = frames.length;

  if (count === 1) {
    const el = buildFrameElement(frames[0], true);
    el.style.flex = '1';
    container.appendChild(el);
    return;
  }

  if (count === 2) {
    const row = document.createElement('div');
    row.className = 'cockpit-row cockpit-main-row';
    frames.forEach(frame => {
      const el = buildFrameElement(frame, frame.id === selectedId);
      row.appendChild(el);
    });
    container.appendChild(row);
    return;
  }

  if (count <= 4) {
    /* 2 top, rest bottom */
    const topRow = document.createElement('div');
    topRow.className = 'cockpit-row cockpit-top-row';
    for (let i = 0; i < 2 && i < count; i++) {
      const el = buildFrameElement(frames[i], frames[i].id === selectedId);
      topRow.appendChild(el);
    }
    container.appendChild(topRow);

    const bottomRow = document.createElement('div');
    bottomRow.className = 'cockpit-row cockpit-bottom-row';
    for (let i = 2; i < count; i++) {
      const el = buildFrameElement(frames[i], frames[i].id === selectedId);
      bottomRow.appendChild(el);
    }
    container.appendChild(bottomRow);
    return;
  }

  /* 5-6: status strip (1) + main row (2) + bottom row (rest) */
  const statusStrip = document.createElement('div');
  statusStrip.className = 'cockpit-row cockpit-status-strip';
  const statusEl = buildFrameElement(frames[0], frames[0].id === selectedId);
  statusStrip.appendChild(statusEl);
  container.appendChild(statusStrip);

  const mainRow = document.createElement('div');
  mainRow.className = 'cockpit-row cockpit-main-row';
  for (let i = 1; i < 3 && i < count; i++) {
    const el = buildFrameElement(frames[i], frames[i].id === selectedId);
    mainRow.appendChild(el);
  }
  container.appendChild(mainRow);

  const bottomRow = document.createElement('div');
  bottomRow.className = 'cockpit-row cockpit-bottom-row';
  for (let i = 3; i < count; i++) {
    const el = buildFrameElement(frames[i], frames[i].id === selectedId);
    bottomRow.appendChild(el);
  }
  container.appendChild(bottomRow);
}

/* ==================================================================
   T-PANEL — 60% top primary, 40% bottom row
   ================================================================== */
function renderTPanel(container, frames, selectedId) {
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  const others = frames.filter(f => f.id !== selected.id);

  const topSection = document.createElement('div');
  topSection.className = 'tpanel-top';
  const primaryEl = buildFrameElement(selected, true);
  topSection.appendChild(primaryEl);
  container.appendChild(topSection);

  if (others.length > 0) {
    const bottomSection = document.createElement('div');
    bottomSection.className = 'tpanel-bottom';
    others.forEach(frame => {
      const el = buildFrameElement(frame, false);
      bottomSection.appendChild(el);
    });
    container.appendChild(bottomSection);
  }
}

/* ==================================================================
   MOSAIC — selected frame 50% left, others tiled right column
   ================================================================== */
function renderMosaic(container, frames, selectedId) {
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  const others = frames.filter(f => f.id !== selected.id);

  const primary = document.createElement('div');
  primary.className = 'mosaic-primary';
  const primaryEl = buildFrameElement(selected, true);
  primary.appendChild(primaryEl);
  container.appendChild(primary);

  if (others.length > 0) {
    const tiledColumn = document.createElement('div');
    tiledColumn.className = 'mosaic-tiles';
    others.forEach(frame => {
      const el = buildFrameElement(frame, false);
      tiledColumn.appendChild(el);
    });
    container.appendChild(tiledColumn);
  }
}

/* ==================================================================
   WORKFLOW — numbered step sidebar + content area
   ================================================================== */
function renderWorkflow(container, frames, selectedId, state) {
  const sidebar = document.createElement('div');
  sidebar.className = 'workflow-sidebar';

  const sidebarTitle = document.createElement('div');
  sidebarTitle.className = 'workflow-sidebar-title';
  sidebarTitle.textContent = 'Steps';
  sidebar.appendChild(sidebarTitle);

  frames.forEach((frame, index) => {
    const step = document.createElement('button');
    step.className = 'workflow-step' + (frame.id === selectedId ? ' active' : '');
    step.setAttribute('data-avid', 'BTN-workflow-step-' + frame.id);
    step.setAttribute('data-frame', frame.id);
    step.setAttribute('data-action', 'select');
    step.setAttribute('role', 'tab');
    step.setAttribute('aria-selected', frame.id === selectedId ? 'true' : 'false');
    step.setAttribute('aria-label', 'Step ' + (index + 1) + ': ' + frame.title);

    const badge = document.createElement('span');
    badge.className = 'workflow-step-badge';
    badge.textContent = String(index + 1);

    const label = document.createElement('span');
    label.className = 'workflow-step-label';
    label.textContent = frame.title;

    step.appendChild(badge);
    step.appendChild(label);
    sidebar.appendChild(step);
  });

  container.appendChild(sidebar);

  /* Content area — show selected frame */
  const contentArea = document.createElement('div');
  contentArea.className = 'workflow-content';
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  if (selected) {
    const el = buildFrameElement(selected, true);
    contentArea.appendChild(el);
  }
  container.appendChild(contentArea);
}

/* ==================================================================
   CAROUSEL — 3D perspective card swipe (prev / current / next)
   ================================================================== */
function renderCarousel(container, frames, selectedId) {
  const track = document.createElement('div');
  track.className = 'carousel-track';

  const selectedIndex = Math.max(0, frames.findIndex(f => f.id === selectedId));

  /* navigation: previous arrow */
  if (frames.length > 1) {
    const prevBtn = document.createElement('button');
    prevBtn.className = 'carousel-nav carousel-prev';
    prevBtn.setAttribute('data-avid', 'BTN-carousel-prev');
    prevBtn.setAttribute('aria-label', 'Previous frame');
    prevBtn.textContent = '\u2039';
    const prevIndex = (selectedIndex - 1 + frames.length) % frames.length;
    prevBtn.setAttribute('data-frame', frames[prevIndex].id);
    prevBtn.setAttribute('data-action', 'select');
    container.appendChild(prevBtn);
  }

  /* render 3 cards: prev, current, next */
  const indices = [];
  if (frames.length === 1) {
    indices.push({ idx: 0, position: 'center' });
  } else if (frames.length === 2) {
    const prevI = (selectedIndex - 1 + frames.length) % frames.length;
    indices.push({ idx: prevI, position: 'left' });
    indices.push({ idx: selectedIndex, position: 'center' });
  } else {
    const prevI = (selectedIndex - 1 + frames.length) % frames.length;
    const nextI = (selectedIndex + 1) % frames.length;
    indices.push({ idx: prevI, position: 'left' });
    indices.push({ idx: selectedIndex, position: 'center' });
    indices.push({ idx: nextI, position: 'right' });
  }

  indices.forEach(({ idx, position }) => {
    const frame = frames[idx];
    const card = document.createElement('div');
    card.className = 'carousel-card carousel-' + position;

    const el = buildFrameElement(frame, position === 'center');
    card.appendChild(el);
    track.appendChild(card);
  });

  container.appendChild(track);

  /* navigation: next arrow */
  if (frames.length > 1) {
    const nextBtn = document.createElement('button');
    nextBtn.className = 'carousel-nav carousel-next';
    nextBtn.setAttribute('data-avid', 'BTN-carousel-next');
    nextBtn.setAttribute('aria-label', 'Next frame');
    nextBtn.textContent = '\u203A';
    const nextIndex = (selectedIndex + 1) % frames.length;
    nextBtn.setAttribute('data-frame', frames[nextIndex].id);
    nextBtn.setAttribute('data-action', 'select');
    container.appendChild(nextBtn);
  }
}

/* ==================================================================
   SPATIAL DICE — 4 corners + 1 center (dice-5 pattern)
   ================================================================== */
function renderSpatialDice(container, frames, selectedId) {
  /* corner grid */
  const grid = document.createElement('div');
  grid.className = 'dice-grid';

  /* up to 4 corner frames */
  const cornerFrames = frames.filter(f => f.id !== selectedId).slice(0, 4);
  cornerFrames.forEach(frame => {
    const cell = document.createElement('div');
    cell.className = 'dice-corner';
    const el = buildFrameElement(frame, false);
    cell.appendChild(el);
    grid.appendChild(cell);
  });

  /* fill empty corners if fewer than 4 */
  for (let i = cornerFrames.length; i < 4; i++) {
    const empty = document.createElement('div');
    empty.className = 'dice-corner dice-corner-empty';
    grid.appendChild(empty);
  }

  container.appendChild(grid);

  /* center overlay */
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  if (selected) {
    const center = document.createElement('div');
    center.className = 'dice-center';
    const el = buildFrameElement(selected, true);
    center.appendChild(el);
    container.appendChild(center);
  }
}

/* ==================================================================
   GALLERY — masonry-like media grid
   ================================================================== */
function renderGallery(container, frames, selectedId) {
  frames.forEach(frame => {
    const el = buildFrameElement(frame, frame.id === selectedId);
    container.appendChild(el);
  });
}

/* ==================================================================
   TRIPTYCH — 3-panel book spread with CSS perspective + rotateY
   ================================================================== */
function renderTriptych(container, frames, selectedId) {
  const selected = frames.find(f => f.id === selectedId) || frames[0];
  const others = frames.filter(f => f.id !== selected.id);

  /* left panel */
  const leftPanel = document.createElement('div');
  leftPanel.className = 'triptych-panel triptych-left';
  if (others.length > 0) {
    const el = buildFrameElement(others[0], false);
    leftPanel.appendChild(el);
  } else {
    leftPanel.innerHTML = '<div class="triptych-empty">Add frames</div>';
  }

  /* center panel */
  const centerPanel = document.createElement('div');
  centerPanel.className = 'triptych-panel triptych-center';
  const centerEl = buildFrameElement(selected, true);
  centerPanel.appendChild(centerEl);

  /* right panel */
  const rightPanel = document.createElement('div');
  rightPanel.className = 'triptych-panel triptych-right';
  if (others.length > 1) {
    const el = buildFrameElement(others[1], false);
    rightPanel.appendChild(el);
  } else {
    rightPanel.innerHTML = '<div class="triptych-empty">Add frames</div>';
  }

  container.appendChild(leftPanel);
  container.appendChild(centerPanel);
  container.appendChild(rightPanel);
}

/* ==================================================================
   HELPERS
   ================================================================== */
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
