console.log("uBlock Origin content script loaded");

// Connect to the background script
const port = browser.runtime.connect({name: "ublock-content"});

// Send page information to the background script
port.postMessage({
  action: "contentScriptReady",
  url: window.location.href,
  domain: window.location.hostname
});

// Listen for messages from the background script
port.onMessage.addListener(message => {
  console.log("Content script received message:", message);
  
  if (message.action === "injectCSS") {
    // Inject CSS rules for element hiding
    const style = document.createElement('style');
    style.textContent = message.css;
    (document.head || document.documentElement).appendChild(style);
  } else if (message.action === "executeScript") {
    // Execute cosmetic filtering scripts
    try {
      eval(message.script);
    } catch (e) {
      console.error("Error executing uBlock script:", e);
    }
  }
});

// Observe DOM changes to apply cosmetic filtering to new elements
const observer = new MutationObserver(mutations => {
  port.postMessage({
    action: "domChanged",
    url: window.location.href
  });
});

// Start observing the document
observer.observe(document, {
  childList: true,
  subtree: true
});

// Notify when the page is fully loaded
window.addEventListener('load', () => {
  port.postMessage({
    action: "pageLoaded",
    url: window.location.href
  });
});
