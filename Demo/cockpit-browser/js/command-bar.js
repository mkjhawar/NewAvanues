/* ============================================================
   Cockpit Browser — command-bar.js
   Context-aware bottom command bar with pill chips.
   States: MAIN, ADD_FRAME, LAYOUT_PICKER, FRAME_ACTIONS
   ============================================================ */

import { CONTENT_TYPES } from './frame-manager.js';
import { LAYOUT_MODES } from './layout-engine.js';

/**
 * Render the command bar chips into the given container element.
 * Returns an array of { action, payload } bound to each chip for event handling.
 */
function renderCommandBar(container, state) {
  container.innerHTML = '';

  const chipsWrapper = document.createElement('div');
  chipsWrapper.className = 'command-bar-chips';

  const actions = [];

  switch (state.commandBarState) {

    case 'main':
      actions.push(...renderMainChips(chipsWrapper, state));
      break;

    case 'add_frame':
      actions.push(...renderAddFrameChips(chipsWrapper, state));
      break;

    case 'layout_picker':
      actions.push(...renderLayoutChips(chipsWrapper, state));
      break;

    case 'frame_actions':
      actions.push(...renderFrameActionChips(chipsWrapper, state));
      break;

    default:
      actions.push(...renderMainChips(chipsWrapper, state));
  }

  container.appendChild(chipsWrapper);
  return actions;
}

/* ---- MAIN state ---- */
function renderMainChips(wrapper, state) {
  const actions = [];

  /* Layout chip */
  const layoutChip = makeChip('\u25A6', 'Layout', 'NAV-layout');
  actions.push({ el: layoutChip, action: 'set_bar_state', payload: 'layout_picker' });
  wrapper.appendChild(layoutChip);

  /* Add Frame chip */
  const addChip = makeChip('+', 'Add Frame', 'BTN-add-frame');
  actions.push({ el: addChip, action: 'set_bar_state', payload: 'add_frame' });
  wrapper.appendChild(addChip);

  /* Selected frame chip (if a frame is selected) */
  if (state.selectedFrameId) {
    const frame = state.frames.find(f => f.id === state.selectedFrameId);
    if (frame) {
      const contentType = CONTENT_TYPES[frame.contentType];
      const frameChip = makeChip(contentType ? contentType.icon : '\uD83D\uDCC1', frame.title, 'NAV-frame-actions');
      frameChip.classList.add('active');
      actions.push({ el: frameChip, action: 'set_bar_state', payload: 'frame_actions' });
      wrapper.appendChild(frameChip);
    }
  }

  return actions;
}

/* ---- ADD_FRAME state ---- */
function renderAddFrameChips(wrapper, state) {
  const actions = [];

  /* back chip */
  const backChip = makeChip('\u2190', '', 'BTN-back-add');
  backChip.classList.add('cmd-chip-back');
  actions.push({ el: backChip, action: 'set_bar_state', payload: 'main' });
  wrapper.appendChild(backChip);

  /* one chip per content type */
  const displayTypes = ['web', 'pdf', 'image', 'video', 'note', 'terminal', 'whiteboard', 'camera', 'media', 'photo'];
  displayTypes.forEach(type => {
    const ct = CONTENT_TYPES[type];
    if (!ct) return;
    const chip = makeChip(ct.icon, ct.label, 'BTN-add-' + type);
    actions.push({ el: chip, action: 'add_frame', payload: type });
    wrapper.appendChild(chip);
  });

  return actions;
}

/* ---- LAYOUT_PICKER state ---- */
function renderLayoutChips(wrapper, state) {
  const actions = [];

  /* back chip */
  const backChip = makeChip('\u2190', '', 'BTN-back-layout');
  backChip.classList.add('cmd-chip-back');
  actions.push({ el: backChip, action: 'set_bar_state', payload: 'main' });
  wrapper.appendChild(backChip);

  /* one chip per layout mode */
  Object.entries(LAYOUT_MODES).forEach(([key, mode]) => {
    const chip = makeChip(mode.icon, mode.label, 'NAV-layout-' + key);
    if (state.layoutMode === key) {
      chip.classList.add('active');
    }
    actions.push({ el: chip, action: 'set_layout', payload: key });
    wrapper.appendChild(chip);
  });

  return actions;
}

/* ---- FRAME_ACTIONS state ---- */
function renderFrameActionChips(wrapper, state) {
  const actions = [];

  /* back chip */
  const backChip = makeChip('\u2190', '', 'BTN-back-frame');
  backChip.classList.add('cmd-chip-back');
  actions.push({ el: backChip, action: 'set_bar_state', payload: 'main' });
  wrapper.appendChild(backChip);

  if (state.selectedFrameId) {
    const frame = state.frames.find(f => f.id === state.selectedFrameId);
    if (frame) {
      /* Minimize chip */
      const minChip = makeChip(frame.minimized ? '\u25B3' : '\u25BD', frame.minimized ? 'Restore' : 'Minimize', 'BTN-min-frame');
      actions.push({ el: minChip, action: 'toggle_minimize', payload: frame.id });
      wrapper.appendChild(minChip);

      /* Maximize chip */
      const maxChip = makeChip(frame.maximized ? '\u25A3' : '\u25A1', frame.maximized ? 'Restore' : 'Maximize', 'BTN-max-frame');
      actions.push({ el: maxChip, action: 'toggle_maximize', payload: frame.id });
      wrapper.appendChild(maxChip);

      /* Close chip */
      const closeChip = makeChip('\u2715', 'Close', 'BTN-close-frame');
      actions.push({ el: closeChip, action: 'remove_frame', payload: frame.id });
      wrapper.appendChild(closeChip);
    }
  }

  return actions;
}

/* ---- Chip builder ---- */
function makeChip(iconText, label, avid) {
  const chip = document.createElement('button');
  chip.className = 'cmd-chip';
  chip.setAttribute('data-avid', avid);
  chip.setAttribute('aria-label', label || iconText);
  chip.setAttribute('role', 'button');
  chip.tabIndex = 0;

  if (iconText) {
    const icon = document.createElement('span');
    icon.className = 'chip-icon';
    icon.textContent = iconText;
    icon.setAttribute('aria-hidden', 'true');
    chip.appendChild(icon);
  }

  if (label) {
    const text = document.createElement('span');
    text.textContent = label;
    chip.appendChild(text);
  }

  return chip;
}

export {
  renderCommandBar,
};
