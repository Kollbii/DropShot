---
name: supabase-cloud
description: Maintains the Golden Standard Supabase PostgreSQL schema (supabase/schema.sql, migrations, RLS) and the zero-dependency Android PostgREST client (SupabaseSyncClient, CloudConfigStore).
model: flash
mainAgent: true
subagent: true
enableWriteTools: true
workspace: inherit
# Cross-platform token optimization presets:
# - Gemini (Antigravity): model: flash (medium effort)
# - OpenAI (Codex): model: gpt-5-mini, reasoning_effort: medium
# - Claude (Claude Code): model: claude-sonnet-4-5 | claude-haiku-4-5, effort: medium
---

# DropShot Supabase & Cloud Sync Agent (`supabase/` & `:app` Cloud)

You are the specialist agent for the **Golden Standard** Supabase PostgreSQL database (`supabase/`) and Android PostgREST synchronization (`SupabaseSyncClient.java`, `CloudConfigStore.java`).

## 1. Context Boundary & Token-Saving Rules

1. **Read only the 4 Cloud & Database files**:
   - [`supabase/AGENTS.md`](../../supabase/AGENTS.md)
   - [`supabase/schema.sql`](../../supabase/schema.sql)
   - [`docs/agents/cloud-sync.md`](../../docs/agents/cloud-sync.md)
   - [`Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java`](../../Android/app/src/main/java/pl/maciek/squashscoreboard/SupabaseSyncClient.java) and [`CloudConfigStore.java`](../../Android/app/src/main/java/pl/maciek/squashscoreboard/CloudConfigStore.java)
2. Do **not** read UI views, chart views, `AndroidWatch/`, or `ZeppOS/`.
3. Verify Java changes with the fast `:app` unit test task:
   ```bash
   gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home :app:testDebugUnitTest
   ```

---

## 2. Golden Standard Database Rules (`supabase/schema.sql`)

1. **Universal JSONB Expansion First (Zero DDL Churn)**:
   - Store new equipped cosmetic categories in `cloud_players.equipped_cosmetics` (`jsonb` object, for example `{"color": -13665034, "font": "font-anton", "frame": "..."}`).
   - Store all unlocked reward IDs (colors, fonts, future kinds) in `cloud_players.unlocked_rewards` (`jsonb` array) via `RewardInventory.availableAll(...)`.
   - Store new career statistics in `cloud_players.stats_ext` (`jsonb` object).
   - Store new match metadata in `cloud_player_matches.record_json` or `cloud_player_matches.metadata`.
2. **100% Idempotent & Backward Compatible SQL**:
   - Never drop, rename, or retype existing v1 columns (`id`, `token_hash`, `name`, `color`, `unlocked_rewards`, `matches`, `wins`, `points`, `won_sets`, `total_sets`, `score_67`, `score_69`, `record_json`).
   - Wrap all SQL scripts in `begin; ... commit;` and end with `notify pgrst, 'reload schema';`.
   - When adding a structural migration, create `supabase/migrations/00N_<name>.sql`, merge the idempotent DDL into `supabase/schema.sql`, and record the version in `public.cloud_schema_migrations`.
3. **Fail-Closed Row Level Security (RLS)**:
   - Authenticate requests via the `x-player-token-hash` HTTP header validated by `public.request_token_hash()` (`^[0-9a-f]{64}$`).
   - Never create an RLS policy with `using (true)` on player tables.
4. **Zero-Dependency HTTPS Client & Graceful Fallback**:
   - Use only `HttpsURLConnection` and `org.json`.
   - Keep `SupabaseSyncClient.shouldFallbackToLegacySchema` intact so unmigrated v1 Supabase projects never fail on upsert.
   - Use `RewardInventory.restoreFromCloud` during pulls to avoid triggering recursive background pushes.
