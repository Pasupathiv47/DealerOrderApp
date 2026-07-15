package com.dealerapp.orders

object OrderUtils {
    fun buildOrderText(db: DBHelper, orderId: Long): String {
        val header = db.getOrderHeader(orderId)
        val lines = db.getOrderLines(orderId)
        return buildString {
            appendLine("ORDER #$orderId")
            appendLine("Dealer: ${header?.dealerName ?: ""}")
            appendLine("Location: ${header?.location ?: ""}")
            appendLine("Mobile: ${header?.mobile ?: ""}")
            appendLine("Date: ${header?.date ?: ""}")
            appendLine()
            appendLine("Items:")
            lines.forEachIndexed { i, line ->
                appendLine("${i + 1}. ${line.itemName} - ${line.variant} - ${line.color} x ${line.qty}")
            }
        }
    }
}
