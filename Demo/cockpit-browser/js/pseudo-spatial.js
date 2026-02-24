/* ============================================================
   Cockpit Browser — pseudo-spatial.js
   PseudoSpatial parallax engine matching KMP PseudoSpatialController.
   4 depth layers, gyroscope + mouse input, card tilt effect.
   ============================================================ */

/* ================================================================
   CONFIGURATION
   ================================================================ */

/** Parallax multipliers per depth layer (px max offset). */
const LAYER_CONFIG = [
  { layer: 0, multiplier: 0.3, maxPx: 12 },  /* background — subtle shift */
  { layer: 1, multiplier: 0.6, maxPx: 8  },   /* mid-ground — frames/panels */
  { layer: 2, multiplier: 1.0, maxPx: 4  },   /* foreground — active content */
  { layer: 3, multiplier: 0.0, maxPx: 0  },   /* HUD — locked in place */
];

/** Smoothing factor for input interpolation (lerp). */
const SMOOTHING = 0.15;

/** Mouse deadzone — center 10% of viewport produces no movement. */
const DEADZONE = 0.1;

/** Card tilt max degrees. */
const TILT_MAX_DEG = 3;

/* ================================================================
   STATE
   ================================================================ */

let enabled = false;
let animFrameId = null;

/** Normalized input (-1..+1) — raw from sensor/mouse. */
let rawX = 0;
let rawY = 0;

/** Smoothed input (-1..+1) — after lerp. */
let smoothX = 0;
let smoothY = 0;

/** Whether we have a gyroscope source active. */
let hasGyro = false;

/** Bound event handlers (stored for cleanup). */
let onDeviceOrientation = null;
let onMouseMove = null;

/** Card tilt: tracked frame elements with their handlers. */
const tiltHandlers = new WeakMap();
let tiltObserver = null;

/* ================================================================
   INPUT SOURCES
   ================================================================ */

/**
 * DeviceOrientation handler (mobile).
 * beta = tilt forward/back (-180..180), gamma = tilt left/right (-90..90).
 * We normalize to -1..+1 using sensible tilt ranges.
 */
function handleDeviceOrientation(e) {
  if (!enabled) return;
  hasGyro = true;

  /* beta: 0 = flat, ±90 = vertical. Map ±45 range to -1..+1. */
  const beta = e.beta || 0;
  rawY = clamp(beta / 45, -1, 1);

  /* gamma: 0 = upright, ±90 = tilted sideways. Map ±30 range to -1..+1. */
  const gamma = e.gamma || 0;
  rawX = clamp(gamma / 30, -1, 1);
}

/**
 * Mouse move handler (desktop fallback).
 * Maps cursor position relative to viewport center to -1..+1.
 */
function handleMouseMove(e) {
  if (!enabled || hasGyro) return;

  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const cx = vw / 2;
  const cy = vh / 2;

  /* Relative position from center: -1..+1 */
  let nx = (e.clientX - cx) / cx;
  let ny = (e.clientY - cy) / cy;

  /* Apply deadzone — center 10% produces zero */
  nx = applyDeadzone(nx, DEADZONE);
  ny = applyDeadzone(ny, DEADZONE);

  rawX = clamp(nx, -1, 1);
  rawY = clamp(ny, -1, 1);
}

/* ================================================================
   ANIMATION LOOP
   ================================================================ */

function tick() {
  if (!enabled) return;

  /* Lerp smoothing */
  smoothX = lerp(smoothX, rawX, SMOOTHING);
  smoothY = lerp(smoothY, rawY, SMOOTHING);

  /* Apply parallax transforms to all layered elements */
  for (let i = 0; i < LAYER_CONFIG.length; i++) {
    const cfg = LAYER_CONFIG[i];
    if (cfg.maxPx === 0) continue; /* layer 3 HUD — skip */

    const elements = document.querySelectorAll('[data-parallax-layer="' + cfg.layer + '"]');
    const offsetX = smoothX * cfg.maxPx;
    const offsetY = smoothY * cfg.maxPx;

    for (let j = 0; j < elements.length; j++) {
      elements[j].style.transform = 'translate3d(' + offsetX.toFixed(2) + 'px, ' + offsetY.toFixed(2) + 'px, 0)';
    }
  }

  animFrameId = requestAnimationFrame(tick);
}

