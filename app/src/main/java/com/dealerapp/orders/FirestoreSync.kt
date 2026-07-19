package com.dealerapp.orders

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreSync {
    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun sanitize(s: String): String = s.replace(Regex("[^A-Za-z0-9]"), "_")

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
                for (doc in snapshot.documents) {
                    val familyName = doc.getString("family") ?: continue
                    val variantText = doc.getString("variant") ?: continue
                    val mop = doc.getDouble("mop")
                    val dp = doc.getDouble("dp")
                    val stock = doc.getLong("stockQty")

                    if (mop == null && dp == null && stock == null) continue

                    val family = dbHelper.getFamilyByName(familyName) ?: continue
                    val existing = dbHelper.getVariantByFamilyIdAndText(family.id, variantText) ?: continue

                    val newMop = mop ?: existing.mop
                    val newDp = dp ?: existing.dp
                    val newStock = stock?.toInt() ?: existing.stockQty

                    dbHelper.updateVariantStockAndPrice(existing.id, newMop, newDp, newStock)
                    updated++
                }
                onDone(true, "Updated $updated item(s) from cloud")
            }
            .addOnFailureListener { e -> onDone(false, "Pull failed: ${e.message}") }
    }
}
