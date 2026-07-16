package com.dealerapp.orders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

class VariantAdapter(
    context: Context,
    private val items: MutableList<VariantDetail>,
    private val onEdit: (VariantDetail) -> Unit,
    private val onDelete: (VariantDetail) -> Unit
) : ArrayAdapter<VariantDetail>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_variant, parent, false)
        val item = items[position]
        view.findViewById<TextView>(R.id.variantRowText).text =
            "${item.text}\nMOP: ₹${"%.2f".format(item.mop)}   DP: ₹${"%.2f".format(item.dp)}"
        view.findViewById<Button>(R.id.variantEditBtn).setOnClickListener { onEdit(item) }
        view.findViewById<Button>(R.id.variantDeleteBtn).setOnClickListener { onDelete(item) }
        return view
    }
}
