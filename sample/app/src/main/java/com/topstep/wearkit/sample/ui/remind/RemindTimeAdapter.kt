package com.topstep.wearkit.sample.ui.remind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.databinding.ItemRemindTimesBinding
import com.topstep.wearkit.sample.utils.AppUtils

class RemindTimeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sources: ArrayList<RemindTime>? = null
    var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RemindTimesViewHolder(
            ItemRemindTimesBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is RemindTimesViewHolder) return
        val items = this.sources ?: return
        val item = items[position]

        holder.viewBind.remindTimeTv.text = AppUtils.minute2Duration(item.time)
        holder.viewBind.remindTimeDelete.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                val pos = items[actionPosition]
               // pos.isDelete = !pos.isDelete
                listener?.onItemDelete(actionPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    interface Listener {
        fun onItemDelete(position: Int)
    }

    private class RemindTimesViewHolder(val viewBind: ItemRemindTimesBinding) : RecyclerView.ViewHolder(viewBind.root)

}