package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView
    private val typeLabels = listOf("RAM + Storage", "Size (e.g. 32 inch, 43 inch)", "No Variants")
    private val typeValues = listOf("ram_storage", "size", "none")

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
        listView.adapter = CategoryAdapter(
            this, categories,
            onEdit = { cat -> showEditTypeDialog(cat) },
            onDelete = { cat ->
                db.deleteCategory(cat.id)
                Toast.makeText(this, "Category removed", Toast.LENGTH_SHORT).show()
                refresh()
            }
        )
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val nameEt = view.findViewById<EditText>(R.id.categoryName)
        val typeSpinner = view.findViewById<Spinner>(R.id.categoryTypeSpinner)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeLabels)

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val typeValue = typeValues[typeSpinner.selectedItemPosition]
                if (name.isEmpty()) {
                    Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addCategory(name, typeValue)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTypeDialog(cat: CategoryDetail) {
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeLabels)
        val currentIdx = typeValues.indexOf(cat.variantType)
        if (currentIdx >= 0) spinner.setSelection(currentIdx)
        val padding = (16 * resources.displayMetrics.density).toInt()
        spinner.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Change Variant Type for \"${cat.name}\"")
            .setView(spinner)
            .setPositiveButton("Save") { _, _ ->
                val newType = typeValues[spinner.selectedItemPosition]
                db.updateCategoryType(cat.id, newType)
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
