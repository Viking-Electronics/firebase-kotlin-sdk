package dev.gitlive.firebase.storage

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual class FirebaseStorage {
    /** The maximum time to retry a download if a failure occurs. */
    actual var maxDownloadRetryTime: Duration
        get() = TODO("Not yet implemented")
        set(value) {}

    /** The maximum time to retry operations other than upload and download if a failure occurs.  */
    actual var maxOperationRetryTime: Duration
        get() = TODO("Not yet implemented")
        set(value) {}

    /** The maximum time to retry an upload if a failure occurs.  */
    actual var maxUploadRetryTime: Duration
        get() = TODO("Not yet implemented")
        set(value) {}

    /** Creates a new StorageReference initialized at the root Firebase Storage location.   */
    actual fun getReference(): StorageReference {
        TODO("Not yet implemented")
    }

    /** Creates a new StorageReference initialized with a child Firebase Storage location.   */
    actual fun getReference(location: String): StorageReference {
        TODO("Not yet implemented")
    }

    /** Creates a StorageReference given a gs:// or // URL pointing to a Firebase Storage location.   */
    actual fun getReferenceFromUrl(fullUrl: String): StorageReference {
        TODO("Not yet implemented")
    }

    /** Modifies this FirebaseStorage instance to communicate with the Storage emulator.  */
    actual fun useEmulator(host: String, port: Int) {
    }

}