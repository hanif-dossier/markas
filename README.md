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

- fajar: masjid berlentera di hutan berkabut (ilustrasi Pixabay `2024/02/28/12/50/ai-generated-8602008`)
- pagi: Ivan Aivazovsky, *View of Constantinople and the Bosphorus* (potongan laut, kapal, matahari; kerumunan di tepi dibuang)
- siang: kota tua putih berkubah merah (ilustrasi Pixabay `2023/11/13/16/10/city-8385926`)
- sore: unta di padang saat matahari terbenam, siluet kota di kejauhan (ilustrasi Pixabay `2024/05/07/12/26/ai-generated-8745709`)
- senja: pelabuhan kapal layar saat matahari terbenam (ilustrasi Pixabay `2023/05/26/17/59/ai-generated-8019916`, dipotong tanpa dermaga)
- malam: masjid berlampu berpantul di air di bawah bintang (ilustrasi Pixabay `2024/01/27/09/46/ai-generated-8535605`)

Ilustrasi Pixabay berlisensi Pixabay Content License (bebas dipakai komersial tanpa
atribusi); sumbernya hanya 1280 px sehingga ditajamkan 4x dengan Real-ESRGAN lalu
dipotong. Lukisan Aivazovsky domain publik (Wikimedia Commons). Berkas mentah dan
kandidat lain ada di `langit/pilihan/` dan `langit/keemasan/`; gambar bazar kiriman
pemilik (`langit/bazar-senja-hd.jpg`) tetap disimpan dan menjadi bahan ikon aplikasi
(`ikon-*.png?v=4`: kubah dan menara di depan langit senja, tanpa kios dan orang).

## Aplikasi Android (`aplikasi/`)

Pembungkus Capacitor yang memuat markasku.my.id di dalam WebView, ditambah bagian native
(Java, `aplikasi/android/app/src/main/java/my/id/markasku/`) untuk **alarm adzan yang
berbunyi walau aplikasi tertutup dan layar mati**:

- `AdzanPlugin` — jembatan dari index.html (`Capacitor.registerPlugin('Adzan')`): `atur`,
  `status`, `coba`, `stop`, `mintaIzinAlarm`, `abaikanBaterai`.
- `WaktuSholat` — rumus jadwal sholat yang sama dengan index.html (Kemenag, ihtiyat +2).
- `Penjadwal` — simpan pengaturan (SharedPreferences) dan pasang satu alarm tepat waktu
  (`AlarmManager.setAlarmClock`) untuk waktu sholat berikutnya.
- `AlarmReceiver` → `AdzanService` (foreground service, MediaPlayer `USAGE_ALARM`, berkas
  `res/raw/adzan_*.mp3`, tombol Berhenti di notifikasi) → pasang alarm berikutnya lagi.
- `BootReceiver` — pasang ulang setelah HP dinyalakan atau jam/zona waktu berubah.

Di index.html, kalau `window.Capacitor.isNativePlatform()` benar: pengaturan adzan dikirim
ke native lewat `sinkronAdzanNatif()` (saat dibuka dan saat Pengaturan disimpan), suara
in-app dimatikan (`cekAdzan` lewat), tombol Coba memakai layanan native, dan kotak
notifikasi push disembunyikan. Jadi satu index.html melayani web dan aplikasi.

Membangun: tidak perlu Android Studio. Setiap push yang menyentuh `aplikasi/` menjalankan
`.github/workflows/android.yml` (Ubuntu + JDK 21 + SDK 36) → artefak **markasku-debug**
(`app-debug.apk`) di tab Actions. Kalau secret `KEYSTORE_B64`, `KEYSTORE_PASS`, `KEY_ALIAS`
sudah diisi, workflow juga membuat AAB rilis bertanda tangan untuk Play Store.
Menjalankan lokal (kalau ada JDK + SDK): `cd aplikasi && npm ci && npx cap sync android &&
cd android && ./gradlew assembleDebug`.

## Suara dan latar buatan pengguna

Di Pengaturan, pengguna bisa **menambah suara adzan sendiri** (MP3/OGG/WAV ≤ 12 MB) dan
**mengganti latar Beranda** per fase waktu atau satu gambar untuk semua. Keduanya disimpan
di IndexedDB perangkat (`markasku-media`: store `suara` dan `latar`), tidak diunggah ke
server. Gambar dikecilkan lewat canvas jadi 1920 px (kepala) dan 640 px (latar buram).
Kode: `idb`, `muatMedia`, `tambahSuara`, `hapusSuara`, `gantiLatar`, `hapusLatar`,
`pasangLatar` (menimpa `.langit .foto` lewat style inline dan `body::before` lewat
`--latar-khusus`). Di aplikasi Android, berkas suara juga dikirim ke native
(`AdzanPlugin.simpanSuara` → `filesDir/suara/<id>`) supaya alarm saat aplikasi tertutup
memutar suara pilihan itu (`suara: "khusus:<id>"`).

Email kontak resmi: officialmarkasku@gmail.com (privasi.html, listing Play Store).

## Lokasi dunia, metode hitung, bahasa

- **Lokasi**: Pengaturan → kotak cari kota (daftar kota Indonesia lokal + geocoding
  Open-Meteo, gratis tanpa kunci) atau tombol "📍 Lokasi HP" (Geolocation + Nominatim untuk
  nama kota dan kode negara). Jam dihitung dari koordinat itu dan **zona waktu perangkat**,
  jadi pengguna di negara mana pun mendapat jam setempat.
- **Metode** (`METODE` di index.html, `WaktuSholat.Metode` di Android, `Metode` di Edge
  Function): Kemenag 20/18, MUIS/JAKIM 20/18, MWL 18/17, ISNA 15/15, Mesir 19,5/17,5,
  Umm al-Qura 18,5 + Maghrib 90 menit, Karachi 18/18, Diyanet 18/17, UOIF 12/12, Rusia 16/15;
  Ashar Syafi'i (1×) atau Hanafi (2×). Dipilih otomatis dari kode negara (`NEGARA_METODE`).
  Lintang tinggi: kalau sudut tidak tercapai (musim panas Eropa), Subuh = terbit − malam/7,
  Isya = maghrib + malam/7. Metode ikut ke server push lewat `markas_push.waktu.metode`
  (jsonb, tanpa ubah skema) dan ke Android lewat `AdzanPlugin.atur({metode})`.
- **Bahasa**: id (asli), en, ar. Kode tetap Indonesia; `KAMUS` (frasa persis) dan `KATA`
  (hari, bulan, nama sholat, kata umum) diterapkan ke DOM oleh `terjemahkan()` +
  `MutationObserver`. Arab memakai `dir="rtl"`. Pilihan disimpan di localStorage
  `markasku-bahasa`; pemilihnya ada di bilah tamu dan Pengaturan → Tampilan. Menambah
  terjemahan: isi `KAMUS.en` / `KAMUS.ar` (kunci = teks Indonesia persis).
