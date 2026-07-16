package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditOrderActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var lineListView: ListView
    private var families: List<ItemFamily> = emptyList()
    private val orderLines = mutableListOf<OrderLine>()
    private var orderId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_order)
        db = DBHelper(this)

        orderId = intent.getLongExtra("order_id", -1)
        val header = db.getOrderHeader(orderId)
        findViewById<TextView>(R.id.editOrderDealerText).text = "Dealer: ${header?.dealerName ?: ""}"

        lineListView = findViewById(R.id.lineListView)
        families = db.getFamilies()
        orderLines.addAll(db.getOrderLines(orderId))

        if (families.isEmpty()) Toast.makeText(this, "Add items first", Toast.LENGTH_LONG).show()

        findViewById<Button>(R.id.btnAddLine).setOnClickListener { showAddLineDialog() }
        findViewById<Button>(R.id.btnSaveOrder).setOnClickListener { saveChanges() }

        refreshLines()
    }

    private fun refreshLines() {
        val rows = orderLines.mapIndexed { index, line ->
            RowData(index.toLong(), "${line.itemName} - ${line.variant} - ${line.color} x${line.qty}")
        }.toMutableList()
        lineListView.adapter = GenericAdapter(this, rows) { row ->
            orderLines.removeAt(row.id.toInt())
            refreshLines()
        }
    }

    private fun showAddLineDialog() {
        if (families.isEmpty()) {
            Toast.makeText(this, "No items available", Toast.LENGTH_SHORT).show()
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_order_line, null)
        val brandSpinner = view.findViewById<Spinner>(R.id.lineBrandSpinner)
        val familySpinner = view.findViewById<Spinner>(R.id.lineItemSpinner)
        val variantSpinner = view.findViewById<Spinner>(R.id.lineVariantSpinner)
        val colorSpinner = view.findViewById<Spinner>(R.id.lineColorSpinner)
        val qtyEt = view.findViewById<EditText>(R.id.lineQty)

        val brandOptions = listOf("All Brands") + families.map { if (it.brand.isBlank()) "Unassigned" else it.brand }.distinct().sorted()
        brandSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brandOptions)

        var filteredFamilies: List<ItemFamily> = families

        fun updateVariantColor(pos: Int) {
            if (filteredFamilies.isEmpty()) return
            val family = filteredFamilies[pos]
            val variants = db.getVariants(family.id).map { it.text }
            val colors = db.getColors(family.id).map { it.text }
            variantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (variants.isEmpty()) listOf("-") else variants)
            colorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (colors.isEmpty()) listOf("-") else colors)
        }

        fun updateFamilySpinner(brandLabel: String) {
            filteredFamilies = if (brandLabel == "All Brands") {
                families
            } else {
                families.filter { (if (it.brand.isBlank()) "Unassigned" else it.brand) == brandLabel }
            }
            familySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filteredFamilies.map { it.name })
            if (filteredFamilies.isNotEmpty()) updateVariantColor(0)
        }
        updateFamilySpinner("All Brands")

        brandSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateFamilySpinner(brandOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        familySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateVariantColor(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Add Item to Order")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                if (filteredFamilies.isEmpty()) {
                    Toast.makeText(this, "No items in this brand", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val itemName = familySpinner.selectedItem?.toString() ?: return@setPositiveButton
                val variant = variantSpinner.selectedItem?.toString() ?: "-"
                val color = colorSpinner.selectedItem?.toString() ?: "-"
                val qty = qtyEt.text.toString().trim().toIntOrNull() ?: 1
                orderLines.add(OrderLine(itemName, variant, color, qty))
                refreshLines()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChanges() {
        if (orderLines.isEmpty()) {
            Toast.makeText(this, "Order must have at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        db.updateOrderLines(orderId, orderLines)
        Toast.makeText(this, "Order updated", Toast.LENGTH_SHORT).show()
        finish()
    }
}
