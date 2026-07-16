package com.dealerapp.orders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class DealerGroupedAdapter(
    private val context: Context,
    private val rows: List<GroupRow>,
    private val onMove: (Dealer) -> Unit,
    private val onDelete: (Dealer) -> Unit
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
            is DealerRow -> {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_dealer, parent, false)
                val dealer = row.dealer
                view.findViewById<TextView>(R.id.dealerRowText).text = "${dealer.name}\n${dealer.mobile}"
                view.findViewById<Button>(R.id.dealerMoveBtn).setOnClickListener { onMove(dealer) }
                view.findViewById<Button>(R.id.dealerDeleteBtn).setOnClickListener { onDelete(dealer) }
                view
            }
            else -> convertView ?: View(context)
        }
    }
}
