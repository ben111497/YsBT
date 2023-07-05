package com.ys.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.ys.bt.databinding.ItemBluetoothBinding

class BTListAdapter(private var context: Context, private var list: ArrayList<BluetoothDevice>): ArrayAdapter<BluetoothDevice>(context, 0, list) {
    private var listener: BTListClickListener? = null
    interface BTListClickListener {
        fun onClick(device: BluetoothDevice)
    }

    fun setListener(l: BTListClickListener) { listener = l }

    private class ViewHolder(v: View) {
        val deviceName: TextView = v.findViewById<TextView>(R.id.deviceName)
        val uuid: TextView = v.findViewById<TextView>(R.id.uuid)
        val address: TextView = v.findViewById<TextView>(R.id.address)
        val tvConnect: TextView = v.findViewById<TextView>(R.id.tvConnect)
    }

    override fun getCount(): Int {
        return list.size
    }

    @SuppressLint("InflateParams", "MissingPermission")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = ItemBluetoothBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        val item = getItem(position) ?: return view
        holder.tvConnect.setOnClickListener { listener?.onClick(item) }
        holder.deviceName.text = item.name ?: "N/A"
        holder.uuid.text = if (item.uuids.isNullOrEmpty()) "NOT BONDED" else "${item.uuids[0]}"
        holder.address.text = item.address ?: "-"

        return view
    }
}