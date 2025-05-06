package com.topstep.wearkit.sample.ui.sync

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemSportListBinding
import com.topstep.wearkit.sample.entity.SportEntity
import com.topstep.wearkit.sample.utils.AppUtils
import java.math.BigDecimal
import java.math.RoundingMode

class SportAdapter : RecyclerView.Adapter<SportAdapter.ItemViewHolder>() {

    var sources: List<SportEntity>? = null

    fun addSport(result: List<SportEntity>?) {
        this.sources = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemSportListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.sportName.text = item.sportName
        holder.viewBind.sportTime.text = holder.viewBind.sportTime.context.getString(R.string.sport_total_duration, AppUtils.secondsToHMS(item.duration))
        holder.viewBind.sportDistance.visibility = if (item.distance.toInt() > 0) View.VISIBLE else View.GONE
        holder.viewBind.sportDistance.text = holder.viewBind.sportDistance.context.getString(R.string.activity_goal_distance) + ":" + (item.distance / 1000f).formatToTwoDecimalPlaces()
        holder.viewBind.sportCalories.text = holder.viewBind.sportCalories.context.getString(R.string.unit_k_calories_param, item.calories.toInt())
        holder.viewBind.sportAvgHeartRate.text = holder.viewBind.sportAvgHeartRate.context.getString(R.string.sport_avg_heart_rate, item.avgHeartRate)
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    private fun Double.formatToTwoDecimalPlaces(): Double {
        return BigDecimal(this).setScale(2, RoundingMode.DOWN).toDouble()
    }


    class ItemViewHolder(val viewBind: ItemSportListBinding) : RecyclerView.ViewHolder(viewBind.root)

}