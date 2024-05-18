package com.topstep.wearkit.sample.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.databinding.ItemContactsListBinding
import com.topstep.wearkit.sample.model.ContactsBean

class ContactsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var sources: ArrayList<ContactsBean>? = null
    var listener: Listener? = null
    var isShow: Boolean = false
    var isClick: Boolean = false

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
        holder.mviewBind.tvName.text = item.name
        holder.mviewBind.tvNumber.text = item.number
        holder.mviewBind.conIvF.isSelected = item.isdelete
         holder.mviewBind.conIvF.setOnClickListener {
             val actionPosition = holder.bindingAdapterPosition
             if (actionPosition != RecyclerView.NO_POSITION) {
                 var pos = items[actionPosition]
                 pos.isdelete = !pos.isdelete
                 notifyDataSetChanged()
                 listener?.onItemDelete(actionPosition)
             }
         }

    }

    override fun getItemCount(): Int {
        val size = sources?.size ?: 0
        return size
    }

    fun getClick(): Boolean {
        for (i in sources!!.size - 1 downTo 0) {
            var fwContacts = sources?.get(i)
            if (fwContacts != null) {
                if (fwContacts.isdelete) {
                    isClick = true
                    break
                } else {
                    isClick = false
                }
            }
        }
        return isClick
    }


    interface Listener {
        fun onItemDelete(position: Int)
    }


    private class BookViewHolder(val mviewBind: ItemContactsListBinding) :
        RecyclerView.ViewHolder(mviewBind.root)

}