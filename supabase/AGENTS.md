# AGENTS.md — Supabase Database (Golden Standard)

These rules apply to `supabase/` and all cloud synchronization code in `Android/app/`.
They add to the root [AGENTS.md](../AGENTS.md) and [docs/agents/cloud-sync.md](../docs/agents/cloud-sync.md).

---

## 1. Golden Standard Declaration

> [!IMPORTANT]
> `supabase/schema.sql` and `supabase/migrations/` are the **Golden Standard** for AI agents to understand how the DropShot Supabase PostgreSQL database is and will be configured.
> Never guess column names, types, or Row Level Security (RLS) rules. Read [`schema.sql`](schema.sql) first.

---

## 2. Universal Database Architecture

The schema uses a **Hybrid Universal Design**:

1. **Stable Relational Core (v1 compatible)**:
   - `public.cloud_players` keeps `id`, `token_hash`, `name`, `color`, `unlocked_rewards`, `matches`, `wins`, `points`, `won_sets`, `total_sets`, `score_67`, `score_69`, and `updated_at`.
   - `public.cloud_player_matches` keeps `(player_id, match_id)`, `finished`, `finished_at`, `record_json`, and `updated_at`.
2. **Universal JSONB Expansion Slots (v2+)**:
   - `cloud_players.equipped_cosmetics` (`jsonb` object): Stores active cosmetics by category key (`"color"`, `"font"`, and future slots such as `"frame"` or `"animation"`).
   - `cloud_players.stats_ext` (`jsonb` object): Stores extended analytics (`serves_total`, `serves_won`, `receives_total`, `receives_won`, `right_picks`, `left_picks`, `laplace_win_prob`, `wilson_lower_95`, `markov_serve_share`, and future rating metrics).
   - `cloud_players.metadata` and `cloud_player_matches.metadata` (`jsonb` objects): Reserved for future client flags, tournament tags, or watch telemetry.
   - `cloud_players.schema_version` (`int4`): Tracks the client payload schema version (`CURRENT_SCHEMA_VERSION = 2`).
   - `public.cloud_schema_migrations`: Records applied SQL migration versions.

---

## 3. Rules for Database Evolution

1. **Prefer JSONB keys before DDL changes**:
   - Add new cosmetic slots inside `equipped_cosmetics`.
   - Add new unlocked reward IDs (any `RewardCatalog.Kind`) to the `unlocked_rewards` JSONB array.
   - Add new career metrics inside `stats_ext`.
   - Add new match properties inside `record_json` or `cloud_player_matches.metadata`.
2. **Never break v1 or older clients**:
   - Do not drop, rename, or change the type of any existing column.
   - Give every new column a `NOT NULL DEFAULT ...` value and add it with `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
3. **Keep SQL scripts 100% idempotent**:
   - `supabase/schema.sql` must run cleanly on both an empty database and a live production database.
   - Whenever you add a structural change, add a numbered file in `supabase/migrations/00N_description.sql`, merge the change into `supabase/schema.sql`, and end with `NOTIFY pgrst, 'reload schema';`.
4. **Preserve fail-closed RLS**:
   - All client requests authenticate with `x-player-token-hash` validated by `public.request_token_hash()`.
   - Never create an RLS policy with `USING (true)` on player tables.
5. **Keep client fallback intact**:
   - `SupabaseSyncClient` must continue to handle older databases gracefully and validate all downloaded fields through `InputValidator`.
