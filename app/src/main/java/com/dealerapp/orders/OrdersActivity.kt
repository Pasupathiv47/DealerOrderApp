package com.dealerapp.orders

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OrdersActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "New Order"
            setOnClickListener { startActivity(Intent(this@OrdersActivity, NewOrderActivity::class.java)) }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val orders = db.getOrders().toMutableList()
        listView.adapter = OrderAdapter(this, db, orders) { order ->
            db.deleteOrder(order.id)
            Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show()
            refresh()
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val order = orders[position]
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("order_id", order.id)
            startActivity(intent)
        }
    }
}
