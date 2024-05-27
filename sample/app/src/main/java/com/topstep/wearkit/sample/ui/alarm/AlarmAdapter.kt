package com.topstep.wearkit.sample.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.apis.model.WKAlarm
import com.topstep.wearkit.sample.databinding.ItemAlarmListBinding
import com.topstep.wearkit.sample.utils.AppUtils

class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.ItemViewHolder>() {

    var sources: MutableList<WKAlarm>? = null
    val deleteIndexes = ArrayList<Int>()

    /**
     * Add a contacts
     * @return Add success
     */
    fun addAlarm(alarm: WKAlarm) {
        val list = this.sources ?: ArrayList<WKAlarm>().also {
            this.sources = it
        }
        list.add(alarm)
        this.sources = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemAlarmListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.alarmTime.text = "${item.hour} : ${item.minute}"
        holder.viewBind.alarmWeek.text = AppUtils.getWeek(holder.viewBind.alarmWeek.context, item.repeat)
        holder.viewBind.conIvF.isSelected = deleteIndexes.contains(position)
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
        if (deleteIndexes.contains(index)) {
            deleteIndexes.remove(index)
        } else {
            deleteIndexes.add(index)
        }
    }

    class ItemViewHolder(val viewBind: ItemAlarmListBinding) : RecyclerView.ViewHolder(viewBind.root)

}