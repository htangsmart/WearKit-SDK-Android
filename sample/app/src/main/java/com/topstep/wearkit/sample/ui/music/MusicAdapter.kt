package com.topstep.wearkit.sample.ui.music

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.databinding.ItemMusicListBinding

class MusicAdapter : RecyclerView.Adapter<MusicAdapter.ItemViewHolder>() {

    var sources: List<MusicBean>? = null
    val pushIndexes = ArrayList<Int>()

    /**
     * Add a contacts
     * @return Add success
     */
    fun addMusic(result: List<MusicBean>?) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemMusicListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.musicSinger.text = item.singer
        holder.viewBind.musicName.text = item.musicName
        holder.viewBind.conIvF.isSelected = pushIndexes.contains(position)
        holder.viewBind.conIvF.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                addDeleteIndex(actionPosition)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    private fun addDeleteIndex(index: Int) {
        if (pushIndexes.contains(index)) {
            pushIndexes.remove(index)
        } else {
            pushIndexes.add(index)
        }
    }

    class ItemViewHolder(val viewBind: ItemMusicListBinding) : RecyclerView.ViewHolder(viewBind.root)

}