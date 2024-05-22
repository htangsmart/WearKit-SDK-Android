package com.topstep.wearkit.sample.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.databinding.ItemContactsListBinding
import com.topstep.wearkit.sample.model.ContactsBean

class ContactsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sources: ArrayList<ContactsBean>? = null
     var mDeleteIndex = ArrayList<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BookViewHolder(
            ItemContactsListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is BookViewHolder) return
        val items = this.sources ?: return
        val item = items[position]
        holder.mViewBind.tvName.text = item.name
        holder.mViewBind.tvNumber.text = item.number
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

    private class BookViewHolder(val mViewBind: ItemContactsListBinding) :
        RecyclerView.ViewHolder(mViewBind.root)

}