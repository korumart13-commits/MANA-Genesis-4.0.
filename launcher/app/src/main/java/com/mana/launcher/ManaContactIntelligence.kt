package com.mana.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.net.Uri

data class ManaContactMatch(
    val contactId: Long,
    val displayName: String,
    val matchedNumber: String
)

/** Local-only contact lookup. No contact data is uploaded or sent to cloud providers. */
class ManaContactIntelligence(private val context: Context) {
    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun findByPhone(phone: String): ManaContactMatch? {
        if (!hasPermission()) throw SecurityException("READ_CONTACTS permission has not been granted")
        val normalized = phone.filter { it.isDigit() || it == '+' }
        require(normalized.length >= 5) { "Phone number is too short" }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.NUMBER
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return ManaContactMatch(
                    contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)),
                    displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)).orEmpty(),
                    matchedNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)).orEmpty()
                )
            }
        }
        return null
    }
}
