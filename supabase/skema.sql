-- Markas: satu tabel untuk semua dokumen pengguna (pengaturan, harian/<tgl>, keuangan/aruskas, dst).
-- Jalankan sekali di Supabase: SQL Editor -> tempel -> Run. Aman dijalankan ulang.
create table if not exists public.markas_dokumen (
  user_id uuid not null references auth.users (id) on delete cascade,
  path text not null,
  data jsonb not null default '{}'::jsonb,
  diperbarui timestamptz not null default now(),
  primary key (user_id, path)
);
alter table public.markas_dokumen enable row level security;

-- Tiap orang hanya bisa membaca dan mengubah barisnya sendiri. Tidak ada admin yang bisa membaca data orang lain.
drop policy if exists "markas baca sendiri" on public.markas_dokumen;
create policy "markas baca sendiri" on public.markas_dokumen
  for select to authenticated using (user_id = auth.uid());
drop policy if exists "markas tulis sendiri" on public.markas_dokumen;
create policy "markas tulis sendiri" on public.markas_dokumen
  for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "markas ubah sendiri" on public.markas_dokumen;
create policy "markas ubah sendiri" on public.markas_dokumen
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "markas hapus sendiri" on public.markas_dokumen;
create policy "markas hapus sendiri" on public.markas_dokumen
  for delete to authenticated using (user_id = auth.uid());

create index if not exists markas_dokumen_harian on public.markas_dokumen (user_id, path text_pattern_ops);