/* ================================================================
   CARD TILT EFFECT
   ================================================================ */

/**
 * Attach tilt effect to a single .frame-window element.
 * On hover, applies subtle rotateX/rotateY based on cursor position
 * relative to the frame center (max ±3 degrees).
 */
function attachTilt(frameEl) {
  if (tiltHandlers.has(frameEl)) return; /* already attached */

  function onEnter() {
    if (!enabled) return;
    frameEl.style.transition = 'transform 0.2s ease-out, box-shadow 0.2s ease-out';
  }

  function onMove(e) {
    if (!enabled) return;

    const rect = frameEl.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;

    /* Normalized -1..+1 relative to frame center */
    const nx = clamp((e.clientX - cx) / (rect.width / 2), -1, 1);
    const ny = clamp((e.clientY - cy) / (rect.height / 2), -1, 1);

    /* rotateY for horizontal, rotateX for vertical (inverted for natural feel) */
    const rotY = nx * TILT_MAX_DEG;
    const rotX = -ny * TILT_MAX_DEG;

    /* Combine tilt with any parallax offset the parent layer may have */
    frameEl.style.transform = 'perspective(600px) rotateX(' + rotX.toFixed(2) + 'deg) rotateY(' + rotY.toFixed(2) + 'deg)';
  }

  function onLeave() {
    /* Reset to neutral — parallax loop will re-apply layer offset on next tick */
    frameEl.style.transition = 'transform 0.3s ease-out, box-shadow 0.3s ease-out';
    frameEl.style.transform = '';
  }

  frameEl.addEventListener('mouseenter', onEnter);
  frameEl.addEventListener('mousemove', onMove);
  frameEl.addEventListener('mouseleave', onLeave);

  tiltHandlers.set(frameEl, { onEnter, onMove, onLeave });
}

/**
 * Detach tilt effect from a single .frame-window element.
 */
function detachTilt(frameEl) {
  const handlers = tiltHandlers.get(frameEl);
  if (!handlers) return;

  frameEl.removeEventListener('mouseenter', handlers.onEnter);
  frameEl.removeEventListener('mousemove', handlers.onMove);
  frameEl.removeEventListener('mouseleave', handlers.onLeave);
  frameEl.style.transform = '';

  tiltHandlers.delete(frameEl);
}

/**
 * Scan DOM for .frame-window elements and attach tilt.
 * Uses MutationObserver to catch dynamically added frames.
 */
function setupTiltObserver() {
  /* Attach to any existing frames */
  document.querySelectorAll('.frame-window').forEach(attachTilt);

  /* Watch for new frames added to the DOM */
  if (tiltObserver) tiltObserver.disconnect();

  tiltObserver = new MutationObserver((mutations) => {
    if (!enabled) return;
    for (const mutation of mutations) {
      for (const node of mutation.addedNodes) {
        if (node.nodeType !== Node.ELEMENT_NODE) continue;
        if (node.classList && node.classList.contains('frame-window')) {
          attachTilt(node);
        }
        /* Also check descendants */
        if (node.querySelectorAll) {
          node.querySelectorAll('.frame-window').forEach(attachTilt);
        }
      }
      for (const node of mutation.removedNodes) {
        if (node.nodeType !== Node.ELEMENT_NODE) continue;
        if (node.classList && node.classList.contains('frame-window')) {
          detachTilt(node);
        }
        if (node.querySelectorAll) {
          node.querySelectorAll('.frame-window').forEach(detachTilt);
        }
      }
    }
  });

  tiltObserver.observe(document.body, { childList: true, subtree: true });
}

