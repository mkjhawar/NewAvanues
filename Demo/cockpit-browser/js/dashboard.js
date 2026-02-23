/* ============================================================
   Cockpit Browser — dashboard.js
   Dashboard / launcher view: module tiles, recent sessions,
   templates, and footer.
   ============================================================ */

/* Module definitions with emoji icons for B1 */
const MODULES = [
  { id: 'voiceavanue',      icon: '\uD83C\uDF99\uFE0F', name: 'VoiceAvanue',      desc: 'Voice command interface',      contentType: 'web'        },
  { id: 'webavanue',        icon: '\uD83C\uDF10',       name: 'WebAvanue',        desc: 'Voice-first web browser',      contentType: 'web'        },
  { id: 'photoavanue',      icon: '\uD83D\uDCF8',       name: 'PhotoAvanue',      desc: 'Camera and photo gallery',     contentType: 'photo'      },
  { id: 'noteavanue',       icon: '\uD83D\uDCDD',       name: 'NoteAvanue',       desc: 'Voice-first rich notes',       contentType: 'note'       },
  { id: 'pdfavanue',        icon: '\uD83D\uDCC4',       name: 'PDFAvanue',        desc: 'PDF document viewer',          contentType: 'pdf'        },
  { id: 'videoavanue',      icon: '\uD83C\uDFA5',       name: 'VideoAvanue',      desc: 'Video player and editor',      contentType: 'video'      },
  { id: 'mediaavanue',      icon: '\uD83C\uDFB5',       name: 'MediaAvanue',      desc: 'Audio player and podcasts',    contentType: 'media'      },
  { id: 'whiteboardavanue', icon: '\uD83D\uDDBC\uFE0F', name: 'WhiteboardAvanue', desc: 'Drawing and annotation',       contentType: 'whiteboard' },
  { id: 'cameraavanue',     icon: '\uD83D\uDCF7',       name: 'CameraAvanue',     desc: 'Live camera feed',             contentType: 'camera'     },
  { id: 'terminalavanue',   icon: '\u2328\uFE0F',       name: 'TerminalAvanue',   desc: 'Terminal emulator',            contentType: 'terminal'   },
];

/* Sample recent sessions for the strip */
const RECENT_SESSIONS = [
  { id: 'recent-1', title: 'Research Notes',   frames: 3, ago: '2h ago'  },
  { id: 'recent-2', title: 'Code Review',      frames: 2, ago: '5h ago'  },
  { id: 'recent-3', title: 'Media Workspace',  frames: 4, ago: '1d ago'  },
  { id: 'recent-4', title: 'PDF Analysis',     frames: 1, ago: '2d ago'  },
  { id: 'recent-5', title: 'Web Browsing',     frames: 2, ago: '3d ago'  },
];

/* Templates */
const TEMPLATES = [
  { id: 'tmpl-research',  icon: '\uD83D\uDD2C', name: 'Research',    frames: ['web', 'note', 'pdf']       },
  { id: 'tmpl-media',     icon: '\uD83C\uDFAC', name: 'Media Edit',  frames: ['video', 'image', 'media']  },
  { id: 'tmpl-writing',   icon: '\u270D\uFE0F',  name: 'Writing',     frames: ['note', 'web']              },
  { id: 'tmpl-present',   icon: '\uD83D\uDCCA', name: 'Present',     frames: ['pdf', 'whiteboard']        },
];

/**
 * Build the full dashboard DOM and return it, along with action bindings.
 */
