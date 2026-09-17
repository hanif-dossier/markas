// Memindah akun (tanpa sandi, UUID sama) dan data Markasku dari proyek Supabase lama (bersama Hanif Dossier)
// ke proyek baru "markasku". Aman dijalankan ulang. Tidak mencetak email lengkap maupun kunci.
//   node supabase/pindah-proyek.mjs            # jalankan
//   node supabase/pindah-proyek.mjs --lihat    # hanya hitung, tidak menulis
// Kunci: lama = beranda/.supabase.env, baru = rahasia/supabase-markasku.txt
import fs from 'node:fs';
const baca = f => Object.fromEntries(fs.readFileSync(f, 'utf8').split(/\r?\n/).filter(l => /^[A-Z_]+=/.test(l)).map(l => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()]));
const LAMA = baca('D:/Ai Agent/beranda/.supabase.env'), BARU = baca('D:/Ai Agent/rahasia/supabase-markasku.txt');
if (!/^sb_secret_|^eyJ/.test(BARU.SUPABASE_SECRET || '')) { console.error('SUPABASE_SECRET proyek baru belum terisi / bentuknya tidak dikenal'); process.exit(1); }
const LIHAT = process.argv.includes('--lihat');
const kepala = k => ({ apikey: k, Authorization: 'Bearer ' + k, 'Content-Type': 'application/json' });
const samar = e => String(e || '').replace(/^(.{3}).*(@.*)$/, '$1…$2');
const ambil = async (p, jalur) => { const r = await fetch(p.SUPABASE_URL + jalur, { headers: kepala(p.SUPABASE_SECRET) }); if (!r.ok) throw new Error(jalur + ' ' + r.status + ' ' + (await r.text()).slice(0, 200)); return r.json(); };

const TABEL = ['markas_dokumen', 'markas_masukan', 'markas_nilai', 'markas_push'];
const data = {}; for (const t of TABEL) data[t] = await ambil(LAMA, `/rest/v1/${t}?select=*`);
const pemakai = new Set(TABEL.flatMap(t => data[t].map(r => r.user_id).filter(Boolean)));
const semua = (await ambil(LAMA, '/auth/v1/admin/users?per_page=1000')).users;
const pindah = semua.filter(u => u.user_metadata?.aplikasi === 'markas' || /@markasku\.my\.id$/i.test(u.email) || pemakai.has(u.id));
console.log('akun di proyek lama:', semua.length, '| dipindah:', pindah.length, '| baris data:', TABEL.map(t => t + '=' + data[t].length).join(', '));

const adaBaru = new Map((await ambil(BARU, '/auth/v1/admin/users?per_page=1000')).users.map(u => [u.id, u]));
let dibuat = 0, sudah = 0, gagal = 0;
for (const u of pindah) {
  if (adaBaru.has(u.id)) { sudah++; continue; }
  if (LIHAT) { console.log('  akan dibuat:', samar(u.email)); continue; }
  const r = await fetch(BARU.SUPABASE_URL + '/auth/v1/admin/users', { method: 'POST', headers: kepala(BARU.SUPABASE_SECRET),
    body: JSON.stringify({ id: u.id, email: u.email, email_confirm: !!u.email_confirmed_at, user_metadata: { ...(u.user_metadata || {}), aplikasi: 'markas', pindahan: true } }) });
  if (r.ok) { const j = await r.json(); if (j.id !== u.id) { console.log('  PERINGATAN UUID berubah:', samar(u.email)); gagal++; } else dibuat++; }
  else { gagal++; console.log('  gagal', samar(u.email), r.status, (await r.text()).slice(0, 160)); }
}
console.log('akun: dibuat', dibuat, '| sudah ada', sudah, '| gagal', gagal);
if (LIHAT || gagal) process.exit(gagal ? 1 : 0);

const idBaru = new Set((await ambil(BARU, '/auth/v1/admin/users?per_page=1000')).users.map(u => u.id));
for (const t of TABEL) {
  const baris = data[t].filter(r => !r.user_id || idBaru.has(r.user_id));
  if (!baris.length) { console.log(t + ': kosong'); continue; }
  const r = await fetch(BARU.SUPABASE_URL + `/rest/v1/${t}`, { method: 'POST', headers: { ...kepala(BARU.SUPABASE_SECRET), Prefer: 'resolution=merge-duplicates,return=minimal' }, body: JSON.stringify(baris) });
  console.log(t + ':', r.ok ? 'tersalin ' + baris.length + ' baris' : 'GAGAL ' + r.status + ' ' + (await r.text()).slice(0, 200), baris.length < data[t].length ? '(dilewati ' + (data[t].length - baris.length) + ' tanpa akun)' : '');
}
for (const t of TABEL) console.log('periksa', t, '→ baru:', (await ambil(BARU, `/rest/v1/${t}?select=*`)).length, '/ lama:', data[t].length);
