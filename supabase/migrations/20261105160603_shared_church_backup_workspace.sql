-- Replace per-account backup ownership with a shared church workspace.
-- No Android client uploads have been enabled, so the previous per-user table is empty.
drop policy if exists "backup owners read own manifests" on public.cloud_backup_manifests;
drop policy if exists "backup owners add own manifests" on public.cloud_backup_manifests;
drop policy if exists "backup owners read own backup objects" on storage.objects;
drop policy if exists "backup owners upload own backup objects" on storage.objects;
drop table if exists public.cloud_backup_manifests;

create table public.church_cloud_workspaces (
  id uuid primary key default gen_random_uuid(),
  creation_key uuid not null unique,
  display_name text not null check (length(display_name) between 1 and 120),
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);

create table public.church_cloud_members (
  church_id uuid not null references public.church_cloud_workspaces(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('owner', 'editor', 'viewer')),
  joined_at timestamptz not null default now(),
  primary key (church_id, user_id)
);

create table public.church_cloud_invites (
  token uuid primary key default gen_random_uuid(),
  church_id uuid not null references public.church_cloud_workspaces(id) on delete cascade,
  role text not null check (role in ('editor', 'viewer')),
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '7 days'),
  redeemed_by uuid references auth.users(id),
  redeemed_at timestamptz
);

create table public.cloud_backup_manifests (
  id uuid primary key default gen_random_uuid(),
  church_id uuid not null references public.church_cloud_workspaces(id) on delete cascade,
  created_by uuid not null references auth.users(id),
  object_path text not null unique,
  generation bigint not null check (generation > 0),
  format_version integer not null check (format_version > 0),
  byte_size bigint not null check (byte_size between 1 and 146800640),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  created_at timestamptz not null default now(),
  unique (church_id, generation)
);

create table public.cloud_backup_heads (
  church_id uuid primary key references public.church_cloud_workspaces(id) on delete cascade,
  manifest_id uuid not null unique references public.cloud_backup_manifests(id),
  generation bigint not null check (generation > 0),
  updated_at timestamptz not null default now()
);

create index church_cloud_members_user_idx on public.church_cloud_members (user_id, church_id);
create index cloud_backup_manifests_church_created_idx on public.cloud_backup_manifests (church_id, created_at desc);

alter table public.church_cloud_workspaces enable row level security;
alter table public.church_cloud_members enable row level security;
alter table public.church_cloud_invites enable row level security;
alter table public.cloud_backup_manifests enable row level security;
alter table public.cloud_backup_heads enable row level security;

create or replace function public.is_church_backup_member(p_church_id uuid)
returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.church_cloud_members m
    where m.church_id = p_church_id and m.user_id = auth.uid()
  );
$$;

create or replace function public.can_write_church_backup(p_church_id uuid)
returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.church_cloud_members m
    where m.church_id = p_church_id
      and m.user_id = auth.uid()
      and m.role in ('owner', 'editor')
  );
$$;

create or replace function public.create_church_backup_workspace(p_creation_key uuid, p_display_name text)
returns uuid
language plpgsql security definer set search_path = ''
as $$
declare
  v_church_id uuid;
  v_name text;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if p_creation_key is null then raise exception 'creation_key_required' using errcode = '22023'; end if;
  v_name := left(trim(coalesce(p_display_name, '')), 120);
  if v_name = '' then v_name := 'Eglise Lumina'; end if;

  insert into public.church_cloud_workspaces (creation_key, display_name, created_by)
  values (p_creation_key, v_name, auth.uid())
  on conflict (creation_key) do nothing;

  select w.id into v_church_id
  from public.church_cloud_workspaces w
  where w.creation_key = p_creation_key and w.created_by = auth.uid();
  if v_church_id is null then raise exception 'creation_key_not_owned' using errcode = '42501'; end if;

  insert into public.church_cloud_members (church_id, user_id, role)
  values (v_church_id, auth.uid(), 'owner')
  on conflict (church_id, user_id) do nothing;

  return v_church_id;
end;
$$;

create or replace function public.create_church_backup_invite(p_church_id uuid, p_role text default 'editor')
returns uuid
language plpgsql security definer set search_path = ''
as $$
declare
  v_token uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if p_role not in ('editor', 'viewer') then raise exception 'invalid_invite_role' using errcode = '22023'; end if;
  if not exists (
    select 1 from public.church_cloud_members m
    where m.church_id = p_church_id and m.user_id = auth.uid() and m.role = 'owner'
  ) then raise exception 'church_owner_required' using errcode = '42501'; end if;

  insert into public.church_cloud_invites (church_id, role, created_by)
  values (p_church_id, p_role, auth.uid())
  returning token into v_token;
  return v_token;
end;
$$;

create or replace function public.accept_church_backup_invite(p_token uuid)
returns uuid
language plpgsql security definer set search_path = ''
as $$
declare
  v_church_id uuid;
  v_role text;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select i.church_id, i.role into v_church_id, v_role
  from public.church_cloud_invites i
  where i.token = p_token and i.redeemed_at is null and i.expires_at > now()
  for update;
  if v_church_id is null then raise exception 'invite_invalid_or_expired' using errcode = '22023'; end if;

  update public.church_cloud_invites
  set redeemed_by = auth.uid(), redeemed_at = now()
  where token = p_token and redeemed_at is null;
  if not found then raise exception 'invite_already_used' using errcode = '22023'; end if;

  insert into public.church_cloud_members (church_id, user_id, role)
  values (v_church_id, auth.uid(), v_role)
  on conflict (church_id, user_id) do nothing;
  return v_church_id;
