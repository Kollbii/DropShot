-- ============================================================================
-- DropShot — Universal Supabase PostgreSQL Schema (Golden Standard)
-- ============================================================================
-- GOLDEN STANDARD FOR AI AGENTS:
-- This file (`supabase/schema.sql`), the incremental scripts in
-- `supabase/migrations/`, and `docs/agents/cloud-sync.md` are the single
-- source of truth for how the DropShot Supabase database is and will be
-- configured. Read `supabase/AGENTS.md` before you modify any table, column,
-- RLS policy, or `SupabaseSyncClient.java` payload.
--
-- CHARACTERISTICS:
-- 1. 100% Idempotent: Safe to run on an empty Supabase project OR on an
--    existing v1 deployment without losing player profiles or match history.
-- 2. Backward Compatible: Keeps every v1 column in `cloud_players` and
--    `cloud_player_matches` unchanged so older app builds continue to work.
-- 3. Universally Expandable: Uses typed JSONB extension maps
--    (`equipped_cosmetics`, `stats_ext`, `metadata`) and `schema_version` so
--    new lootbox categories (fonts, frames, animations), new statistical
--    metrics, and future metadata do not require destructive DDL migrations.
-- 4. Zero-Dependency PostgREST + RLS Security: Authenticates players through
--    the `x-player-token-hash` HTTP header (64-char lowercase SHA-256 hex).
-- ============================================================================

begin;

create extension if not exists pgcrypto;

-- ----------------------------------------------------------------------------
-- 1. Schema migration ledger
-- ----------------------------------------------------------------------------
create table if not exists public.cloud_schema_migrations (
    version integer primary key,
    name text not null,
    applied_at timestamptz not null default now()
);

comment on table public.cloud_schema_migrations is
    'Tracks applied DropShot database schema versions (Golden Standard ledger).';

-- ----------------------------------------------------------------------------
-- 2. Core table: public.cloud_players
-- ----------------------------------------------------------------------------
create table if not exists public.cloud_players (
    id uuid primary key default gen_random_uuid(),
    token_hash text not null unique,
    name text not null,
    color integer not null default -13665034,
    unlocked_rewards jsonb not null default '[]'::jsonb,
    matches integer not null default 0,
    wins integer not null default 0,
    points integer not null default 0,
    won_sets integer not null default 0,
    total_sets integer not null default 0,
    score_67 integer not null default 0,
    score_69 integer not null default 0,
    updated_at timestamptz not null default now(),
    -- Universal v2+ expansion columns:
    equipped_cosmetics jsonb not null default '{}'::jsonb,
    stats_ext jsonb not null default '{}'::jsonb,
    metadata jsonb not null default '{}'::jsonb,
    schema_version integer not null default 2
);

-- Upgrade existing v1 deployments safely in place
alter table public.cloud_players
    add column if not exists equipped_cosmetics jsonb not null default '{}'::jsonb,
    add column if not exists stats_ext jsonb not null default '{}'::jsonb,
    add column if not exists metadata jsonb not null default '{}'::jsonb,
    add column if not exists schema_version integer not null default 2;

-- Backfill equipped_cosmetics from legacy color column when empty
update public.cloud_players
set equipped_cosmetics = jsonb_build_object(
    'color', color,
    'font', coalesce(equipped_cosmetics ->> 'font', 'font-default')
)
where equipped_cosmetics = '{}'::jsonb;

comment on table public.cloud_players is
    'Player profiles, career counters, unlocked lootbox rewards, and universal JSONB cosmetics/statistics extensions.';
comment on column public.cloud_players.equipped_cosmetics is
    'Universal slot map for active cosmetics (example: {"color": -13665034, "font": "font-anton", "frame": "...", "animation": "..."}).';
comment on column public.cloud_players.stats_ext is
    'Universal career statistics map (serve hold, side-out, Markov serve share, Bayesian Laplace, Wilson 95% bound, [R]/[L] picks, future ratings).';
