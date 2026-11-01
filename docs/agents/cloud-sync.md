# Supabase Cloud Synchronization Guide

This document describes the cloud synchronization architecture for AI agents.

---

## 1. Overview

DropShot syncs player profiles, career statistics, and match records with a Supabase PostgreSQL backend.
It uses PostgREST REST APIs over HTTPS.

| Component | Path | Responsibility |
|---|---|---|
| Credentials Store | [`CloudConfigStore.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java) | Encrypts project URL, public key, and token hashes at rest. |
| Sync Client | [`SupabaseSyncClient.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java) | Performs HTTP requests to PostgREST endpoints. |

---

## 2. Authentication Model

1. **Public Anon Key**:
   * Stored in [`CloudConfigStore`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java).
   * Sent in `apikey` and `Authorization: Bearer <anonKey>` headers.
2. **Player Secret Token**:
   * Each cloud player has a secret passphrase.
   * The app hashes the passphrase with SHA-256.
   * The app sends the 64-character hex digest in the `x-player-token-hash` HTTP header.
   * PostgreSQL Row Level Security (RLS) policies compare this header with the stored `token_hash`.

---

## 3. Database Schema

The backend uses 2 PostgreSQL tables:

### 3.1 `cloud_players`
Stores player career summaries and unlocked rewards.

| Column | Type | Purpose |
|---|---|---|
| `id` | `uuid` | Primary key. Matches local profile UUID. |
| `token_hash` | `text` | Lowercase 64-character SHA-256 hex digest. Unique. |
| `name` | `text` | Display name of the player. |
| `color` | `integer` | Current player color (ARGB integer). |
| `unlocked_rewards` | `jsonb` | Array of reward ID strings. |
| `matches` | `integer` | Total completed matches. |
| `wins` | `integer` | Total won matches. |
| `points` | `integer` | Total scored points. |
| `score_67` | `integer` | Total times reaching 6-7 or 7-6 score. |
| `score_69` | `integer` | Total times reaching 6-9 or 9-6 score. |

### 3.2 `cloud_player_matches`
Stores individual match records for historical review.

| Column | Type | Purpose |
|---|---|---|
| `player_id` | `uuid` | References `cloud_players.id`. |
| `match_id` | `bigint` | Unique match identifier. |
| `finished` | `boolean` | True when the match ended normally. |
| `finished_at` | `bigint` | Epoch millisecond timestamp. |
| `record_json` | `jsonb` | Complete serialized [`MatchRecord`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MatchRecord.java). |

---

## 4. Background Synchronization Lifecycle

1. **Match Completion**:
   * [`MainActivity.archiveCurrentMatch()`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) calls [`SupabaseSyncClient.syncMatchInBackground`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java#L263-L272).
   * Runs asynchronously on [`AsyncRepositoryExecutor`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/AsyncRepositoryExecutor.java).
   * Never blocks the UI thread.
   * If the device is offline, fails quietly without error alerts.
2. **Profile Updates**:
   * [`ProfileStore.update`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/ProfileStore.java) calls [`SupabaseSyncClient.syncProfileInBackground`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java#L275-L291).
   * Pushes latest colors and unlocked rewards to Supabase.
3. **Profile Deletion**:
   * When a profile is deleted, [`CloudConfigStore.unlinkProfile`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java#L91-L97) removes the saved token hash.

---

## 5. Security Rules for Cloud Code

1. Use only `HttpsURLConnection`. Do not use third-party HTTP libraries.
2. Verify that URLs use `https://` via [`CloudConfigStore.isValidHttpsEndpoint`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java#L143-L158).
3. Do not log the secret token, token hash, or anon key.
4. Keep the 512 KB response cap to prevent out-of-memory errors.
5. Validate all pulled profiles with [`InputValidator.sanitizePlayerName`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/InputValidator.java#L56-L62).
