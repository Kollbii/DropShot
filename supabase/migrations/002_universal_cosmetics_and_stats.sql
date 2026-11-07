-- ============================================================================
-- Migration 002: Universal Cosmetics, Extended Statistics, Triggers & RLS (v2)
-- Safely upgrades an existing v1 Supabase deployment in place without data loss.
-- ============================================================================

begin;

create table if not exists public.cloud_schema_migrations (
    version integer primary key,
    name text not null,
    applied_at timestamptz not null default now()
);

alter table public.cloud_players
    add column if not exists equipped_cosmetics jsonb not null default '{}'::jsonb,
    add column if not exists stats_ext jsonb not null default '{}'::jsonb,
    add column if not exists metadata jsonb not null default '{}'::jsonb,
    add column if not exists schema_version integer not null default 2;

alter table public.cloud_player_matches
    add column if not exists metadata jsonb not null default '{}'::jsonb;

update public.cloud_players
set equipped_cosmetics = jsonb_build_object(
    'color', color,
    'font', coalesce(equipped_cosmetics ->> 'font', 'font-default')
)
where equipped_cosmetics = '{}'::jsonb;

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

create index if not exists idx_cloud_players_updated_at
    on public.cloud_players (updated_at desc);

create index if not exists idx_cloud_players_unlocked_rewards_gin
    on public.cloud_players using gin (unlocked_rewards);

create index if not exists idx_cloud_players_equipped_cosmetics_gin
    on public.cloud_players using gin (equipped_cosmetics);

create index if not exists idx_cloud_player_matches_player_finished
    on public.cloud_player_matches (player_id, finished_at desc);

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

insert into public.cloud_schema_migrations (version, name)
values (2, '002_universal_cosmetics_and_stats')
on conflict (version) do nothing;

notify pgrst, 'reload schema';

commit;
