package com.dealerapp.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OrderDetailActivity : AppCompatActivity() {
    private var orderId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
        orderId = intent.getLongExtra("order_id", -1)
        loadDetails()
    }

    override fun onResume() {
        super.onResume()
        loadDetails()
    }

    private fun loadDetails() {
        val db = DBHelper(this)
        val text = OrderUtils.buildOrderText(db, orderId)
        findViewById<TextView>(R.id.orderDetailText).text = text

        findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Order", text))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(shareIntent, "Share Order"))
        }

        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            val intent = Intent(this, EditOrderActivity::class.java)
            intent.putExtra("order_id", orderId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            db.deleteOrder(orderId)
            Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
