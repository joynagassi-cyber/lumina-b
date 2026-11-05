-- Editors may invite editors or viewers; viewer members remain read-only.
drop function if exists public.create_church_backup_invite(uuid, text);
create or replace function public.create_church_backup_invite(p_church_id uuid, p_role text default 'editor')
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_token uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if p_role not in ('editor', 'viewer') then raise exception 'invalid_invite_role' using errcode = '22023'; end if;
  if not exists (
    select 1 from public.church_cloud_members m
    where m.church_id = p_church_id
      and m.user_id = auth.uid()
      and m.role in ('owner', 'editor')
  ) then raise exception 'church_backup_write_forbidden' using errcode = '42501'; end if;

  insert into public.church_cloud_invites (church_id, role, created_by)
  values (p_church_id, p_role, auth.uid())
  returning token into v_token;
  return v_token;
end;
$$;
