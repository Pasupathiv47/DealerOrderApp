package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DealersActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)

        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Dealer"
            setOnClickListener { showAddDialog() }
        }
        findViewById<Button>(R.id.btnSecondAction).apply {
            text = "Manage Locations"
            visibility = android.view.View.VISIBLE
            setOnClickListener { startActivity(Intent(this@DealersActivity, LocationsActivity::class.java)) }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val dealers = db.getDealers()
        val rows = mutableListOf<GroupRow>()
        var lastLocation: String? = null
        for (d in dealers) {
            val label = if (d.location.isBlank()) "Unassigned Location" else d.location
            if (label != lastLocation) {
                rows.add(HeaderRow(label))
                lastLocation = label
            }
            rows.add(DealerRow(d))
        }
        listView.adapter = DealerGroupedAdapter(
            this, rows,
            onMove = { dealer -> showMoveLocationDialog(dealer) },
            onDelete = { dealer ->
                db.deleteDealer(dealer.id)
                Toast.makeText(this, "Dealer removed", Toast.LENGTH_SHORT).show()
                refresh()
            }
        )
    }

    private fun locationOptions(): List<String> = listOf("Unassigned") + db.getLocations().map { it.text }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_dealer, null)
        val nameEt = view.findViewById<EditText>(R.id.dealerName)
        val locSpinner = view.findViewById<Spinner>(R.id.dealerLocation)
        val mobEt = view.findViewById<EditText>(R.id.dealerMobile)
        locSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationOptions())

        AlertDialog.Builder(this)
            .setTitle("Add Dealer")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val locSelected = locSpinner.selectedItem?.toString() ?: "Unassigned"
                val loc = if (locSelected == "Unassigned") "" else locSelected
                val mob = mobEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                } else {
                    db.addDealer(name, loc, mob)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveLocationDialog(dealer: Dealer) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_move_group, null)
        val label = view.findViewById<TextView>(R.id.moveLabel)
        val spinner = view.findViewById<Spinner>(R.id.moveInput)
        label.text = "Move \"${dealer.name}\" to which location?"
        val options = locationOptions()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val currentLabel = if (dealer.location.isBlank()) "Unassigned" else dealer.location
        val currentIndex = options.indexOf(currentLabel)
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        AlertDialog.Builder(this)
            .setTitle("Move Dealer")
            .setView(view)
            .setPositiveButton("Move") { _, _ ->
                val selected = spinner.selectedItem?.toString() ?: "Unassigned"
                val newLoc = if (selected == "Unassigned") "" else selected
                db.updateDealerLocation(dealer.id, newLoc)
                Toast.makeText(this, "Moved to ${if (newLoc.isBlank()) "Unassigned Location" else newLoc}", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
