# Supabase Cloud Synchronization & Database Guide

This document describes the cloud synchronization architecture and the **Golden Standard** database configuration for AI agents.

> [!IMPORTANT]
> **Database Golden Standard**: [`supabase/schema.sql`](file:///Users/kollbi/repos/DropShot/supabase/schema.sql), [`supabase/migrations/`](file:///Users/kollbi/repos/DropShot/supabase/migrations/), and [`supabase/AGENTS.md`](file:///Users/kollbi/repos/DropShot/supabase/AGENTS.md) are the single source of truth for how the DropShot Supabase PostgreSQL database is and will be configured. Always read [`supabase/schema.sql`](file:///Users/kollbi/repos/DropShot/supabase/schema.sql) before you modify any cloud table, column, RLS policy, or sync payload.

---

## 1. Overview

DropShot syncs player profiles, equipped cosmetics (colors and fonts), unlocked lootbox rewards, career statistics, and match records with a Supabase PostgreSQL backend.
It uses PostgREST REST APIs over HTTPS with zero third-party SDKs.

| Component | Path | Responsibility |
|---|---|---|
| Canonical DB Schema | [`supabase/schema.sql`](file:///Users/kollbi/repos/DropShot/supabase/schema.sql) | Idempotent PostgreSQL tables, universal JSONB expansion slots, GIN/B-tree indexes, triggers, and RLS policies. |
| Versioned Migrations | [`supabase/migrations/`](file:///Users/kollbi/repos/DropShot/supabase/migrations/) | Incremental SQL scripts (`001_initial_schema.sql`, `002_universal_cosmetics_and_stats.sql`). |
| Credentials Store | [`CloudConfigStore.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java) | Encrypts project URL, public key, and token hashes at rest via `SecureStorageEngine`. |
| Sync Client | [`SupabaseSyncClient.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java) | Performs HTTPS PostgREST upserts/queries with automatic v1 schema fallback. |

---

## 2. Authentication Model

1. **Public Key**:
   * Stored encrypted in [`CloudConfigStore`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java).
   * Sent in `apikey` (and `Authorization: Bearer <anonKey>` when the key is a JWT starting with `eyJ`).
2. **Player Secret Token**:
   * Each cloud player chooses a secret passphrase.
   * The app hashes the passphrase with SHA-256 (`CloudConfigStore.sha256Hex`).
   * The app sends the lowercase 64-character hex digest in the `x-player-token-hash` HTTP header.
   * PostgreSQL Row Level Security (RLS) policies validate the header via `public.request_token_hash()` and compare it against `cloud_players.token_hash`.

---

## 3. Universal Database Schema (`supabase/schema.sql`)

The backend uses a **Hybrid Universal Design**: fixed relational columns preserve 100% v1 compatibility while typed JSONB extension maps (`equipped_cosmetics`, `stats_ext`, `metadata`) let future features expand without destructive DDL migrations.

### 3.1 `public.cloud_players`
Stores player profiles, career counters, unlocked rewards, and universal JSONB extensions.

| Column | Type | Purpose |
|---|---|---|
| `id` | `uuid` | Primary key. Matches local profile UUID. |
| `token_hash` | `text` | Lowercase 64-character SHA-256 hex digest (`UNIQUE`). |
| `name` | `text` | Sanitized display name of the player (`1..64` chars). |
| `color` | `integer` (`int4`) | Equipped player color key (ARGB integer). |
| `unlocked_rewards` | `jsonb` (array) | Array of all unlocked reward IDs across all kinds (colors, fonts, future cosmetics). |
| `matches` | `integer` (`int4`) | Total completed matches. |
| `wins` | `integer` (`int4`) | Total won matches. |
| `points` | `integer` (`int4`) | Total scored points. |
| `won_sets` | `integer` (`int4`) | Total games won. |
| `total_sets` | `integer` (`int4`) | Total games played. |
| `score_67` | `integer` (`int4`) | Total times reaching `6-7` or `7-6` score. |
| `score_69` | `integer` (`int4`) | Total times reaching `6-9` or `9-6` score. |
| `updated_at` | `timestamptz` | Automatically maintained by `public.set_updated_at()` trigger. |
| `equipped_cosmetics` | `jsonb` (object) | Universal active cosmetics map (`{"color": -13665034, "font": "font-anton", ...}`). |
| `stats_ext` | `jsonb` (object) | Universal extended career analytics (`serves_total`, `serves_won`, `receives_total`, `receives_won`, `right_picks`, `left_picks`, `laplace_win_prob`, `wilson_lower_95`, `markov_serve_share`, etc.). |
| `metadata` | `jsonb` (object) | Universal extension object for future client flags or preferences. |
| `schema_version` | `integer` (`int4`) | Payload version written by the client (`CURRENT_SCHEMA_VERSION = 2`). |

### 3.2 `public.cloud_player_matches`
Stores individual match records for historical review (`PRIMARY KEY (player_id, match_id)`).

| Column | Type | Purpose |
|---|---|---|
| `player_id` | `uuid` | Foreign key referencing `cloud_players.id` (`ON DELETE CASCADE`). |
| `match_id` | `bigint` (`int8`) | Cryptographically random 63-bit match identifier. |
| `finished` | `boolean` (`bool`) | `true` when the match ended normally. |
| `finished_at` | `bigint` (`int8`) | Epoch millisecond timestamp. |
| `record_json` | `jsonb` (object) | Complete serialized [`MatchRecord`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MatchRecord.java) (including `initialServer` and `pointSides`). |
| `updated_at` | `timestamptz` | Automatically maintained by `public.set_updated_at()` trigger. |
| `metadata` | `jsonb` (object) | Universal per-match extension object. |

### 3.3 `public.cloud_schema_migrations`
Ledger table tracking applied SQL migration versions (`version`, `name`, `applied_at`).

---

## 4. Background Synchronization Lifecycle

1. **Match Completion**:
   * [`MainActivity.archiveCurrentMatch()`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) calls [`SupabaseSyncClient.syncMatchInBackground`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java).
   * Runs asynchronously on [`AsyncRepositoryExecutor`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/AsyncRepositoryExecutor.java) and never blocks the UI thread.
2. **Profile & Cosmetic Updates**:
   * [`ProfileStore.update`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/ProfileStore.java) calls [`SupabaseSyncClient.syncProfileInBackground`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java) whenever a player renames their profile or equips a color or font in [`PlayerCustomizationActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/PlayerCustomizationActivity.java).
3. **Lootbox Unlocks**:
   * [`RewardInventory`](file:///Users/kollbi/repos/DropShot/Android/lootbox/src/main/java/pl/maciek/squashscoreboard/lootbox/RewardInventory.java) notifies `SupabaseSyncClient.syncProfileByIdInBackground` on new ticket-verified unlocks so newly won colors and fonts upload automatically.
   * Cloud pulls use `RewardInventory.restoreFromCloud` so restored rewards do not trigger redundant push loops.
4. **Graceful v1 Fallback**:
   * If a client pushes v2 columns (`equipped_cosmetics`, `stats_ext`, `schema_version`) to a Supabase project that has not run `supabase/schema.sql` yet (HTTP `400` / `PGRST204`), `SupabaseSyncClient` automatically retries with base v1 columns so sync never fails.
5. **Profile Deletion**:
   * When a profile is deleted locally, [`CloudConfigStore.unlinkProfile`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java) removes the saved token hash.

---

## 5. Security Rules for Cloud Code

1. Use only `HttpsURLConnection`. Do not add third-party HTTP or Supabase client libraries.
2. Verify that URLs use `https://` via [`CloudConfigStore.isValidHttpsEndpoint`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java).
3. Do not log the secret token, token hash, or public key.
4. Keep the 512 KB response cap to prevent out-of-memory errors.
5. Validate all pulled names, profile IDs, and cosmetic IDs with [`InputValidator`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/InputValidator.java).
