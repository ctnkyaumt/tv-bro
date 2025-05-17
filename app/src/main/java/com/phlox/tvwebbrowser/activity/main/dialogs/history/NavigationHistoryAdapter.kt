package com.phlox.tvwebbrowser.activity.main.dialogs.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebHistoryItem
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R

class NavigationHistoryAdapter(
    private val historyItems: List<WebHistoryItem>,
    private val onItemClickListener: (WebHistoryItem) -> Unit
) : RecyclerView.Adapter<NavigationHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.tvTitle)
        val urlTextView: TextView = view.findViewById(R.id.tvUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_popup_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]
        holder.titleTextView.text = item.title ?: "No Title"
        holder.urlTextView.text = item.url ?: ""
        
        holder.itemView.setOnClickListener {
            onItemClickListener(item)
        }
    }

    override fun getItemCount() = historyItems.size
}
