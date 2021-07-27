package dev.gitlive.firebase.storage

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Returns the FirebaseStorage, initialized with the default FirebaseApp. */
actual val Firebase.storage: FirebaseStorage
    get() = TODO("Not yet implemented")

/** Returns the FirebaseStorage, initialized with the default FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(bucketUrl: String): FirebaseStorage {
    TODO("Not yet implemented")
}

/** Returns the FirebaseStorage, initialized with a custom FirebaseApp */
actual fun Firebase.storage(app: FirebaseApp): FirebaseStorage {
    TODO("Not yet implemented")
}

/** Returns the FirebaseStorage, initialized with a custom FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(
    app: FirebaseApp,
    bucketUrl: String
): FirebaseStorage {
    TODO("Not yet implemented")
}


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

    internal actual val _activeUploadJobs: MutableList<FirebaseStorageJob.UploadJob>
        get() = TODO("Not yet implemented")
    internal actual val _activeDownloadJobs: MutableList<FirebaseStorageJob.DownloadJob>
        get() = TODO("Not yet implemented")
    actual val activeUploadJobs: List<FirebaseStorageJob.UploadJob>
        get() = TODO("Not yet implemented")
    actual val activeDownloadJobs: List<FirebaseStorageJob.DownloadJob>
        get() = TODO("Not yet implemented")
    internal actual val jobHandler: JobHandler
        get() = TODO("Not yet implemented")

}

actual class StorageReference {
    internal actual val jobHandler: JobHandler
        get() = TODO("Not yet implemented")
    actual val name: String
        get() = TODO("Not yet implemented")
    actual val parent: StorageReference?
        get() = TODO("Not yet implemented")
    actual val path: String
        get() = TODO("Not yet implemented")
    actual val root: StorageReference
        get() = TODO("Not yet implemented")
    actual val bucket: String
        get() = TODO("Not yet implemented")
    actual val storage: FirebaseStorage
        get() = TODO("Not yet implemented")

    actual suspend fun getMetadata(): StorageMetadata {
        TODO("Not yet implemented")
    }

    actual suspend fun updateMetadata(metadata: StorageMetadata): StorageMetadata {
        TODO("Not yet implemented")
    }

    actual fun activeDownloadJobs(): List<FirebaseStorageJob.DownloadJob> {
        TODO("Not yet implemented")
    }

    actual fun activeUploadJobs(): List<FirebaseStorageJob.UploadJob> {
        TODO("Not yet implemented")
    }

    actual fun child(path: String): StorageReference {
        TODO("Not yet implemented")
    }

    actual suspend fun delete() {
    }

    actual suspend fun getDownloadUri(): String {
        TODO("Not yet implemented")
    }

    actual suspend fun getData(
        scope: CoroutineScope,
        maxDownloadSizeBytes: Long,
        onComplete: (data: Data?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun getFile(
        scope: CoroutineScope,
        destination: URI,
        onComplete: (uri: URI?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun getStream(
        scope: CoroutineScope,
        processor: StreamProcessor?,
        fallback: IosFallback.Download
    ): FirebaseStorageJob.DownloadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun putData(
        scope: CoroutineScope,
        data: Data,
        metadata: StorageMetadata?,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit
    ): FirebaseStorageJob.UploadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun putFile(
        scope: CoroutineScope,
        file: URI,
        metadata: StorageMetadata?,
        existingURI: URI?,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit
    ): FirebaseStorageJob.UploadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun putStream(
        scope: CoroutineScope,
        inputStream: Any,
        metadata: StorageMetadata?,
        fallback: IosFallback.Upload
    ): FirebaseStorageJob.UploadJob {
        TODO("Not yet implemented")
    }

    actual suspend fun listAll(): ListResult {
        TODO("Not yet implemented")
    }

    actual suspend fun list(
        maxResults: Int,
        pageToken: String?
    ): ListResult {
        TODO("Not yet implemented")
    }

}

actual class URI
actual class Data
actual class Error
actual class StorageMetadata {
    actual val name: String?
        get() = TODO("Not yet implemented")
    actual val path: String?
        get() = TODO("Not yet implemented")
    actual val reference: StorageReference?
        get() = TODO("Not yet implemented")
    actual val sizeInBytes: Long
        get() = TODO("Not yet implemented")
    actual val creationTimeMillis: Long
        get() = TODO("Not yet implemented")
    actual val updatedTimeMillis: Long
        get() = TODO("Not yet implemented")
    actual val bucket: String?
        get() = TODO("Not yet implemented")
    actual val cacheControl: String?
        get() = TODO("Not yet implemented")
    actual val contentDisposition: String?
        get() = TODO("Not yet implemented")
    actual val contentEncoding: String?
        get() = TODO("Not yet implemented")
    actual val contentLanguage: String?
        get() = TODO("Not yet implemented")
    actual val contentType: String?
        get() = TODO("Not yet implemented")
    actual val generation: String?
        get() = TODO("Not yet implemented")
    actual val metadataGeneration: String?
        get() = TODO("Not yet implemented")
    actual val md5Hash: String?
        get() = TODO("Not yet implemented")
    actual val customMetadataKeys: Set<String>
        get() = TODO("Not yet implemented")

    actual fun getCustomMetadata(key: String): String? {
        TODO("Not yet implemented")
    }

}

actual class ListResult {
    actual val items: List<StorageReference>
        get() = TODO("Not yet implemented")
    actual val pageToken: String?
        get() = TODO("Not yet implemented")
    actual val prefixes: List<StorageReference>
        get() = TODO("Not yet implemented")
}

actual sealed class FirebaseStorageJob {
    actual abstract val task: FirebaseStorageTask
    actual abstract val jobHandler: JobHandler
    actual val job: Job
        get() = TODO("Not yet implemented")

    actual fun cancel() {
    }

    actual fun start(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun pause(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun resume(): Boolean {
        TODO("Not yet implemented")
    }

    actual class DownloadJob : FirebaseStorageJob() {
        override val task: FirebaseStorageTask
            get() = TODO("Not yet implemented")
        override val jobHandler: JobHandler
            get() = TODO("Not yet implemented")

    }
    actual class UploadJob : FirebaseStorageJob(){
        override val task: FirebaseStorageTask
            get() = TODO("Not yet implemented")
        override val jobHandler: JobHandler
            get() = TODO("Not yet implemented")

    }

}

actual sealed class FirebaseStorageTask {
    actual open class FirebaseStorageSnapshotBase {
        actual val error: FirebaseStorageException?
            get() = TODO("Not yet implemented")
    }

    actual sealed class Upload : FirebaseStorageTask() {
        actual abstract val snapshot: UploadSnapshot

        actual class UploadSnapshot :
            FirebaseStorageSnapshotBase() {
            actual val bytesUploaded: Long
                get() = TODO("Not yet implemented")
            actual val totalBytes: Long
                get() = TODO("Not yet implemented")
            actual val uploadURI: URI?
                get() = TODO("Not yet implemented")
            actual val metadata: StorageMetadata?
                get() = TODO("Not yet implemented")
        }

        actual class Data : Upload() {
            override val snapshot: UploadSnapshot
                get() = TODO("Not yet implemented")
        }

        actual class File : Upload() {
            override val snapshot: UploadSnapshot
                get() = TODO("Not yet implemented")
        }

        actual class Stream : Upload() {
            override val snapshot: UploadSnapshot
                get() = TODO("Not yet implemented")
        }

    }

    actual sealed class Download : FirebaseStorageTask() {
        actual class Data : Download()
        actual class File : Download() {
            actual val snapshot: DownloadSnapshot
                get() = TODO("Not yet implemented")

            actual open class DownloadSnapshot :
                FirebaseStorageSnapshotBase() {
                actual val totalBytes: Long
                    get() = TODO("Not yet implemented")
                actual val bytesDownloaded: Long
                    get() = TODO("Not yet implemented")
            }
        }

        actual class Stream : Download() {
            actual val snapshot: StreamDownloadSnapshot
                get() = TODO("Not yet implemented")

            actual class StreamDownloadSnapshot :
                FirebaseStorageSnapshotBase() {
                actual val totalBytes: Long
                    get() = TODO("Not yet implemented")
                actual val bytesDownloaded: Long
                    get() = TODO("Not yet implemented")
                actual val stream: Any?
                    get() = TODO("Not yet implemented")
            }
        }
    }

}

actual interface StreamProcessor
actual class FirebaseStorageException : FirebaseException(TODO(), TODO())

actual val FirebaseStorageException.code: StorageExceptionCode
    get() = TODO("Not yet implemented")

actual enum class StorageExceptionCode {
    UNKNOWN, OBJECT_NOT_FOUND, BUCKET_NOT_FOUND, PROJECT_NOT_FOUND, QUOTA_EXCEEDED, NOT_AUTHENTICATED, NOT_AUTHORIZED, RETRY_LIMIT_EXCEEDED, INVALID_CHECKSUM, CANCELED
}