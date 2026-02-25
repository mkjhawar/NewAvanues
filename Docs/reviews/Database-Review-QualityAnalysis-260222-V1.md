# Database Quality Report — 260222

## Summary
SCORE: 63 / 100 | HEALTH: YELLOW
FILES: 264 kt, 60 sq | KMP: yes (Android + iOS + Desktop)

The module is functionally solid at the schema level with good index coverage,
proper WAL/FK bootstrap, and battle-tested design choices (keyset pagination,
batch-existence queries, atomic increments). The critical concerns are: (1)
`deriveSchemaFromMigrations = false` + `verifyMigrations = false` — SQLDelight
cannot enforce migrations, so every schema change that has happened via in-place
table edits is invisible to the migration system; (2) two parallel, overlapping
table families for the same concepts (scraping: `scraped_element` vs
`screen_states`/`exploration_sessions`/`navigation_edges`; web: `scraped_web_elements`
+ `generated_web_commands` vs `scraped_web_command`); (3) FK constraints that were
commented out with no enforcement path; (4) several dangerous LIKE '%...%' full-table
scans on high-volume tables; and (5) Rule 7 violation in a repository file header.

---

## Schema Overview

| Table | Columns | Indices | Foreign Keys | Notes |
|-------|---------|---------|--------------|-------|
| scraped_app | 14 | 3 | 0 | Core scraping anchor |
| scraped_element | 27 | 6 | 1 (appId→scraped_app) | Central element store |
| scraped_hierarchy | 5 | 3 | 0 (FKs commented out) | Orphaned-row risk |
| commands_generated | 20 | 7 | 0 (FK commented out) | Largest likely table |
| commands_static | 13 | 7 (incl. unique) | 0 | Static VOS seed commands |
| commands_scraped | 10 | 3 | 0 | LearnApp legacy path |
| screen_context | 15 | 4 | 1 (appId→scraped_app) | Screen metadata |
| screen_transition | 8 | 3 | 0 | Navigation graph |
| user_interaction | 7 | 4 | 0 | Interaction log |
| element_state_history | 8 | 4 | 0 | State change log |
| element_relationship | 8 | 4 | 0 (FKs removed) | Semantic graph |
| element_command | 8 | 3 | 0 | Manual overrides |
| element_quality_metric | 9 | 2 | 0 | Metadata quality |
| custom_command | 12 | 3 | 0 | User macros |
| command_history_entry | 11 | 4 | 0 | Execution history |
| command_usage | 10 | 5 | 0 | Usage telemetry |
| context_preference | 6 | 2+unique | 0 | Bayesian context scoring |
| user_preference | 4 | 1 | 0 | KV store |
| user_sequence | 10 | 3 | 0 | Macro sequences |
| avid_elements | 10 | 4 | 0 | AVID registry |
| avid_hierarchy | 6 | 4 | 2 (both→avid_elements) | AVID tree |
| avid_aliases | 5 | 2 | 1 (avid→avid_elements) | AVID name aliases |
| avid_analytics | 8 | 3 | 1 (avid→avid_elements) | AVID usage metrics |
| tab | 17 | 4 | 1 (group_id→tab_group) | Browser tabs |
| tab_group | 6 | 1 | 0 | Tab groups |
| favorite | 10 | 2 | 0 | Bookmarks |
| favorite_folder | 7 | 0 | 0 | Bookmark folders |
| favorite_tag | 2 (PK comp.) | 0 | 1 (favorite_id→favorite) | Tag many-to-many |
| history_entry | 11 | 3 | 0 | Browser history |
| download | 14 | 3 | 0 | Downloads |
| browser_settings | 41 | 0 | 0 | Singleton settings |
| site_permission | 4 (PK comp.) | 1 | 0 | Per-domain perms |
| session | 5 | 1 | 0 | Crash recovery |
| session_tab | 13 (PK comp.) | 1 | 1 (session_id→session) | Session tabs |
| learned_apps | 20 | 4 | 0 | LearnApp anchor |
| screen_states | 6 | 3 | 1 (package→learned_apps) | LearnApp screens |
| exploration_sessions | 8 | 3 | 1 (package→learned_apps) | LearnApp sessions |
| navigation_edges | 7 | 6 | 2 (package+session) | LearnApp nav graph |
| app_consent_history | 4 | 3 | 1 (package→learned_apps) | Consent tracking |
| scraped_websites | 10 | 4 | 1 (self: parent_url_hash) | Web scraping sites |
| scraped_web_elements | 12 | 5 | 1 (url_hash→scraped_websites) | Web elements |
| generated_web_commands | 10 | 5 | 1 (url_hash→scraped_websites) | Old web command store |
| scraped_web_command | 22 | 6 | 0 | New web command store |
| web_app_whitelist | 12 | 3 | 0 | Domain whitelist |
| plugins | 11 | 3 | 0 | Plugin registry |
| plugin_dependencies | 5 | 2+unique | 2 (both→plugins) | Plugin deps |
| plugin_permissions | 6 | 2+unique | 1 (plugin→plugins) | Plugin permissions |
| system_checkpoints | 6 | 2 | 0 | System snapshots |
| language_model | 10 | 3 | 0 | Downloaded models |
| recognition_learning | 9 | 4 | 0 | STT learning data |
| gesture_learning | 8 | 2 | 0 | Gesture recognition |
| touch_gesture | 8 | 2 | 0 | Touch gestures |
| vos_file_registry | 19 | 5 | 0 | VOS file tracking |
| phrase_suggestion | 8 | 0 | 0 | Phrase proposals |
| app_version | 4 | 2 | 0 | App version tracking |
| app_category_override | 7 | 2 | 0 | App category map |
| app_pattern_group | 6 | 2+unique | 0 | Category patterns |
| device_profile | 8 | 2 | 0 | Device config |
| usage_statistic | 7 | 3 | 0 | General metrics |
| error_report | 9 | 3 | 0 | Error log |
| database_version | 5 | 0 | 0 | Schema version |
| analytics_settings | 7 | 0 | 0 | Analytics singleton |
| retention_settings | 6 | 0 | 0 | Retention singleton |
| note_entity | 12 | 5 | 0 | Rich notes |
| note_folder | 9 | 2 | 0 | Note folders |
| note_attachment | 11 | 2 | 1 (note_id→note_entity) | Note attachments |
| CockpitSession | 7 | 2 | 0 | Cockpit sessions |
| CockpitFrame | 15 | 2 | 1 (sessionId→CockpitSession) | Cockpit frames |
| CockpitPinnedFrame | 6 | 0 | 1 (sessionId→CockpitSession) | PiP frames |
| CockpitWorkflowStep | 6 | 1 | 1 (sessionId→CockpitSession) | Workflow steps |
| CockpitTimelineEvent | 7 | 1 | 1 (sessionId→CockpitSession) | Timeline |
| CockpitCrossFrameLink | 7 | 2 | 1 (sessionId→CockpitSession) | Frame links |

