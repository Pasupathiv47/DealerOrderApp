package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
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
        val rows = mutableListOf<GroupRow>()
        var lastBrand: String? = null
        for (f in families) {
            val label = if (f.brand.isBlank()) "Unassigned Brand" else f.brand
            if (label != lastBrand) {
                rows.add(HeaderRow(label))
                lastBrand = label
            }
            rows.add(FamilyRow(f))
        }
        listView.adapter = FamilyGroupedAdapter(
            this, rows,
            onOpen = { family ->
                val intent = Intent(this, FamilyDetailActivity::class.java)
                intent.putExtra("family_id", family.id)
                startActivity(intent)
            },
            onMove = { family -> showMoveBrandDialog(family) },
            onDelete = { family ->
                db.deleteFamily(family.id)
                Toast.makeText(this, "Item family removed", Toast.LENGTH_SHORT).show()
                refresh()
            }
        )
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val nameEt = view.findViewById<EditText>(R.id.itemName)
        val catSpinner = view.findViewById<Spinner>(R.id.itemCategory)
        val brandEt = view.findViewById<AutoCompleteTextView>(R.id.itemBrand)
        catSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        brandEt.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, db.getDistinctBrands()))

        AlertDialog.Builder(this)
            .setTitle("Add Item Family")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val category = catSpinner.selectedItem?.toString() ?: categories[0]
                val brand = brandEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                } else {
                    val id = db.addFamily(name, category, brand)
                    val intent = Intent(this, FamilyDetailActivity::class.java)
                    intent.putExtra("family_id", id)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveBrandDialog(family: ItemFamily) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_move_group, null)
        val label = view.findViewById<TextView>(R.id.moveLabel)
        val input = view.findViewById<AutoCompleteTextView>(R.id.moveInput)
        label.text = "Move \"${family.name}\" to which brand?"
        input.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, db.getDistinctBrands()))
        input.setText(family.brand)

        AlertDialog.Builder(this)
            .setTitle("Move Item")
            .setView(view)
            .setPositiveButton("Move") { _, _ ->
                val newBrand = input.text.toString().trim()
                db.updateFamilyBrand(family.id, newBrand)
                Toast.makeText(this, "Moved to ${if (newBrand.isBlank()) "Unassigned Brand" else newBrand}", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
