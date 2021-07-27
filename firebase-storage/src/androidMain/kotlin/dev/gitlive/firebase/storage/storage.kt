package dev.gitlive.firebase.storage

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@Suppress()
/** Returns the FirebaseStorage, initialized with the default FirebaseApp. */
actual val Firebase.storage: FirebaseStorage
    get() = FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance())

/** Returns the FirebaseStorage, initialized with the default FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(bucketUrl: String): FirebaseStorage =
    FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance(bucketUrl))
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp */
actual fun Firebase.storage(app: FirebaseApp): FirebaseStorage =
    FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance(app.android))
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(app: FirebaseApp, bucketUrl: String): FirebaseStorage =
    FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance(app.android, bucketUrl))

@OptIn(ExperimentalTime::class)
actual class FirebaseStorage internal constructor(val android: com.google.firebase.storage.FirebaseStorage){
   internal actual val _activeDownloadJobs = mutableListOf<FirebaseStorageJob.DownloadJob>()
    internal actual val _activeUploadJobs = mutableListOf<FirebaseStorageJob.UploadJob>()

    internal actual val jobHandler = object: JobHandler {
        override fun addToList(job: FirebaseStorageJob){
            when(job) {
                is FirebaseStorageJob.UploadJob -> if (!_activeUploadJobs.contains(job)) _activeUploadJobs.add(job)
                is FirebaseStorageJob.DownloadJob -> if (!_activeDownloadJobs.contains(job)) _activeDownloadJobs.add(job)
            }
        }

        override fun removeFromList(job: FirebaseStorageJob) {
            when(job) {
                is FirebaseStorageJob.UploadJob -> if (_activeUploadJobs.contains(job)) _activeUploadJobs.remove(job)
                is FirebaseStorageJob.DownloadJob -> if (_activeDownloadJobs.contains(job)) _activeDownloadJobs.remove(job)
            }
        }
    }

    actual val activeUploadJobs: List<FirebaseStorageJob.UploadJob> = _activeUploadJobs
    actual val activeDownloadJobs: List<FirebaseStorageJob.DownloadJob> = _activeDownloadJobs

    /** The maximum time to retry a download if a failure occurs. */
    actual var maxDownloadRetryTime: Duration
        get() = android.maxDownloadRetryTimeMillis.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            android.maxDownloadRetryTimeMillis = value.inWholeMilliseconds
        }

    /** The maximum time to retry operations other than upload and download if a failure occurs.  */
    actual var maxOperationRetryTime: Duration
        get() = android.maxOperationRetryTimeMillis.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            android.maxOperationRetryTimeMillis = value.inWholeMilliseconds
        }

    /** The maximum time to retry an upload if a failure occurs.  */
    actual var maxUploadRetryTime: Duration
        get() = android.maxUploadRetryTimeMillis.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            android.maxUploadRetryTimeMillis = value.inWholeMilliseconds
        }

    /** Creates a new StorageReference initialized at the root Firebase Storage location.   */
    actual fun getReference(): StorageReference = StorageReference(android.reference)

    /** Creates a new StorageReference initialized with a child Firebase Storage location.   */
    actual fun getReference(location: String): StorageReference = StorageReference(android.getReference(location))

    /** Creates a StorageReference given a gs:// or // URL pointing to a Firebase Storage location.   */
    actual fun getReferenceFromUrl(fullUrl: String): StorageReference = StorageReference(android.getReferenceFromUrl(fullUrl))

    /** Modifies this FirebaseStorage instance to communicate with the Storage emulator.  */
    actual fun useEmulator(host: String, port: Int) = android.useEmulator(host, port)
}