**Total tables: 67** across 60 .sq files (ElementCommand.sq defines 2 tables; BrowserTables.sq defines 8 tables).

---

## P0 Critical Issues

- **[build.gradle.kts:162-163]** `deriveSchemaFromMigrations.set(false)` + `verifyMigrations.set(false)` — SQLDelight is in "generate from schema" mode, meaning no `.sqm` migration files exist or are verified. Every schema change that has been made iteratively (dozens of `-- FIX` comments, `Schema v3`, `Schema v4` evolution markers in GeneratedCommand.sq) was applied by dropping and recreating the database, or by accumulating changes directly in the DDL. In production, upgrading users will lose all data unless the app also has a separate Room/manual migration path. This is a data-loss-on-upgrade risk for all 67 tables if the database was ever shipped.

- **[ExplorationSession.sq:7 / LearnedApp.sq:7 / AppConsentHistory.sq:7]** `Author: Agent 1 (LearnApp Migration Specialist)` — Rule 7 violation. AI agent identity in file headers. Must be removed or replaced with "Manoj Jhawar".

- **[SQLDelightScrapedElementRepository.kt:7]** `Author: VOS4 Development Team` with `Code-Reviewed-By: CCA` — `CCA` likely denotes an AI system. Rule 7 violation. Must be removed.

