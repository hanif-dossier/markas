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

## Gambar langit

`langit/{fajar,pagi,siang,sore,senja,malam}.jpg` (1920×860) untuk kepala Beranda dan
`*-kecil.jpg` (640px) untuk latar halaman yang diburamkan; berganti mengikuti fase waktu
sholat. Aturan pemilik: suasana **hidup bergaya lukisan sinematik, tanpa sosok manusia**.

- fajar: masjid berlentera di hutan berkabut (ilustrasi AI, Pixabay 8464147... lihat `catatan/`)
- pagi: Ivan Aivazovsky, *View of Constantinople and the Bosphorus* (potongan laut, kapal, matahari; kerumunan di tepi dibuang)
- siang: kota tua putih berkubah merah (ilustrasi Pixabay)
- sore: unta di padang saat matahari terbenam, siluet kota di kejauhan (ilustrasi Pixabay)
- senja: pelabuhan kapal layar saat matahari terbenam (ilustrasi Pixabay, dipotong tanpa dermaga)
- malam: masjid berlampu berpantul di air di bawah bintang (ilustrasi Pixabay)

Ilustrasi Pixabay berlisensi Pixabay Content License (bebas dipakai komersial tanpa
atribusi); sumbernya hanya 1280 px sehingga ditajamkan 4x dengan Real-ESRGAN lalu
dipotong. Lukisan Aivazovsky domain publik (Wikimedia Commons). Berkas mentah dan
kandidat lain ada di `langit/pilihan/` dan `langit/keemasan/`; gambar bazar kiriman
pemilik (`langit/bazar-senja-hd.jpg`) tetap disimpan dan menjadi bahan ikon aplikasi
(`ikon-*.png?v=4`: kubah dan menara di depan langit senja, tanpa kios dan orang).
