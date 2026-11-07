# DropShot — Supabase Database Setup (`supabase/`)

This directory contains the **Golden Standard** PostgreSQL schema and migrations for DropShot cloud synchronization.

---

## 1. Files

| File | Purpose |
|---|---|
| [`schema.sql`](schema.sql) | Complete, idempotent setup and upgrade script. Run this in the Supabase SQL Editor for both new and existing projects. |
| [`migrations/001_initial_schema.sql`](migrations/001_initial_schema.sql) | Baseline v1 tables (`cloud_players` and `cloud_player_matches`). |
| [`migrations/002_universal_cosmetics_and_stats.sql`](migrations/002_universal_cosmetics_and_stats.sql) | Incremental v2 upgrade adding universal JSONB expansion columns (`equipped_cosmetics`, `stats_ext`, `metadata`), GIN indexes, `updated_at` triggers, and RLS helpers. |
| [`AGENTS.md`](AGENTS.md) | Mandatory database rules for AI agents and contributors. |

---

## 2. How to Create or Upgrade Your Supabase Database

1. Open your project in the Supabase Dashboard.
2. Open **SQL Editor** and click **New query**.
3. Copy the full contents of [`supabase/schema.sql`](schema.sql) into the editor.
4. Click **Run**.

> [!NOTE]
> [`schema.sql`](schema.sql) is idempotent. If you already have `cloud_players` and `cloud_player_matches` with live data, it preserves every row, adds the v2 universal columns (`equipped_cosmetics`, `stats_ext`, `metadata`, `schema_version`), backfills equipped colors, enables Row Level Security (RLS), and reloads the PostgREST schema cache automatically.

---

## 3. How to Expand the Database in the Future

- **New cosmetic category** (for example, `frame` or `animation`): Store the equipped ID in `cloud_players.equipped_cosmetics` (for example, `{"color": -13665034, "font": "font-anton", "frame": "frame-gold"}`) and include unlocked IDs in `cloud_players.unlocked_rewards`. No SQL table change is necessary.
- **New player statistic**: Add the metric key to `cloud_players.stats_ext` in `SupabaseSyncClient.java`. No SQL table change is necessary.
- **New table or indexed column**: Add `supabase/migrations/003_<feature>.sql`, merge the idempotent DDL into `supabase/schema.sql`, and update [`docs/agents/cloud-sync.md`](../docs/agents/cloud-sync.md).
