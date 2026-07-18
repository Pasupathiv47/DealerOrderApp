package com.dealerapp.orders

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class DBHelper(context: Context) : SQLiteOpenHelper(context, "dealer_orders.db", null, 9) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE dealers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, location TEXT, mobile TEXT)")
        db.execSQL("CREATE TABLE item_families (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, brand TEXT DEFAULT '')")
        db.execSQL("CREATE TABLE family_variants (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, variant_text TEXT, mop_price REAL DEFAULT 0, dp_price REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE family_colors (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, color_text TEXT)")
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, dealer_name TEXT, dealer_location TEXT, dealer_mobile TEXT, order_date TEXT)")
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, item_name TEXT, variant TEXT, color TEXT, qty INTEGER, dp_price REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE variant_groups (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE variant_options (id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER, name TEXT)")
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, variant_group_id INTEGER)")
        seedDefaults(db)
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        val ramGroupId = db.insert("variant_groups", null, ContentValues().apply { put("name", "RAM + Storage") })
        val sizeGroupId = db.insert("variant_groups", null, ContentValues().apply { put("name", "Size") })

        val catDefaults = listOf(
            "Smartphones" to ramGroupId,
            "Tabs" to ramGroupId,
            "Accessories" to null,
            "TV" to sizeGroupId,
            "Home Appliances" to sizeGroupId
        )
        for ((name, groupId) in catDefaults) {
            val cv = ContentValues().apply {
                put("name", name)
                if (groupId != null) put("variant_group_id", groupId) else putNull("variant_group_id")
            }
            db.insert("categories", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("CREATE TABLE IF NOT EXISTS brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS variant_options (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS item_families (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS family_variants (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, variant_text TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS family_colors (id INTEGER PRIMARY KEY AUTOINCREMENT, family_id INTEGER, color_text TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS variant_groups (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")

        try { db.execSQL("ALTER TABLE item_families ADD COLUMN brand TEXT DEFAULT ''") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE family_variants ADD COLUMN mop_price REAL DEFAULT 0") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE family_variants ADD COLUMN dp_price REAL DEFAULT 0") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE order_items ADD COLUMN dp_price REAL DEFAULT 0") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE categories ADD COLUMN variant_type TEXT") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE variant_options ADD COLUMN option_type TEXT") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE categories ADD COLUMN variant_group_id INTEGER") } catch (e: Exception) { }
        try { db.execSQL("ALTER TABLE variant_options ADD COLUMN group_id INTEGER") } catch (e: Exception) { }

        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS items")
        }

        val cc = db.rawQuery("SELECT COUNT(*) FROM categories", null)
        cc.moveToFirst()
        val catCount = cc.getInt(0)
        cc.close()
        if (catCount == 0) {
            seedDefaults(db)
        }

        if (oldVersion in 1..8) {
            val gc = db.rawQuery("SELECT COUNT(*) FROM variant_groups", null)
            gc.moveToFirst()
            val groupCount = gc.getInt(0)
            gc.close()

            if (groupCount == 0) {
                val ramGroupId = db.insert("variant_groups", null, ContentValues().apply { put("name", "RAM + Storage") })
                val sizeGroupId = db.insert("variant_groups", null, ContentValues().apply { put("name", "Size") })

                try {
                    db.execSQL("UPDATE variant_options SET group_id=$ramGroupId WHERE option_type='ram_storage' OR option_type IS NULL")
                    db.execSQL("UPDATE variant_options SET group_id=$sizeGroupId WHERE option_type='size'")
                } catch (e: Exception) { }

                try {
                    db.execSQL("UPDATE categories SET variant_group_id=$ramGroupId WHERE variant_type='ram_storage' OR variant_type IS NULL")
                    db.execSQL("UPDATE categories SET variant_group_id=$sizeGroupId WHERE variant_type='size'")
                    db.execSQL("UPDATE categories SET variant_group_id=NULL WHERE variant_type='none'")
                } catch (e: Exception) { }
            }
        }
    }

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

    fun addFamily(name: String, category: String, brand: String): Long {
        val cv = ContentValues().apply { put("name", name); put("category", category); put("brand", brand) }
        val familyId = writableDatabase.insert("item_families", null, cv)
        addColor(familyId, "Any Colour")
        val catDetail = getCategoryByName(category)
        if (catDetail?.variantGroupId == null) {
            addVariant(familyId, "Standard", 0.0, 0.0)
        }
        return familyId
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

    fun addVariantGroup(name: String): Long {
        val cv = ContentValues().apply { put("name", name) }
        return writableDatabase.insert("variant_groups", null, cv)
    }
    fun deleteVariantGroup(id: Long) {
        val db = writableDatabase
        db.delete("variant_options", "group_id=?", arrayOf(id.toString()))
        db.execSQL("UPDATE categories SET variant_group_id=NULL WHERE variant_group_id=$id")
        db.delete("variant_groups", "id=?", arrayOf(id.toString()))
    }
    fun getVariantGroups(): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM variant_groups ORDER BY name", null)
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }
    fun getVariantGroupByName(name: String): RowData? {
        val c = readableDatabase.rawQuery("SELECT id, name FROM variant_groups WHERE name=?", arrayOf(name))
        var result: RowData? = null
        if (c.moveToFirst()) result = RowData(c.getLong(0), c.getString(1))
        c.close()
        return result
    }
    fun getOrCreateVariantGroupId(name: String): Long {
        val existing = getVariantGroupByName(name)
        if (existing != null) return existing.id
        return addVariantGroup(name)
    }

    fun addVariantOption(groupId: Long, name: String): Long {
        val cv = ContentValues().apply { put("group_id", groupId); put("name", name) }
        return writableDatabase.insert("variant_options", null, cv)
    }
    fun deleteVariantOption(id: Long) { writableDatabase.delete("variant_options", "id=?", arrayOf(id.toString())) }
    fun getVariantOptions(groupId: Long): List<RowData> {
        val list = mutableListOf<RowData>()
        val c = readableDatabase.rawQuery("SELECT id, name FROM variant_options WHERE group_id=? ORDER BY name", arrayOf(groupId.toString()))
        while (c.moveToNext()) list.add(RowData(c.getLong(0), c.getString(1)))
        c.close()
        return list
    }

    fun addCategory(name: String, variantGroupId: Long?): Long {
        val cv = ContentValues().apply {
            put("name", name)
            if (variantGroupId != null) put("variant_group_id", variantGroupId) else putNull("variant_group_id")
        }
        return writableDatabase.insert("categories", null, cv)
    }
    fun updateCategoryGroup(id: Long, variantGroupId: Long?) {
        val cv = ContentValues().apply {
            if (variantGroupId != null) put("variant_group_id", variantGroupId) else putNull("variant_group_id")
        }
        writableDatabase.update("categories", cv, "id=?", arrayOf(id.toString()))
    }
    fun deleteCategory(id: Long) { writableDatabase.delete("categories", "id=?", arrayOf(id.toString())) }
    fun getCategories(): List<CategoryDetail> {
        val list = mutableListOf<CategoryDetail>()
        val c = readableDatabase.rawQuery(
            "SELECT c.id, c.name, c.variant_group_id, g.name FROM categories c LEFT JOIN variant_groups g ON c.variant_group_id = g.id ORDER BY c.name",
            null
        )
        while (c.moveToNext()) {
            val groupId = if (c.isNull(2)) null else c.getLong(2)
            val groupName = if (c.isNull(3)) "No Variants" else c.getString(3)
            list.add(CategoryDetail(c.getLong(0), c.getString(1), groupId, groupName))
        }
        c.close()
        return list
    }
    fun getCategoryByName(name: String): CategoryDetail? {
        val c = readableDatabase.rawQuery(
            "SELECT c.id, c.name, c.variant_group_id, g.name FROM categories c LEFT JOIN variant_groups g ON c.variant_group_id = g.id WHERE c.name=?",
            arrayOf(name)
        )
        var result: CategoryDetail? = null
        if (c.moveToFirst()) {
            val groupId = if (c.isNull(2)) null else c.getLong(2)
            val groupName = if (c.isNull(3)) "No Variants" else c.getString(3)
            result = CategoryDetail(c.getLong(0), c.getString(1), groupId, groupName)
        }
        c.close()
        return result
    }

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

    fun exportItemsJson(): JSONObject {
        val root = JSONObject()
        val familiesArr = JSONArray()
        for (f in getFamilies()) {
            val fo = JSONObject()
            fo.put("name", f.name)
            fo.put("category", f.category)
            fo.put("brand", f.brand)
            val variantsArr = JSONArray()
            for (v in getVariantDetails(f.id)) {
                val vo = JSONObject()
                vo.put("text", v.text)
                vo.put("mop", v.mop)
                vo.put("dp", v.dp)
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

        val groupsArr = JSONArray()
        for (g in getVariantGroups()) groupsArr.put(g.text)
        root.put("variant_groups", groupsArr)

        val variantOptionsArr = JSONArray()
        for (g in getVariantGroups()) {
            for (v in getVariantOptions(g.id)) {
                val vo = JSONObject()
                vo.put("name", v.text)
                vo.put("type", g.text)
                variantOptionsArr.put(vo)
            }
        }
        root.put("variant_options", variantOptionsArr)

        val categoriesArr = JSONArray()
        for (c in getCategories()) {
            val co = JSONObject()
            co.put("name", c.name)
            co.put("type", if (c.variantGroupId == null) "" else c.variantGroupName)
            categoriesArr.put(co)
        }
        root.put("categories", categoriesArr)

        return root
    }

    fun exportDealersJson(): JSONObject {
        val root = JSONObject()
        val dealersArr = JSONArray()
        for (d in getDealers()) {
            val o = JSONObject()
            o.put("name", d.name)
            o.put("location", d.location)
            o.put("mobile", d.mobile)
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
                lo.put("item_name", line.itemName)
                lo.put("variant", line.variant)
                lo.put("color", line.color)
                lo.put("qty", line.qty)
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
            o.put("name", d.name)
            o.put("location", d.location)
            o.put("mobile", d.mobile)
            dealersArr.put(o)
        }
        root.put("dealers", dealersArr)

        val familiesArr = JSONArray()
        for (f in getFamilies()) {
            val fo = JSONObject()
            fo.put("name", f.name)
            fo.put("category", f.category)
            fo.put("brand", f.brand)
            val variantsArr = JSONArray()
            for (v in getVariantDetails(f.id)) {
                val vo = JSONObject()
                vo.put("text", v.text)
                vo.put("mop", v.mop)
                vo.put("dp", v.dp)
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

        val groupsArr = JSONArray()
        for (g in getVariantGroups()) groupsArr.put(g.text)
        root.put("variant_groups", groupsArr)

        val variantOptionsArr = JSONArray()
        for (g in getVariantGroups()) {
            for (v in getVariantOptions(g.id)) {
                val vo = JSONObject()
                vo.put("name", v.text)
                vo.put("type", g.text)
                variantOptionsArr.put(vo)
            }
        }
        root.put("variant_options", variantOptionsArr)

        val categoriesArr = JSONArray()
        for (c in getCategories()) {
            val co = JSONObject()
            co.put("name", c.name)
            co.put("type", if (c.variantGroupId == null) "" else c.variantGroupName)
            categoriesArr.put(co)
        }
        root.put("categories", categoriesArr)

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
                lo.put("item_name", line.itemName)
                lo.put("variant", line.variant)
                lo.put("color", line.color)
                lo.put("qty", line.qty)
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

    private fun resolveGroupIdFromLegacyOrName(typeStr: String): Long? {
        if (typeStr.isBlank() || typeStr == "none") return null
        val mapped = when (typeStr) {
            "ram_storage" -> "RAM + Storage"
            "size" -> "Size"
            else -> typeStr
        }
        return getOrCreateVariantGroupId(mapped)
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
                db.execSQL("DELETE FROM variant_options")
                db.execSQL("DELETE FROM variant_groups")
                db.execSQL("DELETE FROM categories")
            }

            val dealersArr = root.optJSONArray("dealers") ?: JSONArray()
            for (i in 0 until dealersArr.length()) {
                val o = dealersArr.getJSONObject(i)
                val cv = ContentValues().apply {
                    put("name", o.optString("name"))
                    put("location", o.optString("location"))
                    put("mobile", o.optString("mobile"))
                }
                db.insert("dealers", null, cv)
            }

            val groupsArr = root.optJSONArray("variant_groups") ?: JSONArray()
            for (i in 0 until groupsArr.length()) {
                val name = groupsArr.getString(i)
                getOrCreateVariantGroupId(name)
            }

            val familiesArr = root.optJSONArray("families") ?: JSONArray()
            for (i in 0 until familiesArr.length()) {
                val fo = familiesArr.getJSONObject(i)
                val fcv = ContentValues().apply {
                    put("name", fo.optString("name"))
                    put("category", fo.optString("category"))
                    put("brand", fo.optString("brand", ""))
                }
                val familyId = db.insert("item_families", null, fcv)

                val variantsArr = fo.optJSONArray("variants") ?: JSONArray()
                for (j in 0 until variantsArr.length()) {
                    val vItem = variantsArr.get(j)
                    if (vItem is JSONObject) {
                        val vcv = ContentValues().apply {
                            put("family_id", familyId)
                            put("variant_text", vItem.optString("text"))
                            put("mop_price", vItem.optDouble("mop", 0.0))
                            put("dp_price", vItem.optDouble("dp", 0.0))
                        }
                        db.insert("family_variants", null, vcv)
                    } else {
                        val vcv = ContentValues().apply {
                            put("family_id", familyId)
                            put("variant_text", vItem.toString())
                        }
                        db.insert("family_variants", null, vcv)
                    }
                }

                val colorsArr = fo.optJSONArray("colors") ?: JSONArray()
                for (j in 0 until colorsArr.length()) {
                    val ccv = ContentValues().apply {
                        put("family_id", familyId)
                        put("color_text", colorsArr.getString(j))
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

            val variantOptionsArr = root.optJSONArray("variant_options") ?: JSONArray()
            for (i in 0 until variantOptionsArr.length()) {
                val vItem = variantOptionsArr.get(i)
                if (vItem is JSONObject) {
                    val typeStr = vItem.optString("type", "")
                    val groupId = resolveGroupIdFromLegacyOrName(typeStr) ?: getOrCreateVariantGroupId("RAM + Storage")
                    val cv = ContentValues().apply {
                        put("group_id", groupId)
                        put("name", vItem.optString("name"))
                    }
                    db.insert("variant_options", null, cv)
                }
            }

            val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArr.length()) {
                val cItem = categoriesArr.get(i)
                val name: String
                val typeStr: String
                if (cItem is JSONObject) {
                    name = cItem.optString("name")
                    typeStr = cItem.optString("type", "")
                } else {
                    name = cItem.toString()
                    typeStr = "ram_storage"
                }
                val groupId = resolveGroupIdFromLegacyOrName(typeStr)
                val exists = db.rawQuery("SELECT id FROM categories WHERE name=?", arrayOf(name))
                val already = exists.moveToFirst()
                exists.close()
                if (!already) {
                    val cv = ContentValues().apply {
                        put("name", name)
                        if (groupId != null) put("variant_group_id", groupId) else putNull("variant_group_id")
                    }
                    db.insert("categories", null, cv)
                }
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
                        put("order_id", orderId)
                        put("item_name", lo.optString("item_name"))
                        put("variant", lo.optString("variant"))
                        put("color", lo.optString("color"))
                        put("qty", lo.optInt("qty", 1))
                        put("dp_price", lo.optDouble("dp_price", 0.0))
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
