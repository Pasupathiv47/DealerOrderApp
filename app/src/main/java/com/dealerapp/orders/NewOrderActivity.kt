package com.dealerapp.orders

import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Date

class NewOrderActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var locationSpinner: Spinner
    private lateinit var dealerSpinner: Spinner
    private lateinit var lineListView: ListView
    private var allDealers: List<Dealer> = emptyList()
    private var filteredDealers: List<Dealer> = emptyList()
    private var families: List<ItemFamily> = emptyList()
    private val orderLines = mutableListOf<OrderLine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)
        db = DBHelper(this)

        locationSpinner = findViewById(R.id.locationSpinner)
        dealerSpinner = findViewById(R.id.dealerSpinner)
        lineListView = findViewById(R.id.lineListView)

        allDealers = db.getDealers()
        families = db.getFamilies()

        if (allDealers.isEmpty()) Toast.makeText(this, "Add a dealer first", Toast.LENGTH_LONG).show()
        if (families.isEmpty()) Toast.makeText(this, "Add items first", Toast.LENGTH_LONG).show()

        val locationOptions = listOf("All Locations") + allDealers.map { if (it.location.isBlank()) "Unassigned" else it.location }.distinct().sorted()
        locationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationOptions)

        fun updateDealerSpinner(locationLabel: String) {
            filteredDealers = if (locationLabel == "All Locations") {
                allDealers
            } else {
                allDealers.filter { (if (it.location.isBlank()) "Unassigned" else it.location) == locationLabel }
            }
            dealerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filteredDealers.map { it.name })
        }
        updateDealerSpinner("All Locations")

        locationSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                updateDealerSpinner(locationOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnAddLine).setOnClickListener { showAddLineDialog() }
        findViewById<Button>(R.id.btnSaveOrder).setOnClickListener { saveOrder() }

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
        val categorySpinner = view.findViewById<Spinner>(R.id.lineCategorySpinner)
        val brandSpinner = view.findViewById<Spinner>(R.id.lineBrandSpinner)
        val familySpinner = view.findViewById<Spinner>(R.id.lineItemSpinner)
        val variantSpinner = view.findViewById<Spinner>(R.id.lineVariantSpinner)
        val colorSpinner = view.findViewById<Spinner>(R.id.lineColorSpinner)
        val qtyEt = view.findViewById<EditText>(R.id.lineQty)

        val categoryOptions = listOf("All Categories") + families.map { if (it.category.isBlank()) "No Category" else it.category }.distinct().sorted()
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)

        var categoryFiltered: List<ItemFamily> = families
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

        AlertDialog.Builder(this)
            .setTitle("Add Item to Order")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                if (filteredFamilies.isEmpty()) {
                    Toast.makeText(this, "No items match this filter", Toast.LENGTH_SHORT).show()
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

    private fun saveOrder() {
        if (filteredDealers.isEmpty()) {
            Toast.makeText(this, "No dealer selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (orderLines.isEmpty()) {
            Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        val dealer = filteredDealers[dealerSpinner.selectedItemPosition]
        val date = DateFormat.format("dd-MMM-yyyy hh:mm a", Date()).toString()
        db.createOrder(dealer.name, dealer.location, dealer.mobile, date, orderLines)
        Toast.makeText(this, "Order saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
