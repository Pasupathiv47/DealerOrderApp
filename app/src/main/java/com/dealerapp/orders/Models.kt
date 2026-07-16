package com.dealerapp.orders

data class Dealer(val id: Long, val name: String, val location: String, val mobile: String)
data class ItemFamily(val id: Long, val name: String, val category: String, val brand: String = "")
data class OrderLine(val itemName: String, val variant: String, val color: String, val qty: Int)
data class OrderSummary(val id: Long, val dealerName: String, val date: String, val location: String = "", val mobile: String = "")
data class RowData(val id: Long, val text: String)

sealed class GroupRow
data class HeaderRow(val title: String) : GroupRow()
data class FamilyRow(val family: ItemFamily) : GroupRow()
data class DealerRow(val dealer: Dealer) : GroupRow()
