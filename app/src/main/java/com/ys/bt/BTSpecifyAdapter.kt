package com.ys.bt

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.ys.bt.databinding.ItemServiceDetailBinding

class BTSpecifyAdapter(context: Context, private var list: List<BTData>): ArrayAdapter<BTSpecifyAdapter.BTData>(context, 0, list) {
    data class BTData(var mac: String, var time: String, val message: String)

    private class ViewHolder(v: View) {
        val tvStatus: TextView = v.findViewById<TextView>(R.id.tvStatus)
        val tvAddress: TextView = v.findViewById<TextView>(R.id.tvAddress)
        val tvService: TextView = v.findViewById<TextView>(R.id.tvService)
        val tvTime: TextView = v.findViewById<TextView>(R.id.tvTime)
        val tvUuid: TextView = v.findViewById<TextView>(R.id.uuid)
        val llTest: LinearLayout = v.findViewById<LinearLayout>(R.id.llTest)
    }

    override fun getCount(): Int {
        return list.size
    }

    @SuppressLint("InflateParams", "MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = ItemServiceDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        val item = getItem(count - position - 1) ?: return view

        holder.tvAddress.text = item.mac
        holder.tvTime.text = item.time
        holder.tvService.text = item.message
        holder.tvStatus.visibility = View.GONE
        holder.tvUuid.visibility = View.GONE

        return view
    }
}