end;
$$;

create or replace function public.publish_church_backup(
  p_church_id uuid,
  p_expected_generation bigint,
  p_object_path text,
  p_format_version integer,
  p_byte_size bigint,
  p_sha256 text
)
returns table (manifest_id uuid, generation bigint)
language plpgsql security definer set search_path = ''
as $$
declare
  v_current_generation bigint;
  v_next_generation bigint;
  v_manifest_id uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if not public.can_write_church_backup(p_church_id) then
    raise exception 'church_backup_write_forbidden' using errcode = '42501';
  end if;
  if p_expected_generation is null or p_expected_generation < 0 then
    raise exception 'invalid_expected_generation' using errcode = '22023';
  end if;
  if p_format_version is null or p_format_version <= 0 then raise exception 'invalid_format_version' using errcode = '22023'; end if;
  if p_byte_size is null or p_byte_size < 1 or p_byte_size > 146800640 then raise exception 'invalid_backup_size' using errcode = '22023'; end if;
  if p_sha256 is null or p_sha256 !~ '^[0-9a-f]{64}$' then raise exception 'invalid_backup_checksum' using errcode = '22023'; end if;
  if p_object_path is null or p_object_path !~ ('^churches/' || p_church_id::text || '/[0-9a-f-]{36}\.lumina$') then
    raise exception 'invalid_backup_object_path' using errcode = '22023';
  end if;

  -- Serialize publishers on the church row, then compare the caller's base version.
  perform 1 from public.church_cloud_workspaces w where w.id = p_church_id for update;
  if not found then raise exception 'church_workspace_not_found' using errcode = '22023'; end if;
  select h.generation into v_current_generation
  from public.cloud_backup_heads h where h.church_id = p_church_id;
  v_current_generation := coalesce(v_current_generation, 0);
  if v_current_generation <> p_expected_generation then
    raise exception 'backup_version_conflict' using errcode = '40001';
  end if;

  if not exists (
    select 1 from storage.objects o
    where o.bucket_id = 'lumina-backups' and o.name = p_object_path
  ) then raise exception 'uploaded_backup_object_not_found' using errcode = '22023'; end if;

  v_next_generation := v_current_generation + 1;
  insert into public.cloud_backup_manifests (
    church_id, created_by, object_path, generation, format_version, byte_size, sha256
  ) values (
    p_church_id, auth.uid(), p_object_path, v_next_generation, p_format_version, p_byte_size, p_sha256
  ) returning id into v_manifest_id;

  insert into public.cloud_backup_heads (church_id, manifest_id, generation, updated_at)
  values (p_church_id, v_manifest_id, v_next_generation, now())
  on conflict (church_id) do update
  set manifest_id = excluded.manifest_id,
      generation = excluded.generation,
      updated_at = excluded.updated_at;

  return query select v_manifest_id, v_next_generation;
end;
$$;

create policy "church members read workspaces"
  on public.church_cloud_workspaces for select to authenticated
  using (public.is_church_backup_member(id));

create policy "church members read membership list"
  on public.church_cloud_members for select to authenticated
  using (user_id = auth.uid() or public.is_church_backup_member(church_id));

create policy "church members read backup history"
  on public.cloud_backup_manifests for select to authenticated
  using (public.is_church_backup_member(church_id));

create policy "church members read current backup pointer"
  on public.cloud_backup_heads for select to authenticated
  using (public.is_church_backup_member(church_id));

create policy "church members read shared backup objects"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'lumina-backups'
    and (storage.foldername(name))[1] = 'churches'
    and exists (
      select 1 from public.church_cloud_members m
      where m.church_id::text = (storage.foldername(name))[2]
        and m.user_id = auth.uid()
    )
  );

create policy "church editors upload shared backup objects"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'lumina-backups'
    and (storage.foldername(name))[1] = 'churches'
    and exists (
      select 1 from public.church_cloud_members m
      where m.church_id::text = (storage.foldername(name))[2]
        and m.user_id = auth.uid()
        and m.role in ('owner', 'editor')
    )
  );

revoke all on function public.is_church_backup_member(uuid) from public, anon;
revoke all on function public.can_write_church_backup(uuid) from public, anon;
revoke all on function public.create_church_backup_workspace(uuid, text) from public, anon;
revoke all on function public.create_church_backup_invite(uuid, text) from public, anon;
revoke all on function public.accept_church_backup_invite(uuid) from public, anon;
revoke all on function public.publish_church_backup(uuid, bigint, text, integer, bigint, text) from public, anon;
grant execute on function public.is_church_backup_member(uuid) to authenticated;
grant execute on function public.can_write_church_backup(uuid) to authenticated;
grant execute on function public.create_church_backup_workspace(uuid, text) to authenticated;
grant execute on function public.create_church_backup_invite(uuid, text) to authenticated;
grant execute on function public.accept_church_backup_invite(uuid) to authenticated;
grant execute on function public.publish_church_backup(uuid, bigint, text, integer, bigint, text) to authenticated;
