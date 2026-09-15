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

-- ---------------------------------------------------------------
-- Hapus akun sendiri (tombol "Hapus akun saya" di halaman Akun).
-- Klien tidak boleh menghapus auth.users langsung, jadi lewat fungsi
-- security definer yang hanya menghapus baris milik pemanggil (auth.uid()).
-- Baris markas_dokumen ikut terhapus lewat "on delete cascade".
-- Dijalankan di SQL Editor 15 Sep 2026.
-- ---------------------------------------------------------------
create or replace function public.hapus_akun_saya()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'belum masuk';
  end if;
  delete from public.anggota where id = auth.uid();
  delete from auth.users where id = auth.uid();
end;
$$;
revoke all on function public.hapus_akun_saya() from public;
grant execute on function public.hapus_akun_saya() to authenticated;

-- ---------------------------------------------------------------
-- Saran, kritik & bantuan (formulir di halaman Akun).
-- Siapa pun (termasuk tamu) boleh mengirim; hanya pemilik Markas yang boleh
-- membaca dan menandai "dibalas". Dijalankan di SQL Editor 15 Sep 2026.
-- ---------------------------------------------------------------
create table if not exists public.markas_masukan (
  id       bigint generated always as identity primary key,
  user_id  uuid references auth.users (id) on delete set null,
  email    text,
  jenis    text not null default 'saran' check (jenis in ('saran','kritik','masalah','tanya')),
  pesan    text not null check (char_length(pesan) between 3 and 2000),
  halaman  text,
  dibuat   timestamptz not null default now(),
  dibalas  boolean not null default false
);
alter table public.markas_masukan enable row level security;
drop policy if exists "masukan kirim" on public.markas_masukan;
create policy "masukan kirim" on public.markas_masukan
  for insert to anon, authenticated with check (user_id is null or user_id = auth.uid());
drop policy if exists "masukan baca pemilik" on public.markas_masukan;
create policy "masukan baca pemilik" on public.markas_masukan
  for select to authenticated using (auth.uid() = 'ac358e3f-0316-48eb-a09a-3ac6ee8e0739');
drop policy if exists "masukan ubah pemilik" on public.markas_masukan;
create policy "masukan ubah pemilik" on public.markas_masukan
  for update to authenticated using (auth.uid() = 'ac358e3f-0316-48eb-a09a-3ac6ee8e0739');