actual class StorageReference internal constructor(val android: com.google.firebase.storage.StorageReference) {

    actual val name: String
        get() = android.name
    actual val parent: StorageReference?
        get() = android.parent?.let { StorageReference(it) }
    actual val path: String
        get() = android.path
    actual val root: StorageReference
        get() = StorageReference(android.root)
    actual val bucket: String
        get() = android.bucket
    actual val storage: FirebaseStorage
        get() = FirebaseStorage(android.storage)

    internal actual val jobHandler = storage.jobHandler

    actual fun activeDownloadJobs(): List<FirebaseStorageJob.DownloadJob> = storage._activeDownloadJobs

    actual fun activeUploadJobs(): List<FirebaseStorageJob.UploadJob> = storage._activeUploadJobs


    actual fun child(path: String): StorageReference = StorageReference(android.child(path))

    actual suspend fun delete() = android.delete().await().run { Unit }

    actual suspend fun getDownloadUri(): String = android.downloadUrl.await().toString()


    actual suspend fun getMetadata(): StorageMetadata = StorageMetadata(android.metadata.await())

    actual suspend fun updateMetadata(metadata: StorageMetadata): StorageMetadata = StorageMetadata(android.updateMetadata(metadata.android).await())

    actual suspend fun list(
        maxResults: Int,
        pageToken: String?
    ): ListResult = ListResult(
        pageToken?.let { token ->
            android.list(maxResults, token).await()
        } ?: android.list(maxResults).await()
    )

    actual suspend fun listAll(): ListResult = ListResult(android.listAll().await())

    actual suspend fun getData(
        scope: CoroutineScope,
        maxDownloadSizeBytes: Long,
        onComplete: (data: Data?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(
        FirebaseStorageTask.Download.Data(
            android.getBytes(maxDownloadSizeBytes)
        ),
        jobHandler,
        scope
    )

    actual suspend fun getFile(
        scope: CoroutineScope,
        destination: URI,
        onComplete: (uri: URI?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(
        FirebaseStorageTask.Download.File(
            android.getFile(destination.android)
        ),
        jobHandler,
        scope
    )

    actual suspend fun getStream(
        scope: CoroutineScope,
        processor: StreamProcessor?,
        fallback: IosFallback.Download
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(
        FirebaseStorageTask.Download.Stream(
            processor?.let { android.getStream(processor) } ?: android.stream
        ),
        jobHandler,
        scope
    )



    actual suspend fun putData(
        scope: CoroutineScope,
        data: Data,
        metadata: StorageMetadata?,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit
    ): FirebaseStorageJob.UploadJob = FirebaseStorageJob.UploadJob(
        FirebaseStorageTask.Upload.Data(
            metadata?.let { android.putBytes(data.android, it.android) }
                ?: android.putBytes(data.android)
        ),
        jobHandler,
        scope
    )

    actual suspend fun putFile(
        scope: CoroutineScope,
        file: URI,
        metadata: StorageMetadata?,
        existingURI: URI?,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit
    ): FirebaseStorageJob.UploadJob = FirebaseStorageJob.UploadJob(
        FirebaseStorageTask.Upload.File(
            android.putFile(file.android, metadata?.android, existingURI?.android)
        ),
        jobHandler,
        scope
    )

    actual suspend fun putStream(
        scope: CoroutineScope,
        inputStream: Any,
        metadata: StorageMetadata?,
        fallback: IosFallback.Upload
    ): FirebaseStorageJob.UploadJob = FirebaseStorageJob.UploadJob(
        FirebaseStorageTask.Upload.Stream(
            metadata?.let { android.putStream(inputStream as InputStream, it.android) }
                ?: android.putStream(inputStream as InputStream)
        ),
        jobHandler,
        scope
    )
}

actual typealias StreamProcessor = com.google.firebase.storage.StreamDownloadTask.StreamProcessor

actual class URI internal constructor(val android: Uri) {
    actual companion object {
        actual fun fromString(stringUri: String): URI? = URI(Uri.parse(stringUri))
    }
}
actual class Data internal constructor(val android: ByteArray)
actual class Error


actual class StorageMetadata internal constructor(private val internal: com.google.firebase.storage.StorageMetadata) {
    private var builder = com.google.firebase.storage.StorageMetadata.Builder(internal)

    val android: com.google.firebase.storage.StorageMetadata
        get() = builder.build()

    actual val name: String?
        get() = android.name
    actual val path: String?
        get() = android.path
    actual val reference: StorageReference?
        get() = android.reference?.let { StorageReference(it) }
    actual val sizeInBytes: Long
        get() = android.sizeBytes
    actual val creationTimeMillis: Long
        get() = android.creationTimeMillis
    actual val updatedTimeMillis: Long
        get() = android.updatedTimeMillis
    actual val bucket: String?
        get() = android.bucket
    actual val generation: String?
        get() = android.generation
    actual val metadataGeneration: String?
        get() = android.metadataGeneration
    actual val md5Hash: String?
        get() = android.md5Hash
    actual val customMetadataKeys: Set<String>
        get() = android.customMetadataKeys

    actual var cacheControl: String?
        get() = builder.cacheControl
        set(value) {
            builder = builder.setCacheControl(value)
        }
    actual var contentDisposition: String?
        get() = builder.contentDisposition
        set(value) {
            builder = builder.setContentDisposition(value)
        }
    actual var contentEncoding: String?
        get() = builder.contentEncoding
        set(value) {
            builder = builder.setContentEncoding(value)
        }
    actual var contentLanguage: String?
        get() = builder.contentLanguage
        set(value) {
            builder = builder.setContentLanguage(value)
        }
    actual var contentType: String?
        get() = builder.contentType
        set(value) {
            builder = builder.setContentType(value)
        }

    actual fun setCustomMetadata(key: String, value: String?) {
        builder = builder.setCustomMetadata(key, value)
    }

    actual fun getCustomMetadata(key: String): String? = android.getCustomMetadata(key)
}

actual class ListResult internal constructor(val android: com.google.firebase.storage.ListResult) {
    actual val items: List<StorageReference>
        get() = android.items.map { StorageReference(it) }
    actual val pageToken: String?
        get() = android.pageToken
    actual val prefixes: List<StorageReference>
        get() = android.prefixes.map { StorageReference(it) }
}

actual typealias FirebaseStorageException = com.google.firebase.storage.StorageException
actual val FirebaseStorageException.code: StorageExceptionCode
    get() = StorageExceptionCode.fromErrorCode(errorCode)

actual enum class StorageExceptionCode(val internalCode: Int) {
    UNKNOWN(com.google.firebase.storage.StorageException.ERROR_UNKNOWN),
    OBJECT_NOT_FOUND(com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND),
    BUCKET_NOT_FOUND(com.google.firebase.storage.StorageException.ERROR_BUCKET_NOT_FOUND),
    PROJECT_NOT_FOUND(com.google.firebase.storage.StorageException.ERROR_PROJECT_NOT_FOUND),
    QUOTA_EXCEEDED(com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED),
    NOT_AUTHENTICATED(com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED),
    NOT_AUTHORIZED(com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED),
    RETRY_LIMIT_EXCEEDED(com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED),
    INVALID_CHECKSUM(com.google.firebase.storage.StorageException.ERROR_INVALID_CHECKSUM),
    CANCELED(com.google.firebase.storage.StorageException.ERROR_CANCELED);

    companion object {
        fun fromErrorCode(errorCode: Int): StorageExceptionCode
            = values().firstOrNull {
                it.internalCode == errorCode
            } ?: UNKNOWN
    }
}



actual sealed class FirebaseStorageTask {
    abstract val internalTask: Task<*>
    actual open class FirebaseStorageSnapshotBase internal constructor(
        val task: Task<*>
    ){
        actual val error: FirebaseStorageException?
            get() = task.exception?.let { FirebaseStorageException.fromException(it) }
    }

    actual sealed class Upload : FirebaseStorageTask() {
        actual abstract val snapshot: UploadSnapshot

        actual class UploadSnapshot internal constructor(
            val android: com.google.firebase.storage.UploadTask
        ): FirebaseStorageSnapshotBase(android) {
            actual val bytesUploaded: Long
                get() = android.snapshot.bytesTransferred
            actual val totalBytes: Long
                get() = android.snapshot.totalByteCount
            actual val uploadURI: URI?
                get() = android.snapshot.uploadSessionUri?.let { URI(it) }
            actual val metadata: StorageMetadata?
                get() = android.snapshot.metadata?.let { StorageMetadata(it) }
        }

        actual class Data internal constructor(
            override val internalTask: com.google.firebase.storage.UploadTask,

        ): Upload() {
            override val snapshot: UploadSnapshot
                get() = UploadSnapshot(internalTask)
        }
        actual class File internal constructor(
            override val internalTask: com.google.firebase.storage.UploadTask,
        ): Upload() {
            override val snapshot: UploadSnapshot
                get() = UploadSnapshot(internalTask)
        }
        actual class Stream internal constructor(
            override val internalTask: com.google.firebase.storage.UploadTask,
        ): Upload() {
            override val snapshot: UploadSnapshot
                get() = UploadSnapshot(internalTask)
        }

    }

    actual sealed class Download : FirebaseStorageTask() {
        actual class Data internal constructor(
            override val internalTask: Task<ByteArray>
        ) : Download()
        actual class File internal constructor(
            override val internalTask: com.google.firebase.storage.FileDownloadTask,
        ): Download() {
            actual val snapshot: DownloadSnapshot
                get() = DownloadSnapshot(internalTask)

            actual open class DownloadSnapshot internal constructor(
                val android: com.google.firebase.storage.FileDownloadTask
            ): FirebaseStorageSnapshotBase(android) {
                actual val totalBytes: Long
                    get() = android.snapshot.totalByteCount
                actual val bytesDownloaded: Long
                    get() = android.snapshot.bytesTransferred
            }
        }

        actual class Stream internal constructor(
            override val internalTask: com.google.firebase.storage.StreamDownloadTask
        ) : Download() {
            actual val snapshot: StreamDownloadSnapshot
                get() = StreamDownloadSnapshot(internalTask)

            actual class StreamDownloadSnapshot internal constructor(
                val android: com.google.firebase.storage.StreamDownloadTask
            ): FirebaseStorageSnapshotBase(android) {
                actual val totalBytes: Long
                    get() = android.snapshot.totalByteCount
                actual val bytesDownloaded: Long
                    get() = android.snapshot.bytesTransferred
                actual val stream: Any?
                    get() = android.snapshot.stream
            }
        }
    }
}

actual sealed class FirebaseStorageJob(scope: CoroutineScope) {
    actual abstract val task: FirebaseStorageTask
    actual abstract val jobHandler: JobHandler

    actual val job: Job = scope.launch {
        task.internalTask.await()
    }.apply {
        invokeOnCompletion { jobHandler.removeFromList(this@FirebaseStorageJob) }
    }

    actual fun cancel() = job.cancel()

    actual fun start(): Boolean {
        jobHandler.addToList(this)
        return job.start()
    }

    actual fun pause(): Boolean = (task.internalTask as? StorageTask<*>)?.pause() ?: false

    actual fun resume(): Boolean = (task.internalTask as? StorageTask<*>)?.resume() ?: false

    actual class DownloadJob internal constructor(
        override val task: FirebaseStorageTask.Download,
        override val jobHandler: JobHandler,
        scope: CoroutineScope
    ) : FirebaseStorageJob(scope)

    actual class UploadJob internal constructor(
        override val task: FirebaseStorageTask.Upload,
        override val jobHandler: JobHandler,
        scope: CoroutineScope
    ): FirebaseStorageJob(scope)
}