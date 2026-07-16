package com.dealerapp.orders

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class DBHelper(context: Context) : SQLiteOpenHelper(context, "dealer_orders.db", null, 7) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE dealers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, location TEXT, mobile TEXT)")
        db.execSQL("CREATE TABLE item_families (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, brand TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE family_variants (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, variant_text TEXT, mop_price REAL DEFAULT 0, dp_price REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE family_colors (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, color_text TEXT)")
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, dealer_name TEXT, dealer_location TEXT, dealer_mobile TEXT, order_date TEXT)")
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, item_name TEXT, variant TEXT, color TEXT, qty INTEGER, dp_price REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE variant_options (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        seedDefaultCategories(db)
    }

    private fun seedDefaultCategories(db: SQLiteDatabase) {
        val defaults = listOf("Smartphones", "Tabs", "Accessories", "TV", "Home Appliances")
        for (name in defaults) {
            val cv = ContentValues().apply { put("name", name) }
            db.insert("categories", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS items")
            db.execSQL("CREATE TABLE IF NOT EXISTS item_families (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS family_variants (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, variant_text TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS family_colors (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, color_text TEXT)")
        }
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE item_families ADD COLUMN brand TEXT DEFAULT ''") } catch (e: Exception) { }
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS variant_options (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
            val c = db.rawQuery("SELECT COUNT(*) FROM categories", null)
            c.moveToFirst()
            val count = c.getInt(0)
            c.close()
            if (count == 0) {
                seedDefaultCategories(db)
                val existingCats = db.rawQuery("SELECT DISTINCT category FROM item_families WHERE category IS NOT NULL AND category != ''", null)
                while (existingCats.moveToNext()) {
                    val catName = existingCats.getString(0)
                    val exists = db.rawQuery("SELECT id FROM categories WHERE name=?", arrayOf(catName))
                    val already = exists.moveToFirst()
                    exists.close()
                    if (!already) {
                        val cv = ContentValues().apply { put("name", catName) }
                        db.insert("categories", null, cv)
                    }
                }
                existingCats.close()
            }
        }
        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE family_variants ADD COLUMN mop_price REAL DEFAULT 0") } catch (e: Exception) { }
            try { db.execSQL("ALTER TABLE family_variants ADD COLUMN dp_price REAL DEFAULT 0") } catch (e: Exception) { }
            try { db.execSQL("ALTER TABLE order_items ADD COLUMN dp_price REAL DEFAULT 0") } catch (e: Exception) { }
        }
    }

    // Dealers
    fun addDealer(name: String, location: String, mobile: String): Long {
        val cv = ContentValues().apply { put("name", name); put("location", location); put("mobile", mobile) }
        return writableDatabase.insert("dealers", null, cv)
    }
    fun deleteDealer(id: Long) { writableDatabase.delete("dealers", "id=?", arrayOf(id.toString())) }
    fun getDealers(): List<Dealer> {
        val list = mutableListOf<Dealer>()
        val c = readableDatabase.rawQuery("SELECT id, name, location, mobile FROM dealers ORDER BY location, name", null)
        while (c.moveToNext()) list.add(Dealer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)))
        c.close()
        return list
    }
    fun updateDealerLocation(id: Long, location: String) {
        val cv = ContentValues().apply { put("location", location) }
        writableDatabase.update("dealers", cv, "id=?", arrayOf(id.toString()))
    }

    // Families
    fun addFamily(name: String, category: String, brand: String): Long {
        val cv = ContentValues().apply { put("name", name); put("category", category); put("brand", brand) }
        return writableDatabase.insert("item_families", null, cv)
    }
    fun deleteFamily(id: Long) {
        val db = writableDatabase
        db.delete("family_variants", "family_id=?", arrayOf(id.toString()))
        db.delete("family_colors", "family_id=?", arrayOf(id.toString()))
        db.delete("item_families", "id=?", arrayOf(id.toString()))
    }
    fun getFamilies(): List<ItemFamily> {
        val list = mutableListOf<ItemFamily>()
        val c = readableDatabase.rawQuery("SELECT id, name, category, brand FROM item_families ORDER BY brand, name", null)
        while (c.moveToNext()) list.add(ItemFamily(c.getLong(0), c.getString(1), c.getString(2), c.getString(3) ?: ""))
        c.close()
        return list
    }
    fun getFamily(id: Long): ItemFamily? {
        val c = readableDatabase.rawQuery("SELECT id, name, category, brand FROM item_families WHERE id=?", arrayOf(id.toString()))
        var result: ItemFamily? = null
        if (c.moveToFirst()) result = ItemFamily(c.getLong(0), c.getString(1), c.getString(2), c.getString(3) ?: "")
        c.close()
        return result
    }
    fun updateFamilyBrand(id: Long, brand: String) {
        val cv = ContentValues().apply { put("brand", brand) }
        writableDatabase.update("item_families", cv, "id=?", arrayOf(id.toString()))
    }
    fun updateFamilyCategory(id: Long, category: String) {
        val cv = ContentValues().apply { put("category", category) }
        writableDatabase.update("item_families", cv, "id=?", arrayOf(id.toString()))
    }

    // Variants (per family, with pricing)
    fun addVariant(familyId: Long, text: String, mop: Double, dp: Double): Long {
        val cv = ContentValues().apply {
            put("family_id", familyId); put("variant_text", text)
            put("mop_price", mop); put("dp_price", dp)
        }
        return writableDatabase.insert("family_variants", null, cv)
    }
    fun updateVariantPrice(id: Long, mop: Double, dp: Double) {
        val cv = ContentValues().apply { put("mop_price", mop); put("dp_price", dp) }
        writableDatabase.update("family_variants", cv, "id=?", arrayOf(id.toString()))
    }
    fun deleteVariant(id: Long) { writableDatabase.delete("family_variants", "id=?", arrayOf(id.toString())) }
    fun getVariantDetails(familyId: Long): List<VariantDetail> {
        val list = mutableListOf<VariantDetail>()
        val c = readableDatabase.rawQuery("SELECT id, variant_text, mop_price, dp_price FROM family_variants WHERE family_id=? ORDER BY id", arrayOf(familyId.toString()))
        while (c.moveToNext()) list.add(VariantDetail(c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3)))
        c.close()
        return list
    }

    // Colors (per family)
    fun addColor(familyId: Long, text: String): Long {
        val cv = ContentValues().apply { put("family_id", familyId); put("color_text", text) }
        return writableDatabase.insert("family_colors", null, cv)
    }
    fun deleteColor(id: Long) { writableDatabase.delete("family_colors", "id=?", arrayOf(id.toString())) }
    fun getColors(familyId: Long): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, color_text FROM family_colors WHERE family_id=? ORDER BY id", arrayOf(familyId.toString()))
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Brands (master list)
    fun addBrand(name: String): Long {
        val cv = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("brands", null, cv)
    }
    fun deleteBrand(id: Long) { writableDatabase.delete("brands", "id=?", arrayOf(id.toString())) }
    fun getBrands(): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM brands ORDER BY name", null)
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Locations (master list)
    fun addLocation(name: String): Long {
        val cv = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("locations", null, cv)
    }
    fun deleteLocation(id: Long) { writableDatabase.delete("locations", "id=?", arrayOf(id.toString())) }
    fun getLocations(): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM locations ORDER BY name", null)
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Variant Options (master list of RAM/Storage labels)
    fun addVariantOption(name: String): Long {
        val cv = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("variant_options", null, cv)
    }
    fun deleteVariantOption(id: Long) { writableDatabase.delete("variant_options", "id=?", arrayOf(id.toString())) }
    fun getVariantOptions(): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM variant_options ORDER BY name", null)
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Categories (master list)
    fun addCategory(name: String): Long {
        val cv = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("categories", null, cv)
    }
    fun deleteCategory(id: Long) { writableDatabase.delete("categories", "id=?", arrayOf(id.toString())) }
    fun getCategories(): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM categories ORDER BY name", null)
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Orders
    fun createOrder(dealerName: String, dealerLocation: String, dealerMobile: String, date: String, lines: List<OrderLine>): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("dealer_name", dealerName); put("dealer_location", dealerLocation)
                put("dealer_mobile", dealerMobile); put("order_date", date)
            }
            val orderId = db.insert("orders", null, cv)
            for (line in lines) {
                val lcv = ContentValues().apply {
                    put("order_id", orderId); put("item_name", line.itemName)
                    put("variant", line.variant); put("color", line.color); put("qty", line.qty)
                    put("dp_price", line.dpPrice)
                }
                db.insert("order_items", null, lcv)
            }
            db.setTransactionSuccessful()
            return orderId
        } finally {
            db.endTransaction()
        }
    }

    fun updateOrderLines(orderId: Long, lines: List<OrderLine>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("order_items", "order_id=?", arrayOf(orderId.toString()))
            for (line in lines) {
                val cv = ContentValues().apply {
                    put("order_id", orderId); put("item_name", line.itemName)
                    put("variant", line.variant); put("color", line.color); put("qty", line.qty)
                    put("dp_price", line.dpPrice)
                }
                db.insert("order_items", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteOrder(id: Long) {
        val db = writableDatabase
        db.delete("order_items", "order_id=?", arrayOf(id.toString()))
        db.delete("orders", "id=?", arrayOf(id.toString()))
    }

    fun getOrders(): List<OrderSummary> {
        val list = mutableListOf<OrderSummary>()
        val c = readableDatabase.rawQuery("SELECT id, dealer_name, order_date FROM orders ORDER BY id DESC", null)
        while (c.moveToNext()) list.add(OrderSummary(c.getLong(0), c.getString(1), c.getString(2)))
        c.close()
        return list
    }

    fun getOrderLines(orderId: Long): List<OrderLine> {
        val list = mutableListOf<OrderLine>()
        val c = readableDatabase.rawQuery("SELECT item_name, variant, color, qty, dp_price FROM order_items WHERE order_id=?", arrayOf(orderId.toString()))
        while (c.moveToNext()) list.add(OrderLine(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getDouble(4)))
        c.close()
        return list
    }

    fun getOrderTotal(orderId: Long): Double {
        var total = 0.0
        for (line in getOrderLines(orderId)) total += line.dpPrice * line.qty
        return total
    }

    fun getOrderHeader(orderId: Long): OrderSummary? {
        val c = readableDatabase.rawQuery("SELECT id, dealer_name, order_date, dealer_location, dealer_mobile FROM orders WHERE id=?", arrayOf(orderId.toString()))
        var result: OrderSummary? = null
        if (c.moveToFirst()) result = OrderSummary(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))
        c.close()
        return result
    }

    // ---------- BACKUP & RESTORE ----------

    fun exportItemsJson(): JSONObject {
        val root = JSONObject()
        val familiesArr = JSONArray()
        for (f in getFamilies()) {
            val fo = JSONObject()
            fo.put("name", f.name); fo.put("category", f.category); fo.put("brand", f.brand)
            val variantsArr = JSONArray()
            for (v in getVariantDetails(f.id)) {
                val vo = JSONObject()
                vo.put("text", v.text); vo.put("mop", v.mop); vo.put("dp", v.dp)
                variantsArr.put(vo)
            }
            val colorsArr = JSONArray()
            for (c in getColors(f.id)) colorsArr.put(c.text)
            fo.put("variants", variantsArr)
            fo.put("colors", colorsArr)
            familiesArr.put(fo)
        }
        root.put("families", familiesArr)

        val brandsArr = JSONArray()
        for (b in getBrands()) brandsArr.put(b.text)
        root.put("brands", brandsArr)

        val variantOptionsArr = JSONArray()
        for (v in getVariantOptions()) variantOptionsArr.put(v.text)
        root.put("variant_options", variantOptionsArr)

        val categoriesArr = JSONArray()
        for (c in getCategories()) categoriesArr.put(c.text)
        root.put("categories", categoriesArr)

        return root
    }

    fun exportDealersJson(): JSONObject {
        val root = JSONObject()
        val dealersArr = JSONArray()
        for (d in getDealers()) {
            val o = JSONObject()
            o.put("name", d.name); o.put("location", d.location); o.put("mobile", d.mobile)
            dealersArr.put(o)
        }
        root.put("dealers", dealersArr)

        val locationsArr = JSONArray()
        for (l in getLocations()) locationsArr.put(l.text)
        root.put("locations", locationsArr)

        return root
    }

    fun exportOrdersJson(): JSONObject {
        val root = JSONObject()
        val ordersArr = JSONArray()
        for (o in getOrders()) {
            val header = getOrderHeader(o.id)
            val oo = JSONObject()
            oo.put("dealer_name", header?.dealerName ?: "")
            oo.put("dealer_location", header?.location ?: "")
            oo.put("dealer_mobile", header?.mobile ?: "")
            oo.put("order_date", header?.date ?: "")
            val itemsArr = JSONArray()
            for (line in getOrderLines(o.id)) {
                val lo = JSONObject()
                lo.put("item_name", line.itemName); lo.put("variant", line.variant)
                lo.put("color", line.color); lo.put("qty", line.qty)
                lo.put("dp_price", line.dpPrice)
                itemsArr.put(lo)
            }
            oo.put("items", itemsArr)
            oo.put("total_dp", getOrderTotal(o.id))
            ordersArr.put(oo)
        }
        root.put("orders", ordersArr)
        return root
    }

    fun exportToJson(): JSONObject {
        val root = JSONObject()

        val dealersArr = JSONArray()
        for (d in getDealers()) {
            val o = JSONObject()
            o.put("name", d.name); o.put("location", d.location); o.put("mobile", d.mobile)
            dealersArr.put(o)
        }
        root.put("dealers", dealersArr)

        val familiesArr = JSONArray()
        for (f in getFamilies()) {
            val fo = JSONObject()
            fo.put("name", f.name); fo.put("category", f.category); fo.put("brand", f.brand)
            val variantsArr = JSONArray()
            for (v in getVariantDetails(f.id)) {
                val vo = JSONObject()
                vo.put("text", v.text); vo.put("mop", v.mop); vo.put("dp", v.dp)
                variantsArr.put(vo)
            }
            val colorsArr = JSONArray()
            for (c in getColors(f.id)) colorsArr.put(c.text)
            fo.put("variants", variantsArr)
            fo.put("colors", colorsArr)
            familiesArr.put(fo)
        }
        root.put("families", familiesArr)

        val brandsArr = JSONArray()
        for (b in getBrands()) brandsArr.put(b.text)
        root.put("brands", brandsArr)

        val locationsArr = JSONArray()
        for (l in getLocations()) locationsArr.put(l.text)
        root.put("locations", locationsArr)

        val variantOptionsArr = JSONArray()
        for (v in getVariantOptions()) variantOptionsArr.put(v.text)
        root.put("variant_options", variantOptionsArr)

        val categoriesArr = JSONArray()
        for (c in getCategories()) categoriesArr.put(c.text)
        root.put("categories", categoriesArr)

        val ordersArr = JSONArray()
        for (o in getOrders()) {
            val header = getOrderHeader(o.id)
            val oo = JSONObject()
            oo.put("dealer_name", header?.dealerName ?: "")
            oo.put("dealer_location", header?.location ?: "")
            oo.put("dealer_mobile", header?.mobile ?: ""
