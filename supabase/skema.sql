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

-- ---------------------------------------------------------------
-- Jempol suka / tidak (seperti Play Store) di halaman Akun.
-- Satu suara per pengenal (akun: "u:<uid>", tamu: "b:<acak>" di localStorage).
-- Siapa pun boleh mengirim/mengubah suaranya; jumlahnya dibaca lewat fungsi
-- nilai_markas() (security definer) supaya baris mentah tetap tertutup.
-- Dijalankan di SQL Editor 15 Sep 2026.
-- ---------------------------------------------------------------
create table if not exists public.markas_nilai (
  pengenal   text primary key,
  user_id    uuid references auth.users (id) on delete set null,
  suka       boolean not null,
  dibuat     timestamptz not null default now(),
  diperbarui timestamptz not null default now()
);
alter table public.markas_nilai enable row level security;
drop policy if exists "nilai kirim" on public.markas_nilai;
create policy "nilai kirim" on public.markas_nilai for insert to anon, authenticated with check (true);
drop policy if exists "nilai ubah" on public.markas_nilai;
create policy "nilai ubah" on public.markas_nilai for update to anon, authenticated using (true) with check (true);
drop policy if exists "nilai baca pemilik" on public.markas_nilai;
create policy "nilai baca pemilik" on public.markas_nilai for select to authenticated using (auth.uid() = 'ac358e3f-0316-48eb-a09a-3ac6ee8e0739');
create or replace function public.nilai_markas()
returns json language sql security definer stable set search_path = public as $$
  select json_build_object('suka', count(*) filter (where suka), 'tidak', count(*) filter (where not suka)) from public.markas_nilai;
$$;
revoke all on function public.nilai_markas() from public;
grant execute on function public.nilai_markas() to anon, authenticated;

-- Upsert lewat REST gagal untuk anon (ON CONFLICT butuh kebijakan SELECT), jadi suara
-- dikirim lewat fungsi security definer; kebijakan insert/update langsung dicabut.
drop policy if exists "nilai kirim" on public.markas_nilai;
drop policy if exists "nilai ubah" on public.markas_nilai;
create or replace function public.kirim_nilai(p_pengenal text, p_suka boolean)
returns void language plpgsql security definer set search_path = public as $$
begin
  if p_pengenal is null or length(p_pengenal) < 3 or length(p_pengenal) > 80 then raise exception 'pengenal tidak sah'; end if;
  insert into public.markas_nilai (pengenal, user_id, suka) values (p_pengenal, auth.uid(), p_suka)
  on conflict (pengenal) do update set suka = excluded.suka, user_id = coalesce(excluded.user_id, markas_nilai.user_id), diperbarui = now();
end;
$$;
revoke all on function public.kirim_nilai(text, boolean) from public;
grant execute on function public.kirim_nilai(text, boolean) to anon, authenticated;

-- ---------------------------------------------------------------
-- Notifikasi adzan (Web Push) walau Markas tertutup.
-- Klien menyimpan langganan push + koordinatnya di sini; Edge Function
-- "adzan-push" (supabase/functions/adzan-push) dipanggil pg_cron tiap menit
-- dan mengirim notifikasi saat waktu sholat masuk. Dijalankan 15 Sep 2026.
-- ---------------------------------------------------------------
create table if not exists public.markas_push (
  endpoint   text primary key,
  user_id    uuid not null references auth.users (id) on delete cascade,
  langganan  jsonb not null,
  lat        double precision not null,
  lng        double precision not null,
  kota       text,
  tz         integer not null default 420,      -- selisih menit dari UTC (WIB = 420)
  waktu      jsonb not null default '{"subuh":true,"dzuhur":true,"ashar":true,"maghrib":true,"isya":true}',
  aktif      boolean not null default true,
  terakhir   text,                              -- "YYYY-MM-DD:subuh" yang terakhir dikirim
  dibuat     timestamptz not null default now(),
  diperbarui timestamptz not null default now()
);
alter table public.markas_push enable row level security;
drop policy if exists "push milik sendiri" on public.markas_push;
create policy "push milik sendiri" on public.markas_push
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- pg_cron + pg_net: panggil Edge Function tiap menit (ganti <CRON_KEY> dengan isi .push.env)
create extension if not exists pg_cron;
create extension if not exists pg_net;
select cron.schedule('adzan-push-tiap-menit', '* * * * *', $$
  select net.http_post(
    url := 'https://hzxfheydtrjhizbwbddh.supabase.co/functions/v1/adzan-push',
    headers := '{"Content-Type":"application/json","x-cron-key":"<CRON_KEY>"}'::jsonb,
    body := '{}'::jsonb);
$$);