function renderDashboard() {
  const view = document.createElement('div');
  view.className = 'dashboard-view';
  view.setAttribute('role', 'main');
  view.setAttribute('aria-label', 'Cockpit Dashboard');

  const actions = [];

  /* ---- Module tiles ---- */
  const modulesTitle = sectionTitle('Modules');
  view.appendChild(modulesTitle);

  const grid = document.createElement('div');
  grid.className = 'dashboard-grid';

  MODULES.forEach(mod => {
    const tile = document.createElement('div');
    tile.className = 'dashboard-tile';
    tile.setAttribute('data-avid', 'BTN-module-' + mod.id);
    tile.setAttribute('role', 'button');
    tile.setAttribute('aria-label', 'Open ' + mod.name);
    tile.tabIndex = 0;
    tile.setAttribute('data-module', mod.id);

    const icon = document.createElement('div');
    icon.className = 'tile-icon';
    icon.textContent = mod.icon;
    icon.setAttribute('aria-hidden', 'true');

    const info = document.createElement('div');
    info.className = 'tile-info';

    const name = document.createElement('div');
    name.className = 'tile-name';
    name.textContent = mod.name;

    const desc = document.createElement('div');
    desc.className = 'tile-desc';
    desc.textContent = mod.desc;

    info.appendChild(name);
    info.appendChild(desc);
    tile.appendChild(icon);
    tile.appendChild(info);
    grid.appendChild(tile);

    actions.push({ el: tile, action: 'open_module', payload: mod });
  });

  view.appendChild(grid);

  /* ---- Recent sessions ---- */
  const recentTitle = sectionTitle('Recent Sessions');
  view.appendChild(recentTitle);

  const strip = document.createElement('div');
  strip.className = 'recent-strip';
  strip.setAttribute('role', 'list');
  strip.setAttribute('aria-label', 'Recent sessions');

  RECENT_SESSIONS.forEach(session => {
    const card = document.createElement('div');
    card.className = 'recent-card';
    card.setAttribute('data-avid', 'BTN-recent-' + session.id);
    card.setAttribute('role', 'listitem');
    card.tabIndex = 0;
    card.setAttribute('aria-label', session.title + ', ' + session.frames + ' frames, ' + session.ago);

    const title = document.createElement('div');
    title.className = 'recent-card-title';
    title.textContent = session.title;

    const meta = document.createElement('div');
    meta.className = 'recent-card-meta';
    meta.textContent = session.frames + ' frames \u00B7 ' + session.ago;

    card.appendChild(title);
    card.appendChild(meta);
    strip.appendChild(card);

    actions.push({ el: card, action: 'open_recent', payload: session });
  });

  view.appendChild(strip);

  /* ---- Templates ---- */
  const templatesTitle = sectionTitle('Templates');
  view.appendChild(templatesTitle);

  const templatesSection = document.createElement('div');
  templatesSection.className = 'templates-section';

  const templatesGrid = document.createElement('div');
  templatesGrid.className = 'templates-grid';

  TEMPLATES.forEach(tmpl => {
    const card = document.createElement('div');
    card.className = 'template-card';
    card.setAttribute('data-avid', 'BTN-template-' + tmpl.id);
    card.setAttribute('role', 'button');
    card.setAttribute('aria-label', tmpl.name + ' template with ' + tmpl.frames.length + ' frames');
    card.tabIndex = 0;

    const icon = document.createElement('div');
    icon.className = 'template-icon';
    icon.textContent = tmpl.icon;
    icon.setAttribute('aria-hidden', 'true');

    const name = document.createElement('div');
    name.className = 'template-name';
    name.textContent = tmpl.name;

    card.appendChild(icon);
    card.appendChild(name);
    templatesGrid.appendChild(card);

    actions.push({ el: card, action: 'open_template', payload: tmpl });
  });

  templatesSection.appendChild(templatesGrid);
  view.appendChild(templatesSection);

  /* ---- Footer ---- */
  const footer = document.createElement('div');
  footer.className = 'dashboard-footer';
  footer.textContent = 'VoiceOS\u00AE Avanues EcoSystem';
  view.appendChild(footer);

  return { element: view, actions };
}

function sectionTitle(text) {
  const el = document.createElement('div');
  el.className = 'dashboard-section-title';
  el.textContent = text;
  return el;
}

export {
  MODULES,
  RECENT_SESSIONS,
  TEMPLATES,
  renderDashboard,
};
