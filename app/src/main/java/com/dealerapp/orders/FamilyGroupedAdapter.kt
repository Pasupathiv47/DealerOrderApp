package com.dealerapp.orders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class FamilyGroupedAdapter(
    private val context: Context,
    private val rows: List<GroupRow>,
    private val onOpen: (ItemFamily) -> Unit,
    private val onMove: (ItemFamily) -> Unit,
    private val onDelete: (ItemFamily) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = rows.size
    override fun getItem(position: Int): Any = rows[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getViewTypeCount(): Int = 2
    override fun getItemViewType(position: Int): Int = if (rows[position] is HeaderRow) 0 else 1
    override fun isEnabled(position: Int): Boolean = rows[position] !is HeaderRow
    override fun areAllItemsEnabled(): Boolean = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val row = rows[position]) {
            is HeaderRow -> {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_header, parent, false)
                view.findViewById<TextView>(R.id.headerText).text = row.title
                view
            }
            is FamilyRow -> {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_family, parent, false)
                val family = row.family
                val catLabel = if (family.category.isBlank()) "No Category" else family.category
                view.findViewById<TextView>(R.id.familyRowText).text = "${family.name} ($catLabel)"
                view.findViewById<Button>(R.id.familyMoveBtn).setOnClickListener { onMove(family) }
                view.findViewById<Button>(R.id.familyDeleteBtn).setOnClickListener { onDelete(family) }
                view.setOnClickListener { onOpen(family) }
                view
            }
            else -> convertView ?: View(context)
        }
    }
}
