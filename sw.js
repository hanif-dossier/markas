// Service worker Markasku: ambil dari jaringan dulu, salinan terakhir dipakai kalau offline (data tetap dari Supabase).
// Juga menerima Web Push (notifikasi adzan dari Edge Function adzan-push) dan membuka Markasku saat notifikasi diketuk.
const CACHE = 'markas-v4';
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', e => e.waitUntil(clients.claim()));
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;
  e.respondWith(fetch(e.request).then(r => { const c = r.clone(); caches.open(CACHE).then(k => k.put(e.request, c)); return r; }).catch(() => caches.match(e.request)));
});
self.addEventListener('push', e => {
  let p = {}; try { p = e.data ? e.data.json() : {}; } catch { p = { title: 'Markasku', body: e.data ? e.data.text() : '' }; }
  e.waitUntil(self.registration.showNotification(p.title || 'Waktu sholat', {
    body: p.body || '', icon: 'ikon-192.png', badge: 'ikon-192.png', tag: p.tag || 'markas', renotify: true,
    vibrate: [200, 100, 200, 100, 400], data: { url: p.url || 'https://markasku.my.id/' }
  }));
});
self.addEventListener('notificationclick', e => {
  e.notification.close();
  const url = (e.notification.data && e.notification.data.url) || 'https://markasku.my.id/';
  e.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
    for (const c of list) { if ('focus' in c) { c.navigate(url); return c.focus(); } }
    return clients.openWindow(url);
  }));
});
