package com.phlox.tvwebbrowser.activity.main.dialogs.history

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.phlox.tvwebbrowser.R

class NavigationHistoryDialog(
    context: Context,
    private val historyList: WebBackForwardList,
    private val isForwardHistory: Boolean,
    private val currentIndex: Int,
    private val onHistoryItemSelected: (WebHistoryItem) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_popup_dialog)

        val titleTextView = findViewById<TextView>(R.id.tvTitle)
        val recyclerView = findViewById<RecyclerView>(R.id.rvHistoryItems)

        titleTextView.text = if (isForwardHistory) {
            context.getString(R.string.forward_history)
        } else {
            context.getString(R.string.back_history)
        }

        // Get history items based on direction (back or forward)
        val historyItems = getHistoryItems(historyList, isForwardHistory, currentIndex)

        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = NavigationHistoryAdapter(historyItems) { item ->
            onHistoryItemSelected(item)
            dismiss()
        }
    }

    private fun getHistoryItems(
        historyList: WebBackForwardList,
        isForwardHistory: Boolean,
        currentIndex: Int
    ): List<WebHistoryItem> {
        val items = mutableListOf<WebHistoryItem>()
        
        if (isForwardHistory) {
            // Get forward history items (from newest to oldest)
            for (i in 1..historyList.size - currentIndex - 1) {
                historyList.getItemAtIndex(currentIndex + i)?.let { items.add(it) }
            }
        } else {
            // Get back history items (from newest to oldest)
            for (i in 1..currentIndex) {
                historyList.getItemAtIndex(currentIndex - i)?.let { items.add(it) }
            }
        }
        
        return items
    }
}
