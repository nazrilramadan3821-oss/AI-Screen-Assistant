(function () {
  const S = window.NIKO_SOLVER;
  if (!S || document.getElementById("niko-ui")) return;
  const box = document.createElement("div");
  box.id = "niko-ui";
  box.style.cssText = "position:fixed;right:12px;bottom:90px;z-index:999999;text-align:right";
  box.innerHTML =
    '<div hidden style="width:260px;max-height:50vh;overflow:auto;background:#111;color:#fff;padding:10px;border-radius:10px;text-align:left">' +
    '<input type="password" placeholder="APP_TOKEN" style="width:100%;box-sizing:border-box;padding:8px">' +
    '<button id="niko-solve">Pindai & Jawab</button> <button id="niko-fill" disabled>Isi Jawaban</button>' +
    '<pre style="white-space:pre-wrap"></pre></div><button id="niko-open">NIKO</button>';
  document.body.appendChild(box);
  const q = s => box.querySelector(s);
  const input = q("input"), out = q("pre"), fill = q("#niko-fill"), solve = q("#niko-solve");
  try { input.value = localStorage.getItem("nikoToken") || ""; } catch (e) {}
  q("#niko-open").onclick = () => { q("div").hidden = !q("div").hidden; };
  solve.onclick = async () => {
    S.token = input.value.trim();
    try { localStorage.setItem("nikoToken", S.token); } catch (e) {}
    S.reset(); fill.disabled = true; solve.disabled = true;
    out.textContent = "Memproses...";
    try {
      await S.solve();
      out.textContent = S.preview().map((p, i) =>
        (i + 1) + ". " + p.answer + (p.matched ? " ✓" : " (tidak cocok)")).join("\n");
      fill.disabled = false;
    } catch (e) { out.textContent = e.message; }
    solve.disabled = false;
  };
  fill.onclick = () => { out.textContent += "\nTerisi: " + S.fillAnswers(); };
})();
