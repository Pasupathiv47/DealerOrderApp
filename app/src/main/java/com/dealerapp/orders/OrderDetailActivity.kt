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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        val db = DBHelper(this)
        val orderId = intent.getLongExtra("order_id", -1)
        val header = db.getOrderHeader(orderId)
        val lines = db.getOrderLines(orderId)

        val text = buildString {
            appendLine("ORDER #$orderId")
            appendLine("Dealer: ${header?.dealerName ?: ""}")
            appendLine("Location: ${header?.location ?: ""}")
            appendLine("Mobile: ${header?.mobile ?: ""}")
            appendLine("Date: ${header?.date ?: ""}")
            appendLine()
            appendLine("Items:")
            lines.forEachIndexed { i, line ->
                appendLine("${i + 1}. ${line.itemName} - ${line.variant} - ${line.color} x ${line.qty}")
            }
        }

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

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            db.deleteOrder(orderId)
            Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
