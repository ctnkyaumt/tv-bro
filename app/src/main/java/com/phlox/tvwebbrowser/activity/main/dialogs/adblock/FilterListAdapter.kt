package com.phlox.tvwebbrowser.activity.main.dialogs.adblock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.FilterList

/**
 * Adapter for displaying adblock filter lists in a RecyclerView
 */
class FilterListAdapter(
    private val filterLists: MutableList<FilterList>,
    private val onFilterToggled: (FilterList, Boolean) -> Unit
) : RecyclerView.Adapter<FilterListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.cbEnabled)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val description: TextView = itemView.findViewById(R.id.tvDescription)

        init {
            // Handle checkbox changes
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val filterList = filterLists[position]
                    filterList.enabled = isChecked
                    onFilterToggled(filterList, isChecked)
                }
            }

            // Make the entire item clickable to toggle the checkbox
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filterList = filterLists[position]
        holder.title.text = filterList.title
        holder.description.text = filterList.description
        holder.checkbox.isChecked = filterList.enabled
    }

    override fun getItemCount() = filterLists.size

    fun updateData(newFilterLists: List<FilterList>) {
        filterLists.clear()
        filterLists.addAll(newFilterLists)
        notifyDataSetChanged()
    }
}
