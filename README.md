# Markas — pusat kendali diri

Aplikasi web (bisa dipasang di HP) untuk siapa saja: jadwal sholat kota sendiri,
checklist kebiasaan, hari bersih & streak disiplin, jurnal pagi, arus kas, bayar
hutang per minggu, target financial freedom, rencana 3 tahun, dan catatan keputusan.

Alamat: <https://hanif-dossier.github.io/markas/>

Ini **salinan produk** dari dasbor pribadi pemilik (`D:\Ai Agent\dashboard-pribadi\`,
Markas Hanif). Yang pribadi tidak disentuh; yang di sini isinya kosong/contoh dan
tiap pengguna punya datanya sendiri.

## Cara kerja

| Berkas | Isi |
|---|---|
| `index.html` | Seluruh aplikasi (tanpa build tool). Layar masuk/daftar → dasbor. |
| `supabase/skema.sql` | Tabel `markas_dokumen` + kebijakan RLS: tiap orang hanya bisa membaca/menulis barisnya sendiri. **Jalankan sekali** di Supabase → SQL Editor. |
| `manifest.webmanifest`, `sw.js`, `ikon-*.png` | Supaya bisa dipasang di layar utama HP (PWA). |

Penyimpanan: satu baris per dokumen per pengguna (`pengaturan/utama`, `harian/<tanggal>`,
`keuangan/aruskas`, `keuangan/aset`, `fokus/daftar`, `jadwal/rutinitas`, `rencana/3tahun`,
`keputusan/daftar`, `kuliah/semester5`) di tabel `markas_dokumen`, kolom `data` (jsonb).
Belum masuk → "Mode coba" menyimpan di `localStorage` browser itu saja.

Akun memakai proyek Supabase yang sama dengan Hanif Dossier (`fqpktykrkpqaztnpqgxz`).
Pendaftar Markas diberi metadata `aplikasi: 'markas'`; mereka juga masuk tabel
`anggota` lewat trigger lama dengan status `menunggu`, itu tidak berpengaruh ke Markas.

## Memperbarui

```
git add -A && git commit -m "..." && git push
```
GitHub Pages membangun ±30 detik. Uji lokal: `preview_start markas` (port 8085).
