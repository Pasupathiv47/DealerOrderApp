package com.dealerapp.orders

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.Date

class BackupActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private var pendingRestoreJson: JSONObject? = null
    private var pendingBackupType: String = "all"

    companion object {
        const val REQ_CREATE_FILE = 101
        const val REQ_OPEN_FILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        db = DBHelper(this)

        findViewById<TextView>(R.id.backupInfoText).text =
            "Choose what to back up. Restore will read any backup file and load only what it contains."

        findViewById<Button>(R.id.btnBackupItems).setOnClickListener { startBackup("items") }
        findViewById<Button>(R.id.btnBackupDealers).setOnClickListener { startBackup("dealers") }
        findViewById<Button>(R.id.btnBackupOrders).setOnClickListener { startBackup("orders") }
        findViewById<Button>(R.id.btnBackupAll).setOnClickListener { startBackup("all") }
        findViewById<Button>(R.id.btnRestoreNow).setOnClickListener { startRestorePick() }
    }

    private fun startBackup(backupType: String) {
        pendingBackupType = backupType
        val prefix = when (backupType) {
            "items" -> "dealer_orders_items_"
            "dealers" -> "dealer_orders_dealers_"
            "orders" -> "dealer_orders_history_"
            else -> "dealer_orders_backup_"
        }
        val fileName = prefix + DateFormat.format("yyyyMMdd_HHmm", Date()).toString() + ".json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, REQ_CREATE_FILE)
    }

    private fun startRestorePick() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQ_OPEN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        val uri: Uri = data.data ?: return

        when (requestCode) {
            REQ_CREATE_FILE -> {
                try {
                    val json = when (pendingBackupType) {
                        "items" -> db.exportItemsJson()
                        "dealers" -> db.exportDealersJson()
                        "orders" -> db.exportOrdersJson()
                        else -> db.exportToJson()
                    }
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toString(2).toByteArray())
                    }
                    Toast.makeText(this, "Backup saved successfully", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            REQ_OPEN_FILE -> {
                try {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text.isNullOrBlank()) {
                        Toast.makeText(this, "File is empty or unreadable", Toast.LENGTH_LONG).show()
                        return
                    }
                    val json = JSONObject(text)
                    pendingRestoreJson = json
                    confirmRestoreMode()
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid backup file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmRestoreMode() {
        AlertDialog.Builder(this)
            .setTitle("Restore Data")
            .setMessage("Merge will add backup data on top of what's already in the app.\n\nReplace All will erase ALL current data (dealers, items, orders) before loading the backup, even if this file only contains part of it.")
            .setPositiveButton("Merge") { _, _ -> doRestore(false) }
            .setNegativeButton("Replace All") { _, _ -> confirmReplaceAll() }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun confirmReplaceAll() {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("This will permanently delete ALL current Dealers, Items and Orders before restoring the backup.")
            .setPositiveButton("Yes, Replace All") { _, _ -> doRestore(true) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doRestore(clearFirst: Boolean) {
        val json = pendingRestoreJson ?: return
        try {
            db.importFromJson(json, clearFirst)
            Toast.makeText(this, "Restore completed successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        pendingRestoreJson = null
    }
}
