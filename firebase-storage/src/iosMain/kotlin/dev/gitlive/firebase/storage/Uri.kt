package dev.gitlive.firebase.storage

import platform.Foundation.NSURL

actual class URI internal constructor(val ios: NSURL) {
    actual companion object {
        actual fun fromString(stringUri: String): URI = NSURL.URLWithString(stringUri)?.let { URI(it) } ?: URI(
            NSURL(string = stringUri)
        )
    }
}