package com.topstep.wearkit.sample.ui.sync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemSleepListBinding
import com.topstep.wearkit.sample.entity.SleepEntity
import com.topstep.wearkit.sample.utils.AppUtils

class SleepAdapter : RecyclerView.Adapter<SleepAdapter.ItemViewHolder>() {

    var sources: List<SleepEntity>? = null


    fun addActivity(result: List<SleepEntity>?) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemSleepListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.sleepTime.text = AppUtils.convertTimestampToDate(item.timestamp)
        holder.viewBind.sleepCountTime.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_total_duration, AppUtils.secondsToHMS(item.duration))
        holder.viewBind.sleepDeep.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_deep, AppUtils.secondsToHMS(item.deep))
        holder.viewBind.sleepLight.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_light, AppUtils.secondsToHMS(item.light))
        holder.viewBind.sleepAwake.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_awake, AppUtils.secondsToHMS(item.awake))
        holder.viewBind.sleepRem.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_rem, AppUtils.secondsToHMS(item.rem))
        holder.viewBind.sleepNap.text = holder.viewBind.sleepCountTime.context.getString(R.string.sleep_nap, AppUtils.secondsToHMS(item.nap))
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    class ItemViewHolder(val viewBind: ItemSleepListBinding) : RecyclerView.ViewHolder(viewBind.root)

}