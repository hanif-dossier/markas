// Edge Function "pindah-sandi" (proyek markasku). Dipakai SEKALI per akun pindahan.
// Latar: 17 Sep 2026 Markasku pindah dari proyek Supabase bersama Hanif Dossier ke proyek sendiri. Akun dipindah
// dengan UUID sama tetapi TANPA sandi (hash sandi tidak diekspor). Supaya pengguna tidak perlu "Lupa sandi":
// ketika masuk gagal, klien mengirim email + sandi yang BARU SAJA diketik pemiliknya ke sini; fungsi ini memeriksanya
// ke proyek lama (login biasa). Kalau benar dan akunnya memang pindahan yang belum bersandi, sandi itu dipasang di sini.
// Deploy: Verify JWT dimatikan. Tidak butuh secret tambahan (SUPABASE_URL & SUPABASE_SERVICE_ROLE_KEY bawaan).
import { createClient } from "npm:@supabase/supabase-js@2";

const LAMA_URL = "https://fqpktykrkpqaztnpqgxz.supabase.co";
const LAMA_KUNCI = "sb_publishable_UcFfp0XqHZWMZ2jpSRlDvg_vySECBzo";   // kunci publik proyek lama (memang publik)
const CORS = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type", "Access-Control-Allow-Methods": "POST, OPTIONS" };
const jawab = (badan: unknown, status = 200) => new Response(JSON.stringify(badan), { status, headers: { ...CORS, "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return jawab({ ok: false, alasan: "metode" }, 405);
  let email = "", sandi = "";
  try { const b = await req.json(); email = String(b.email || "").trim().toLowerCase(); sandi = String(b.sandi || ""); } catch { /* badan rusak */ }
  if (!email || sandi.length < 6 || sandi.length > 200) return jawab({ ok: false, alasan: "isian" }, 400);

  // 1. Sandi itu benar di proyek lama? (login biasa; pembatas percobaan proyek lama ikut berlaku)
  const r = await fetch(`${LAMA_URL}/auth/v1/token?grant_type=password`, { method: "POST", headers: { apikey: LAMA_KUNCI, "Content-Type": "application/json" }, body: JSON.stringify({ email, password: sandi }) });
  if (!r.ok) return jawab({ ok: false, alasan: "salah" }, 401);
  const lama = await r.json();
  const id = lama?.user?.id;
  if (!id || String(lama.user.email || "").toLowerCase() !== email) return jawab({ ok: false, alasan: "salah" }, 401);

  // 2. Akun yang sama (UUID sama) harus ada di sini dan masih berstatus pindahan tanpa sandi.
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
  const { data: kini, error: e1 } = await admin.auth.admin.getUserById(id);
  if (e1 || !kini?.user || String(kini.user.email || "").toLowerCase() !== email) return jawab({ ok: false, alasan: "tidak-ada" }, 404);
  if (kini.user.user_metadata?.pindahan !== true) return jawab({ ok: false, alasan: "sudah" }, 409);

  // 3. Pasang sandi dan cabut tanda pindahan, supaya fungsi ini tidak bisa dipakai lagi untuk akun itu.
  const { error: e2 } = await admin.auth.admin.updateUserById(id, { password: sandi, user_metadata: { ...kini.user.user_metadata, pindahan: false } });
  if (e2) return jawab({ ok: false, alasan: "gagal" }, 500);
  return jawab({ ok: true });
});
