package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
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
            text = "Add Item Family"
            setOnClickListener { showAddDialog() }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val families = db.getFamilies()
        val rows = families.map { RowData(it.id, "${it.name} (${it.category})\nTap to manage variants & colours") }.toMutableList()
        listView.adapter = GenericAdapter(this, rows) { row ->
            db.deleteFamily(row.id)
            Toast.makeText(this, "Item family removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val family = families[position]
            val intent = Intent(this, FamilyDetailActivity::class.java)
            intent.putExtra("family_id", family.id)
            startActivity(intent)
        }
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val nameEt = view.findViewById<EditText>(R.id.itemName)
        val catSpinner = view.findViewById<Spinner>(R.id.itemCategory)
        catSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        AlertDialog.Builder(this)
            .setTitle("Add Item Family")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val category = catSpinner.selectedItem?.toString() ?: categories[0]
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                } else {
                    val id = db.addFamily(name, category)
                    val intent = Intent(this, FamilyDetailActivity::class.java)
                    intent.putExtra("family_id", id)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
