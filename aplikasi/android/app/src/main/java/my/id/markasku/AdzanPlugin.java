package my.id.markasku;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/**
 * Jembatan dari halaman web Markasku (index.html) ke alarm adzan native.
 * Dari JavaScript: Capacitor.registerPlugin('Adzan').atur({...}), .status(), .coba(), .stop(),
 * .mintaIzinAlarm(), .abaikanBaterai().
 */
@CapacitorPlugin(name = "Adzan", permissions = { @Permission(strings = { Manifest.permission.POST_NOTIFICATIONS }, alias = "notif") })
public class AdzanPlugin extends Plugin {

    /** Simpan pengaturan dari halaman Pengaturan lalu pasang alarm berikutnya. */
    @PluginMethod
    public void atur(PluginCall call) {
        SharedPreferences.Editor e = Penjadwal.pref(getContext()).edit();
        e.putBoolean("aktif", call.getBoolean("aktif", false));
        e.putString("suara", call.getString("suara", "adzan-1"));
        e.putInt("volume", call.getInt("volume", 80));
        e.putString("kota", call.getString("kota", ""));
        Double lat = call.getDouble("lat"), lng = call.getDouble("lng");
        if (lat != null) e.putLong("lat", Double.doubleToLongBits(lat));
        if (lng != null) e.putLong("lng", Double.doubleToLongBits(lng));
        JSObject waktu = call.getObject("waktu");
        for (String id : WaktuSholat.ID) e.putBoolean("waktu_" + id, waktu == null || waktu.optBoolean(id, true));
        e.apply();
        Penjadwal.jadwalkan(getContext());
        if (call.getBoolean("aktif", false) && Build.VERSION.SDK_INT >= 33 && getPermissionState("notif") != PermissionState.GRANTED) {
            requestPermissionForAlias("notif", call, "izinSelesai");
        } else call.resolve(status());
    }

    @PermissionCallback
    private void izinSelesai(PluginCall call) { call.resolve(status()); }

    @PluginMethod
    public void status(PluginCall call) { call.resolve(status()); }

    private JSObject status() {
        Context ctx = getContext(); SharedPreferences p = Penjadwal.pref(ctx);
        JSObject o = new JSObject();
        o.put("aktif", p.getBoolean("aktif", false));
        o.put("berikutNama", p.getString("berikutNama", null));
        o.put("berikutMs", p.getLong("berikutMs", 0));
        o.put("izinNotif", Build.VERSION.SDK_INT < 33 || getPermissionState("notif") == PermissionState.GRANTED);
        o.put("izinAlarm", Penjadwal.bisaTepat(ctx));
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        o.put("abaikanBaterai", pm.isIgnoringBatteryOptimizations(ctx.getPackageName()));
        return o;
    }

    /** Buka layar sistem "Alarm & pengingat" kalau izin alarm tepat waktu belum diberikan (Android 12+). */
    @PluginMethod
    public void mintaIzinAlarm(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Penjadwal.bisaTepat(getContext())) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i);
        }
        call.resolve(status());
    }

    /** Minta dikecualikan dari hemat baterai supaya alarm tidak ditunda HP tertentu (Xiaomi, Oppo, Vivo). */
    @PluginMethod
    public void abaikanBaterai(PluginCall call) {
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i);
        }
        call.resolve(status());
    }

    /** Tombol "Coba" di Pengaturan: putar lewat layanan native supaya volumenya sama dengan alarm sungguhan. */
    @PluginMethod
    public void coba(PluginCall call) {
        Intent s = new Intent(getContext(), AdzanService.class);
        s.setAction(AdzanService.AKSI_PUTAR);
        s.putExtra("nama", "uji coba");
        s.putExtra("suara", call.getString("suara", "adzan-1"));
        s.putExtra("volume", call.getInt("volume", 80));
        ContextCompat.startForegroundService(getContext(), s);
        call.resolve();
    }

    /** Berkas suara unggahan pengguna: filesDir/suara/<id> (tanpa ekstensi; MediaPlayer mengenali isinya). */
    public static java.io.File berkasSuara(Context ctx, String id) {
        java.io.File dir = new java.io.File(ctx.getFilesDir(), "suara"); if (!dir.exists()) dir.mkdirs();
        return new java.io.File(dir, id.replaceAll("[^A-Za-z0-9_-]", ""));
    }

    /** Terima suara dari halaman web (base64) dan simpan supaya layanan alarm bisa memutarnya saat aplikasi tertutup. */
    @PluginMethod
    public void simpanSuara(PluginCall call) {
        String id = call.getString("id"), data = call.getString("data");
        if (id == null || data == null) { call.reject("id dan data wajib"); return; }
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(berkasSuara(getContext(), id))) {
            out.write(android.util.Base64.decode(data, android.util.Base64.DEFAULT));
            call.resolve();
        } catch (Exception e) { call.reject("gagal menyimpan: " + e.getMessage()); }
    }

    @PluginMethod
    public void hapusSuara(PluginCall call) {
        String id = call.getString("id"); if (id != null) berkasSuara(getContext(), id).delete();
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent s = new Intent(getContext(), AdzanService.class); s.setAction(AdzanService.AKSI_STOP);
        getContext().startService(s);
        call.resolve();
    }
}
