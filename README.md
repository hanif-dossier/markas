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

## Gambar langit (lukisan)

`langit/{fajar,pagi,siang,sore,senja,malam}.jpg` (1920×860) untuk kepala Beranda dan
`*-kecil.jpg` (640px) untuk latar halaman yang diburamkan; berganti mengikuti fase waktu
sholat. Temanya **adegan kehidupan dunia Islam klasik yang hidup**, bukan foto bangunan:

- fajar: Ivan Aivazovsky, *View of Constantinople and the Bosphorus* (matahari terbit, kapal, orang di tepi pantai)
- pagi: Alberto Pasini, *Market Day in Constantinople* (pasar ramai di tepi Bosphorus)
- siang: Gustav Bauernfeind, *A Street Scene, Damascus* (jalanan pasar, unta, pedagang)
- sore: Ivan Aivazovsky, *Konstantinopel* (langit jingga, masjid dan perahu)
- senja: "Ottoman bazaar at evening", gambar kiriman pemilik, ditajamkan 4x dengan Real-ESRGAN
  (HD utuh: `langit/bazar-senja-hd.jpg`)
- malam: Ivan Aivazovsky, *Bosporus with the Hagia Sophia and the Maiden's Tower in the Moonlight*

Semua lukisan domain publik dari Wikimedia Commons (pelukis wafat lebih dari 100 tahun
lalu), aman untuk Play Store. Cadangan yang sudah dipotong ada di `langit/pilihan/`
(kapal berkabut untuk fajar, pasar Kairo karya Biseo untuk pagi, Bayram karya Zonaro untuk
siang, cahaya petang Aivazovsky untuk sore, Menara Galata purnama untuk malam): tinggal
salin menimpa `langit/<fase>.jpg` lalu buat ulang versi kecil 640×287. Foto Pexels lama
ada di riwayat git (commit 2c3f960). Ikon aplikasi (`ikon-*.png?v=3`) dipotong dari
gambar bazar senja.
