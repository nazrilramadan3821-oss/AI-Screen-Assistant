(function () {
  var btn = document.getElementById("saveBtn");
  var a = document.createElement("a");
  a.textContent = "💾 Simpan ke galeri";
  a.href = "/api/last.jpg";
  a.style.cssText = btn.style.cssText + ";display:block;text-align:center;text-decoration:none;box-sizing:border-box";
  a.addEventListener("click", function () { a.href = "/api/last.jpg?t=" + Date.now(); });
  btn.replaceWith(a);
})();
