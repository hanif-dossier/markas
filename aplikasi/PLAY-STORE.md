# Bahan listing Google Play — Markasku

Salin-tempel ke Play Console. Semua sudah dalam batas panjang yang diminta Google.

## Identitas

- **Nama aplikasi** (maks 30): `Markasku`
- **Nama paket**: `my.id.markasku`
- **Kategori**: Gaya Hidup (Lifestyle). Alternatif: Produktivitas.
- **Kebijakan privasi**: https://markasku.my.id/privasi.html
- **Email kontak**: abdullahhanif033@gmail.com
- **Situs**: https://markasku.my.id

## Deskripsi singkat (maks 80)

`Jadwal sholat, adzan, kebiasaan, jurnal, keuangan, dan rencana dalam satu markas.`

## Deskripsi lengkap (maks 4000)

```
Markasku adalah pusat kendali diri: satu tempat untuk mengatur hari, ibadah, uang, dan rencana hidupmu.

WAKTU SHOLAT & ADZAN
• Jadwal sholat lima waktu untuk kotamu (metode Kemenag), tampil di Beranda dengan hitung mundur.
• Alarm adzan yang berbunyi tepat waktu walau aplikasi tertutup dan layar mati. Pilih suara adzan dan volumenya.
• Centang sholat langsung dari jadwal harian; lihat rekap 30 hari.

BERANDA YANG HIDUP
• Kepala Beranda berganti mengikuti waktu: fajar, pagi, siang, sore, senja, malam.
• Ringkasan hari ini: jadwal, kebiasaan, jurnal, arus kas, fokus utama, target terdekat.

JADWAL & KEBIASAAN
• Rutinitas harian yang otomatis masuk ke jadwal Senin–Minggu.
• Kalender untuk tenggat yang masih jauh.
• Catat pelanggaran aturan dirimu sendiri dan lihat streak disiplin.

FOKUS, KEUANGAN, RENCANA
• Tugas, masalah, dan target dengan nominal uang (rupiah atau mata uang asing).
• Pemasukan, pengeluaran, aset, dan grafik ke mana uangmu pergi tiap bulan.
• Rencana per tahun dalam empat kuartal.

JURNAL & KEPUTUSAN
• Jurnal harian dan catatan keputusan penting beserta alasannya.

DATA MILIKMU
• Coba dulu sebagai tamu tanpa akun. Daftar gratis kalau mau menyimpan.
• Tanpa iklan, tanpa pelacakan. Ekspor atau hapus semua datamu kapan saja.

Markasku dibuat untuk dipakai setiap hari: sederhana di HP, lega di laptop.
```

## Aset grafis

| Aset | Ukuran | Berkas |
|---|---|---|
| Ikon aplikasi | 512×512 PNG, tanpa transparansi | `aplikasi/play/ikon-512.png` |
| Gambar promosi (feature graphic) | 1024×500 PNG | `aplikasi/play/promosi-1024x500.png` |
| Tangkapan layar HP | 1080×1920 PNG, minimal 2 | `aplikasi/play/layar-*.png` |

## Kuesioner Play Console (jawaban)

- **Keamanan data**: mengumpulkan email (akun), info pribadi lain tidak; data pengguna (isi aplikasi) disimpan
  di server, terenkripsi saat transit, pengguna bisa minta dihapus (menu Akun). Tidak dibagikan ke pihak ketiga.
- **Iklan**: tidak ada. **Pembelian dalam aplikasi**: tidak ada.
- **Rating konten**: kuesioner IARC, semua "tidak" → Semua Umur.
- **Target audiens**: 18 tahun ke atas (paling sederhana; hindari kebijakan Keluarga).
- **Izin sensitif**: `SCHEDULE_EXACT_ALARM` → fungsi inti aplikasi adalah alarm adzan (pengingat waktu sholat
  yang harus tepat menit). `FOREGROUND_SERVICE_MEDIA_PLAYBACK` → memutar suara adzan saat alarm berbunyi.
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → supaya alarm tidak ditunda; hanya diminta kalau pengguna menekan tombolnya.
- **Aplikasi kesehatan / keuangan?** Bukan; catatan keuangan pribadi manual, tidak terhubung ke bank.

## Alur unggah pertama

1. Daftar Play Console (play.google.com/console, US$25 sekali). Verifikasi identitas oleh pemilik sendiri.
2. Buat aplikasi → nama Markasku, bahasa Indonesia, aplikasi, gratis.
3. Isi "Setelan aplikasi" (dashboard) satu per satu memakai jawaban di atas.
4. Listing utama: teks di atas + aset grafis.
5. Rilis → Pengujian internal → unggah `app-release.aab` (artefak `markasku-rilis` dari GitHub Actions).
   Play App Signing: pilih "gunakan kunci upload yang saya unggah"? Tidak perlu; cukup terima Play App Signing,
   AAB yang ditandatangani kunci upload kita sudah cukup.
6. Setelah pengujian internal jalan di HP pemilik, buka ke Produksi dan kirim untuk ditinjau (biasanya 1–7 hari).

Menaikkan versi untuk rilis berikutnya: ubah `versionCode` (+1) dan `versionName` di
`aplikasi/android/app/build.gradle`, push, ambil AAB baru dari Actions.
