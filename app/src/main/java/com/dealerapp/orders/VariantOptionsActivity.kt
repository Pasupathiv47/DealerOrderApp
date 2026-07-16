package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VariantOptionsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Variant"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val options = db.getVariantOptions().toMutableList()
        listView.adapter = GenericAdapter(this, options) { row ->
            db.deleteVariantOption(row.id)
            Toast.makeText(this, "Variant option removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val et = EditText(this)
        et.hint = "Variant (e.g. 8GB+128GB)"
        val padding = (16 * resources.displayMetrics.density).toInt()
        et.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Add Variant Option")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Variant text required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addVariantOption(name)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