comment on column public.cloud_players.metadata is
    'Universal extensible key-value object for future client settings or flags.';

-- ----------------------------------------------------------------------------
-- 3. Core table: public.cloud_player_matches
-- ----------------------------------------------------------------------------
create table if not exists public.cloud_player_matches (
    player_id uuid not null references public.cloud_players(id) on delete cascade,
    match_id bigint not null,
    finished boolean not null default false,
    finished_at bigint not null default 0,
    record_json jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    -- Universal v2+ expansion column:
    metadata jsonb not null default '{}'::jsonb,
    primary key (player_id, match_id)
);

alter table public.cloud_player_matches
    add column if not exists metadata jsonb not null default '{}'::jsonb;

comment on table public.cloud_player_matches is
    'Per-player match history records synchronized from DropShot devices.';
comment on column public.cloud_player_matches.record_json is
    'Complete serialized MatchRecord JSON (scores, durations, point timeline, initialServer, pointSides).';
comment on column public.cloud_player_matches.metadata is
    'Universal per-match extension object (venue, watch telemetry, tags).';

-- ----------------------------------------------------------------------------
-- 4. Integrity constraints (idempotent)
-- ----------------------------------------------------------------------------
do $$
begin
    alter table public.cloud_players
        add constraint chk_cloud_players_token_hash_hex
        check (token_hash ~ '^[0-9a-f]{64}$');
exception
    when duplicate_object then null;
end $$;

do $$
begin
    alter table public.cloud_players
        add constraint chk_cloud_players_name_len
        check (char_length(name) between 1 and 64);
exception
    when duplicate_object then null;
end $$;

do $$
begin
    alter table public.cloud_players
        add constraint chk_cloud_players_non_negative_stats
        check (
            matches >= 0
            and wins >= 0
            and points >= 0
            and won_sets >= 0
            and total_sets >= 0
            and score_67 >= 0
            and score_69 >= 0
        );
exception
    when duplicate_object then null;
end $$;

do $$
begin
    alter table public.cloud_players
        add constraint chk_cloud_players_jsonb_shapes
        check (
            jsonb_typeof(unlocked_rewards) = 'array'
            and jsonb_typeof(equipped_cosmetics) = 'object'
            and jsonb_typeof(stats_ext) = 'object'
            and jsonb_typeof(metadata) = 'object'
        );
exception
    when duplicate_object then null;
end $$;

do $$
begin
    alter table public.cloud_player_matches
        add constraint chk_cloud_player_matches_jsonb_shapes
        check (
            jsonb_typeof(record_json) = 'object'
            and jsonb_typeof(metadata) = 'object'
            and match_id > 0
            and finished_at >= 0
        );
exception
    when duplicate_object then null;
end $$;

-- ----------------------------------------------------------------------------
-- 5. Indexes for scalable queries and JSONB inspection
-- ----------------------------------------------------------------------------
create index if not exists idx_cloud_players_updated_at
    on public.cloud_players (updated_at desc);

create index if not exists idx_cloud_players_unlocked_rewards_gin
    on public.cloud_players using gin (unlocked_rewards);

create index if not exists idx_cloud_players_equipped_cosmetics_gin
    on public.cloud_players using gin (equipped_cosmetics);

create index if not exists idx_cloud_player_matches_player_finished
    on public.cloud_player_matches (player_id, finished_at desc);

-- ----------------------------------------------------------------------------
-- 6. Automatic `updated_at` timestamp trigger
-- ----------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists trg_cloud_players_updated_at on public.cloud_players;
create trigger trg_cloud_players_updated_at
    before update on public.cloud_players
    for each row
    execute function public.set_updated_at();

drop trigger if exists trg_cloud_player_matches_updated_at on public.cloud_player_matches;
create trigger trg_cloud_player_matches_updated_at
    before update on public.cloud_player_matches
    for each row
    execute function public.set_updated_at();

