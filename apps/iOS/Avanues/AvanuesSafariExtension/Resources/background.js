// Avanues Safari Extension - Background Script
// Non-persistent on iOS (loaded on demand, unloaded after idle)

browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === "startVoiceRecognition") {
    // Relay to native app for SFSpeechRecognizer access
    browser.runtime.sendNativeMessage(
      "com.augmentalis.avanues.ios",
      { command: "startVoiceRecognition" },
      (response) => {
        if (response && response.transcription) {
          // Forward transcription to content script
          browser.tabs.sendMessage(sender.tab.id, {
            action: "executeVoiceCommand",
            command: response.transcription
          });
        }
        sendResponse(response);
      }
    );
    return true; // Keep channel open for async response
  }

  if (message.action === "ping") {
    browser.runtime.sendNativeMessage(
      "com.augmentalis.avanues.ios",
      { command: "ping" },
      (response) => {
        sendResponse(response);
      }
    );
    return true;
  }
});
