package com.dealerapp.orders

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FamilyDetailActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private var familyId: Long = -1
    private lateinit var variantList: ListView
    private lateinit var colorList: ListView

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

        findViewById<Button>(R.id.btnAddVariant).setOnClickListener {
            val et = findViewById<EditText>(R.id.variantInput)
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                db.addVariant(familyId, text)
                et.setText("")
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
