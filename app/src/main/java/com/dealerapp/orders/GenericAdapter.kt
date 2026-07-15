package com.dealerapp.orders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

class GenericAdapter(
    context: Context,
    private val items: MutableList<RowData>,
    private val onDelete: (RowData) -> Unit
) : ArrayAdapter<RowData>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_generic, parent, false)
        val item = items[position]
        view.findViewById<TextView>(R.id.rowText).text = item.text
        view.findViewById<Button>(R.id.rowDeleteBtn).setOnClickListener { onDelete(item) }
        return view
    }
}
