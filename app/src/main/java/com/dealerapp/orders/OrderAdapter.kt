package com.dealerapp.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class OrderAdapter(
    context: Context,
    private val db: DBHelper,
    private val orders: MutableList<OrderSummary>,
    private val onDelete: (OrderSummary) -> Unit
) : ArrayAdapter<OrderSummary>(context, 0, orders) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_order, parent, false)
        val order = orders[position]
        view.findViewById<TextView>(R.id.orderRowText).text = "Order #${order.id} - ${order.dealerName}\n${order.date}"

        view.findViewById<Button>(R.id.orderRowCopyBtn).setOnClickListener {
            val text = OrderUtils.buildOrderText(db, order.id)
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Order", text))
            Toast.makeText(context, "Order #${order.id} copied", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.orderRowEditBtn).setOnClickListener {
            val intent = Intent(context, EditOrderActivity::class.java)
            intent.putExtra("order_id", order.id)
            context.startActivity(intent)
        }

        view.findViewById<Button>(R.id.orderRowDeleteBtn).setOnClickListener {
            onDelete(order)
        }

        return view
    }
}
