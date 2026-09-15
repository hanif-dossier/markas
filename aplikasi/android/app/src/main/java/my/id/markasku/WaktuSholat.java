package my.id.markasku;

import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Hitung jadwal sholat: rumus astronomi umum, sudut Subuh/Isya mengikuti metode negara pengguna
 * (bawaan Kemenag 20°/18°), ihtiyat +2 menit, aturan sepertujuh malam untuk lintang tinggi.
 * Rumusnya sama persis dengan waktuSholat() di index.html dan Edge Function adzan-push.
 */
public final class WaktuSholat {
    public static final String[] ID = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
    public static final String[] NAMA = {"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    /** Parameter metode: sudut Subuh, sudut Isya (0 kalau pakai menit), Isya = Maghrib + menit, faktor bayangan Ashar (1 Syafi'i, 2 Hanafi). */
    public static final class Metode {
        public double subuh = 20, isya = 18, isyaMenit = 0; public int asr = 1;
        public static Metode dari(SharedPreferences p) {
            Metode m = new Metode();
            m.subuh = Double.longBitsToDouble(p.getLong("m_subuh", Double.doubleToLongBits(20)));
            m.isya = Double.longBitsToDouble(p.getLong("m_isya", Double.doubleToLongBits(18)));
            m.isyaMenit = Double.longBitsToDouble(p.getLong("m_isyaMenit", Double.doubleToLongBits(0)));
            m.asr = p.getInt("m_asr", 1);
            return m;
        }
    }

    private WaktuSholat() {}

    /** Menit sejak tengah malam untuk lima waktu, urutan sama dengan ID. */
    public static int[] hitung(int y, int mo, int d, double lat, double lng, double tz, Metode m) {
        if (m == null) m = new Metode();
        double rad = Math.PI / 180;
        int a = Math.floorDiv(14 - mo, 12), yy = y + 4800 - a, mm = mo + 12 * a - 3;
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
        double terbit = noon - sudut(0.833, la, decl), maghrib = noon + sudut(0.833, la, decl), malam = 24 - (maghrib - terbit);
        double subuh = noon - sudut(m.subuh, la, decl);
        double isya = m.isyaMenit > 0 ? maghrib + m.isyaMenit / 60 : noon + sudut(m.isya, la, decl);
        if (Math.abs(cosT(m.subuh, la, decl)) > 1) subuh = terbit - malam / 7;          // lintang tinggi: sepertujuh malam
        if (m.isyaMenit <= 0 && Math.abs(cosT(m.isya, la, decl)) > 1) isya = maghrib + malam / 7;
        double t = m.asr + Math.tan(Math.abs(la - decl)); double ang = Math.atan(1 / t);
        double c = (Math.sin(ang) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl));
        double ashar = noon + Math.acos(Math.max(-1, Math.min(1, c))) / rad / 15;
        return new int[]{ keMenit(subuh) + 2, keMenit(noon) + 2, keMenit(ashar) + 2, keMenit(maghrib) + 2, keMenit(isya) + 2 };
    }

    /** Jadwal untuk suatu hari di zona waktu perangkat. */
    public static int[] hitung(Calendar hari, double lat, double lng, Metode m) {
        double tz = TimeZone.getDefault().getOffset(hari.getTimeInMillis()) / 3600000.0;
        return hitung(hari.get(Calendar.YEAR), hari.get(Calendar.MONTH) + 1, hari.get(Calendar.DAY_OF_MONTH), lat, lng, tz, m);
    }

    private static double cosT(double ang, double la, double decl) {
        double rad = Math.PI / 180;
        return (-Math.sin(ang * rad) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl));
    }
    private static double sudut(double ang, double la, double decl) {
        return Math.acos(Math.max(-1, Math.min(1, cosT(ang, la, decl)))) / (Math.PI / 180) / 15;
    }
    private static int keMenit(double h) { return (int) Math.round(mod(mod(h, 24) + 24, 24) * 60); }
    private static double mod(double a, double b) { return a % b; }   // sama dengan % JavaScript (tanda ikut a)
}
