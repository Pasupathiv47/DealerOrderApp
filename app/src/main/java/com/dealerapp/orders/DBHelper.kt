package com.dealerapp.orders

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "dealer_orders.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE dealers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, location TEXT, mobile TEXT)")
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, variants TEXT, colors TEXT)")
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, dealer_name TEXT, dealer_location TEXT, dealer_mobile TEXT, order_date TEXT)")
        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, item_name TEXT, variant TEXT, color TEXT, qty INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS dealers")
        db.execSQL("DROP TABLE IF EXISTS items")
        db.execSQL("DROP TABLE IF EXISTS orders")
        db.execSQL("DROP TABLE IF EXISTS order_items")
        onCreate(db)
    }

    fun addDealer(name: String, location: String, mobile: String): Long {
        val cv = ContentValues().apply {
            put("name", name); put("location", location); put("mobile", mobile)
        }
        return writableDatabase.insert("dealers", null, cv)
    }

    fun deleteDealer(id: Long) {
        writableDatabase.delete("dealers", "id=?", arrayOf(id.toString()))
    }

    fun getDealers(): List<Dealer> {
        val list = mutableListOf<Dealer>()
        val c = readableDatabase.rawQuery("SELECT id, name, location, mobile FROM dealers ORDER BY name", null)
        while (c.moveToNext()) {
            list.add(Dealer(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)))
        }
        c.close()
        return list
    }

    fun addItem(name: String, category: String, variants: String, colors: String): Long {
        val cv = ContentValues().apply {
            put("name", name); put("category", category); put("variants", variants); put("colors", colors)
        }
        return writableDatabase.insert("items", null, cv)
    }

    fun deleteItem(id: Long) {
        writableDatabase.delete("items", "id=?", arrayOf(id.toString()))
    }

    fun getItems(): List<Item> {
        val list = mutableListOf<Item>()
        val c = readableDatabase.rawQuery("SELECT id, name, category, variants, colors FROM items ORDER BY name", null)
        while (c.moveToNext()) {
            list.add(Item(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)))
        }
        c.close()
        return list
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
                }
                db.insert("order_items", null, lcv)
            }
            db.setTransactionSuccessful()
            return orderId
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
        while (c.moveToNext()) {
            list.add(OrderSummary(c.getLong(0), c.getString(1), c.getString(2)))
        }
        c.close()
        return list
    }

    fun getOrderLines(orderId: Long): List<OrderLine> {
        val list = mutableListOf<OrderLine>()
        val c = readableDatabase.rawQuery("SELECT item_name, variant, color, qty FROM order_items WHERE order_id=?", arrayOf(orderId.toString()))
        while (c.moveToNext()) {
            list.add(OrderLine(c.getString(0), c.getString(1), c.getString(2), c.getInt(3)))
        }
        c.close()
        return list
    }

    fun getOrderHeader(orderId: Long): OrderSummary? {
        val c = readableDatabase.rawQuery("SELECT id, dealer_name, order_date, dealer_location, dealer_mobile FROM orders WHERE id=?", arrayOf(orderId.toString()))
        var result: OrderSummary? = null
        if (c.moveToFirst()) {
            result = OrderSummary(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))
        }
        c.close()
        return result
    }
}
