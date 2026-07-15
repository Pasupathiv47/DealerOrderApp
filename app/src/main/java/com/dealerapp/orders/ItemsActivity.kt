package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ItemsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView
    private val categories = listOf("Smartphones", "Tabs", "Accessories", "TV", "Home Appliances")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Item"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val items = db.getItems()
        val rows = items.map { RowData(it.id, "${it.name} (${it.category})\nVariants: ${it.variants}\nColors: ${it.colors}") }.toMutableList()
        listView.adapter = GenericAdapter(this, rows) { row ->
            db.deleteItem(row.id)
            Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val nameEt = view.findViewById<EditText>(R.id.itemName)
        val catSpinner = view.findViewById<Spinner>(R.id.itemCategory)
        val variantsEt = view.findViewById<EditText>(R.id.itemVariants)
        val colorsEt = view.findViewById<EditText>(R.id.itemColors)

        catSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        AlertDialog.Builder(this)
            .setTitle("Add Item")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val category = catSpinner.selectedItem?.toString() ?: categories[0]
                val variants = variantsEt.text.toString().trim()
                val colors = colorsEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Item name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addItem(name, category, variants, colors)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
