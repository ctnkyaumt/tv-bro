package com.phlox.tvwebbrowser.activity.main.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.model.HomePageLink
import com.phlox.tvwebbrowser.singleton.FaviconsPool
import kotlinx.coroutines.*

class HomePageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface Callback {
        fun onNavigate(url: String)
        fun onAddNew()
        fun onEdit(link: HomePageLink)
        fun onDelete(link: HomePageLink)
        fun onReorder(newOrder: List<HomePageLink>)
    }

    private val recycler: RecyclerView
    private var adapter: CardsAdapter
    private var callback: Callback? = null

    private var moveMode = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_homepage, this, true)
        recycler = findViewById(R.id.rvCards)
        recycler.layoutManager = GridLayoutManager(context, 4)
        adapter = CardsAdapter(
            onClick = { position ->
                val item = adapter.items.getOrNull(position) ?: return@CardsAdapter
                if (item.isAddItem) {
                    callback?.onAddNew()
                } else {
                    val url = item.link?.dest_url ?: item.link?.url
                    if (!url.isNullOrEmpty()) callback?.onNavigate(url)
                }
            },
            onLongPressOk = { position, anchor ->
                val item = adapter.items.getOrNull(position) ?: return@CardsAdapter true
                if (item.isAddItem) return@CardsAdapter true
                showItemMenu(item, position, anchor)
                true
            }
        )
        recycler.adapter = adapter

        // Handle DPAD reordering when move mode enabled
        recycler.setOnKeyListener { _, keyCode, event ->
            if (!moveMode) return@setOnKeyListener false
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val lm = recycler.layoutManager as GridLayoutManager
            val span = lm.spanCount
            val pos = currentPosition()
            if (pos == RecyclerView.NO_POSITION) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> moveItem(pos, pos - 1)
                KeyEvent.KEYCODE_DPAD_RIGHT -> moveItem(pos, pos + 1)
                KeyEvent.KEYCODE_DPAD_UP -> moveItem(pos, pos - span)
                KeyEvent.KEYCODE_DPAD_DOWN -> moveItem(pos, pos + span)
                KeyEvent.KEYCODE_BACK -> { exitMoveMode(); true }
                KeyEvent.KEYCODE_MENU -> { exitMoveMode(); true }
                else -> false
            }
        }
    }

    fun setCallback(cb: Callback) { this.callback = cb }

    fun setData(links: List<HomePageLink>) {
        adapter.submitLinks(links)
    }

    private fun currentPosition(): Int {
        val view = recycler.focusedChild ?: return RecyclerView.NO_POSITION
        return recycler.getChildAdapterPosition(view)
    }

    private fun moveItem(from: Int, to: Int): Boolean {
        if (to < 0 || to >= adapter.items.size - 1) return true // keep + at the end
        if (from == to) return true
        val item = adapter.items.removeAt(from)
        adapter.items.add(if (to >= adapter.items.size) adapter.items.size else to, item)
        adapter.notifyItemMoved(from, to)
        return true
    }

    private fun exitMoveMode() {
        if (!moveMode) return
        moveMode = false
        adapter.setMoveMode(false)
        // persist
        val newOrder = adapter.items.filter { !it.isAddItem }.map { it.link!! }
        callback?.onReorder(newOrder)
    }

    private fun showItemMenu(item: CardItem, position: Int, anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, context.getString(R.string.move))
        popup.menu.add(0, 2, 1, context.getString(R.string.edit))
        popup.menu.add(0, 3, 2, context.getString(R.string.delete))
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> { // Move
                    moveMode = true
                    adapter.setMoveMode(true)
                    true
                }
                2 -> { callback?.onEdit(item.link!!); true }
                3 -> { callback?.onDelete(item.link!!); true }
                else -> false
            }
        }
        popup.setOnDismissListener { exitMoveMode() }
        popup.show()
    }

    data class CardItem(val link: HomePageLink?, val isAddItem: Boolean)

    private class CardVH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val title: TextView = view.findViewById(R.id.tvTitle)
    }

    private inner class CardsAdapter(
        val onClick: (Int) -> Unit,
        val onLongPressOk: (Int, View) -> Boolean
    ) : RecyclerView.Adapter<CardVH>() {
        val items = mutableListOf<CardItem>()
        private var moveMode = false
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        fun submitLinks(links: List<HomePageLink>) {
            items.clear()
            items.addAll(links.map { CardItem(it, false) })
            items.add(CardItem(null, true)) // add card at the end
            notifyDataSetChanged()
        }

        fun setMoveMode(enabled: Boolean) {
            moveMode = enabled
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun getItemViewType(position: Int): Int = if (items[position].isAddItem) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val layout = if (viewType == 0) R.layout.item_home_card else R.layout.item_home_add_card
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            v.isFocusable = true
            v.isFocusableInTouchMode = true
            return CardVH(v)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            val item = items[position]
            if (item.isAddItem) {
                holder.icon.setImageResource(R.drawable.ic_baseline_add_box_24)
                holder.icon.setColorFilter(Color.GRAY)
                holder.title.text = context.getString(R.string.add)
            } else {
                holder.title.text = item.link?.title ?: ""
                holder.icon.setImageResource(R.drawable.ic_baseline_add_box_24)
                holder.icon.setColorFilter(Color.GRAY)
                val url = item.link?.dest_url ?: item.link?.url
                if (!url.isNullOrEmpty()) {
                    scope.launch {
                        val bmp = withContext(Dispatchers.IO) { FaviconsPool.get(url) }
                        if (bmp != null && holder.adapterPosition == position) {
                            holder.icon.setColorFilter(null)
                            holder.icon.setImageBitmap(bmp)
                        }
                    }
                }
            }
            holder.itemView.alpha = if (moveMode) 0.8f else 1f
            holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
            holder.itemView.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.action == KeyEvent.ACTION_DOWN && event.isLongPress) {
                    onLongPressOk(holder.bindingAdapterPosition, v)
                } else false
            }
        }
    }
}
