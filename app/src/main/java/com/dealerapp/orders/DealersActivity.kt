package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DealersActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Dealer"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val dealers = db.getDealers()
        val rows = dealers.map { RowData(it.id, "${it.name}\n${it.location} | ${it.mobile}") }.toMutableList()
        listView.adapter = GenericAdapter(this, rows) { row ->
            db.deleteDealer(row.id)
            Toast.makeText(this, "Dealer removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_dealer, null)
        val nameEt = view.findViewById<EditText>(R.id.dealerName)
        val locEt = view.findViewById<EditText>(R.id.dealerLocation)
        val mobEt = view.findViewById<EditText>(R.id.dealerMobile)

        AlertDialog.Builder(this)
            .setTitle("Add Dealer")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val loc = locEt.text.toString().trim()
                val mob = mobEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addDealer(name, loc, mob)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
