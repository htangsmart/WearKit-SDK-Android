package com.topstep.wearkit.sample.ui.dial.style.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.databinding.ItemDialBackgroundThumbBinding
import com.topstep.wearkit.sample.utils.glideShowImage

class DialBackgroundThumbAdapter : RecyclerView.Adapter<DialBackgroundThumbAdapter.ViewHolder>() {

    var selectPosition = 0
        private set

    var items: List<Uri> = emptyList()
        set(value) {
            field = value
            if (value.isEmpty()) {
                selectPosition = 0
            } else if (selectPosition !in value.indices) {
                selectPosition = value.lastIndex
            }
            notifyDataSetChanged()
        }

    var listener: Listener? = null

    fun select(position: Int) {
        if (position !in items.indices || selectPosition == position) return
        selectPosition = position
        notifyDataSetChanged()
        listener?.onItemSelect(position, items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDialBackgroundThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        glideShowImage(holder.viewBind.imgThumb, items[position])
        holder.viewBind.viewSelected.isVisible = selectPosition == position
        holder.itemView.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                select(actionPosition)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val viewBind: ItemDialBackgroundThumbBinding) : RecyclerView.ViewHolder(viewBind.root)

    interface Listener {
        fun onItemSelect(position: Int, uri: Uri)
    }
}
