package com.dealerapp.orders

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreSync {
    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun sanitize(s: String): String = s.lowercase().trim().replace(Regex("[^a-z0-9]"), "_")

    fun pushCatalog(dbHelper: DBHelper, onDone: (Boolean, String) -> Unit) {
        val families = dbHelper.getFamilies()
        val batch = db.batch()
        var count = 0
        for (f in families) {
            val variants = dbHelper.getVariantDetails(f.id)
            for (v in variants) {
                val docId = sanitize(f.name) + "__" + sanitize(v.text)
                val docRef = db.collection("variants").document(docId)
                val data = hashMapOf(
                    "family" to f.name,
                    "brand" to f.brand,
                    "category" to f.category,
                    "variant" to v.text
                )
                batch.set(docRef, data, SetOptions.merge())
                count++
            }
        }
        if (count == 0) {
            onDone(true, "No items to push yet")
            return
        }
        batch.commit()
            .addOnSuccessListener { onDone(true, "Pushed $count item(s) to cloud") }
            .addOnFailureListener { e -> onDone(false, "Push failed: ${e.message}") }
    }

    fun pullPriceStock(dbHelper: DBHelper, onDone: (Boolean, String) -> Unit) {
        db.collection("variants").get()
            .addOnSuccessListener { snapshot ->
                var updated = 0
                var created = 0
                for (doc in snapshot.documents) {
                    val familyName = doc.getString("family") ?: continue
                    val variantText = doc.getString("variant") ?: continue
                    val brand = doc.getString("brand") ?: ""
                    val category = doc.getString("category") ?: ""
                    val mop = doc.getDouble("mop") ?: 0.0
                    val dp = doc.getDouble("dp") ?: 0.0
                    val stock = (doc.getLong("stockQty") ?: 0L).toInt()

                    var family = dbHelper.getFamilyByName(familyName)
                    if (family == null) {
                        dbHelper.ensureCategoryExists(category)
                        dbHelper.ensureBrandExists(brand)
                        val newId = dbHelper.addFamilyFromCloud(familyName, category, brand)
                        family = dbHelper.getFamily(newId)
                        created++
                    }
                    if (family == null) continue

                    val existingVariant = dbHelper.getVariantByFamilyIdAndText(family.id, variantText)
                    if (existingVariant == null) {
                        dbHelper.addVariant(family.id, variantText, mop, dp, stock)
                    } else {
                        val keepMop = if (mop > 0.0) mop else existingVariant.mop
                        dbHelper.updateVariantStockAndPrice(existingVariant.id, keepMop, dp, stock)
                    }
                    updated++
                }
                val msg = if (created > 0) "Updated $updated item(s), $created new" else "Updated $updated item(s)"
                onDone(true, msg)
            }
            .addOnFailureListener { e -> onDone(false, "Pull failed: ${e.message}") }
    }
}