-- ----------------------------------------------------------------------------
-- 7. Header-based token authentication & Row Level Security (RLS)
-- ----------------------------------------------------------------------------
-- Extracts and validates `x-player-token-hash` from PostgREST request headers.
-- Returns NULL when missing or malformed so equality comparisons fail closed.
create or replace function public.request_token_hash()
returns text
language sql
stable
as $$
    select case
        when raw_hash ~ '^[0-9a-f]{64}$' then raw_hash
        else null
    end
    from (
        select lower(trim(coalesce(
            (nullif(current_setting('request.headers', true), '')::json ->> 'x-player-token-hash'),
            ''
        ))) as raw_hash
    ) h;
$$;

alter table public.cloud_players enable row level security;
alter table public.cloud_player_matches enable row level security;

grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on public.cloud_players to anon, authenticated;
grant select, insert, update, delete on public.cloud_player_matches to anon, authenticated;

-- Policies for `public.cloud_players`
drop policy if exists cloud_players_select_own on public.cloud_players;
create policy cloud_players_select_own
    on public.cloud_players
    for select
    to anon, authenticated
    using (token_hash = public.request_token_hash());

drop policy if exists cloud_players_insert_own on public.cloud_players;
create policy cloud_players_insert_own
    on public.cloud_players
    for insert
    to anon, authenticated
    with check (
        public.request_token_hash() is not null
        and token_hash = public.request_token_hash()
    );

drop policy if exists cloud_players_update_own on public.cloud_players;
create policy cloud_players_update_own
    on public.cloud_players
    for update
    to anon, authenticated
    using (token_hash = public.request_token_hash())
    with check (token_hash = public.request_token_hash());

drop policy if exists cloud_players_delete_own on public.cloud_players;
create policy cloud_players_delete_own
    on public.cloud_players
    for delete
    to anon, authenticated
    using (token_hash = public.request_token_hash());

-- Policies for `public.cloud_player_matches`
drop policy if exists cloud_player_matches_select_own on public.cloud_player_matches;
create policy cloud_player_matches_select_own
    on public.cloud_player_matches
    for select
    to anon, authenticated
    using (
        exists (
            select 1
            from public.cloud_players cp
            where cp.id = cloud_player_matches.player_id
              and cp.token_hash = public.request_token_hash()
        )
    );

drop policy if exists cloud_player_matches_insert_own on public.cloud_player_matches;
create policy cloud_player_matches_insert_own
    on public.cloud_player_matches
    for insert
    to anon, authenticated
    with check (
        exists (
            select 1
            from public.cloud_players cp
            where cp.id = cloud_player_matches.player_id
              and cp.token_hash = public.request_token_hash()
        )
    );

drop policy if exists cloud_player_matches_update_own on public.cloud_player_matches;
create policy cloud_player_matches_update_own
    on public.cloud_player_matches
    for update
    to anon, authenticated
    using (
        exists (
            select 1
            from public.cloud_players cp
            where cp.id = cloud_player_matches.player_id
              and cp.token_hash = public.request_token_hash()
        )
    )
    with check (
        exists (
            select 1
            from public.cloud_players cp
            where cp.id = cloud_player_matches.player_id
              and cp.token_hash = public.request_token_hash()
        )
    );

drop policy if exists cloud_player_matches_delete_own on public.cloud_player_matches;
create policy cloud_player_matches_delete_own
    on public.cloud_player_matches
    for delete
    to anon, authenticated
    using (
        exists (
            select 1
            from public.cloud_players cp
            where cp.id = cloud_player_matches.player_id
              and cp.token_hash = public.request_token_hash()
        )
    );

-- Record applied migrations
insert into public.cloud_schema_migrations (version, name)
values
    (1, '001_initial_schema'),
    (2, '002_universal_cosmetics_and_stats')
on conflict (version) do nothing;

-- Reload PostgREST schema cache immediately
notify pgrst, 'reload schema';

commit;
