package com.orange.tpms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.orange.obd.test.databinding.ItemCommandBinding

class CommandAddAdapter(
    private val data: ArrayList<MutableMap<String, Any>>
) : BaseAdapter() {
    interface Listener{
        fun remove(id: String)
        fun click(command: String)
    }

    var l: Listener? = null

    override fun getCount(): Int = data.size
    override fun getItem(position: Int): MutableMap<String, Any> = data[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            val v = ItemCommandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            view = v.root
            holder = ViewHolder(
                tvRemove   = v.tvRemove,
                tvCommand   = v.tvCommand,
                clItem = v.clItem
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)
        holder.tvCommand.text = item["command"].toString()
        holder.tvRemove.setOnClickListener { l?.remove(item["id"].toString()) }
        holder.clItem.setOnClickListener { l?.click(item["command"].toString()) }
        return view
    }

    private data class ViewHolder(
        val tvRemove: TextView,
        val tvCommand: TextView,
        val clItem: ConstraintLayout
    )
}
