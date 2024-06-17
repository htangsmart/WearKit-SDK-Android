package com.topstep.wearkit.sample.ui.sync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemStepListBinding
import com.topstep.wearkit.sample.entity.ActivityEntity
import com.topstep.wearkit.sample.utils.AppUtils

class StepAdapter : RecyclerView.Adapter<StepAdapter.ItemViewHolder>() {

    var sources: List<ActivityEntity>? = null


    fun addActivity(result: List<ActivityEntity>?) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemStepListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.stepTime.text = AppUtils.convertTimestampToDate(item.timestamp)
        holder.viewBind.stepTv.text = holder.viewBind.stepTv.context.getString(R.string.unit_step_param, item.steps)

    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }


    class ItemViewHolder(val viewBind: ItemStepListBinding) : RecyclerView.ViewHolder(viewBind.root)

}