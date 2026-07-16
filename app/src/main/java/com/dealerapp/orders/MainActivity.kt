package com.dealerapp.orders

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnDealers).setOnClickListener {
            startActivity(Intent(this, DealersActivity::class.java))
        }
        findViewById<Button>(R.id.btnItems).setOnClickListener {
            startActivity(Intent(this, ItemsActivity::class.java))
        }
        findViewById<Button>(R.id.btnNewOrder).setOnClickListener {
            startActivity(Intent(this, NewOrderActivity::class.java))
        }
        findViewById<Button>(R.id.btnOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }
    }
}
