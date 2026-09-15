// Edge Function "adzan-push": dipanggil pg_cron tiap menit. Untuk tiap langganan push yang aktif,
// hitung jadwal sholat hari ini (koordinat & zona waktu pengguna, metode Kemenag seperti di index.html)
// dan kirim notifikasi Web Push saat sebuah waktu sholat masuk. Sekali per waktu per hari (kolom terakhir).
// Rahasia (Edge Function Secrets): VAPID_PUBLIC, VAPID_PRIVATE, VAPID_MAIL, CRON_KEY.
import { createClient } from "npm:@supabase/supabase-js@2";
import webpush from "npm:web-push@3";

const SHOLAT: [string, string][] = [["subuh", "Subuh"], ["dzuhur", "Dzuhur"], ["ashar", "Ashar"], ["maghrib", "Maghrib"], ["isya", "Isya"]];
const pad = (n: number) => String(n).padStart(2, "0");
const jam = (m: number) => `${pad(Math.floor(m / 60) % 24)}.${pad(m % 60)}`;

function waktuSholat(y: number, m: number, d: number, lat: number, lng: number, tz: number) {
  const rad = Math.PI / 180;
  const a = Math.floor((14 - m) / 12), yy = y + 4800 - a, mm = m + 12 * a - 3;
  const jd = d + Math.floor((153 * mm + 2) / 5) + 365 * yy + Math.floor(yy / 4) - Math.floor(yy / 100) + Math.floor(yy / 400) - 32045 - 0.5 + (12 - tz) / 24;
  const D = jd - 2451545.0;
  const g = rad * ((357.529 + 0.98560028 * D) % 360), q = (280.459 + 0.98564736 * D) % 360;
  const L = rad * ((q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g)) % 360);
  const e = rad * (23.439 - 0.00000036 * D);
  let RA = Math.atan2(Math.cos(e) * Math.sin(L), Math.cos(L)) / rad / 15; RA = ((RA % 24) + 24) % 24;
  const decl = Math.asin(Math.sin(e) * Math.sin(L));
  const EqT = q / 15 - RA;
  const noon = 12 + tz - lng / 15 - EqT;
  const la = lat * rad;
  const T = (ang: number) => { const c = (-Math.sin(ang * rad) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl)); return Math.acos(Math.max(-1, Math.min(1, c))) / rad / 15; };
  const asr = () => { const t = 1 + Math.tan(Math.abs(la - decl)); const ang = Math.atan(1 / t); const c = (Math.sin(ang) - Math.sin(la) * Math.sin(decl)) / (Math.cos(la) * Math.cos(decl)); return Math.acos(Math.max(-1, Math.min(1, c))) / rad / 15; };
  const toMin = (h: number) => Math.round(((h % 24) + 24) % 24 * 60);
  return { subuh: toMin(noon - T(20)) + 2, dzuhur: toMin(noon) + 2, ashar: toMin(noon + asr()) + 2, maghrib: toMin(noon + T(0.833)) + 2, isya: toMin(noon + T(18)) + 2 } as Record<string, number>;
}

Deno.serve(async (req) => {
  if (req.headers.get("x-cron-key") !== Deno.env.get("CRON_KEY")) return new Response("ditolak", { status: 401 });
  webpush.setVapidDetails("mailto:" + Deno.env.get("VAPID_MAIL"), Deno.env.get("VAPID_PUBLIC")!, Deno.env.get("VAPID_PRIVATE")!);
  const sb = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: rows, error } = await sb.from("markas_push").select("*").eq("aktif", true);
  if (error) return Response.json({ error: error.message }, { status: 500 });
  const now = Date.now(); let kirim = 0, hapus = 0, gagal = 0;
  for (const r of rows ?? []) {
    const lokal = new Date(now + (r.tz ?? 420) * 60000);   // kolom UTC dari tanggal yang sudah digeser = waktu lokal pengguna
    const y = lokal.getUTCFullYear(), m = lokal.getUTCMonth() + 1, d = lokal.getUTCDate(), nm = lokal.getUTCHours() * 60 + lokal.getUTCMinutes();
    const wt = waktuSholat(y, m, d, r.lat, r.lng, (r.tz ?? 420) / 60);
    const tk = `${y}-${pad(m)}-${pad(d)}`;
    for (const [id, nama] of SHOLAT) {
      if (r.waktu && r.waktu[id] === false) continue;
      const t = wt[id]; if (nm < t || nm > t + 2) continue;           // jendela 3 menit, jaga-jaga cron terlambat
      const kunci = `${tk}:${id}`; if (r.terakhir === kunci) continue;
      const isi = JSON.stringify({ title: `Waktu ${nama} telah masuk`, body: `Pukul ${jam(t)}${r.kota ? " · " + r.kota : ""}. Ketuk untuk mencentang di Markasku.`, url: "https://markasku.my.id/#jadwal", tag: "adzan-" + id });
      try {
        await webpush.sendNotification(r.langganan, isi, { TTL: 300, urgency: "high" });
        kirim++; await sb.from("markas_push").update({ terakhir: kunci, diperbarui: new Date().toISOString() }).eq("endpoint", r.endpoint);
      } catch (e) {
        const kode = (e as { statusCode?: number }).statusCode;
        if (kode === 404 || kode === 410) { await sb.from("markas_push").delete().eq("endpoint", r.endpoint); hapus++; }   // langganan kedaluwarsa
        else gagal++;
      }
    }
  }
  return Response.json({ langganan: (rows ?? []).length, kirim, hapus, gagal });
});
