package com.phlox.tvwebbrowser.activity.main

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.FilterList
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModel
import com.phlox.tvwebbrowser.utils.observable.ObservableValue
import com.phlox.tvwebbrowser.webengine.gecko.GeckoWebEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension
import java.util.*

class AdblockModel : ActiveModel() {
    companion object {
        val TAG: String = AdblockModel::class.java.simpleName
        const val AUTO_UPDATE_INTERVAL_MINUTES = 60 * 24 * 30 //30 days
        private const val PREFS_FILTER_LISTS = "ublock_filter_lists"
        private const val KEY_FILTER_LISTS = "filter_lists"
    }

    val clientLoading = ObservableValue(false)
    val config = TVBro.config
    private var uBlockExtension: WebExtension? = null
    private var isUBlockEnabled = true
    private val prefs: SharedPreferences by lazy {
        TVBro.instance.getSharedPreferences(PREFS_FILTER_LISTS, Context.MODE_PRIVATE)
    }

    // Default filter lists
    private val defaultFilterLists = listOf(
        FilterList(
            "easylist",
            "EasyList",
            "General purpose ad blocking list",
            "https://easylist.to/easylist/easylist.txt",
            true
        ),
        FilterList(
            "easyprivacy",
            "EasyPrivacy",
            "Tracking protection list",
            "https://easylist.to/easylist/easyprivacy.txt",
            true
        ),
        FilterList(
            "fanboy-annoyance",
            "Fanboy's Annoyance List",
            "Blocks annoying elements like cookie notices",
            "https://secure.fanboy.co.nz/fanboy-annoyance.txt",
            false
        ),
        FilterList(
            "ublock-filters",
            "uBlock Origin Filters",
            "uBlock Origin's own filter list",
            "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            true
        ),
        FilterList(
            "adguard-generic",
            "AdGuard Base Filter",
            "AdGuard general purpose filter",
            "https://filters.adtidy.org/extension/ublock/filters/2_without_easylist.txt",
            false
        )
    )

    init {
        // uBlock extension is loaded by GeckoWebEngine
        // We just need to observe when it's available
        observeUBlockExtension()
    }

    private fun observeUBlockExtension() {
        if (config.isWebEngineGecko()) {
            GeckoWebEngine.appWebExtension.subscribe { extension ->
                if (extension?.metaData?.description?.contains("uBlock") == true) {
                    Log.d(TAG, "uBlock extension loaded")
                    uBlockExtension = extension
                    updateFilters()
                }
            }
        }
    }

    fun loadAdBlockList(forceReload: Boolean) = modelScope.launch {
        if (clientLoading.value) return@launch
        val checkDate = Calendar.getInstance()
        checkDate.timeInMillis = config.adBlockListLastUpdate
        checkDate.add(Calendar.MINUTE, AUTO_UPDATE_INTERVAL_MINUTES)
        val now = Calendar.getInstance()
        val needUpdate = forceReload || checkDate.before(now)
        
        if (needUpdate) {
            updateFilters()
        }
        
        config.adBlockListLastUpdate = now.timeInMillis
    }

    /**
     * Get the saved filter lists or the default ones if none are saved
     */
    fun getFilterLists(): List<FilterList> {
        val savedLists = prefs.getString(KEY_FILTER_LISTS, null)
        if (savedLists != null) {
            try {
                val jsonArray = JSONArray(savedLists)
                val lists = mutableListOf<FilterList>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    lists.add(
                        FilterList(
                            obj.getString("id"),
                            obj.getString("title"),
                            obj.getString("description"),
                            obj.getString("url"),
                            obj.getBoolean("enabled"),
                            obj.optLong("lastUpdated", 0)
                        )
                    )
                }
                
                return lists
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing saved filter lists", e)
            }
        }
        
        return defaultFilterLists
    }

    /**
     * Save the filter lists to preferences
     */
    private fun saveFilterLists(lists: List<FilterList>) {
        try {
            val jsonArray = JSONArray()
            
            for (list in lists) {
                val obj = JSONObject()
                obj.put("id", list.id)
                obj.put("title", list.title)
                obj.put("description", list.description)
                obj.put("url", list.url)
                obj.put("enabled", list.enabled)
                obj.put("lastUpdated", list.lastUpdated)
                jsonArray.put(obj)
            }
            
            prefs.edit().putString(KEY_FILTER_LISTS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving filter lists", e)
        }
    }

    /**
     * Update the filter lists in the uBlock extension
     */
    fun updateFilterLists(lists: List<FilterList>) {
        // Save the lists to preferences
        saveFilterLists(lists)
        
        // Update the filters in the extension
        updateFilters()
    }

    /**
     * Update all filters in the uBlock extension
     */
    private fun updateFilters() {
        if (config.isWebEngineGecko() && uBlockExtension != null) {
            try {
                // Get the enabled filter lists
                val enabledLists = getFilterLists().filter { it.enabled }
                
                // Send message to uBlock extension to update filters
                val port = uBlockExtension?.getMessageDelegate()
                val message = JSONObject().apply {
                    put("action", "updateFilters")
                    
                    // Add the filter lists
                    val listsArray = JSONArray()
                    for (list in enabledLists) {
                        val listObj = JSONObject()
                        listObj.put("id", list.id)
                        listObj.put("url", list.url)
                        listsArray.put(listObj)
                    }
                    
                    put("lists", listsArray)
                }
                
                port?.onMessage(message, null)
                Log.d(TAG, "Sent updateFilters message to uBlock with ${enabledLists.size} lists")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating uBlock filters", e)
                Toast.makeText(TVBro.instance, "Error updating ad-blocker filters", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isAd(url: Uri, type: String?, baseUri: Uri): Boolean {
        if (!isUBlockEnabled) return false
        
        // For WebView engine, we would need a different implementation
        // This is just a placeholder that returns false
        if (!config.isWebEngineGecko()) return false
        
        // The actual filtering is done by the uBlock extension
        // This method is called by the WebEngineWindowProviderCallback
        // which is connected to the extension's port
        // The extension will decide whether to block the request
        
        // For debugging purposes, we can log some information
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "isAd check: $url, type: $type, baseUri: $baseUri")
        }
        
        // The actual blocking happens in the extension
        // This method just needs to return true for requests that should be blocked
        // The extension will handle the actual filtering logic
        return false
    }
    
    fun toggleAdBlockEnabled() {
        isUBlockEnabled = !isUBlockEnabled
        if (config.isWebEngineGecko() && uBlockExtension != null) {
            try {
                // Send message to uBlock extension to toggle enabled state
                val port = uBlockExtension?.getMessageDelegate()
                val message = JSONObject().apply {
                    put("action", "toggleEnabled")
                }
                port?.onMessage(message, null)
                Log.d(TAG, "Sent toggleEnabled message to uBlock")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling uBlock", e)
            }
        }
    }
}