- **[SQLDelightScrapedAppRepository.kt:7]** Same `Author: VOS4 Development Team` + `Code-Reviewed-By: CCA` — Rule 7 violation.

- **[ScrapedHierarchy.sq:10-11]** Foreign keys are commented out:
  ```sql
  -- FOREIGN KEY (parentElementHash) REFERENCES scraped_element(elementHash) ON DELETE CASCADE,
  -- FOREIGN KEY (childElementHash) REFERENCES scraped_element(elementHash) ON DELETE CASCADE,
  ```
  However, the comment says they were disabled because `scraped_element.elementHash` is no longer a UNIQUE or PRIMARY KEY column (the unique constraint is now `(elementHash, screen_hash)`). The SQLite constraint is correct — but this means `scraped_hierarchy` has NO referential protection. Orphaned rows accumulate every time a `scraped_element` row is deleted. A data integrity gap exists that cannot be fixed without redesigning the FK relationship to reference the composite key or adding a surrogate PK to `scraped_element`.

- **[GeneratedCommand.sq:33]** FK to `scraped_element.elementHash` also commented out for the same reason. `commands_generated` can accumulate orphaned rows indefinitely when elements are deleted. `scraped_app ON DELETE CASCADE` does not chain through to `commands_generated` because that FK was never activated.

- **[ElementRelationship.sq:4-7]** FKs removed "because scraped_element uses composite unique key". Same orphan accumulation risk. No enforcement path.

---

## P1 High Issues

- **[generated_web_commands vs scraped_web_command — dual web command stores]** Two separate, incompatible web command tables exist in the same database:
  - `generated_web_commands` (GeneratedWebCommand.sq) — references `scraped_websites.url_hash`, uses comma-separated `synonyms`, no confidence field, created 2025-12-17
  - `scraped_web_command` (ScrapedWebCommand.sq) — references `domain_id` only (no FK to `scraped_websites`), has confidence, JSON synonyms, approval workflow, created 2026-01-14
  These represent the same concept at different evolution stages. Code that reads one and not the other produces incomplete results. No migration path from old to new. `generated_web_commands` appears to be the legacy table that should have been deprecated.

- **[screen_context vs screen_states — duplicate screen concepts]** Two tables store screen metadata:
  - `screen_context` (ScreenContext.sq) — used by VoiceOSCore scraping pipeline, FK to `scraped_app`
  - `screen_states` (ScreenState.sq) — used by LearnApp, FK to `learned_apps`
  Both store `screen_hash + package_name + activity_name + element_count`. They model the same entity from two separate data pipelines. No cross-reference. Consumers looking at one table see a subset of reality.

- **[learned_apps vs scraped_app — parallel app registries]** Two tables register apps:
  - `scraped_app` — `appId TEXT PK`, FK anchor for `scraped_element` / `screen_context`
  - `learned_apps` — `package_name TEXT PK`, FK anchor for `screen_states` / `exploration_sessions` / `navigation_edges`
  The two systems use different primary key types (opaque appId vs raw packageName) with no cross-FK linking them. Querying "what is the full learning state of Gmail?" requires two separate queries to unrelated table families.

- **[phrase_suggestion — zero indices on queried columns]** `phrase_suggestion` has no indices at all despite queries on `status`, `locale`, and `command_id`. `getPendingByLocale` filters on `(status, locale)` — full table scan on every call. This is called on the voice recognition hot path.

