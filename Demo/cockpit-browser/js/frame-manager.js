/* ============================================================
   Cockpit Browser — frame-manager.js
   Frame CRUD, content type registry, DOM generation.
   ============================================================ */

/* Content type registry with icons and labels */
const CONTENT_TYPES = {
  web:        { icon: '\uD83C\uDF10', label: 'Web',        desc: 'Web browser frame' },
  pdf:        { icon: '\uD83D\uDCC4', label: 'PDF',        desc: 'PDF document viewer' },
  image:      { icon: '\uD83D\uDDBC\uFE0F', label: 'Image',   desc: 'Image viewer' },
  video:      { icon: '\uD83C\uDFA5', label: 'Video',      desc: 'Video player' },
  note:       { icon: '\uD83D\uDCDD', label: 'Note',       desc: 'Rich text notes' },
  terminal:   { icon: '\u2328\uFE0F', label: 'Terminal',   desc: 'Terminal emulator' },
  whiteboard: { icon: '\uD83D\uDDBC\uFE0F', label: 'Whiteboard', desc: 'Drawing canvas' },
  camera:     { icon: '\uD83D\uDCF7', label: 'Camera',     desc: 'Camera feed' },
  media:      { icon: '\uD83C\uDFB5', label: 'Media',      desc: 'Audio player' },
  photo:      { icon: '\uD83D\uDCF8', label: 'Photo',      desc: 'Photo gallery' },
};

let nextFrameId = 1;

function createFrame(state, contentType) {
  const ct = CONTENT_TYPES[contentType];
  if (!ct) return null;

  const frame = {
    id: 'frame-' + nextFrameId++,
    title: ct.label + ' ' + (state.frames.filter(f => f.contentType === contentType).length + 1),
    contentType: contentType,
    minimized: false,
    maximized: false,
  };

  state.frames.push(frame);
  state.selectedFrameId = frame.id;
  return frame;
}

function removeFrame(state, frameId) {
  const idx = state.frames.findIndex(f => f.id === frameId);
  if (idx === -1) return;

  state.frames.splice(idx, 1);

  if (state.selectedFrameId === frameId) {
    state.selectedFrameId = state.frames.length > 0 ? state.frames[0].id : null;
  }

  /* if no frames remain, go back to dashboard */
  if (state.frames.length === 0) {
    state.view = 'dashboard';
    state.commandBarState = 'main';
  }
}

function toggleMinimize(state, frameId) {
  const frame = state.frames.find(f => f.id === frameId);
  if (!frame) return;
  frame.minimized = !frame.minimized;
  if (frame.minimized) {
    frame.maximized = false;
  }
}

function toggleMaximize(state, frameId) {
  const frame = state.frames.find(f => f.id === frameId);
  if (!frame) return;
  frame.maximized = !frame.maximized;
  if (frame.maximized) {
    frame.minimized = false;
    state.selectedFrameId = frameId;
  }
}

function selectFrame(state, frameId) {
  const frame = state.frames.find(f => f.id === frameId);
  if (!frame) return;
  state.selectedFrameId = frameId;
}

function getFrame(state, frameId) {
  return state.frames.find(f => f.id === frameId) || null;
}

function buildFrameElement(frame, isSelected) {
  const ct = CONTENT_TYPES[frame.contentType] || CONTENT_TYPES.web;
  const el = document.createElement('div');
  el.className = 'frame-window' + (isSelected ? ' selected' : '') + (frame.minimized ? ' minimized' : '');
  el.id = frame.id;
  el.setAttribute('data-avid', 'FRAME-' + frame.id);
  el.setAttribute('role', 'region');
  el.setAttribute('aria-label', frame.title + ' frame');
  el.tabIndex = 0;

  /* Header */
  const header = document.createElement('div');
  header.className = 'frame-header';

  const contentIcon = document.createElement('span');
  contentIcon.className = 'frame-content-icon';
  contentIcon.textContent = ct.icon;
  contentIcon.setAttribute('aria-hidden', 'true');

  const title = document.createElement('span');
  title.className = 'frame-title';
  title.textContent = frame.title;

  const trafficLights = document.createElement('div');
  trafficLights.className = 'traffic-lights';

  const redDot = document.createElement('button');
  redDot.className = 'traffic-dot red';
  redDot.setAttribute('aria-label', 'Close ' + frame.title);
  redDot.setAttribute('data-avid', 'BTN-close-' + frame.id);
  redDot.setAttribute('data-action', 'close');
  redDot.setAttribute('data-frame', frame.id);
  redDot.innerHTML = '&times;';

  const yellowDot = document.createElement('button');
  yellowDot.className = 'traffic-dot yellow';
  yellowDot.setAttribute('aria-label', 'Minimize ' + frame.title);
  yellowDot.setAttribute('data-avid', 'BTN-minimize-' + frame.id);
  yellowDot.setAttribute('data-action', 'minimize');
  yellowDot.setAttribute('data-frame', frame.id);
  yellowDot.innerHTML = '&minus;';

  const greenDot = document.createElement('button');
  greenDot.className = 'traffic-dot green';
  greenDot.setAttribute('aria-label', 'Maximize ' + frame.title);
  greenDot.setAttribute('data-avid', 'BTN-maximize-' + frame.id);
  greenDot.setAttribute('data-action', 'maximize');
  greenDot.setAttribute('data-frame', frame.id);
  greenDot.innerHTML = '+';

  trafficLights.appendChild(redDot);
  trafficLights.appendChild(yellowDot);
  trafficLights.appendChild(greenDot);

  header.appendChild(contentIcon);
  header.appendChild(title);
  header.appendChild(trafficLights);

  /* Content (placeholder for B1) */
  const content = document.createElement('div');
  content.className = 'frame-content';

  const placeholder = document.createElement('div');
  placeholder.className = 'frame-content-placeholder';

  const pIcon = document.createElement('div');
  pIcon.className = 'placeholder-icon';
  pIcon.textContent = ct.icon;
  pIcon.setAttribute('aria-hidden', 'true');

  const pLabel = document.createElement('div');
  pLabel.className = 'placeholder-label';
  pLabel.textContent = ct.label;

  const pHint = document.createElement('div');
  pHint.className = 'placeholder-hint';
  pHint.textContent = ct.desc;

  placeholder.appendChild(pIcon);
  placeholder.appendChild(pLabel);
  placeholder.appendChild(pHint);
  content.appendChild(placeholder);

  el.appendChild(header);
  el.appendChild(content);

  return el;
}

export {
  CONTENT_TYPES,
  createFrame,
  removeFrame,
  toggleMinimize,
  toggleMaximize,
  selectFrame,
  getFrame,
  buildFrameElement,
};
