-- Allow a device to retry after a network timeout without consuming its own invite twice.
create or replace function public.accept_church_backup_invite(p_token uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_church_id uuid;
  v_role text;
  v_redeemed_by uuid;
  v_redeemed_at timestamptz;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select i.church_id, i.role, i.redeemed_by, i.redeemed_at
  into v_church_id, v_role, v_redeemed_by, v_redeemed_at
  from public.church_cloud_invites i
  where i.token = p_token and i.expires_at > now()
  for update;
  if v_church_id is null then raise exception 'invite_invalid_or_expired' using errcode = '22023'; end if;

  if v_redeemed_at is not null then
    if v_redeemed_by = auth.uid() then return v_church_id; end if;
    raise exception 'invite_already_used' using errcode = '22023';
  end if;

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
