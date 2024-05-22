package com.topstep.wearkit.sample.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.apis.model.WKAlarm
import com.topstep.wearkit.sample.databinding.ItemAlarmListBinding
import com.topstep.wearkit.sample.utils.AppUtils

class AlarmAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sources: ArrayList<WKAlarm>? = null
     var mDeleteIndex = ArrayList<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AlarmHolder(
            ItemAlarmListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is AlarmHolder) return
        val items = this.sources ?: return
        val item = items[position]
        holder.mViewBind.alarmTime.text = "${item.hour} : ${item.minute}"
        holder.mViewBind.alarmWeek.text = AppUtils.getWeek(holder.mViewBind.alarmWeek.context, item.repeat)
        holder.mViewBind.conIvF.isSelected = mDeleteIndex.contains(position)
        holder.mViewBind.conIvF.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                addDeleteIndex(actionPosition)
                notifyDataSetChanged()
            }
        }

    }

    override fun getItemCount(): Int {
        return  sources?.size ?: 0
    }
    private fun addDeleteIndex(index: Int) {
        if (mDeleteIndex.contains(index)) {
            mDeleteIndex.remove(index)
        } else {
            mDeleteIndex.add(index)
        }
    }

    private class AlarmHolder(val mViewBind: ItemAlarmListBinding) :
        RecyclerView.ViewHolder(mViewBind.root)

}