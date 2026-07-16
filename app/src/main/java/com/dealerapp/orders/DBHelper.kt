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

    fun importFromJson(root: JSONObject) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Wipe existing data
            db.delete("family_variants", null, null)
            db.delete("family_colors", null, null)
            db.delete("item_families", null, null)
            db.delete("brands", null, null)
            db.delete("locations", null, null)
            db.delete("variant_options", null, null)
            db.delete("categories", null, null)
            db.delete("order_items", null, null)
            db.delete("orders", null, null)
            db.delete("dealers", null, null)

            // Dealers
            root.optJSONArray("dealers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    val cv = ContentValues().apply {
                        put("name", d.optString("name", ""))
                        put("location", d.optString("location", ""))
                        put("mobile", d.optString("mobile", ""))
                    }
                    db.insert("dealers", null, cv)
                }
            }

            // Families (with variants + colors)
            root.optJSONArray("families")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val f = arr.getJSONObject(i)
                    val fcv = ContentValues().apply {
                        put("name", f.optString("name", ""))
                        put("category", f.optString("category", ""))
                        put("brand", f.optString("brand", ""))
                    }
                    val familyId = db.insert("item_families", null, fcv)

                    f.optJSONArray("variants")?.let { vArr ->
                        for (j in 0 until vArr.length()) {
                            val v = vArr.getJSONObject(j)
                            val vcv = ContentValues().apply {
                                put("family_id", familyId)
                                put("variant_text", v.optString("text", ""))
                                put("mop_price", v.optDouble("mop", 0.0))
                                put("dp_price", v.optDouble("dp", 0.0))
                            }
                            db.insert("family_variants", null, vcv)
                        }
                    }

                    f.optJSONArray("colors")?.let { cArr ->
                        for (j in 0 until cArr.length()) {
                            val ccv = ContentValues().apply {
                                put("family_id", familyId)
                                put("color_text", cArr.getString(j))
                            }
                            db.insert("family_colors", null, ccv)
                        }
                    }
                }
            }

            // Brands
            root.optJSONArray("brands")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val cv = ContentValues().apply { put("name", arr.getString(i)) }
                    db.insert("brands", null, cv)
                }
            }

            // Locations
            root.optJSONArray("locations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val cv = ContentValues().apply { put("name", arr.getString(i)) }
                    db.insert("locations", null, cv)
                }
            }

            // Variant options
            root.optJSONArray("variant_options")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val cv = ContentValues().apply { put("name", arr.getString(i)) }
                    db.insert("variant_options", null, cv)
                }
            }

            // Categories
            root.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val cv = ContentValues().apply { put("name", arr.getString(i)) }
                    db.insert("categories", null, cv)
                }
            }

            // Orders
            root.optJSONArray("orders")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val ocv = ContentValues().apply {
                        put("dealer_name", o.optString("dealer_name", ""))
                        put("dealer_location", o.optString("dealer_location", ""))
                        put("dealer_mobile", o.optString("dealer_mobile", ""))
                        put("order_date", o.optString("order_date", ""))
                    }
                    val orderId = db.insert("orders", null, ocv)

                    o.optJSONArray("items")?.let { itemsArr ->
                        for (j in 0 until itemsArr.length()) {
                            val line = itemsArr.getJSONObject(j)
                            val lcv = ContentValues().apply {
                                put("order_id", orderId)
                                put("item_name", line.optString("item_name", ""))
                                put("variant", line.optString("variant", ""))
                                put("color", line.optString("color", ""))
                                put("qty", line.optInt("qty", 0))
                                put("dp_price", line.optDouble("dp_price", 0.0))
                            }
                            db.insert("order_items", null, lcv)
                        }
                    }
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
