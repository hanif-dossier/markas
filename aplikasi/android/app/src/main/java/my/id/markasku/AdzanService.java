package my.id.markasku;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Layanan latar depan yang memutar berkas adzan (res/raw) dengan volume pilihan pengguna.
 * Berjalan walau aplikasi tertutup dan layar mati; notifikasinya punya tombol Berhenti.
 */
public class AdzanService extends Service {
    public static final String AKSI_PUTAR = "my.id.markasku.PUTAR";
    public static final String AKSI_STOP = "my.id.markasku.STOP";
    private static final String KANAL = "adzan";
    private static final int ID_NOTIF = 7;
    private static final long BATAS_MS = 6 * 60_000L;   // jaga-jaga: berhenti sendiri setelah 6 menit

    private MediaPlayer mp;
    private PowerManager.WakeLock kunci;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable batas = this::selesai;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || AKSI_STOP.equals(intent.getAction())) { selesai(); return START_NOT_STICKY; }
        String nama = intent.getStringExtra("nama"); if (nama == null) nama = "sholat";
        long ms = intent.getLongExtra("ms", System.currentTimeMillis());
        SharedPreferences p = Penjadwal.pref(this);
        String suara = intent.hasExtra("suara") ? intent.getStringExtra("suara") : p.getString("suara", "adzan-1");
        int volume = intent.hasExtra("volume") ? intent.getIntExtra("volume", 80) : p.getInt("volume", 80);
        String kota = p.getString("kota", "");

        Notification n = notifikasi(nama, ms, kota);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(ID_NOTIF, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(ID_NOTIF, n);

        putar(suara, volume);
        return START_NOT_STICKY;
    }

    private void putar(String suara, int volume) {
        hentikan();
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            kunci = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "markasku:adzan"); kunci.acquire(BATAS_MS);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            int res = sumber(suara);
            if (res != 0) {
                android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(res);
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()); afd.close();
            } else {
                mp.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));   // "Nada singkat saja"
            }
            float v = Math.max(0f, Math.min(1f, volume / 100f));
            mp.setVolume(v, v);
            mp.setOnCompletionListener(m -> selesai());
            mp.setOnErrorListener((m, a, b) -> { selesai(); return true; });
            mp.prepare(); mp.start();
            handler.postDelayed(batas, BATAS_MS);
        } catch (Exception e) { selesai(); }
    }

    private int sumber(String suara) {
        if ("adzan-1".equals(suara)) return R.raw.adzan_1;
        if ("adzan-2".equals(suara)) return R.raw.adzan_2;
        if ("adzan-3".equals(suara)) return R.raw.adzan_3;
        return 0;
    }

    private Notification notifikasi(String nama, long ms, String kota) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel k = new NotificationChannel(KANAL, "Adzan", NotificationManager.IMPORTANCE_HIGH);
            k.setDescription("Pengingat waktu sholat dengan suara adzan");
            k.setSound(null, null);   // suara diputar sendiri oleh layanan, bukan oleh notifikasi
            k.enableVibration(true);
            nm.createNotificationChannel(k);
        }
        String jam = new SimpleDateFormat("HH.mm", Locale.getDefault()).format(new Date(ms));
        Intent buka = new Intent(this, MainActivity.class); buka.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piBuka = PendingIntent.getActivity(this, 1, buka, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, AdzanService.class); stop.setAction(AKSI_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 2, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Waktu " + nama + " telah masuk")
            .setContentText("Pukul " + jam + (kota.isEmpty() ? "" : " · " + kota) + ". Ketuk untuk mencentang di Markasku.")
            .setContentIntent(piBuka)
            .addAction(0, "Berhenti", piStop)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setVibrate(new long[]{0, 300, 150, 300})
            .build();
    }

    private void hentikan() {
        handler.removeCallbacks(batas);
        if (mp != null) { try { mp.stop(); } catch (Exception ignored) {} try { mp.release(); } catch (Exception ignored) {} mp = null; }
        if (kunci != null && kunci.isHeld()) { kunci.release(); kunci = null; }
    }

    private void selesai() {
        hentikan();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { hentikan(); super.onDestroy(); }
}
