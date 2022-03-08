document.getElementById("navbar").style.display = "none";
fetch(dataUrl).then(r => r.html()).then(html => document.getElementById("content").innerHTML = html);
