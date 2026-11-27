(function () {
  var URL_GAMBAR = "https://niko-gambar.nazril-niko.workers.dev";
  function pasang() {
    var tombol = document.getElementById("genBtn");
    if (!tombol || document.getElementById("ngPanel")) return;
    var gaya = document.createElement("style");
    gaya.textContent =
      "#ngPanel{position:fixed;top:0;left:0;right:0;bottom:0;z-index:99999;display:none;align-items:center;justify-content:center;background:rgba(5,8,18,.85);padding:16px}" +
      "#ngPanel.on{display:flex}" +
      "#ngBox{width:100%;max-width:420px;max-height:92%;overflow:auto;background:#0f1424;border:1px solid #2a3350;border-radius:18px;padding:16px;color:#e8ecff;box-sizing:border-box}" +
      "#ngBox h3{margin:0 0 10px;font-size:18px}" +
      "#ngTxt{width:100%;box-sizing:border-box;min-height:80px;background:#0b1020;color:#e8ecff;border:1px solid #2a3350;border-radius:12px;padding:10px;font:inherit;resize:vertical}" +
      "#ngBox button{border:0;border-radius:12px;padding:10px 14px;font:inherit;font-weight:600;color:#fff;background:linear-gradient(135deg,#3b82f6,#7c3aed);margin:8px 8px 0 0}" +
      "#ngBox button.sek{background:#1b2238;border:1px solid #2a3350}" +
      "#ngBox button:disabled{opacity:.5}" +
      "#ngInfo{margin:10px 0 0;font-size:14px;color:#9aa6d0;min-height:18px}" +
      "#ngImg{display:none;width:100%;border-radius:12px;margin-top:10px}";
    document.head.appendChild(gaya);
    var panel = document.createElement("div");
    panel.id = "ngPanel";
    panel.innerHTML =
      '<div id="ngBox"><h3>Buat gambar</h3>' +
      '<textarea id="ngTxt" placeholder="Contoh: kucing lucu memakai topi astronot"></textarea>' +
      '<div><button id="ngBuat">Buat</button><button id="ngSimpan" class="sek" disabled>Simpan</button><button id="ngTutup" class="sek">Tutup</button></div>' +
      '<div id="ngInfo"></div><img id="ngImg" alt="Hasil gambar"></div>';
    document.body.appendChild(panel);
    var $ = function (id) { return document.getElementById(id); };
    var data = "";
    tombol.addEventListener("click", function (e) { e.preventDefault(); panel.classList.add("on"); });
    $("ngTutup").onclick = function () { panel.classList.remove("on"); };
    $("ngBuat").onclick = async function () {
      var teks = $("ngTxt").value.trim();
      if (!teks) { $("ngInfo").textContent = "Tulis dulu deskripsi gambarnya."; return; }
      var b = $("ngBuat"); b.disabled = true; $("ngSimpan").disabled = true;
      $("ngInfo").textContent = "Sedang membuat gambar...";
      var kendali = new AbortController();
      var waktu = setTimeout(function () { kendali.abort(); }, 90000);
      try {
        var r = await fetch(URL_GAMBAR, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ prompt: teks }), signal: kendali.signal });
        var d = await r.json();
        if (!r.ok || d.error) throw new Error(d.error || "Gagal membuat gambar");
        data = "data:image/jpeg;base64," + d.image;
        var img = $("ngImg"); img.src = data; img.style.display = "block";
        $("ngInfo").textContent = "Selesai.";
        $("ngSimpan").disabled = false;
      } catch (err) {
        $("ngInfo").textContent = "Gagal: " + (err.name === "AbortError" ? "waktu habis, coba lagi" : err.message);
      } finally { clearTimeout(waktu); b.disabled = false; }
    };
    $("ngSimpan").onclick = function () {
      if (!data) return;
      var h = window.NikoSimpan ? NikoSimpan.simpanGambar(data) : null;
      if (h !== null) { $("ngInfo").textContent = (h === "OK") ? "Tersimpan di galeri (folder Pictures/AI Asisten)." : h; return; }
      var a = document.createElement("a");
      a.href = data; a.download = "niko-gambar-" + Date.now() + ".jpg";
      document.body.appendChild(a); a.click(); a.remove();
      $("ngInfo").textContent = "Menyimpan...";
    };
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", pasang);
  else pasang();
})();
