# Current Database Architecture

## Three Separate Databases

### 1. UUID Creator Database (uuid_creator_database.db)
**Purpose:** Universal element registration and alias management
**Tables:**
- uuid_elements (254 Teams elements)
- uuid_aliases (131 Teams aliases)
**Owner:** UUIDCreator library

### 2. App Scraping Database (app_scraping_database.db)
**Purpose:** Element scraping and semantic analysis
**Tables:**
- scraped_apps (85 Teams elements)
- scraped_elements
**Owner:** Unknown module

### 3. LearnApp Database (learnapp_database.db)
**Purpose:** Exploration tracking and learned app management
**Tables:**
- learned_apps (5 Teams elements - WRONG STATS)
- screen_states (0 Teams records - EMPTY!)
- exploration_sessions
- navigation_edges
**Owner:** LearnApp module

## Current Problems

1. **Three Sources of Truth:** Same data stored three times
2. **No Synchronization:** Each system counts independently
3. **Stats Mismatch:** 254 vs 85 vs 5 elements for same app
4. **Empty Tables:** screen_states has 0 records despite exploration
5. **No Validation:** No cross-checks between systems
