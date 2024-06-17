package com.topstep.wearkit.sample.ui.sync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemBloodPressureListBinding
import com.topstep.wearkit.sample.entity.BloodPressureEntity
import com.topstep.wearkit.sample.utils.AppUtils

class BloodPressureAdapter : RecyclerView.Adapter<BloodPressureAdapter.ItemViewHolder>() {

    var sources: List<BloodPressureEntity>? = null


    fun addActivity(result: List<BloodPressureEntity>?) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemBloodPressureListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.bloodPressureTime.text = AppUtils.convertTimestampToDate(item.timestamp)
        holder.viewBind.bloodPressureSbp.text = holder.viewBind.bloodPressureSbp.context.getString(R.string.blood_pressure_sbp, item.sbp)
        holder.viewBind.bloodPressureDbp.text = holder.viewBind.bloodPressureDbp.context.getString(R.string.blood_pressure_dbp, item.dbp)

    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }


    class ItemViewHolder(val viewBind: ItemBloodPressureListBinding) : RecyclerView.ViewHolder(viewBind.root)

}