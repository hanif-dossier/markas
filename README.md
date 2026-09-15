# Markasku — pusat kendali diri

Aplikasi web (bisa dipasang di HP) untuk siapa saja: jadwal sholat kota sendiri,
checklist kebiasaan, hari bersih & streak disiplin, jurnal pagi, arus kas, bayar
hutang per minggu, target financial freedom, rencana 3 tahun, dan catatan keputusan.

Alamat: <https://markasku.my.id/> (alamat lama hanif-dossier.github.io/markas/ dialihkan ke sini)

Ini **salinan produk** dari dasbor pribadi pemilik (`D:\Ai Agent\dashboard-pribadi\`,
Markas Hanif). Yang pribadi tidak disentuh; yang di sini isinya kosong/contoh dan
tiap pengguna punya datanya sendiri.

## Cara kerja

| Berkas | Isi |
|---|---|
| `index.html` | Seluruh aplikasi (tanpa build tool). Tamu langsung melihat dasbor berisi data contoh; Masuk/Daftar muncul saat mau mengisi. |
| `supabase/skema.sql` | Tabel `markas_dokumen` + kebijakan RLS: tiap orang hanya bisa membaca/menulis barisnya sendiri. **Jalankan sekali** di Supabase → SQL Editor. |
| `manifest.webmanifest`, `sw.js`, `ikon-*.png` | Supaya bisa dipasang di layar utama HP (PWA). |

Penyimpanan: satu baris per dokumen per pengguna (`pengaturan/utama`, `harian/<tanggal>`,
`keuangan/aruskas`, `keuangan/aset`, `fokus/daftar`, `jadwal/rutinitas`, `rencana/3tahun`,
`keputusan/daftar`, `kuliah/semester5`) di tabel `markas_dokumen`, kolom `data` (jsonb).
Belum masuk → "Mode coba" menyimpan di `localStorage` browser itu saja.

Akun memakai proyek Supabase yang sama dengan Hanif Dossier (`fqpktykrkpqaztnpqgxz`).
Pendaftar Markasku diberi metadata `aplikasi: 'markas'`; mereka juga masuk tabel
`anggota` lewat trigger lama dengan status `menunggu`, itu tidak berpengaruh ke Markasku.

## Memperbarui

```
git add -A && git commit -m "..." && git push
```
GitHub Pages membangun ±30 detik. Uji lokal: `preview_start markas` (port 8085).

## Suara adzan

`suara/adzan-1.mp3`, `adzan-2.mp3`, `adzan-3.mp3` diunduh dari islamcan.com (koleksi adzan gratis)
untuk alarm adzan di Pengaturan. Alarm berbunyi hanya selama Markasku terbuka (keterbatasan browser).

## Foto langit

`langit/{fajar,pagi,siang,sore,senja,malam}.jpg` (1600×720, ±50–180 KB) dari Pexels
(lisensi Pexels: bebas dipakai tanpa atribusi). ID foto: fajar 35642508, pagi 29198922,
siang 13257827, sore 21939867, senja 4796416, malam 18928472. Dipakai untuk kepala
langit Beranda dan latar halaman (diburamkan) sesuai fase waktu sholat.

Fase **senja** memakai gambar kiriman pemilik, "Ottoman bazaar at evening"
(`langit/senja.jpg`, potongan lebar 1920×860), ditajamkan 4x dengan Real-ESRGAN dari
735×490. Versi HD utuh: `langit/bazar-senja-hd.jpg` (2940×1960). Foto senja lama
dari Pexels disimpan sebagai `langit/senja-awan.jpg` kalau mau dikembalikan.
