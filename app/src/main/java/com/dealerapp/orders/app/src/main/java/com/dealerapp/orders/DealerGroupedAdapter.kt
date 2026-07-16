package com.dealerapp.orders

data class Dealer(val id: Long, val name: String, val location: String, val mobile: String)
data class ItemFamily(val id: Long, val name: String, val category: String, val brand: String = "")
data class VariantDetail(val id: Long, val text: String, val mop: Double, val dp: Double)
data class OrderLine(val itemName: String, val variant: String, val color: String, val qty: Int, val dpPrice: Double = 0.0)
data class OrderSummary(val id: Long, val dealerName: String, val date: String, val location: String = "", val mobile: String = "")
data class RowData(val id: Long, val text: String)

sealed class GroupRow
data class HeaderRow(val title: String) : GroupRow()
data class FamilyRow(val family: ItemFamily) : GroupRow()
data class DealerRow(val dealer: Dealer) : GroupRow()
