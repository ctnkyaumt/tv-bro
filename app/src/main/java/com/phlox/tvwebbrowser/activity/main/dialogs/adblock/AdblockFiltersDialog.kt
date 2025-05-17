package com.phlox.tvwebbrowser.activity.main.dialogs.adblock

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.activity.main.AdblockModel
import com.phlox.tvwebbrowser.model.FilterList
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for managing adblock filter lists
 */
class AdblockFiltersDialog(
    context: Context,
    private val onFiltersUpdated: () -> Unit
) : Dialog(context) {

    private lateinit var adapter: FilterListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnApply: Button
    private lateinit var btnCancel: Button
    private val adblockModel = ActiveModelsRepository.get(AdblockModel::class, context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private val filterLists = mutableListOf(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_adblock_filters, null)
        setContentView(view)
        
        recyclerView = view.findViewById(R.id.rvFilters)
        btnApply = view.findViewById(R.id.btnApply)
        btnCancel = view.findViewById(R.id.btnCancel)
        
        // Load saved filter preferences
        loadFilterPreferences()
        
        // Setup RecyclerView
        adapter = FilterListAdapter(filterLists) { filterList, enabled ->
            // Handle filter toggle
            filterList.enabled = enabled
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        // Setup buttons
        btnApply.setOnClickListener {
            saveFilterPreferences()
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun loadFilterPreferences() {
        // In a real implementation, this would load from SharedPreferences
        // For now, we'll use the default values
    }
    
    private fun saveFilterPreferences() {
        // Save filter preferences
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                // In a real implementation, this would save to SharedPreferences
                // and update the filter lists in the uBlock extension
                
                // Send the updated filter lists to the uBlock extension
                adblockModel.updateFilterLists(filterLists)
            }
            
            // Notify that filters have been updated
            onFiltersUpdated()
        }
    }
    
    companion object {
        fun show(context: Context, onFiltersUpdated: () -> Unit = {}) {
            AdblockFiltersDialog(context, onFiltersUpdated).show()
        }
    }
}
