package com.topstep.wearkit.sample.ui.others

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdParentalControl
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemHsdParentalControlBinding
import com.topstep.wearkit.sample.utils.AppUtils

class HsdParentalControlAdapter : RecyclerView.Adapter<HsdParentalControlAdapter.ItemViewHolder>() {

    var sources: MutableList<HsdParentalControl.Item> = ArrayList()
    var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemHsdParentalControlBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = sources[position]
        val context = holder.itemView.context
        holder.viewBind.tvTitle.text = hsdParentalControlIdName(context, item.id)
        holder.viewBind.tvSummary.text = buildString {
            append(hsdParentalControlModeName(context, item.mode))
            append(" · ")
            append(if (item.isEnabled) "ON" else "OFF")
            item.periods.forEach { period ->
                append('\n')
                append(hsdParentalControlPeriodText(context, period))
            }
        }
        holder.itemView.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                listener?.onItemClick(sources[actionPosition])
            }
        }
        holder.viewBind.btnDelete.clickTrigger {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                listener?.onItemDelete(actionPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return sources.size
    }

    interface Listener {
        fun onItemClick(item: HsdParentalControl.Item)
        fun onItemDelete(position: Int)
    }

    class ItemViewHolder(val viewBind: ItemHsdParentalControlBinding) : RecyclerView.ViewHolder(viewBind.root)
}

internal fun hsdParentalControlIdName(context: Context, id: Int): String {
    val names = context.resources.getStringArray(R.array.hsd_parental_control_ids)
    val index = id - 1
    return names.getOrNull(index) ?: id.toString()
}

internal fun hsdParentalControlModeName(context: Context, mode: Int): String {
    val names = context.resources.getStringArray(R.array.hsd_parental_control_modes)
    return when (mode) {
        HsdParentalControl.Mode.BLOCK -> names.getOrNull(0) ?: "BLOCK"
        HsdParentalControl.Mode.ALLOW -> names.getOrNull(1) ?: "ALLOW"
        else -> mode.toString()
    }
}

internal fun hsdParentalControlPeriodText(context: Context, period: HsdParentalControl.Period): String {
    return "${AppUtils.minute2Duration(period.start)}-${AppUtils.minute2Duration(period.end)} ${AppUtils.getWeek(context, period.repeat)}"
}
