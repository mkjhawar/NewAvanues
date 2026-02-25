/* ============================================================
   Cockpit Browser — persistence.js
   IndexedDB persistence layer for sessions and app state.
   Stores session data (frames, layout, selection) so they
   survive page reloads. Theme is handled separately by
   theme-engine.js via localStorage.
   ============================================================ */

const DB_NAME = 'cockpit-browser';
const DB_VERSION = 1;
const STORE_SESSIONS = 'sessions';
const STORE_APP_STATE = 'appState';

let db = null;

/* ================================================================
   DATABASE INIT
   ================================================================ */

/**
 * Open (or create) the IndexedDB database.
 * Creates object stores on version upgrade.
 * Returns the IDBDatabase instance.
 */
async function initDB() {
  if (db) return db;

  return new Promise((resolve, reject) => {
    let request;
    try {
      request = indexedDB.open(DB_NAME, DB_VERSION);
    } catch (err) {
      console.warn('[Cockpit/IDB] indexedDB.open failed:', err);
      reject(err);
      return;
    }

    request.onupgradeneeded = (event) => {
      const database = event.target.result;

      /* sessions store — keyed by id, indexed on name and updatedAt */
      if (!database.objectStoreNames.contains(STORE_SESSIONS)) {
        const sessionsStore = database.createObjectStore(STORE_SESSIONS, { keyPath: 'id' });
        sessionsStore.createIndex('name', 'name', { unique: false });
        sessionsStore.createIndex('updatedAt', 'updatedAt', { unique: false });
      }

      /* appState store — simple key-value pairs */
      if (!database.objectStoreNames.contains(STORE_APP_STATE)) {
        database.createObjectStore(STORE_APP_STATE, { keyPath: 'key' });
      }
    };

    request.onsuccess = (event) => {
      db = event.target.result;

      /* handle unexpected close (e.g. browser clearing storage) */
      db.onclose = () => {
        console.warn('[Cockpit/IDB] Database connection closed unexpectedly');
        db = null;
      };

      db.onerror = (evt) => {
        console.warn('[Cockpit/IDB] Database error:', evt.target.error);
      };

      resolve(db);
    };

    request.onerror = (event) => {
      console.warn('[Cockpit/IDB] Failed to open database:', event.target.error);
      reject(event.target.error);
    };

    request.onblocked = () => {
      console.warn('[Cockpit/IDB] Database open blocked — close other tabs using this DB');
    };
  });
}

/* ================================================================
   INTERNAL HELPERS
   ================================================================ */

/**
 * Get a transaction and object store, handling the case where
 * the database connection may have been lost.
 */
function getStore(storeName, mode) {
  if (!db) {
    throw new Error('Database not initialized — call initDB() first');
  }
  const tx = db.transaction(storeName, mode);
  return tx.objectStore(storeName);
}

/**
 * Wrap an IDBRequest in a Promise.
 */
function requestToPromise(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

/* ================================================================
   SESSION OPERATIONS
   ================================================================ */

/**
 * Save (upsert) a session into the sessions store.
 * Automatically sets `updatedAt` to now.
 *
 * @param {Object} session - Session object with at minimum { id, name, frames }
 * @returns {Promise<string>} The session ID
 */
async function saveSession(session) {
  try {
    const record = {
      ...session,
      updatedAt: new Date().toISOString(),
      createdAt: session.createdAt || new Date().toISOString(),
    };
    const store = getStore(STORE_SESSIONS, 'readwrite');
    await requestToPromise(store.put(record));
    return record.id;
  } catch (err) {
    console.warn('[Cockpit/IDB] saveSession failed:', err);
    return null;
  }
}

/**
 * Load a single session by its ID.
 *
 * @param {string} sessionId
 * @returns {Promise<Object|null>} The session object, or null if not found
 */
async function loadSession(sessionId) {
  try {
    const store = getStore(STORE_SESSIONS, 'readonly');
    const result = await requestToPromise(store.get(sessionId));
    return result || null;
  } catch (err) {
    console.warn('[Cockpit/IDB] loadSession failed:', err);
    return null;
  }
}

/**
 * List all sessions, sorted by updatedAt descending (most recent first).
 *
 * @returns {Promise<Array>} Array of session objects
 */
async function listSessions() {
  try {
    const store = getStore(STORE_SESSIONS, 'readonly');
    const allSessions = await requestToPromise(store.getAll());

    /* Sort by updatedAt descending */
    allSessions.sort((a, b) => {
      const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
      const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
      return dateB - dateA;
    });

    return allSessions;
  } catch (err) {
    console.warn('[Cockpit/IDB] listSessions failed:', err);
    return [];
  }
}

/**
 * Delete a session by its ID.
 *
 * @param {string} sessionId
 * @returns {Promise<boolean>} true if deletion succeeded
 */
async function deleteSession(sessionId) {
  try {
    const store = getStore(STORE_SESSIONS, 'readwrite');
    await requestToPromise(store.delete(sessionId));
    return true;
  } catch (err) {
    console.warn('[Cockpit/IDB] deleteSession failed:', err);
    return false;
  }
}

/* ================================================================
   APP STATE OPERATIONS (key-value)
   ================================================================ */

/**
 * Save a key-value pair into the appState store.
 *
 * @param {string} key - The state key (e.g. 'currentSessionId', 'layoutMode')
 * @param {*} value - Any JSON-serializable value
 * @returns {Promise<boolean>} true if save succeeded
 */
async function saveAppState(key, value) {
  try {
    const store = getStore(STORE_APP_STATE, 'readwrite');
    await requestToPromise(store.put({ key, value }));
    return true;
  } catch (err) {
    console.warn('[Cockpit/IDB] saveAppState failed:', err);
    return false;
  }
}

/**
 * Load a value by key from the appState store.
 *
 * @param {string} key
 * @returns {Promise<*>} The stored value, or null if not found
 */
async function loadAppState(key) {
  try {
    const store = getStore(STORE_APP_STATE, 'readonly');
    const result = await requestToPromise(store.get(key));
    return result || null;
  } catch (err) {
    console.warn('[Cockpit/IDB] loadAppState failed:', err);
    return null;
  }
}

/* ================================================================
   EXPORTS
   ================================================================ */

export {
  initDB,
  saveSession,
  loadSession,
  listSessions,
  deleteSession,
  saveAppState,
  loadAppState,
};
