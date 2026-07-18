package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Category"
            setOnClickListener { showAddDialog() }
        }
        findViewById<Button>(R.id.btnSecondAction).apply {
            text = "Manage Variant Types"
            visibility = android.view.View.VISIBLE
            setOnClickListener { startActivity(Intent(this@CategoriesActivity, VariantGroupsActivity::class.java)) }
        }
    }

    override fun onResume() {
        super.onResume()
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

    private fun typeOptions(): List<String> = listOf("No Variants") + db.getVariantGroups().map { it.text }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val nameEt = view.findViewById<EditText>(R.id.categoryName)
        val typeSpinner = view.findViewById<Spinner>(R.id.categoryTypeSpinner)
        val options = typeOptions()
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val selected = typeSpinner.selectedItem?.toString() ?: "No Variants"
                if (name.isEmpty()) {
                    Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show()
                } else {
                    val groupId = if (selected == "No Variants") null else db.getVariantGroupByName(selected)?.id
                    db.addCategory(name, groupId)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTypeDialog(cat: CategoryDetail) {
        val options = typeOptions()
        if (options.size <= 1) {
            Toast.makeText(this, "No variant types yet. Create one via Manage Variant Types.", Toast.LENGTH_LONG).show()
        }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val currentIdx = options.indexOf(cat.variantGroupName)
        if (currentIdx >= 0) spinner.setSelection(currentIdx)
        val padding = (16 * resources.displayMetrics.density).toInt()
        spinner.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Change Variant Type for \"${cat.name}\"")
            .setView(spinner)
            .setPositiveButton("Save") { _, _ ->
                val selected = spinner.selectedItem?.toString() ?: "No Variants"
                val groupId = if (selected == "No Variants") null else db.getVariantGroupByName(selected)?.id
                db.updateCategoryGroup(cat.id, groupId)
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
