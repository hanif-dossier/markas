package my.id.markasku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * Menyimpan pengaturan adzan (dari halaman Pengaturan Markasku lewat plugin) dan memasang
 * SATU alarm tepat waktu untuk waktu sholat berikutnya. Setelah alarm berbunyi, AlarmReceiver
 * memanggil jadwalkan() lagi, jadi rantainya terus berjalan tanpa aplikasi dibuka.
 */
public final class Penjadwal {
    public static final String PREF = "adzan";
    private static final int KODE = 1001;

    private Penjadwal() {}

    public static SharedPreferences pref(Context ctx) { return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    /** Cari waktu sholat aktif berikutnya (hari ini atau besok) lalu pasang alarmnya. */
    public static void jadwalkan(Context ctx) {
        SharedPreferences p = pref(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent lama = PendingIntent.getBroadcast(ctx, KODE, new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (lama != null) am.cancel(lama);
        if (!p.getBoolean("aktif", false)) { p.edit().remove("berikutNama").remove("berikutMs").apply(); return; }

        double lat = Double.longBitsToDouble(p.getLong("lat", Double.doubleToLongBits(-6.2)));
        double lng = Double.longBitsToDouble(p.getLong("lng", Double.doubleToLongBits(106.8167)));
        long sekarang = System.currentTimeMillis() + 60_000;   // lewati waktu yang baru saja berbunyi
        WaktuSholat.Metode metode = WaktuSholat.Metode.dari(p);
        Calendar hari = Calendar.getInstance();
        for (int tambah = 0; tambah < 3; tambah++) {
            int[] menit = WaktuSholat.hitung(hari, lat, lng, metode);
            for (int i = 0; i < 5; i++) {
                if (!p.getBoolean("waktu_" + WaktuSholat.ID[i], true)) continue;
                Calendar t = (Calendar) hari.clone();
                t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                t.add(Calendar.MINUTE, menit[i]);
                long ms = t.getTimeInMillis();
                if (ms <= sekarang) continue;
                pasang(ctx, am, i, ms);
                p.edit().putString("berikutNama", WaktuSholat.NAMA[i]).putLong("berikutMs", ms).apply();
                return;
            }
            hari.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static void pasang(Context ctx, AlarmManager am, int idx, long ms) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("idx", idx).putExtra("ms", ms);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, KODE, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent buka = new Intent(ctx, MainActivity.class);
        PendingIntent tampil = PendingIntent.getActivity(ctx, 0, buka, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi);   // izin alarm tepat belum diberikan: bisa meleset beberapa menit
        } else {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(ms, tampil), pi);   // paling andal: kebal Doze & hemat baterai
        }
    }

    public static boolean bisaTepat(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return am.canScheduleExactAlarms();
    }
}