function teardownTiltObserver() {
  if (tiltObserver) {
    tiltObserver.disconnect();
    tiltObserver = null;
  }
  document.querySelectorAll('.frame-window').forEach(detachTilt);
}

/* ================================================================
   LIFECYCLE API
   ================================================================ */

/**
 * Initialize the PseudoSpatial parallax system.
 * @param {boolean} startEnabled — whether to activate immediately.
 */
function initPseudoSpatial(startEnabled) {
  if (typeof startEnabled === 'undefined') startEnabled = true;

  /* Register input listeners (always — they check `enabled` flag) */
  onDeviceOrientation = handleDeviceOrientation;
  onMouseMove = handleMouseMove;

  window.addEventListener('deviceorientation', onDeviceOrientation, { passive: true });
  window.addEventListener('mousemove', onMouseMove, { passive: true });

  if (startEnabled) {
    activateSpatial();
  }
}

/**
 * Enable or disable the PseudoSpatial effect at runtime.
 * @param {boolean} on
 */
function setPseudoSpatialEnabled(on) {
  if (on && !enabled) {
    activateSpatial();
  } else if (!on && enabled) {
    deactivateSpatial();
  }
}

/**
 * Completely destroy the PseudoSpatial system and clean up all listeners.
 */
function destroyPseudoSpatial() {
  deactivateSpatial();

  if (onDeviceOrientation) {
    window.removeEventListener('deviceorientation', onDeviceOrientation);
    onDeviceOrientation = null;
  }
  if (onMouseMove) {
    window.removeEventListener('mousemove', onMouseMove);
    onMouseMove = null;
  }
}

/* ================================================================
   INTERNAL ACTIVATE / DEACTIVATE
   ================================================================ */

function activateSpatial() {
  enabled = true;

  /* Add perspective class to the app container */
  const app = document.getElementById('app');
  if (app) {
    app.classList.add('pseudo-spatial-active');
  }

  /* Start the animation loop */
  if (animFrameId === null) {
    animFrameId = requestAnimationFrame(tick);
  }

  /* Setup card tilt observer */
  setupTiltObserver();
}

function deactivateSpatial() {
  enabled = false;

  /* Cancel animation loop */
  if (animFrameId !== null) {
    cancelAnimationFrame(animFrameId);
    animFrameId = null;
  }

  /* Remove perspective class */
  const app = document.getElementById('app');
  if (app) {
    app.classList.remove('pseudo-spatial-active');
  }

  /* Reset all parallax transforms */
  for (let i = 0; i < LAYER_CONFIG.length; i++) {
    const elements = document.querySelectorAll('[data-parallax-layer="' + LAYER_CONFIG[i].layer + '"]');
    for (let j = 0; j < elements.length; j++) {
      elements[j].style.transform = '';
    }
  }

  /* Reset smoothed values */
  smoothX = 0;
  smoothY = 0;
  rawX = 0;
  rawY = 0;
  hasGyro = false;

  /* Teardown tilt */
  teardownTiltObserver();
}

/* ================================================================
   MATH UTILITIES
   ================================================================ */

function clamp(value, min, max) {
  return value < min ? min : value > max ? max : value;
}

function lerp(current, target, factor) {
  return current + (target - current) * factor;
}

/**
 * Apply deadzone: values within ±deadzone of 0 map to 0,
 * values outside ramp linearly from 0 to ±1.
 */
function applyDeadzone(value, deadzone) {
  const abs = Math.abs(value);
  if (abs < deadzone) return 0;
  const sign = value > 0 ? 1 : -1;
  return sign * ((abs - deadzone) / (1 - deadzone));
}

/* ================================================================
   EXPORTS
   ================================================================ */

export { initPseudoSpatial, setPseudoSpatialEnabled, destroyPseudoSpatial };
