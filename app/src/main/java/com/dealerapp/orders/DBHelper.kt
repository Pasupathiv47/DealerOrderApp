package com.dealerapp.orders

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class DBHelper(context: Context) : SQLiteOpenHelper(context, "dealer_orders.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE dealers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, location TEXT, mobile TEXT)")
        db.execSQL("CREATE TABLE item_families (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, brand TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE family_variants (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, variant_text TEXT)")
        db.execSQL("CREATE TABLE family_colors (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, color_text TEXT)")
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, dealer_name TEXT, dealer_location TEXT, dealer_mobile TEXT, order_date TEXT)")
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, item_name TEXT, variant TEXT, color TEXT, qty INTEGER)")
        db.execSQL("CREATE TABLE brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
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

    // Variants
    fun addVariant(familyId: Long, text: String): Long {
        val cv = ContentValues().apply { put("family_id", familyId); put("variant_text", text) }
        return writableDatabase.insert("family_variants", null, cv)
    }
    fun deleteVariant(id: Long) { writableDatabase.delete("family_variants", "id=?", arrayOf(id.toString())) }
    fun getVariants(familyId: Long): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, variant_text FROM family_variants WHERE family_id=? ORDER BY id", arrayOf(familyId.toString()))
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    // Colors
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
        val c = readableDatabase.rawQuery("SELECT item_name, variant, color, qty FROM order_items WHERE order_id=?", arrayOf(orderId.toString()))
        while (c.moveToNext()) list.add(OrderLine(c.getString(0), c.getString(1), c.getString(2), c.getInt(3)))
        c.close()
        return list
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
            for (v in getVariants(f.id)) variantsArr.put(v.text)
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
                itemsArr.put(lo)
            }
            oo.put("items", itemsArr)
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
            for (v in getVariants(f.id)) variantsArr.put(v.text)
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
                itemsArr.put(lo)
            }
            oo.put("items", itemsArr)
            ordersArr.put(oo)
        }
        root.put("orders", ordersArr)

        return root
    }

    fun importFromJson(root: JSONObject, clearFirst: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (clearFirst) {
                db.execSQL("DELETE FROM dealers")
                db.execSQL("DELETE FROM item_families")
                db.execSQL("DELETE FROM family_variants")
                db.execSQL("DELETE FROM family_colors")
                db.execSQL("DELETE FROM orders")
                db.execSQL("DELETE FROM order_items")
                db.execSQL("DELETE FROM brands")
                db.execSQL("DELETE FROM locations")
            }

            val dealersArr = root.optJSONArray("dealers") ?: JSONArray()
            for (i in 0 until dealersArr.length()) {
                val o = dealersArr.getJSONObject(i)
                val cv = ContentValues().apply {
                    put("name", o.optString("name")); put("location", o.optString("location")); put("mobile", o.optString("mobile"))
                }
                db.insert("dealers", null, cv)
            }

            val familiesArr = root.optJSONArray("families") ?: JSONArray()
            for (i in 0 until familiesArr.length()) {
                val fo = familiesArr.getJSONObject(i)
                val fcv = ContentValues().apply {
                    put("name", fo.optString("name")); put("category", fo.optString("category")); put("brand", fo.optString("brand", ""))
                }
                val familyId = db.insert("item_families", null, fcv)

                val variantsArr = fo.optJSONArray("variants") ?: JSONArray()
                for (j in 0 until variantsArr.length()) {
                    val vcv = ContentValues().apply {
                        put("family_id", familyId); put("variant_text", variantsArr.getString(j))
                    }
                    db.insert("family_variants", null, vcv)
                }

                val colorsArr = fo.optJSONArray("colors") ?: JSONArray()
                for (j in 0 until colorsArr.length()) {
                    val ccv = ContentValues().apply {
                        put("family_id", familyId); put("color_text", colorsArr.getString(j))
                    }
                    db.insert("family_colors", null, ccv)
                }
            }

            val brandsArr = root.optJSONArray("brands") ?: JSONArray()
            for (i in 0 until brandsArr.length()) {
                val cv = ContentValues().apply { put("name", brandsArr.getString(i)) }
                db.insert("brands", null, cv)
            }

            val locationsArr = root.optJSONArray("locations") ?: JSONArray()
            for (i in 0 until locationsArr.length()) {
                val cv = ContentValues().apply { put("name", locationsArr.getString(i)) }
                db.insert("locations", null, cv)
            }

            val ordersArr = root.optJSONArray("orders") ?: JSONArray()
            for (i in 0 until ordersArr.length()) {
                val oo = ordersArr.getJSONObject(i)
                val ocv = ContentValues().apply {
                    put("dealer_name", oo.optString("dealer_name"))
                    put("dealer_location", oo.optString("dealer_location"))
                    put("dealer_mobile", oo.optString("dealer_mobile"))
                    put("order_date", oo.optString("order_date"))
                }
                val orderId = db.insert("orders", null, ocv)

                val itemsArr = oo.optJSONArray("items") ?: JSONArray()
                for (j in 0 until itemsArr.length()) {
                    val lo = itemsArr.getJSONObject(j)
                    val lcv = ContentValues().apply {
                        put("order_id", orderId); put("item_name", lo.optString("item_name"))
                        put("variant", lo.optString("variant")); put("color", lo.optString("color"))
                        put("qty", lo.optInt("qty", 1))
                    }
                    db.insert("order_items", null, lcv)
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
