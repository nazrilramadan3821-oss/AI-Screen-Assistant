(function () {
  try { if (localStorage.getItem("nikoIntro")) return; } catch (e) {}
  const d = document.createElement("div");
  d.style.cssText = "position:fixed;inset:0;z-index:2147483647;background:#111;color:#fff;padding:24px;overflow:auto";
  d.innerHTML = "<h2>Selamat datang di NIKO</h2>" +
    "<p>NIKO membantu menjelaskan materi dan soal latihan.</p>" +
    "<p>Izin: tangkap layar, tampil di atas aplikasi, dan Aksesibilitas (geser dan ketuk).</p>" +
    "<p>Tangkapan layar dikirim ke server dan Google Gemini. Jangan scan password, OTP, atau mobile banking.</p>" +
    "<p>Jangan bagikan token. Jangan dipakai saat ujian. Jawaban AI bisa salah.</p>" +
    "<button style='width:100%;padding:14px'>Saya mengerti</button>";
  d.querySelector("button").onclick = () => {
    try { localStorage.setItem("nikoIntro", "1"); } catch (e) {}
    d.remove();
  };
  document.body.appendChild(d);
})();
