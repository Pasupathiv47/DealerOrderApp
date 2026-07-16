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

class FamilyDetailActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private var familyId: Long = -1
    private lateinit var variantList: ListView
    private lateinit var colorList: ListView
    private lateinit var variantSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_detail)
        db = DBHelper(this)
        familyId = intent.getLongExtra("family_id", -1)
        val family = db.getFamily(familyId)

        val brandLabel = if (family?.brand.isNullOrBlank()) "Unassigned" else family?.brand
        findViewById<TextView>(R.id.familyTitle).text = "${family?.name ?: ""} (${family?.category ?: ""})\nBrand: $brandLabel"

        variantList = findViewById(R.id.variantListView)
        colorList = findViewById(R.id.colorListView)
        variantSpinner = findViewById(R.id.variantSpinner)

        loadVariantOptions()

        findViewById<Button>(R.id.btnManageVariantOptions).setOnClickListener {
            startActivity(Intent(this, VariantOptionsActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddVariant).setOnClickListener {
            if (variantSpinner.adapter == null || variantSpinner.adapter.count == 0) {
                Toast.makeText(this, "Add a variant option first via Manage Variants", Toast.LENGTH_SHORT).show()
            } else {
                val text = variantSpinner.selectedItem.toString()
                db.addVariant(familyId, text)
                refresh()
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

        refresh()
    }

    override fun onResume() {
        super.onResume()
        loadVariantOptions()
    }

    private fun loadVariantOptions() {
        val options = db.getVariantOptions().map { it.text }
        variantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
    }

    private fun refresh() {
        val variants = db.getVariants(familyId).toMutableList()
        variantList.adapter = GenericAdapter(this, variants) { row ->
            db.deleteVariant(row.id)
            Toast.makeText(this, "Variant removed", Toast.LENGTH_SHORT).show()
            refresh()
        }

        val colors = db.getColors(familyId).toMutableList()
        colorList.adapter = GenericAdapter(this, colors) { row ->
            db.deleteColor(row.id)
            Toast.makeText(this, "Colour removed", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }
}
