package my.id.markasku;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Hitung jadwal sholat (metode Kemenag: Subuh 20°, Isya 18°, ihtiyat +2 menit).
 * Rumusnya sama persis dengan waktuSholat() di index.html dan Edge Function adzan-push,
 * supaya alarm native berbunyi di menit yang sama dengan yang tampil di Beranda.
 */
public final class WaktuSholat {
    public static final String[] ID = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
    public static final String[] NAMA = {"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    private WaktuSholat() {}

    /** Menit sejak tengah malam untuk lima waktu, urutan sama dengan ID. */
    public static int[] hitung(int y, int m, int d, double lat, double lng, double tz) {
        double rad = Math.PI / 180;
        int a = Math.floorDiv(14 - m, 12), yy = y + 4800 - a, mm = m + 12 * a - 3;
        double jd = d + Math.floorDiv(153 * mm + 2, 5) + 365 * yy + Math.floorDiv(yy, 4) - Math.floorDiv(yy, 100) + Math.floorDiv(yy, 400) - 32045 - 0.5 + (12 - tz) / 24;
        double D = jd - 2451545.0;
        double g = rad * mod(357.529 + 0.98560028 * D, 360), q = mod(280.459 + 0.98564736 * D, 360);
        double L = rad * mod(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g), 360);
        double e = rad * (23.439 - 0.00000036 * D);
        double RA = Math.atan2(Math.cos(e) * Math.sin(L), Math.cos(L)) / rad / 15; RA = mod(mod(RA, 24) + 24, 24);
        double decl = Math.asin(Math.sin(e) * Math.sin(L));
        double EqT = q / 15 - RA;
        double noon = 12 + tz - lng / 15 - EqT;
        double la = lat * rad;
        double subuh = noon - sudut(20, la, decl), maghrib = noon + sudut(0.833, la, decl), isya = noon + sudut(18, la, decl);
        double t = 1 + Math.tan(Math.abs(la - decl)); double ang = Math.atan(1 / t);
        double c = (Math.sin(ang) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl));
        double ashar = noon + Math.acos(Math.max(-1, Math.min(1, c))) / rad / 15;
        return new int[]{ keMenit(subuh) + 2, keMenit(noon) + 2, keMenit(ashar) + 2, keMenit(maghrib) + 2, keMenit(isya) + 2 };
    }

    /** Jadwal untuk suatu hari di zona waktu perangkat. */
    public static int[] hitung(Calendar hari, double lat, double lng) {
        double tz = TimeZone.getDefault().getOffset(hari.getTimeInMillis()) / 3600000.0;
        return hitung(hari.get(Calendar.YEAR), hari.get(Calendar.MONTH) + 1, hari.get(Calendar.DAY_OF_MONTH), lat, lng, tz);
    }

    private static double sudut(double ang, double la, double decl) {
        double rad = Math.PI / 180;
        double c = (-Math.sin(ang * rad) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl));
        return Math.acos(Math.max(-1, Math.min(1, c))) / rad / 15;
    }

    private static int keMenit(double h) { return (int) Math.round(mod(mod(h, 24) + 24, 24) * 60); }
    private static double mod(double a, double b) { return a % b; }   // sama dengan % JavaScript (tanda ikut a)
}
