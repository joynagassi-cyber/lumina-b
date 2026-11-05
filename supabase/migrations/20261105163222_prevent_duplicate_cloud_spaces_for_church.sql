-- A local church identity maps to at most one cloud workspace.
alter table public.church_cloud_workspaces
  add column if not exists local_church_uid text;

update public.church_cloud_workspaces
set local_church_uid = 'legacy-' || id::text
where local_church_uid is null;

alter table public.church_cloud_workspaces
  alter column local_church_uid set not null;

alter table public.church_cloud_workspaces
  add constraint church_cloud_workspaces_local_church_uid_check
  check (local_church_uid ~ '^[A-Za-z0-9_-]{8,128}$');

create unique index if not exists church_cloud_workspaces_local_church_uid_key
  on public.church_cloud_workspaces (local_church_uid);

drop function if exists public.create_church_backup_workspace(uuid, text);
create or replace function public.create_church_backup_workspace(
  p_creation_key uuid,
  p_display_name text,
  p_local_church_uid text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_church_id uuid;
  v_name text;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if p_creation_key is null then raise exception 'creation_key_required' using errcode = '22023'; end if;
  if p_local_church_uid is null or p_local_church_uid !~ '^[A-Za-z0-9_-]{8,128}$' then
    raise exception 'invalid_local_church_uid' using errcode = '22023';
  end if;
  v_name := left(trim(coalesce(p_display_name, '')), 120);
  if v_name = '' then v_name := 'Eglise Lumina'; end if;

  insert into public.church_cloud_workspaces (creation_key, local_church_uid, display_name, created_by)
  values (p_creation_key, p_local_church_uid, v_name, auth.uid())
  on conflict do nothing;

  select w.id into v_church_id
  from public.church_cloud_workspaces w
  where w.creation_key = p_creation_key and w.created_by = auth.uid();
  if v_church_id is not null then
    insert into public.church_cloud_members (church_id, user_id, role)
    values (v_church_id, auth.uid(), 'owner')
    on conflict (church_id, user_id) do nothing;
    return v_church_id;
  end if;

  select w.id into v_church_id
  from public.church_cloud_workspaces w
  where w.local_church_uid = p_local_church_uid;
  if v_church_id is null then raise exception 'creation_key_not_owned' using errcode = '42501'; end if;
  if not exists (
    select 1 from public.church_cloud_members m
    where m.church_id = v_church_id and m.user_id = auth.uid()
  ) then
    raise exception 'church_already_linked_invitation_required' using errcode = '42501';
  end if;
  return v_church_id;
end;
$$;
