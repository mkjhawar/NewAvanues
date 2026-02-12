// Avanues Safari Extension - Content Script
// Injected into all web pages for voice control overlay
(function() {
  "use strict";

  // Prevent double-injection
  if (window.__avanuesExtensionLoaded) return;
  window.__avanuesExtensionLoaded = true;

  // Create floating mic button
  const micButton = document.createElement("button");
  micButton.id = "avanues-voice-btn";
  micButton.innerHTML = "\uD83C\uDF99";
  micButton.title = "Avanues Voice Control";
  micButton.setAttribute("aria-label", "Activate voice control");
  Object.assign(micButton.style, {
    position: "fixed",
    bottom: "24px",
    right: "24px",
    width: "56px",
    height: "56px",
    borderRadius: "50%",
    background: "linear-gradient(135deg, #1a3a6c 0%, #2563eb 100%)",
    border: "2px solid rgba(255,255,255,0.2)",
    color: "white",
    fontSize: "24px",
    cursor: "pointer",
    zIndex: "2147483647",
    boxShadow: "0 4px 16px rgba(0,0,0,0.3)",
    transition: "transform 0.2s, box-shadow 0.2s",
    display: "flex",
    alignItems: "center",
    justifyContent: "center"
  });

  micButton.addEventListener("mouseenter", () => {
    micButton.style.transform = "scale(1.1)";
    micButton.style.boxShadow = "0 6px 20px rgba(0,0,0,0.4)";
  });
  micButton.addEventListener("mouseleave", () => {
    micButton.style.transform = "scale(1)";
    micButton.style.boxShadow = "0 4px 16px rgba(0,0,0,0.3)";
  });

  micButton.addEventListener("click", () => {
    micButton.innerHTML = "\u23F3";
    browser.runtime.sendMessage({ action: "startVoiceRecognition" }, (response) => {
      micButton.innerHTML = "\uD83C\uDF99";
      if (response && response.error) {
        console.log("[Avanues]", response.error);
      }
    });
  });

  document.body.appendChild(micButton);

  // Listen for voice commands from background script
  browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "executeVoiceCommand") {
      executeCommand(message.command);
      sendResponse({ success: true });
    }
  });

  function executeCommand(command) {
    const lower = command.toLowerCase().trim();
    console.log("[Avanues] Executing voice command:", lower);

    if (lower.startsWith("scroll down")) {
      window.scrollBy({ top: 400, behavior: "smooth" });
    } else if (lower.startsWith("scroll up")) {
      window.scrollBy({ top: -400, behavior: "smooth" });
    } else if (lower.startsWith("go back")) {
      window.history.back();
    } else if (lower.startsWith("go forward")) {
      window.history.forward();
    } else if (lower.startsWith("refresh") || lower.startsWith("reload")) {
      window.location.reload();
    } else if (lower.startsWith("click ")) {
      const num = parseInt(lower.replace("click ", ""), 10);
      if (!isNaN(num)) {
        clickElementByNumber(num);
      }
    }
    // Additional commands will be added in Phase 3
  }

  function clickElementByNumber(num) {
    const labels = document.querySelectorAll("[data-avanues-label]");
    for (const label of labels) {
      if (parseInt(label.getAttribute("data-avanues-label"), 10) === num) {
        const targetId = label.getAttribute("data-avanues-target");
        const target = document.querySelector("[data-avanues-id='" + targetId + "']");
        if (target) {
          target.click();
          return;
        }
      }
    }
    console.log("[Avanues] Element #" + num + " not found");
  }
})();
