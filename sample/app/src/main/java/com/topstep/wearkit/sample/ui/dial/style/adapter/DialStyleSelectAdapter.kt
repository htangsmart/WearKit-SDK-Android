package com.topstep.wearkit.sample.ui.dial.style.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.dial.WKDialStyleConstraint
import com.topstep.wearkit.sample.databinding.ItemDialStyleSelectBinding
import com.topstep.wearkit.sample.utils.glideShowImage

class DialStyleSelectAdapter : RecyclerView.Adapter<DialStyleSelectAdapter.StyleViewHolder>() {

    var selectPosition = 0
        private set

    var items: List<WKDialStyleConstraint.Style>? = null
        set(value) {
            field = value
            if (value != null) {
                if (selectPosition !in value.indices) {
                    selectPosition = 0
                }
                listener?.onItemSelect(selectPosition, value[selectPosition])
            } else {
                selectPosition = 0
            }
        }
    var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        return StyleViewHolder(
            ItemDialStyleSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        val items = this.items ?: return
        glideShowImage(holder.viewBind.imgView, items[position].image)
        holder.viewBind.dotView.isInvisible = selectPosition != position
        holder.itemView.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                selectPosition = actionPosition
                notifyDataSetChanged()
                listener?.onItemSelect(actionPosition, items[actionPosition])
            }
        }
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    class StyleViewHolder(val viewBind: ItemDialStyleSelectBinding) : RecyclerView.ViewHolder(viewBind.root)

    interface Listener {
        fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style)
    }

}