package my.id.markasku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm hilang saat HP dimatikan atau jam/zona waktu berubah; pasang ulang dari pengaturan tersimpan. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) { Penjadwal.jadwalkan(ctx); }
}
