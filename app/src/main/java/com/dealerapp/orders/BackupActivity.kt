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

    companion object {
        const val REQ_CREATE_FILE = 101
        const val REQ_OPEN_FILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        db = DBHelper(this)

        findViewById<TextView>(R.id.backupInfoText).text =
            "Backup saves all Dealers, Items and Orders into one file you choose (Google Drive, phone storage, etc).\n\nRestore reads that file back into the app."

        findViewById<Button>(R.id.btnBackupNow).setOnClickListener { startBackup() }
        findViewById<Button>(R.id.btnRestoreNow).setOnClickListener { startRestorePick() }
    }

    private fun startBackup() {
        val fileName = "dealer_orders_backup_" + DateFormat.format("yyyyMMdd_HHmm", Date()).toString() + ".json"
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
                    val json = db.exportToJson()
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
            .setMessage("Merge will add backup data on top of what's already in the app.\n\nReplace All will erase current data first, then load the backup.")
            .setPositiveButton("Merge") { _, _ -> doRestore(false) }
            .setNegativeButton("Replace All") { _, _ -> confirmReplaceAll() }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun confirmReplaceAll() {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("This will permanently delete all current Dealers, Items and Orders before restoring the backup.")
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
