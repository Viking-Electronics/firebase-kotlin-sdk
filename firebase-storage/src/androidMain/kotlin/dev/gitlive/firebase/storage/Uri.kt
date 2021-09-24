package dev.gitlive.firebase.storage

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
actual class URI constructor(val android: Uri) {
    actual companion object {
        actual fun fromString(stringUri: String): URI = URI(Uri.parse(stringUri))
    }
}