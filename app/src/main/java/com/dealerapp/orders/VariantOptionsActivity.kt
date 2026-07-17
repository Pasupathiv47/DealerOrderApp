package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VariantOptionsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView
    private val typeLabels = listOf("RAM + Storage", "Size (e.g. 32 inch)")
    private val typeValues = listOf("ram_storage", "size")

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
        val options = db.getVariantOptions().map { v ->
            val typeLabel = if (v.type == "size") "Size" else "RAM + Storage"
            RowData(v.id, "${v.text}  —  $typeLabel")
        }.toMutableList()
        listView.adapter = GenericAdapter(this, options) { row ->
            db.deleteVariantOption(row.id)
            Toast.makeText(this, "Variant option removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_variant_option, null)
        val et = view.findViewById<EditText>(R.id.variantOptionInput)
        val typeSpinner = view.findViewById<Spinner>(R.id.variantOptionTypeSpinner)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeLabels)

        AlertDialog.Builder(this)
            .setTitle("Add Variant Option")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                val type = typeValues[typeSpinner.selectedItemPosition]
                if (name.isEmpty()) {
                    Toast.makeText(this, "Variant text required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addVariantOption(name, type)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
