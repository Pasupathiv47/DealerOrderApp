package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FamilyDetailActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private var familyId: Long = -1
    private lateinit var variantList: ListView
    private lateinit var colorList: ListView
    private lateinit var variantSpinner: Spinner
    private lateinit var titleText: TextView
    private lateinit var variantSectionLabel: TextView
    private lateinit var variantHeaderRow: View
    private lateinit var variantAddRow: View
    private lateinit var noVariantsNote: TextView
    private var currentGroupId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_detail)
        db = DBHelper(this)
        familyId = intent.getLongExtra("family_id", -1)

        titleText = findViewById(R.id.familyTitle)
        variantList = findViewById(R.id.variantListView)
        colorList = findViewById(R.id.colorListView)
        variantSpinner = findViewById(R.id.variantSpinner)
        variantSectionLabel = findViewById(R.id.variantSectionLabel)
        variantHeaderRow = findViewById(R.id.variantHeaderRow)
        variantAddRow = findViewById(R.id.variantAddRow)
        noVariantsNote = findViewById(R.id.noVariantsNote)

        updateTitle()
        setupVariantSection()

        findViewById<Button>(R.id.btnChangeCategory).setOnClickListener { showChangeCategoryDialog() }

        findViewById<Button>(R.id.btnManageVariantOptions).setOnClickListener {
            startActivity(Intent(this, VariantOptionsActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddVariant).setOnClickListener {
            if (variantSpinner.adapter == null || variantSpinner.adapter.count == 0) {
                Toast.makeText(this, "Add a variant option first via Manage Variants", Toast.LENGTH_SHORT).show()
            } else {
                val text = variantSpinner.selectedItem.toString()
                showPriceDialog(null, text)
            }
        }

        findViewById<Button>(R.id.btnAddColor).setOnClickListener {
            val et = findViewById<EditText>(R.id.colorInput)
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                db.addColor(familyId, text)
                et.setText("")
                refresh()
            }
        }

        findViewById<Button>(R.id.btnAddAnyColour).setOnClickListener {
            db.addColor(familyId, "Any Colour")
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTitle()
        setupVariantSection()
    }

    private fun updateTitle() {
        val family = db.getFamily(familyId)
        val brandLabel = if (family?.brand.isNullOrBlank()) "Unassigned" else family?.brand
        val catLabel = if (family?.category.isNullOrBlank()) "No Category" else family?.category
        titleText.text = "${family?.name ?: ""}\nCategory: $catLabel\nBrand: $brandLabel"
    }

    private fun setupVariantSection() {
        val family = db.getFamily(familyId)
        val catDetail = family?.let { db.getCategoryByName(it.category) }
        currentGroupId = catDetail?.variantGroupId

        if (currentGroupId == null) {
            variantHeaderRow.visibility = View.GONE
            variantAddRow.visibility = View.GONE
            noVariantsNote.visibility = View.VISIBLE
            noVariantsNote.text = "This category has no variants. Set the price for this item below."
            if (db.getVariantDetails(familyId).isEmpty()) {
                db.addVariant(familyId, "Standard", 0.0, 0.0)
            }
        } else {
            variantHeaderRow.visibility = View.VISIBLE
            variantAddRow.visibility = View.VISIBLE
            noVariantsNote.visibility = View.GONE
            variantSectionLabel.text = "${catDetail?.variantGroupName ?: "Variant"} Options"
            loadVariantOptions(currentGroupId!!)
        }
        refresh()
    }

    private fun loadVariantOptions(groupId: Long) {
        val options = db.getVariantOptions(groupId).map { it.text }
        variantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
    }

    private fun showChangeCategoryDialog() {
        val categories = db.getCategories().map { it.name }
        if (categories.isEmpty()) {
            Toast.makeText(this, "Add a category first via Manage Categories", Toast.LENGTH_SHORT).show()
            return
        }
        val family = db.getFamily(familyId)
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val currentIndex = categories.indexOf(family?.category)
        if (currentIndex >= 0) spinner.setSelection(currentIndex)
        val padding = (16 * resources.displayMetrics.density).toInt()
        spinner.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Change Category")
            .setView(spinner)
            .setPositiveButton("Save") { _, _ ->
                val selected = spinner.selectedItem?.toString() ?: return@setPositiveButton
                db.updateFamilyCategory(familyId, selected)
                updateTitle()
                setupVariantSection()
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPriceDialog(existing: VariantDetail?, variantText: String) {
        val view = layoutInflater.inflate(R.layout.dialog_variant_price, null)
        val label = view.findViewById<TextView>(R.id.priceDialogLabel)
        val mopEt = view.findViewById<EditText>(R.id.mopInput)
        val dpEt = view.findViewById<EditText>(R.id.dpInput)
        label.text = "Set price for $variantText"
        if (existing != null) {
            mopEt.setText(if (existing.mop == 0.0) "" else existing.mop.toString())
            dpEt.setText(if (existing.dp == 0.0) "" else existing.dp.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Variant" else "Edit Price")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val mop = mopEt.text.toString().trim().toDoubleOrNull() ?: 0.0
                val dp = dpEt.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (existing == null) {
                    db.addVariant(familyId, variantText, mop, dp)
                } else {
                    db.updateVariantPrice(existing.id, mop, dp)
                }
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refresh() {
        val variants = db.getVariantDetails(familyId).toMutableList()
        variantList.adapter = VariantAdapter(
            this, variants,
            onEdit = { v -> showPriceDialog(v, v.text) },
            onDelete = { v ->
                if (currentGroupId == null) {
                    Toast.makeText(this, "This category doesn't use variants", Toast.LENGTH_SHORT).show()
                } else {
                    db.deleteVariant(v.id)
                    Toast.makeText(this, "Variant removed", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        )

        val colors = db.getColors(familyId).toMutableList()
        colorList.adapter = GenericAdapter(this, colors) { row ->
            db.deleteColor(row.id)
            Toast.makeText(this, "Colour removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }
}
