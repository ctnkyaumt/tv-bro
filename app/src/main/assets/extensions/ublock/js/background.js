console.log("uBlock Origin for TV Bro loaded");

// Create a connection to the native app
let tvBroPort = browser.runtime.connectNative("tvbro_bg");

// Import our simplified adblock engine
importScripts('/assets/extensions/ublock/js/adblock-engine.js');

// Initialize the adblock engine
adblockEngine.init();

// Handle messages from the native app
tvBroPort.onMessage.addListener(response => {
  console.log("Received message from TV Bro: " + JSON.stringify(response));
  
  if (response.action === "onBeforeRequest") {
    const details = response.details;
    const shouldBlock = adblockEngine.matchesFilter(
      details.url,
      details.type,
      details.originUrl || ""
    );
    
    tvBroPort.postMessage({
      action: "onResolveRequest", 
      data: {
        requestId: details.requestId,
        block: shouldBlock
      }
    });
  } else if (response.action === "updateFilters") {
    // Check if filter lists are provided
    if (response.lists && Array.isArray(response.lists)) {
      // Convert the filter lists to the format expected by the adblock engine
      const filterLists = response.lists.map(list => ({
        id: list.id,
        url: list.url
      }));
      
      // Update the filters with the provided lists
      adblockEngine.updateFilters(filterLists);
    } else {
      // No filter lists provided, just update with current lists
      adblockEngine.updateFilters();
    }
    
    // Send back the filter info
    tvBroPort.postMessage({
      action: "filterInfo",
      data: adblockEngine.getFilterInfo()
    });
  } else if (response.action === "toggleEnabled") {
    const enabled = adblockEngine.toggleEnabled();
    
    // Send back the new enabled state
    tvBroPort.postMessage({
      action: "adblockEnabled",
      data: {
        enabled: enabled
      }
    });
  } else if (response.action === "getFilterInfo") {
    // Send back the filter info
    tvBroPort.postMessage({
      action: "filterInfo",
      data: adblockEngine.getFilterInfo()
    });
  }
});

// Listen for web requests
browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    const shouldBlock = adblockEngine.matchesFilter(
      details.url,
      details.type,
      details.originUrl || ""
    );
    return { cancel: shouldBlock };
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);

// Content script messaging
browser.runtime.onConnect.addListener(port => {
  if (port.name === "ublock-content") {
    port.onMessage.addListener(message => {
      console.log("Message from content script:", message);
      
      if (message.action === "contentScriptReady") {
        // Generate CSS rules for this domain
        const domain = message.domain;
        // In a real implementation, this would generate domain-specific CSS rules
        const css = ""; // Simplified for now
        
        if (css) {
          port.postMessage({
            action: "injectCSS",
            css: css
          });
        }
      }
    });
  }
});

// Notify that uBlock is ready
tvBroPort.postMessage({ 
  action: "uBlockReady",
  data: {
    version: "1.0.0",
    filterInfo: adblockEngine.getFilterInfo()
  }
});
