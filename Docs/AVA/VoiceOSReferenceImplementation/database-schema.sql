-- VoiceOS Execution Database Schema
-- Purpose: Track command execution requests from AVA and other apps
-- Reference: Developer Manual Chapter 36, ADR-006

-- ========================================
-- Execution Requests Table
-- ========================================

CREATE TABLE IF NOT EXISTS execution_requests (
    -- Primary key
    execution_id TEXT PRIMARY KEY NOT NULL,

    -- Request metadata
    command_id TEXT NOT NULL,
    parameters TEXT,  -- JSON format
    requested_by TEXT NOT NULL,  -- Package name of requesting app
    requested_at INTEGER NOT NULL,  -- Unix timestamp (ms)

    -- Execution state
    status TEXT NOT NULL DEFAULT 'pending',  -- pending, executing, success, error
    message TEXT,
    executed_steps INTEGER DEFAULT 0,
    execution_time_ms INTEGER,
    failed_at_step INTEGER,

    -- Timestamps
    started_at INTEGER,
    completed_at INTEGER,
    created_at INTEGER DEFAULT (strftime('%s','now') * 1000),

    -- Indexes for performance
    CHECK (status IN ('pending', 'executing', 'success', 'error'))
);

-- Index for quick lookup by execution_id
CREATE INDEX IF NOT EXISTS idx_execution_id ON execution_requests(execution_id);

-- Index for status queries (find all pending/executing)
CREATE INDEX IF NOT EXISTS idx_status ON execution_requests(status);

-- Index for requesting app queries
CREATE INDEX IF NOT EXISTS idx_requested_by ON execution_requests(requested_by);

-- Index for timestamp queries (cleanup old requests)
CREATE INDEX IF NOT EXISTS idx_created_at ON execution_requests(created_at);

-- ========================================
-- Command Queue Table (Optional - for ordered execution)
-- ========================================

CREATE TABLE IF NOT EXISTS command_queue (
    queue_id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id TEXT NOT NULL,
    priority INTEGER DEFAULT 0,  -- Higher priority = execute first
    queued_at INTEGER NOT NULL,

    FOREIGN KEY (execution_id) REFERENCES execution_requests(execution_id)
);

-- Index for priority queue
CREATE INDEX IF NOT EXISTS idx_queue_priority ON command_queue(priority DESC, queued_at ASC);

-- ========================================
-- Execution Steps Table (Detailed step tracking)
-- ========================================

CREATE TABLE IF NOT EXISTS execution_steps (
    step_id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id TEXT NOT NULL,
    step_number INTEGER NOT NULL,
    action TEXT NOT NULL,  -- OPEN_APP, CLICK, INPUT_TEXT, SELECT
    target_element_id TEXT,
    target_text TEXT,
    parameters TEXT,  -- JSON format
    status TEXT DEFAULT 'pending',  -- pending, executing, success, error
    error_message TEXT,
    started_at INTEGER,
    completed_at INTEGER,

    FOREIGN KEY (execution_id) REFERENCES execution_requests(execution_id),
    CHECK (status IN ('pending', 'executing', 'success', 'error'))
);

-- Index for execution steps lookup
CREATE INDEX IF NOT EXISTS idx_execution_steps ON execution_steps(execution_id, step_number);

-- ========================================
-- Sample Data (for testing)
-- ========================================

-- Example: Successful execution
INSERT INTO execution_requests (
    execution_id,
    command_id,
    parameters,
    requested_by,
    requested_at,
    status,
    message,
    executed_steps,
    execution_time_ms,
    started_at,
    completed_at
) VALUES (
    'exec_sample_001',
    'cmd_call_teams',
    '{"contact":"John Thomas","app":"teams"}',
    'com.augmentalis.ava',
    1700000000000,
    'success',
    'Calling John Thomas on Teams',
    3,
    2500,
    1700000000000,
    1700000002500
);

-- Example: Failed execution
INSERT INTO execution_requests (
    execution_id,
    command_id,
    parameters,
    requested_by,
    requested_at,
    status,
    message,
    executed_steps,
    execution_time_ms,
    failed_at_step,
    started_at,
    completed_at
) VALUES (
    'exec_sample_002',
    'cmd_spotify_play',
    '{"song":"Bohemian Rhapsody"}',
    'com.augmentalis.ava',
    1700000005000,
    'error',
    'Element not found: play_button',
    0,
    1200,
    2,
    1700000005000,
    1700000006200
);

-- Example: Pending execution
INSERT INTO execution_requests (
    execution_id,
    command_id,
    parameters,
    requested_by,
    requested_at,
    status
) VALUES (
    'exec_sample_003',
    'cmd_navigate_home',
    '{}',
    'com.augmentalis.ava',
    1700000010000,
    'pending'
);

-- ========================================
-- Cleanup Query (Remove old requests after 7 days)
-- ========================================

-- DELETE FROM execution_requests
-- WHERE created_at < (strftime('%s','now') - 604800) * 1000
-- AND status IN ('success', 'error');
