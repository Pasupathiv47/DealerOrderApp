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
    private lateinit var totalText: TextView
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
        totalText = findViewById(R.id.totalText)
        families = db.getFamilies()
        orderLines.addAll(db.getOrderLines(orderId))

        if (families.isEmpty()) Toast.makeText(this, "Add items first", Toast.LENGTH_LONG).show()

        findViewById<Button>(R.id.btnAddLine).setOnClickListener { showAddLineDialog() }
        findViewById<Button>(R.id.btnSaveOrder).setOnClickListener { saveChanges() }

        refreshLines()
    }

    private fun refreshLines() {
        val rows = orderLines.mapIndexed { index, line ->
            RowData(index.toLong(), "${line.itemName} - ${line.variant} - ${line.color} x${line.qty}  (₹${"%.2f".format(line.dpPrice * line.qty)})")
        }.toMutableList()
        lineListView.adapter = GenericAdapter(this, rows) { row ->
            orderLines.removeAt(row.id.toInt())
            refreshLines()
        }
        val total = orderLines.sumOf { it.dpPrice * it.qty }
        totalText.text = "Total (DP): ₹${"%.2f".format(total)}"
    }

    private fun showAddLineDialog() {
        if (families.isEmpty()) {
            Toast.makeText(this, "No items available", Toast.LENGTH_SHORT).show()
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_order_line, null)
        val categorySpinner = view.findViewById<Spinner>(R.id.lineCategorySpinner)
        val brandSpinner = view.findViewById<Spinner>(R.id.lineBrandSpinner)
        val familySpinner = view.findViewById<Spinner>(R.id.lineItemSpinner)
        val variantSpinner = view.findViewById<Spinner>(R.id.lineVariantSpinner)
        val priceInfo = view.findViewById<TextView>(R.id.linePriceInfo)
        val colorSpinner = view.findViewById<Spinner>(R.id.lineColorSpinner)
        val qtyEt = view.findViewById<EditText>(R.id.lineQty)
        val addedCountText = view.findViewById<TextView>(R.id.lineAddedCount)

        val categoryOptions = listOf("All Categories") + families.map { if (it.category.isBlank()) "No Category" else it.category }.distinct().sorted()
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)

        var categoryFiltered: List<ItemFamily> = families
        var filteredFamilies: List<ItemFamily> = families
        var currentVariantDetails: List<VariantDetail> = emptyList()
        var addedInThisSession = 0

        fun updatePriceInfo(pos: Int) {
            if (currentVariantDetails.isEmpty() || pos < 0 || pos >= currentVariantDetails.size) {
                priceInfo.text = ""
                return
            }
            val v = currentVariantDetails[pos]
            priceInfo.text = "MOP: ₹${"%.2f".format(v.mop)}   DP: ₹${"%.2f".format(v.dp)}"
        }

        fun updateVariantColor(pos: Int) {
            if (filteredFamilies.isEmpty()) return
            val family = filteredFamilies[pos]
            currentVariantDetails = db.getVariantDetails(family.id)
            val variantTexts = currentVariantDetails.map { it.text }
            val colors = db.getColors(family.id).map { it.text }
            variantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (variantTexts.isEmpty()) listOf("-") else variantTexts)
            colorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (colors.isEmpty()) listOf("-") else colors)
            updatePriceInfo(0)
        }

        fun updateFamilySpinner(brandLabel: String) {
            filteredFamilies = if (brandLabel == "All Brands") {
                categoryFiltered
            } else {
                categoryFiltered.filter { (if (it.brand.isBlank()) "Unassigned" else it.brand) == brandLabel }
            }
            familySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filteredFamilies.map { it.name })
            if (filteredFamilies.isNotEmpty()) updateVariantColor(0)
        }

        fun updateBrandSpinner(categoryLabel: String) {
            categoryFiltered = if (categoryLabel == "All Categories") {
                families
            } else {
                families.filter { (if (it.category.isBlank()) "No Category" else it.category) == categoryLabel }
            }
            val brandOptions = listOf("All Brands") + categoryFiltered.map { if (it.brand.isBlank()) "Unassigned" else it.brand }.distinct().sorted()
            brandSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brandOptions)
            updateFamilySpinner("All Brands")
        }
        updateBrandSpinner("All Categories")

        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateBrandSpinner(categoryOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        brandSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                val selected = (brandSpinner.adapter as ArrayAdapter<String>).getItem(position) ?: "All Brands"
                updateFamilySpinner(selected)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        familySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateVariantColor(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        variantSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updatePriceInfo(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Item to Order")
            .setView(view)
            .setPositiveButton("Add", null)
            .setNegativeButton("Done", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (filteredFamilies.isEmpty()) {
                    Toast.makeText(this, "No items match this filter", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val itemName = familySpinner.selectedItem?.toString() ?: return@setOnClickListener
                val variant = variantSpinner.selectedItem?.toString() ?: "-"
                val color = colorSpinner.selectedItem?.toString() ?: "-"
                val qty = qtyEt.text.toString().trim().toIntOrNull() ?: 1
                val variantPos = variantSpinner.selectedItemPosition
                val dpPrice = if (variantPos >= 0 && variantPos < currentVariantDetails.size) currentVariantDetails[variantPos].dp else 0.0
                orderLines.add(OrderLine(itemName, variant, color, qty, dpPrice))
                refreshLines()
                addedInThisSession++
                addedCountText.text = "$addedInThisSession item(s) added so far. Change selections and tap Add for more, or tap Done to finish."
                Toast.makeText(this, "Added: $itemName - $variant", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
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
