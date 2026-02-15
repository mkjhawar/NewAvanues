// Avanues Safari Extension - Popup Script
document.addEventListener("DOMContentLoaded", () => {
  const statusEl = document.getElementById("status");

  // Ping the native app to check connectivity
  browser.runtime.sendMessage({ action: "ping" }, (response) => {
    if (browser.runtime.lastError) {
      statusEl.textContent = "Extension active (native bridge unavailable)";
      statusEl.className = "status-err";
      return;
    }
    if (response && response.status === "ok") {
      statusEl.textContent = "Connected to Avanues v" + response.version;
      statusEl.className = "status-ok";
    } else {
      statusEl.textContent = "Extension active";
      statusEl.className = "status-ok";
    }
  });
});
