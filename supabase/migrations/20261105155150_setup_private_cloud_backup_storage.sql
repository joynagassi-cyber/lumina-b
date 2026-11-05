insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'lumina-backups',
  'lumina-backups',
  false,
  146800640,
  array['application/octet-stream', 'application/zip']::text[]
)
on conflict (id) do update
set public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create table public.cloud_backup_manifests (
  id uuid primary key,
  owner_id uuid not null references auth.users(id),
  workspace_id text not null check (length(workspace_id) between 1 and 128),
  object_path text not null unique,
  format_version integer not null check (format_version > 0),
  byte_size bigint not null check (byte_size between 1 and 146800640),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  created_at timestamptz not null default now(),
  constraint cloud_backup_object_path_owner check (split_part(object_path, '/', 1) = owner_id::text),
  constraint cloud_backup_object_path_workspace check (split_part(object_path, '/', 2) = workspace_id)
);

create index cloud_backup_manifests_owner_workspace_created_idx
  on public.cloud_backup_manifests (owner_id, workspace_id, created_at desc);

alter table public.cloud_backup_manifests enable row level security;

create policy "backup owners read own manifests"
  on public.cloud_backup_manifests for select to authenticated
  using (owner_id = auth.uid());

create policy "backup owners add own manifests"
  on public.cloud_backup_manifests for insert to authenticated
  with check (owner_id = auth.uid());

create policy "backup owners read own backup objects"
  on storage.objects for select to authenticated
  using (bucket_id = 'lumina-backups' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "backup owners upload own backup objects"
  on storage.objects for insert to authenticated
  with check (bucket_id = 'lumina-backups' and (storage.foldername(name))[1] = auth.uid()::text);
