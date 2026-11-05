-- A deliberately tiny, data-free RPC for scheduled health/keepalive checks.
-- It executes a real PostgreSQL query but cannot read or modify church backups.
create or replace function public.github_keepalive()
returns jsonb
language sql
volatile
security invoker
set search_path = ''
as $function$
  select pg_catalog.jsonb_build_object(
    'ok', true,
    'checked_at', pg_catalog.clock_timestamp()
  );
$function$;

revoke all on function public.github_keepalive() from public;
grant execute on function public.github_keepalive() to anon;
