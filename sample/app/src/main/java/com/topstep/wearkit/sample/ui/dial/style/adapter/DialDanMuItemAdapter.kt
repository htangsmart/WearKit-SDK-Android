package com.topstep.wearkit.sample.ui.dial.style.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemDialDanmuBinding
import com.topstep.wearkit.sample.ui.dial.style.DanMuDraft
import com.topstep.wearkit.sample.ui.dial.style.formatDanMuCoordX
import com.topstep.wearkit.sample.ui.dial.style.formatDanMuCoordY

internal class DialDanMuItemAdapter : RecyclerView.Adapter<DialDanMuItemAdapter.ViewHolder>() {

    var items: MutableList<DanMuDraft> = mutableListOf()
    var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDialDanmuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.viewBind.tvText.text = item.text
        holder.viewBind.tvSummary.text = context.getString(
            R.string.dial_custom_style_danmu_item_summary,
            formatDanMuCoordX(context, item.imageX),
            formatDanMuCoordY(context, item.imageY),
            context.getString(
                if (item.ltr) R.string.dial_custom_style_danmu_direction_right
                else R.string.dial_custom_style_danmu_direction_left
            ),
            item.walkSpeed,
            animationLabel(context, item.animationUri),
        )
        holder.viewBind.btnDelete.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                listener?.onDelete(actionPosition)
            }
        }
        holder.itemView.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                listener?.onItemClick(actionPosition)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun animationLabel(context: Context, name: String?): String {
        return when {
            name?.contains("circle") == true -> context.getString(R.string.dial_custom_style_danmu_animation_1)
            name?.contains("square") == true -> context.getString(R.string.dial_custom_style_danmu_animation_2)
            name?.contains("triangle") == true -> context.getString(R.string.dial_custom_style_danmu_animation_3)
            else -> context.getString(R.string.dial_custom_style_danmu_animation_none)
        }
    }

    class ViewHolder(val viewBind: ItemDialDanmuBinding) : RecyclerView.ViewHolder(viewBind.root)

    interface Listener {
        fun onItemClick(position: Int)
        fun onDelete(position: Int)
    }
}