- **[GeneratedCommand.sq:78]** `fuzzySearch: SELECT * FROM commands_generated WHERE commandText LIKE '%' || ? || '%'` — this is a leading-wildcard LIKE on `commands_generated`, the largest table in the system. SQLite cannot use any index for a leading wildcard. Full table scan on every fuzzy match. On a device with 10k+ generated commands across dozens of apps, this will be a significant source of jank. No FTS5 virtual table alternative exists in this module.

- **[ScrappedCommand.sq:38]** Same leading-wildcard `fuzzySearch` on `commands_scraped`. Same problem.

- **[note_entity.sq:41]** `searchByText: ... WHERE title LIKE '%' || ? || '%' OR markdown_content LIKE '%' || ? || '%'` — double leading-wildcard on `markdown_content` which can be arbitrarily large. No FTS5 table for note content. This will be very slow for large notes collections.

- **[AppConsentHistory.sq:50-62]** `getDontAskAgainApps` is a correlated subquery with a nested `MAX()` subquery:
  ```sql
  AND ach.package_name NOT IN (
      SELECT ach2.package_name FROM app_consent_history ach2
      WHERE ach2.user_choice != 'DONT_ASK_AGAIN'
      AND ach2.timestamp > (
          SELECT MAX(ach3.timestamp) FROM app_consent_history ach3
          WHERE ach3.package_name = ach2.package_name ...
      )
  );
  ```
  O(n²) complexity against a table that grows with every consent dialog shown. Will become slow as the table grows. Should be rewritten as a CTE or window function.

- **[SQLDelightCommandRepository.kt:28]** `phrases = command.phrases.joinToString("|")` — CustomCommand stores phrases as pipe-delimited string even though the schema's `phrases TEXT` column comment says "JSON array of trigger phrases". The adapter (`StringListAdapter`) uses JSON. The repository uses pipe-delimited format. This inconsistency means data written by the repository cannot be decoded by any code expecting JSON format. One of these two serialization formats is wrong.

- **[ScrapedWebsite.sq:20-21]** Self-referential FK `REFERENCES scraped_websites(url_hash) ON DELETE CASCADE` for `parent_url_hash` — this is structurally sound but creates the potential for deep cascade chains when a parent site is deleted, deleting all child URL records. More importantly, `scraped_web_elements` and `generated_web_commands` both FK on `website_url_hash → scraped_websites(url_hash) ON DELETE CASCADE`, meaning deleting a website cascades to delete all its elements AND commands. If a site gets marked stale and deleted, all learning is silently lost.

- **[CockpitFrame.sq:19-20]** `createdAt TEXT` and `updatedAt TEXT` — timestamps stored as TEXT strings, not INTEGER (epoch ms). All other tables in the module use INTEGER for timestamps. This breaks any cross-table time comparison, makes range queries impossible, and makes ordering by timestamp unreliable (string sort vs numeric sort). Same inconsistency in `CockpitSession`, `CockpitWorkflowStep`, `CockpitTimelineEvent`.

- **[favorite_folder]** No foreign key from `favorite.folder_id` to `favorite_folder.id`. Favorites can reference non-existent folders with no cascade delete. Orphaned favorites accumulate when a folder is deleted.

---

## P2 Medium Issues

- **[ScrapedApp.sq:74-75]** Duplicate query definitions:
  - `markFullyLearned` (L72) and `markAsFullyLearned` (L74) are identical SQL statements. One is dead code.
  - `getByHash` vs `getElementByHash` in ScrapedElement.sq (L62-65) — identical queries with different names.
  - `getByHash` vs `getScreenByHash` vs `getByScreenHash` in ScreenContext.sq (L41-45) — three queries for `WHERE screenHash = ?`.
  These duplicates bloat the generated API surface and create confusion about which name to call.

- **[UserInteraction.sq:67-71]** `getSuccessFailureRatio` is a hardcoded stub returning `1.0` always, with a comment acknowledging the field doesn't exist. A query that always returns a constant should not exist in the schema — it misleads callers into thinking ratio tracking is functional.

