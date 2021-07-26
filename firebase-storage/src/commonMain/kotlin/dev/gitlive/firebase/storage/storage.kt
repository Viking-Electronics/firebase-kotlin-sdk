package dev.gitlive.firebase.storage

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Returns the FirebaseStorage, initialized with the default FirebaseApp. */
expect val Firebase.storage: FirebaseStorage

/** Returns the FirebaseStorage, initialized with the default FirebaseApp and a custom Storage Bucket.  */
expect fun Firebase.storage(bucketUrl: String): FirebaseStorage
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp */
expect fun Firebase.storage(app: FirebaseApp): FirebaseStorage
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp and a custom Storage Bucket.  */
expect fun Firebase.storage(app: FirebaseApp, bucketUrl: String): FirebaseStorage

@OptIn(ExperimentalTime::class)
expect class FirebaseStorage {
    internal val _activeUploadJobs: MutableList<FirebaseStorageJob.UploadJob>
    internal val _activeDownloadJobs: MutableList<FirebaseStorageJob.DownloadJob>
    val activeUploadJobs: List<FirebaseStorageJob.UploadJob>
    val activeDownloadJobs: List<FirebaseStorageJob.DownloadJob>

    internal val jobHandler: JobHandler


    /** The maximum time to retry a download if a failure occurs. */
    var maxDownloadRetryTime: Duration
    /** The maximum time to retry operations other than upload and download if a failure occurs.  */
    var maxOperationRetryTime: Duration
    /** The maximum time to retry an upload if a failure occurs.  */
    var maxUploadRetryTime: Duration

    /** Creates a new StorageReference initialized at the root Firebase Storage location.   */
    fun getReference(): StorageReference
    /** Creates a new StorageReference initialized with a child Firebase Storage location.   */
    fun getReference(location: String): StorageReference
    /** Creates a StorageReference given a gs:// or // URL pointing to a Firebase Storage location.   */
    fun getReferenceFromUrl(fullUrl: String): StorageReference

    /** Modifies this FirebaseStorage instance to communicate with the Storage emulator.
     * NOOP on iOS until carthage fixes the issue preventing pulling versions later than 7.11.0
     * */
    fun useEmulator(host: String, port: Int)
}

interface JobHandler {
    fun addToList(job: FirebaseStorageJob)
    fun removeFromList(job: FirebaseStorageJob)
}

expect class StorageReference {
    internal val jobHandler: JobHandler

    val name: String
    val parent: StorageReference?
    val path: String
    val root: StorageReference
    val bucket: String
    val storage: FirebaseStorage

    suspend fun getMetadata(): StorageMetadata
    suspend fun updateMetadata(metadata: StorageMetadata): StorageMetadata

    fun activeDownloadJobs(): List<FirebaseStorageJob.DownloadJob>
    fun activeUploadJobs(): List<FirebaseStorageJob.UploadJob>

    fun child(path: String): StorageReference
    suspend fun delete()

    suspend fun getDownloadUri(): String

    suspend fun getData(
        scope: CoroutineScope,
        maxDownloadSizeBytes: Long,
        onComplete: (data: Data?, error: Error?) -> Unit): FirebaseStorageJob.DownloadJob
    suspend fun getFile(
        scope: CoroutineScope,
        destination: URI,
        onComplete: (uri: URI?, error: Error?) -> Unit): FirebaseStorageJob.DownloadJob
    suspend fun getStream(
        scope: CoroutineScope,
        processor: StreamProcessor? = null,
        fallback: IosFallback.Download): FirebaseStorageJob.DownloadJob

    suspend fun putData(
        scope: CoroutineScope,
        data: Data,
        metadata: StorageMetadata? = null,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit): FirebaseStorageJob.UploadJob
    suspend fun putFile(
        scope: CoroutineScope,
        file: URI,
        metadata: StorageMetadata? = null,
        existingURI: URI? = null,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit): FirebaseStorageJob.UploadJob
    suspend fun putStream(
        scope: CoroutineScope,
        inputStream: Any,
        metadata: StorageMetadata? = null,
        fallback: IosFallback.Upload): FirebaseStorageJob.UploadJob

    suspend fun listAll(): ListResult
    suspend fun list(maxResults: Int, pageToken: String? = null): ListResult
}

expect class URI
expect class Data
expect class Error

sealed class IosFallback {
    sealed class Upload(open val task: FirebaseStorageTask.Upload) {
        class Data(override val task: FirebaseStorageTask.Upload.Data): Upload(task)
        class File(override val task: FirebaseStorageTask.Upload.File): Upload(task)
    }
    sealed class Download(open val task: FirebaseStorageTask.Download) {
        class Data(override val task: FirebaseStorageTask.Download.Data): Download(task)
        class File(override val task: FirebaseStorageTask.Download.File): Download(task)
    }
}


expect class StorageMetadata {
    val name: String?
    val path: String?
    val reference: StorageReference?
    val sizeInBytes: Long
    val creationTimeMillis: Long
    val updatedTimeMillis: Long
    val bucket: String?
    val cacheControl: String?
    val contentDisposition: String?
    val contentEncoding: String?
    val contentLanguage: String?
    val contentType: String?
    val generation: String?
    val metadataGeneration: String?
    val md5Hash: String?
    val customMetadataKeys: Set<String>

    fun getCustomMetadata(key: String): String?
}

expect class ListResult {
    val items: List<StorageReference>
    val pageToken: String?
    val prefixes: List<StorageReference>
}


expect sealed class FirebaseStorageJob {
    abstract val task: FirebaseStorageTask
    abstract val jobHandler: JobHandler
    val job: Job

    fun cancel()
    fun start(): Boolean
    fun pause(): Boolean
    fun resume(): Boolean

    class DownloadJob: FirebaseStorageJob
    class UploadJob: FirebaseStorageJob
}


expect sealed class FirebaseStorageTask {

    open class FirebaseStorageSnapshotBase {
        val error: FirebaseStorageException?
    }

    sealed class Upload: FirebaseStorageTask {
        abstract val snapshot: UploadSnapshot
        class UploadSnapshot: FirebaseStorageSnapshotBase {
            val bytesUploaded: Long
            val totalBytes: Long
            val uploadURI: URI?
            val metadata: StorageMetadata?
        }

        class Data: Upload
        class File: Upload
        class Stream: Upload
    }
    sealed class Download: FirebaseStorageTask {
        class Data: Download
        class File: Download {
            val snapshot: DownloadSnapshot
            open class DownloadSnapshot: FirebaseStorageSnapshotBase {
                val totalBytes: Long
                val bytesDownloaded: Long
            }
        }
        class Stream: Download {
            val snapshot: StreamDownloadSnapshot
            class StreamDownloadSnapshot: FirebaseStorageSnapshotBase {
                val totalBytes: Long
                val bytesDownloaded: Long
                val stream: Any?
            }
        }
    }
}

expect interface StreamProcessor

expect class FirebaseStorageException : FirebaseException

expect val FirebaseStorageException.code: StorageExceptionCode

expect enum class StorageExceptionCode {
    UNKNOWN,
    OBJECT_NOT_FOUND,
    BUCKET_NOT_FOUND,
    PROJECT_NOT_FOUND,
    QUOTA_EXCEEDED,
    NOT_AUTHENTICATED,
    NOT_AUTHORIZED,
    RETRY_LIMIT_EXCEEDED,
    INVALID_CHECKSUM,
    CANCELED
}