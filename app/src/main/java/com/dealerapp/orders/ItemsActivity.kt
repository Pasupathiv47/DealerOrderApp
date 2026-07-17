package com.dealerapp.orders

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ItemsActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var listView: ListView
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var brandFilterSpinner: Spinner
    private var allFamilies: List<ItemFamily> = emptyList()
    private var savedScrollPosition = 0
    private var savedScrollOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_items)
        db = DBHelper(this)
        listView = findViewById(R.id.listView)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        brandFilterSpinner = findViewById(R.id.brandFilterSpinner)

        findViewById<Button>(R.id.btnTopAction).apply {
            text = "Add Item Family"
            setOnClickListener { showAddDialog() }
        }
        findViewById<Button>(R.id.btnSecondAction).apply {
            text = "Manage Brands"
            setOnClickListener { startActivity(Intent(this@ItemsActivity, BrandsActivity::class.java)) }
        }
        findViewById<Button>(R.id.btnThirdAction).apply {
            text = "Manage Categories"
            setOnClickListener { startActivity(Intent(this@ItemsActivity, CategoriesActivity::class.java)) }
        }

        categoryFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                applyFilter()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        brandFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, position: Int, id: Long) {
                applyFilter()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        savedScrollPosition = listView.firstVisiblePosition
        val v = listView.getChildAt(0)
        savedScrollOffset = v?.top ?: 0
    }

    private fun refresh() {
        allFamilies = db.getFamilies()

        val categoryOptions = listOf("All Categories") + db.getCategories().map { it.name }
        val previousCategory = categoryFilterSpinner.selectedItem?.toString()
        categoryFilterSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        val catIdx = categoryOptions.indexOf(previousCategory)
        if (catIdx >= 0) categoryFilterSpinner.setSelection(catIdx)

        val brandOptions = listOf("All Brands") + db.getBrands().map { it.text }
        val previousBrand = brandFilterSpinner.selectedItem?.toString()
        brandFilterSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brandOptions)
        val brandIdx = brandOptions.indexOf(previousBrand)
        if (brandIdx >= 0) brandFilterSpinner.setSelection(brandIdx)

        applyFilter()
    }

    private fun applyFilter() {
        val selectedCategory = categoryFilterSpinner.selectedItem?.toString() ?: "All Categories"
        val selectedBrand = brandFilterSpinner.selectedItem?.toString() ?: "All Brands"

        var families = allFamilies
        if (selectedCategory != "All Categories") families = families.filter { it.category == selectedCategory }
        if (selectedBrand != "All Brands") families = families.filter { (if (it.brand.isBlank()) "Unassigned" else it.brand) == selectedBrand }

        val rows = mutableListOf<GroupRow>()
        var lastBrand: String? = null
        for (f in families) {
            val label = if (f.brand.isBlank()) "Unassigned Brand" else f.brand
            if (label != lastBrand) {
                rows.add(HeaderRow(label))
                lastBrand = label
            }
            rows.add(FamilyRow(f))
        }
        listView.adapter = FamilyGroupedAdapter(
            this, rows,
            onOpen = { family ->
                val intent = Intent(this, FamilyDetailActivity::class.java)
                intent.putExtra("family_id", family.id)
                startActivity(intent)
            },
            onMove = { family -> showMoveBrandDialog(family) },
            onDelete = { family ->
                db.deleteFamily(family.id)
                Toast.makeText(this, "Item family removed", Toast.LENGTH_SHORT).show()
                refresh()
            }
        )

        listView.post {
            listView.setSelectionFromTop(savedScrollPosition, savedScrollOffset)
        }
    }

    private fun brandOptions(): List<String> = listOf("Unassigned") + db.getBrands().map { it.text }
    private fun categoryOptions(): List<String> = db.getCategories().map { it.name }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val nameEt = view.findViewById<EditText>(R.id.itemName)
        val catSpinner = view.findViewById<Spinner>(R.id.itemCategory)
        val brandSpinner = view.findViewById<Spinner>(R.id.itemBrand)
        val cats = categoryOptions()
        catSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, if (cats.isEmpty()) listOf("Add a category first") else cats)
        brandSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, brandOptions())

        AlertDialog.Builder(this)
            .setTitle("Add Item Family")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEt.text.toString().trim()
                val category = if (cats.isEmpty()) "" else catSpinner.selectedItem?.toString() ?: ""
                val brandSelected = brandSpinner.selectedItem?.toString() ?: "Unassigned"
                val brand = if (brandSelected == "Unassigned") "" else brandSelected
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
                } else if (cats.isEmpty()) {
                    Toast.makeText(this, "Add a category first via Manage Categories", Toast.LENGTH_SHORT).show()
                } else {
                    val id = db.addFamily(name, category, brand)
                    val intent = Intent(this, FamilyDetailActivity::class.java)
                    intent.putExtra("family_id", id)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveBrandDialog(family: ItemFamily) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_move_group, null)
        val label = view.findViewById<TextView>(R.id.moveLabel)
        val spinner = view.findViewById<Spinner>(R.id.moveInput)
        label.text = "Move \"${family.name}\" to which brand?"
        val options = brandOptions()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val currentLabel = if (family.brand.isBlank()) "Unassigned" else family.brand
        val currentIndex = options.indexOf(currentLabel)
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        AlertDialog.Builder(this)
            .setTitle("Move Item")
            .setView(view)
            .setPositiveButton("Move") { _, _ ->
                val selected = spinner.selectedItem?.toString() ?: "Unassigned"
                val newBrand = if (selected == "Unassigned") "" else selected
                db.updateFamilyBrand(family.id, newBrand)
                Toast.makeText(this, "Moved to ${if (newBrand.isBlank()) "Unassigned Brand" else newBrand}", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
