// uBlock Origin adblocking engine for TV Bro
// Simplified implementation based on uBlock Origin

class AdblockEngine {
  constructor() {
    this.enabled = true;
    this.filters = new Map();
    this.ready = false;
    this.filterLists = [];
    this.lastUpdated = 0;
  }

  init() {
    console.log("Initializing uBlock adblocking engine");
    this.ready = true;
    return true;
  }

  /**
   * Load filter lists from the provided configuration
   * @param {Array} filterLists - Array of filter list objects with id and url properties
   */
  loadFilterLists(filterLists) {
    console.log(`Loading ${filterLists.length} filter lists`);
    this.filterLists = filterLists;
    this.filters.clear();
    
    // Load each filter list
    const promises = filterLists.map(list => {
      return fetch(list.url)
        .then(response => {
          if (!response.ok) {
            throw new Error(`Failed to fetch ${list.url}: ${response.status} ${response.statusText}`);
          }
          return response.text();
        })
        .then(text => {
          console.log(`Loaded filter list ${list.id} (${text.length} bytes)`);
          this.parseFilterList(text, list.id);
          return list.id; // Return the list ID for successful loads
        })
        .catch(error => {
          console.error(`Error loading filter list ${list.id}:`, error);
          return null; // Return null for failed loads
        });
    });
    
    // Wait for all filter lists to load
    Promise.all(promises).then(results => {
      const successfulLoads = results.filter(id => id !== null);
      console.log(`Successfully loaded ${successfulLoads.length} of ${filterLists.length} filter lists`);
      this.lastUpdated = Date.now();
    });
  }

  parseFilterList(text, listId) {
    console.log(`Parsing filter list ${listId}`);
    const lines = text.split('\n');
    let parsedCount = 0;
    
    lines.forEach(line => {
      line = line.trim();
      // Skip comments and empty lines
      if (line.startsWith('!') || line.startsWith('[') || line === '') {
        return;
      }
      
      // Basic filter parsing
      if (line.includes('##') || line.includes('#@#')) {
        // Cosmetic filter
        this.addCosmeticFilter(line, listId);
        parsedCount++;
      } else if (line.startsWith('@@')) {
        // Exception filter
        this.addExceptionFilter(line.substring(2), listId);
        parsedCount++;
      } else {
        // Network filter
        this.addNetworkFilter(line, listId);
        parsedCount++;
      }
    });
    
    console.log(`Parsed ${parsedCount} filters from ${listId}`);
  }

  addNetworkFilter(filter, listId) {
    // Simplified filter storage
    this.filters.set(filter, { type: 'network', action: 'block', listId });
  }

  addExceptionFilter(filter, listId) {
    this.filters.set(filter, { type: 'network', action: 'allow', listId });
  }

  addCosmeticFilter(filter, listId) {
    this.filters.set(filter, { type: 'cosmetic', listId });
  }

  matchesFilter(url, type, sourceUrl) {
    if (!this.enabled || !this.ready) {
      return false;
    }

    // Simple string matching for demonstration
    // In a real implementation, this would use more sophisticated pattern matching
    for (const [pattern, filterInfo] of this.filters.entries()) {
      if (filterInfo.type !== 'network') {
        continue;
      }

      // Skip exception filters for blocking check
      if (filterInfo.action === 'allow') {
        if (url.includes(pattern)) {
          return false; // Exception matched, don't block
        }
        continue;
      }

      // Check if URL matches filter pattern
      if (url.includes(pattern)) {
        return true; // Block this request
      }
    }

    return false;
  }

  toggleEnabled() {
    this.enabled = !this.enabled;
    console.log(`uBlock adblocking ${this.enabled ? 'enabled' : 'disabled'}`);
    return this.enabled;
  }

  /**
   * Update filter lists with the provided configuration
   * @param {Array} filterLists - Array of filter list objects with id and url properties
   */
  updateFilters(filterLists) {
    console.log(`Updating filter lists: ${JSON.stringify(filterLists)}`);
    this.filters.clear();
    this.loadFilterLists(filterLists || this.filterLists);
  }
  
  /**
   * Get information about the current filter lists
   */
  getFilterInfo() {
    return {
      enabled: this.enabled,
      filterCount: this.filters.size,
      filterLists: this.filterLists,
      lastUpdated: this.lastUpdated
    };
  }
}

// Export the engine
const adblockEngine = new AdblockEngine();
