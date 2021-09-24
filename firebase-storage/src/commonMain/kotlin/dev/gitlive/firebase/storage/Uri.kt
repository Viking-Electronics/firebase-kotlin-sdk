package dev.gitlive.firebase.storage

import kotlinx.serialization.Serializable

@Serializable
expect class URI {
    companion object {
        fun fromString(stringUri: String): URI
    }
}