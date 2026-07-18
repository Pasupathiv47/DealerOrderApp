package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
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
    private lateinit var groupFilterSpinner: Spinner
    private var groups: List<RowData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_variant_options)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        groupFilterSpinner = findViewById(R.id.groupFilterSpinner)

        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Variant"
            setOnClickListener { showAddDialog() }
        }
        findViewById<Button>(R.id.btnSecondAction).apply {
            text = "Manage Variant Types"
            setOnClickListener { startActivity(Intent(this@VariantOptionsActivity, VariantGroupsActivity::class.java)) }
        }

        groupFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                refreshList()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        groups = db.getVariantGroups()
        if (groups.isEmpty()) {
            Toast.makeText(this, "No variant types yet. Tap Manage Variant Types to create one (e.g. RAM + Storage).", Toast.LENGTH_LONG).show()
        }
        val previousSelection = groupFilterSpinner.selectedItem?.toString()
        groupFilterSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groups.map { it.text })
        val idx = groups.map { it.text }.indexOf(previousSelection)
        if (idx >= 0) groupFilterSpinner.setSelection(idx)
        refreshList()
    }

    private fun refreshList() {
        if (groups.isEmpty()) {
            listView.adapter = null
            return
        }
        val pos = groupFilterSpinner.selectedItemPosition
        if (pos < 0 || pos >= groups.size) return
        val selectedGroup = groups[pos]
        val options = db.getVariantOptions(selectedGroup.id).toMutableList()
        listView.adapter = GenericAdapter(this, options) { row ->
            db.deleteVariantOption(row.id)
            Toast.makeText(this, "Variant option removed", Toast.LENGTH_SHORT).show()
            refreshList()
        }
    }

    private fun showAddDialog() {
        if (groups.isEmpty()) {
            Toast.makeText(this, "Create a variant type first via Manage Variant Types", Toast.LENGTH_SHORT).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_add_variant_option, null)
        val et = view.findViewById<EditText>(R.id.variantOptionInput)
        val typeSpinner = view.findViewById<Spinner>(R.id.variantOptionTypeSpinner)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groups.map { it.text })
        val currentFilterPos = groupFilterSpinner.selectedItemPosition
        if (currentFilterPos >= 0) typeSpinner.setSelection(currentFilterPos)

        AlertDialog.Builder(this)
            .setTitle("Add Variant Option")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = et.text.toString().trim()
                val groupId = groups[typeSpinner.selectedItemPosition].id
                if (name.isEmpty()) {
                    Toast.makeText(this, "Variant text required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addVariantOption(groupId, name)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
