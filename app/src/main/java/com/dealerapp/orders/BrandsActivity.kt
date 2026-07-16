package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BrandsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Brand"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val brands = db.getBrands().toMutableList()
        listView.adapter = GenericAdapter(this, brands) { row ->
            db.deleteBrand(row.id)
            Toast.makeText(this, "Brand removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val et = EditText(this)
        et.hint = "Brand name (e.g. Xiaomi)"
        val padding = (16 * resources.displayMetrics.density).toInt()
        et.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Add Brand")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Brand name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addBrand(name)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
