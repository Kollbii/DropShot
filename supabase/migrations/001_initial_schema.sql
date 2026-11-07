-- ============================================================================
-- Migration 001: Baseline DropShot Cloud Schema (v1)
-- Matches the initial `cloud_players` and `cloud_player_matches` deployment.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

create table if not exists public.cloud_schema_migrations (
    version integer primary key,
    name text not null,
    applied_at timestamptz not null default now()
);

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
    updated_at timestamptz not null default now()
);

create table if not exists public.cloud_player_matches (
    player_id uuid not null references public.cloud_players(id) on delete cascade,
    match_id bigint not null,
    finished boolean not null default false,
    finished_at bigint not null default 0,
    record_json jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (player_id, match_id)
);

insert into public.cloud_schema_migrations (version, name)
values (1, '001_initial_schema')
on conflict (version) do nothing;

commit;
