package com.dealerapp.orders

object OrderUtils {
    fun buildOrderText(db: DBHelper, orderId: Long): String {
        val header = db.getOrderHeader(orderId)
        val lines = db.getOrderLines(orderId)
        val total = lines.sumOf { it.dpPrice * it.qty }
        return buildString {
            appendLine("ORDER #$orderId")
            appendLine("Dealer: ${header?.dealerName ?: ""}")
            appendLine("Location: ${header?.location ?: ""}")
            appendLine("Mobile: ${header?.mobile ?: ""}")
            appendLine("Date: ${header?.date ?: ""}")
            appendLine()
            appendLine("Items:")
            lines.forEachIndexed { i, line ->
                val lineTotal = line.dpPrice * line.qty
                appendLine("${i + 1}. ${line.itemName} - ${line.variant} - ${line.color} x ${line.qty}  @ ₹${"%.2f".format(line.dpPrice)} = ₹${"%.2f".format(lineTotal)}")
                appendLine()
            }
            appendLine("TOTAL (DP): ₹${"%.2f".format(total)}")
        }
    }
}
