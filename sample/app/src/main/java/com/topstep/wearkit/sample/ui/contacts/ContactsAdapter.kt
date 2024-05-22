package com.topstep.wearkit.sample.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.apis.model.WKContacts
import com.topstep.wearkit.sample.databinding.ItemContactsListBinding

class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.InnerViewHolder>() {

    var sources: MutableList<WKContacts>? = null
    val deleteIndexes = ArrayList<Int>()

    /**
     * Add a contacts
     * @return Add success
     */
    fun addContacts(contacts: WKContacts): Boolean {
        val list = this.sources ?: ArrayList<WKContacts>().also {
            this.sources = it
        }
        //If exist ,don't add again
        if (list.find { it.number == contacts.number } != null) {
            return false
        }
        list.add(contacts)
        this.sources = list
        notifyDataSetChanged()
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        return InnerViewHolder(
            ItemContactsListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val items = this.sources ?: return
        val item = items[position]
        holder.viewBind.tvName.text = item.name
        holder.viewBind.tvNumber.text = item.number
        holder.viewBind.imgDelete.isSelected = deleteIndexes.contains(position)
        holder.viewBind.imgDelete.setOnClickListener {
            val actionPosition = holder.bindingAdapterPosition
            if (actionPosition != RecyclerView.NO_POSITION) {
                toggleDeleteIndex(actionPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return sources?.size ?: 0
    }

    private fun toggleDeleteIndex(index: Int) {
        if (deleteIndexes.contains(index)) {
            deleteIndexes.remove(index)
        } else {
            deleteIndexes.add(index)
        }
        notifyDataSetChanged()
    }

    class InnerViewHolder(val viewBind: ItemContactsListBinding) : RecyclerView.ViewHolder(viewBind.root)

}