package my.id.markasku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

/** Dipanggil AlarmManager tepat saat waktu sholat masuk: bunyikan adzan, lalu pasang alarm berikutnya. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int idx = intent.getIntExtra("idx", 0);
        Intent s = new Intent(ctx, AdzanService.class);
        s.setAction(AdzanService.AKSI_PUTAR);
        s.putExtra("nama", WaktuSholat.NAMA[Math.max(0, Math.min(4, idx))]);
        s.putExtra("ms", intent.getLongExtra("ms", System.currentTimeMillis()));
        ContextCompat.startForegroundService(ctx, s);
        Penjadwal.jadwalkan(ctx);
    }
}