- **[GeneratedCommand.sq:321-323]** `vacuumDatabase: VACUUM;` as a named SQLDelight query. Calling VACUUM inside a transaction or on the wrong thread will cause an error. SQLite VACUUM cannot run inside a transaction and requires exclusive access. Exposing this as a generated query function without enforcement is a footgun.

- **[phrase_suggestion]** Missing `CREATE INDEX` on `(command_id)`, `(status, locale)`. All three query patterns (`getPendingByLocale`, `getForCommand`, `getAll`) do full scans.

- **[GeneratedWebCommand.sq:11]** `synonyms TEXT NOT NULL` comment says "Comma-separated". `ScrapedWebCommand.sq` stores synonyms as a JSON array. Two incompatible formats for the same semantic field across two closely related tables.

- **[DatabaseVersion.sq]** Single-row sentinel table with `id INTEGER PRIMARY KEY DEFAULT 1`. The schema stores version metadata (json_version, command_count, locales) but there is no SQLDelight migration version table. This `database_version` table serves application-level purposes and does not replace SQLDelight's own schema versioning (`VoiceOSDatabase.Schema.version`). The actual schema version is hardcoded in the generated code — but since `deriveSchemaFromMigrations = false`, there is no canonical version history at all.

- **[ScrapedElement.sq:45]** `CREATE INDEX IF NOT EXISTS idx_scraped_element_app_hash ON scraped_element(appId, elementHash)` — this is a duplicate of the combination covered by `idx_se_hash` + `idx_se_app` separately, but more importantly, it is defined with `IF NOT EXISTS` while all other indexes in the same file use bare `CREATE INDEX`. Inconsistent style. `IF NOT EXISTS` is not needed in SQLDelight DDL (the schema is dropped and recreated on install).

- **[VosFileRegistry.sq:6]** `CREATE TABLE IF NOT EXISTS` — same issue. Most other tables use bare `CREATE TABLE`. The `IF NOT EXISTS` guard in SQLDelight DDL does nothing useful; the schema is re-created fresh.

- **[CommandHistoryRepository.kt placement]** `CommandHistoryRepository.kt` is in `queries/` subdirectory while all other repository implementations are in `repositories/impl/`. Inconsistent placement breaks discoverability.

- **[ExplorationSession.sq:7 / LearnedApp.sq:7 / NavigationEdge.sq:7 / ScreenState.sq:7]** `Author: Agent 1 (LearnApp Migration Specialist)` in multiple file headers — Rule 7 violations (see P0 above for severity classification, repeated here as P2 for the annotation-only files).

- **[AppConsentHistory.sq:71-78]** `hasDontAskAgain` query accepts `package_name` twice (two `?` parameters) and does a correlated `MAX()` subquery per row. This is equivalent to the full O(n²) problem in `getDontAskAgainApps`.

- **[TouchGesture.sq / GestureLearning.sq / DeviceProfile.sq]** No FK linking these to any app or device identity. Data is globally scoped with no partitioning by app or device. If multi-device sync ever lands, all gesture/device data will be mixed.

- **[LanguageModel.sq]** No UNIQUE constraint on `(engine, language, modelName)`. Multiple rows for the same engine+language combination can exist. `INSERT OR REPLACE` on a row with `id` auto-assigned means repeated inserts create duplicate tracking entries rather than updating the existing one.

- **[CockpitWorkflowStep.sq:8]** `frameId TEXT NOT NULL` is stored but has no FK to `CockpitFrame(id)`. A workflow step can reference a deleted frame indefinitely. No `ON DELETE CASCADE`.

- **[CockpitPinnedFrame.sq:3]** `frameId TEXT NOT NULL PRIMARY KEY` — PK is the frameId, but there is no FK to `CockpitFrame(id)`. A pinned-frame row survives frame deletion.

---

## Missing Indices

