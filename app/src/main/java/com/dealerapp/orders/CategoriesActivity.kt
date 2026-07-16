package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Category"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val categories = db.getCategories().toMutableList()
        listView.adapter = GenericAdapter(this, categories) { row ->
            db.deleteCategory(row.id)
            Toast.makeText(this, "Category removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val et = EditText(this)
        et.hint = "Category name (e.g. Wearables)"
        val padding = (16 * resources.displayMetrics.density).toInt()
        et.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addCategory(name)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
