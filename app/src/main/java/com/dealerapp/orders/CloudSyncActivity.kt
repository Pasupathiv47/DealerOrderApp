package com.dealerapp.orders

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CloudSyncActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_sync)
        db = DBHelper(this)
        statusText = findViewById(R.id.syncStatusText)

        findViewById<Button>(R.id.btnPushCatalog).setOnClickListener {
            statusText.text = "Pushing catalog to cloud..."
            FirestoreSync.pushCatalog(db) { success, message ->
                runOnUiThread {
                    statusText.text = message
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<Button>(R.id.btnPullPriceStock).setOnClickListener {
            statusText.text = "Pulling prices and stock from cloud..."
            FirestoreSync.pullPriceStock(db) { success, message ->
                runOnUiThread {
                    statusText.text = message
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