| Table | Column(s) | Query Needing Index | Impact |
|-------|-----------|---------------------|--------|
| `phrase_suggestion` | `(status, locale)` | `getPendingByLocale` | HIGH — hot voice path |
| `phrase_suggestion` | `command_id` | `getForCommand` | HIGH |
| `commands_generated` | `commandText` (FTS) | `fuzzySearch` | HIGH — leading wildcard |
| `commands_scraped` | `commandText` (FTS) | `fuzzySearch` | MEDIUM |
| `note_entity` | `(title, markdown_content)` (FTS) | `searchByText` | MEDIUM |
| `scraped_web_command` | `(domain_id, command_text)` | text-based lookup | MEDIUM |
| `history_entry` | `is_incognito` | filtering non-incognito history | LOW |
| `favorite` | `(folder_id, position)` compound | `selectFavoritesInFolder` ordered | LOW |
| `language_model` | `(engine, language)` compound | `getByEngineAndLanguage` | LOW |
| `recognition_learning` | `(engine, keyValue)` | `getByEngineAndKey` | LOW (already has `idx_rl_engine_type`) |
| `app_consent_history` | `(package_name, user_choice, timestamp)` compound | `hasDontAskAgain` correlated | MEDIUM |

---

## Migration Gaps

- **CRITICAL: No migration system exists.** `deriveSchemaFromMigrations = false` and `verifyMigrations = false` in `build.gradle.kts`. SQLDelight generates the schema from the current `.sq` files but no `.sqm` files exist to describe incremental upgrades. On any production device that has an older database, SQLDelight will call `Schema.migrate()` with the current version number — but since there is no migration logic, it will either crash or silently skip migration steps, depending on how the caller handles the `SqlDriver` upgrade callback. The `VoiceOSDatabase.Schema.version` is an opaque integer that must be incremented manually; there is no protection against accidental omission.

- **Schema evolution markers with no migration paths:** Multiple tables carry embedded schema version comments (`Schema v3 addition`, `Schema v4: Foreign key integrity`, `ADR-014 columns`) describing columns added over time. These additions required in-situ schema changes that were never formalized as migrations. Any user upgrading from a pre-v3 schema to v4 will receive an `android.database.sqlite.SQLiteException: no such column`.

- **`IF NOT EXISTS` guards on some tables** (vos_file_registry, app_version, learned_apps, etc.) suggest those tables were added after initial schema creation and the developer used `IF NOT EXISTS` as a workaround to avoid `ALTER TABLE`. This is a valid workaround for a no-migration-system codebase but leaves inconsistency across table definitions.

---

## Query Efficiency

- **`getByPackage` and `getByPackagePaginated` in GeneratedCommand.sq (L118-250):** These join `commands_generated` to `scraped_element` on `elementHash` to filter by `appId`. This works but is an indirect join instead of using `commands_generated.appId` directly (which exists as of Schema v3). The join-based queries exist for backward compatibility but are slower than `WHERE appId = ?` directly. The direct query form should replace these.

- **`cleanupOldEntries` in CommandHistory.sq (L97-104):** `WHERE id NOT IN (SELECT id FROM ... LIMIT N)` — SQLite materializes the inner subquery and then performs a full scan of the outer table checking membership in the result set. For large tables this is O(n) with a hidden sort. Replace with a keyset approach: `DELETE WHERE id < (SELECT id FROM ... ORDER BY timestamp DESC LIMIT 1 OFFSET N)`.

- **`deleteOldestRecords` in CommandUsage.sq (L71-73):** Same `WHERE id IN (SELECT id ... LIMIT ?)` pattern — materializes the subquery, then deletes by membership. On a large table this is expensive. Better: `DELETE FROM command_usage WHERE timestamp < (SELECT timestamp FROM command_usage ORDER BY timestamp ASC LIMIT 1 OFFSET ?)`.

