package com.topstep.wearkit.sample.ui.music

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.apis.model.file.WKFileInfo
import com.topstep.wearkit.sample.databinding.ItemWatchMusicListBinding

class WatchMusicAdapter : RecyclerView.Adapter<WatchMusicAdapter.ItemViewHolder>() {

    var sources: MutableList<WKFileInfo>? = null
    var listener: Listener? = null

    /**
     * Add a contacts
     * @return Add success
     */
    fun addMusic(result: MutableList<WKFileInfo>) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemWatchMusicListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.musicWatchName.text = item.name
        holder.viewBind.musicDelete.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                val list = items[actionPosition]
                listener?.onItemDelete(list.name, actionPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    interface Listener {
        fun onItemDelete(name: String, pos: Int)
    }


    class ItemViewHolder(val viewBind: ItemWatchMusicListBinding) : RecyclerView.ViewHolder(viewBind.root)

}