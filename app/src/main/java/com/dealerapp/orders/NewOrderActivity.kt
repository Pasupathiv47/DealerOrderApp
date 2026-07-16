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
    private lateinit var dealerSpinner: Spinner
    private lateinit var lineListView: ListView
    private var dealers: List<Dealer> = emptyList()
    private var families: List<ItemFamily> = emptyList()
    private val orderLines = mutableListOf<OrderLine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)
        db = DBHelper(this)

        dealerSpinner = findViewById(R.id.dealerSpinner)
        lineListView = findViewById(R.id.lineListView)

        dealers = db.getDealers()
        families = db.getFamilies()

        if (dealers.isEmpty()) Toast.makeText(this, "Add a dealer first", Toast.LENGTH_LONG).show()
        if (families.isEmpty()) Toast.makeText(this, "Add items first", Toast.LENGTH_LONG).show()

        dealerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dealers.map { it.name })

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
        val familySpinner = view.findViewById<Spinner>(R.id.lineItemSpinner)
        val variantSpinner = view.findViewById<Spinner>(R.id.lineVariantSpinner)
        val colorSpinner = view.findViewById<Spinner>(R.id.lineColorSpinner)
        val qtyEt = view.findViewById<EditText>(R.id.lineQty)

        familySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, families.map { it.name })

        fun updateVariantColor(pos: Int) {
            val family = families[pos]
            val variants = db.getVariants(family.id).map { it.text }
            val colors = db.getColors(family.id).map { it.text }
            variantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (variants.isEmpty()) listOf("-") else variants)
            colorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (colors.isEmpty()) listOf("-") else colors)
        }
        updateVariantColor(0)
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
        if (dealers.isEmpty()) {
            Toast.makeText(this, "Add a dealer first", Toast.LENGTH_SHORT).show()
            return
        }
        if (orderLines.isEmpty()) {
            Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        val dealer = dealers[dealerSpinner.selectedItemPosition]
        val date = DateFormat.format("dd-MMM-yyyy hh:mm a", Date()).toString()
        db.createOrder(dealer.name, dealer.location, dealer.mobile, date, orderLines)
        Toast.makeText(this, "Order saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