- **`getDontAskAgainApps` in AppConsentHistory.sq (L50-62):** O(n²) correlated subquery. Rewrite as:
  ```sql
  SELECT package_name FROM app_consent_history
  GROUP BY package_name
  HAVING MAX(CASE WHEN user_choice = 'DONT_ASK_AGAIN' THEN timestamp ELSE 0 END) >
         MAX(CASE WHEN user_choice != 'DONT_ASK_AGAIN' THEN timestamp ELSE 0 END)
  ```

- **`getAll` queries without LIMIT on high-volume tables:** `scraped_element.getAll()`, `commands_generated.getAll()`, `user_interaction` (via `getByElement`), `command_usage.getAllUsage()` — all return unbounded result sets. In production with thousands of scraping cycles, these will load tens of thousands of rows into memory.

- **`batchCheckExistence` in GeneratedCommand.sq (L341-344):** This is correctly implemented as a batch query — a good pattern replacing N individual existence checks.

- **`getByPackageKeysetPaginated` in GeneratedCommand.sq (L244-251):** Correct keyset pagination — good pattern.

---

## Code Smells

- **Duplicate named queries:** `markFullyLearned` / `markAsFullyLearned` (ScrapedApp.sq), `getByHash` / `getElementByHash` (ScrapedElement.sq), `getByHash` / `getByScreenHash` / `getScreenByHash` (ScreenContext.sq), `updateVisit` / `incrementVisitCount` (ScreenContext.sq), `deleteAllUsage` / `deleteAllRecords` (CommandUsage.sq), `selectTabGroup` / `insertTabGroup` using positional `VALUES ?` vs named params — inconsistency throughout.

- **Inconsistent timestamp types:** Most tables use `INTEGER` (epoch ms). Cockpit tables use `TEXT`. Note tables also use `TEXT`. This makes cross-table temporal queries impossible and is a type system failure.

- **`INSERT OR REPLACE` used where `INSERT OR IGNORE` is correct (and vice versa):** ScrapedElement has both `insert` (OR IGNORE) and `upsertElement` (OR REPLACE) with identical SQL otherwise — callers must choose consciously. The schema comment explains the reasoning but the two-query API is confusing.

- **`VACUUM` as a named SQLDelight query (GeneratedCommand.sq:322):** DDL statements as query objects bypass transaction safety. Should be called via raw driver pragma, not as a generated query.

- **Rule 7 violations in 4+ file headers:** `Author: Agent 1 (LearnApp Migration Specialist)` appears in ExplorationSession.sq, LearnedApp.sq, NavigationEdge.sq, ScreenState.sq. `Code-Reviewed-By: CCA` in SQLDelightScrapedElementRepository.kt and SQLDelightScrapedAppRepository.kt.

- **`phrases` serialization inconsistency:** `CustomCommand.phrases TEXT` — schema comment says "JSON array", repository serializes as pipe-delimited (`joinToString("|")`). `StringListAdapter` uses JSON. One of these is wrong; the data on disk is in pipe format from the repository but the adapter would decode it as invalid JSON.

- **`scraped_web_elements` + `generated_web_commands` vs `scraped_web_command`:** Two parallel web scraping + command stores that will never be unified without a migration. Risk of consuming modules reading from the wrong (stale) table family.

- **`getSuccessFailureRatio` stub (UserInteraction.sq:67-71):** A real query that always returns `1.0` with no actual data. This is a Rule 1 violation at the SQL level — a placeholder function disguised as a query.

- **Missing UNIQUE constraint on `language_model(engine, language, modelName)`:** Multiple identical model records can accumulate without constraint enforcement.

- **`DatabaseFactory.kt`:** `createDatabase()` constructs `VoiceOSDatabase(driver)` with no column adapters passed. If any table requires a `ColumnAdapter` (e.g., for `List<String>` columns like `custom_command.phrases`), omitting adapters will cause a silent type-mapping failure or compile error. The `stringListAdapter` defined in `StringListAdapter.kt` is never referenced in `createDatabase()`. This is a wiring gap — adapters must be injected at construction time in SQLDelight.
