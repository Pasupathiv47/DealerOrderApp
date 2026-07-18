package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VariantGroupsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Variant Type"
            setOnClickListener { showAddDialog() }
        }
        refresh()
    }

    private fun refresh() {
        val groups = db.getVariantGroups().toMutableList()
        listView.adapter = GenericAdapter(this, groups) { row ->
            AlertDialog.Builder(this)
                .setTitle("Delete Variant Type")
                .setMessage("Delete \"${row.text}\"? Any variant options under it will also be removed, and categories using it will switch to No Variants.")
                .setPositiveButton("Delete") { _, _ ->
                    db.deleteVariantGroup(row.id)
                    Toast.makeText(this, "Variant type removed", Toast.LENGTH_SHORT).show()
                    refresh()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAddDialog() {
        val et = EditText(this)
        et.hint = "Type name (e.g. Voltage, Weight, Length)"
        val padding = (16 * resources.displayMetrics.density).toInt()
        et.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Add Variant Type")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Type name required", Toast.LENGTH_SHORT).show()
                } else if (db.getVariantGroupByName(name) != null) {
                    Toast.makeText(this, "This type already exists", Toast.LENGTH_SHORT).show()
                } else {
                    db.addVariantGroup(name)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
