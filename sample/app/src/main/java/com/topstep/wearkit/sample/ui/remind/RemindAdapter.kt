package com.topstep.wearkit.sample.ui.remind

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.apis.model.WKRemind
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ItemRemindListBinding
import com.topstep.wearkit.sample.utils.AppUtils

class RemindAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sources: ArrayList<WKRemind>? = null
    var listener: Listener? = null
    var mDeleteIndex = ArrayList<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AlarmHolder(
            ItemRemindListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is AlarmHolder) return
        val items = this.sources ?: return
        val item = items[position]
        holder.mViewBind.remindTime.text = getName(holder.mViewBind.remindTime.context, item.type, item)
        holder.mViewBind.remindWeek.text = AppUtils.getWeek(holder.mViewBind.remindWeek.context, item.repeat)
        holder.mViewBind.conIvF.isSelected = mDeleteIndex.contains(position)
        holder.mViewBind.conIvF.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                addDeleteIndex(actionPosition)
                notifyDataSetChanged()
            }
        }

        holder.mViewBind.remindLl.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                val pos = items[actionPosition]
                listener?.onItemClick(pos.name, pos.type)
            }
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    private fun addDeleteIndex(index: Int) {
        if (mDeleteIndex.contains(index)) {
            mDeleteIndex.remove(index)
        } else {
            mDeleteIndex.add(index)
        }
    }

    private class AlarmHolder(val mViewBind: ItemRemindListBinding) :
        RecyclerView.ViewHolder(mViewBind.root)

    private fun getName(con: Context, type: WKRemind.Type, item: WKRemind): String {
        return when (type) {
            WKRemind.Type.Sedentary -> {
                con.getString(R.string.ds_remind_sedentary)
            }
            WKRemind.Type.DrinkWater -> {
                con.getString(R.string.ds_remind_drinkWater)
            }
            WKRemind.Type.TakeMedicine -> {
                con.getString(R.string.ds_remind_take_medicine)
            }
            is WKRemind.Type.Custom -> item.name
        }
    }

    interface Listener {

        fun onItemClick(name: String, type: WKRemind.Type)
    }
}