package dev.gitlive.firebase.storage

import cocoapods.FirebaseStorage.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.*
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/** Returns the FirebaseStorage, initialized with the default FirebaseApp. */
actual val Firebase.storage: FirebaseStorage
    get() = TODO("Not yet implemented")

/** Returns the FirebaseStorage, initialized with the default FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(bucketUrl: String): FirebaseStorage =
    FirebaseStorage(cocoapods.FirebaseStorage.FIRStorage.storageWithURL(bucketUrl))
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp */
actual fun Firebase.storage(app: FirebaseApp): FirebaseStorage =
    FirebaseStorage(cocoapods.FirebaseStorage.FIRStorage.storageForApp(app.ios))
/** Returns the FirebaseStorage, initialized with a custom FirebaseApp and a custom Storage Bucket.  */
actual fun Firebase.storage(app: FirebaseApp, bucketUrl: String): FirebaseStorage =
    FirebaseStorage(cocoapods.FirebaseStorage.FIRStorage.storageForApp(app.ios, bucketUrl))

@OptIn(ExperimentalTime::class)
actual class FirebaseStorage internal constructor(val ios: cocoapods.FirebaseStorage.FIRStorage){
    internal actual val _activeUploadJobs: MutableList<FirebaseStorageJob.UploadJob> = mutableListOf()
    internal actual val _activeDownloadJobs: MutableList<FirebaseStorageJob.DownloadJob> = mutableListOf()

    actual val activeUploadJobs: List<FirebaseStorageJob.UploadJob> = _activeUploadJobs
    actual val activeDownloadJobs: List<FirebaseStorageJob.DownloadJob> = _activeDownloadJobs

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

    /** The maximum time to retry a download if a failure occurs. */
    actual var maxDownloadRetryTime: Duration
        get() = ios.maxDownloadRetryTime.toDuration(DurationUnit.SECONDS)
        set(value) {
            ios.maxDownloadRetryTime = value.inWholeSeconds.toDouble()
        }

    /** The maximum time to retry operations other than upload and download if a failure occurs.  */
    actual var maxOperationRetryTime: Duration
        get() = ios.maxOperationRetryTime.toDuration(DurationUnit.SECONDS)
        set(value) {
            ios.maxOperationRetryTime = value.inWholeSeconds.toDouble()
        }

    /** The maximum time to retry an upload if a failure occurs.  */
    actual var maxUploadRetryTime: Duration
        get() = ios.maxUploadRetryTime.toDuration(DurationUnit.SECONDS)
        set(value) {
            ios.maxUploadRetryTime = value.inWholeSeconds.toDouble()
        }

    /** Creates a new StorageReference initialized at the root Firebase Storage location.   */
    actual fun getReference(): StorageReference = StorageReference(ios.reference())

    /** Creates a new StorageReference initialized with a child Firebase Storage location.   */
    actual fun getReference(location: String): StorageReference = StorageReference(ios.referenceWithPath(location))

    /** Creates a StorageReference given a gs:// or // URL pointing to a Firebase Storage location.   */
    actual fun getReferenceFromUrl(fullUrl: String): StorageReference = StorageReference(ios.referenceForURL(fullUrl))

    /** Modifies this FirebaseStorage instance to communicate with the Storage emulator.  */
    actual fun useEmulator(host: String, port: Int) { /*NOOP*/ }
}

actual class StorageReference internal constructor(val ios: cocoapods.FirebaseStorage.FIRStorageReference) {

    actual val name: String
        get() = ios.name
    actual val parent: StorageReference?
        get() = ios.parent()?.let { StorageReference(it) }
    actual val path: String
        get() = ios.fullPath
    actual val root: StorageReference
        get() = StorageReference(ios.root())
    actual val bucket: String
        get() = ios.bucket
    actual val storage: FirebaseStorage
        get() = FirebaseStorage(ios.storage)

    internal actual val jobHandler: JobHandler = storage.jobHandler

    actual fun activeDownloadJobs(): List<FirebaseStorageJob.DownloadJob> = storage._activeDownloadJobs

    actual fun activeUploadJobs(): List<FirebaseStorageJob.UploadJob> = storage._activeUploadJobs



    actual fun child(path: String): StorageReference = StorageReference(ios.child(path))

    actual suspend fun delete() = await { ios.deleteWithCompletion(it) }



    actual suspend fun getDownloadUri(): String = awaitResult<NSURL> { ios.downloadURLWithCompletion(it) }.run { toString() }



    actual suspend fun getMetadata(): StorageMetadata = StorageMetadata(
        awaitResult { ios.metadataWithCompletion(it) }
    )

    actual suspend fun updateMetadata(metadata: StorageMetadata): StorageMetadata = StorageMetadata(
        awaitResult { ios.updateMetadata(metadata.ios, it) }
    )



    actual suspend fun list(
        maxResults: Int,
        pageToken: String?
    ): ListResult = ListResult(
        awaitResult {
            pageToken?.let { token ->
                ios.listWithMaxResults(maxResults.toLong(), token, it)
            } ?: ios.listWithMaxResults(maxResults.toLong(), it)
        }
    )

    actual suspend fun listAll(): ListResult = ListResult(
        awaitResult { ios.listAllWithCompletion(it) }
    )



    actual suspend fun getData(
        scope: CoroutineScope,
        maxDownloadSizeBytes: Long,
        onComplete: (data: Data?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(
        FirebaseStorageTask.Download.Data(
            ios.dataWithMaxSize(maxDownloadSizeBytes) { data: NSData?, error: NSError? ->
                onComplete(data?.let { Data(it) }, error?.let { Error(it) })
            }
        ),
        jobHandler,
        scope,
    )

    actual suspend fun getFile(
        scope: CoroutineScope,
        destination: URI,
        onComplete: (uri: URI?, error: Error?) -> Unit
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(
        FirebaseStorageTask.Download.File(
            ios.writeToFile(destination.ios) { uri: NSURL?, error: NSError? ->
                onComplete(uri?.let { URI(it) }, error?.let { Error(it) })
            }
        ),
        jobHandler,
        scope
    )

    actual suspend fun getStream(
        scope: CoroutineScope,
        processor: StreamProcessor?,
        fallback: IosFallback.Download
    ): FirebaseStorageJob.DownloadJob = FirebaseStorageJob.DownloadJob(fallback.task, jobHandler, scope)



    actual suspend fun putData(
        scope: CoroutineScope,
        data: Data,
        metadata: StorageMetadata?,
        onComplete: (metadata: StorageMetadata?, error: Error?) -> Unit
    ): FirebaseStorageJob.UploadJob = FirebaseStorageJob.UploadJob(
        FirebaseStorageTask.Upload.Data(
            ios.putData(
                data.ios,
                metadata?.ios
            ) { returnedMetadata: FIRStorageMetadata?, error: NSError? ->
                onComplete(returnedMetadata?.let { StorageMetadata(it) }, error?.let { Error(it) })
            }
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
            ios.putFile(
                file.ios,
                metadata?.ios
            ) { returnedMetadata: FIRStorageMetadata?, error: NSError? ->
                onComplete(returnedMetadata?.let { StorageMetadata(it) }, error?.let { Error(it) })
            }
        ),
        jobHandler,
        scope
    )

    actual suspend fun putStream(
        scope: CoroutineScope,
        inputStream: Any,
        metadata: StorageMetadata?,
        fallback: IosFallback.Upload
    ): FirebaseStorageJob.UploadJob = FirebaseStorageJob.UploadJob(fallback.task, jobHandler, scope)
}

actual interface StreamProcessor

actual class URI internal constructor(val ios: NSURL) {
    actual companion object {
        actual fun fromString(stringUri: String): URI? = NSURL.URLWithString(stringUri)?.let { URI(it) }
    }
}

actual class Error internal constructor(val ios: NSError)
actual class Data internal constructor(val ios: NSData)


actual class StorageMetadata internal constructor(
    val ios: cocoapods.FirebaseStorage.FIRStorageMetadata
) {
    actual val name: String?
        get() = ios.name
    actual val path: String?
        get() = ios.path
    actual val reference: StorageReference?
        get() = ios.storageReference?.let { StorageReference(it) }
    actual val sizeInBytes: Long
        get() = ios.size
    actual val creationTimeMillis: Long
        get() = ios.timeCreated?.timeIntervalSinceReferenceDate?.toLong() ?: 0L
    actual val updatedTimeMillis: Long
        get() = ios.updated?.timeIntervalSinceReferenceDate?.toLong() ?: 0L
    actual val bucket: String?
        get() = ios.bucket
    actual val generation: String?
        get() = ios.generation.toString()
    actual val metadataGeneration: String?
        get() = ios.metageneration.toString()
    actual val md5Hash: String?
        get() = ios.md5Hash
    actual val customMetadataKeys: Set<String>
        get() = ios.customMetadata?.keys as? Set<String>  ?: setOf()

    actual var cacheControl: String? = ios.cacheControl
    actual var contentDisposition: String? = ios.contentDisposition
    actual var contentEncoding: String? = ios.contentEncoding
    actual var contentLanguage: String? = ios.contentLanguage
    actual var contentType: String? = ios.contentType

    actual fun getCustomMetadata(key: String): String? = ios.customMetadata?.get(key)?.toString()
    actual fun setCustomMetadata(key: String, value: String?) = ios.setCustomMetadata(mapOf(key to value))

}

actual class ListResult internal constructor(val ios: cocoapods.FirebaseStorage.FIRStorageListResult) {
    actual val items: List<StorageReference>
        get() = ios.items.map { StorageReference(it as cocoapods.FirebaseStorage.FIRStorageReference) }
    actual val pageToken: String?
        get() = ios.pageToken
    actual val prefixes: List<StorageReference>
        get() = ios.prefixes.map { StorageReference(it as cocoapods.FirebaseStorage.FIRStorageReference) }
}


actual sealed class FirebaseStorageTask {
    abstract val controller: FIRStorageTaskManagementProtocol?
    actual open class FirebaseStorageSnapshotBase internal constructor(
        open val ios: FIRStorageTask?
    ){
        actual val error: FirebaseStorageException?
            get() = ios?.snapshot?.error?.let { it.toException() }
    }

    actual sealed class Upload : FirebaseStorageTask() {
        actual abstract val snapshot: UploadSnapshot

        actual class UploadSnapshot internal constructor(
            override val ios: FIRStorageUploadTask?
        ): FirebaseStorageSnapshotBase(ios) {
            actual val bytesUploaded: Long
                get() = ios?.snapshot?.progress?.completedUnitCount ?: -1L
            actual val totalBytes: Long
                get() = ios?.snapshot?.progress?.completedUnitCount ?: -1L
            actual val uploadURI: URI? = null
            actual val metadata: StorageMetadata?
                get() = ios?.snapshot?.metadata?.let { StorageMetadata(it) }
        }

        actual class Data internal constructor(
            val ios: FIRStorageUploadTask,
            override val controller: FIRStorageTaskManagementProtocol? = ios
        ): Upload(){
            override val snapshot: UploadSnapshot
                get() = UploadSnapshot(ios)
        }
        actual class File internal constructor(
            val ios: FIRStorageUploadTask,
            override val controller: FIRStorageTaskManagementProtocol? = ios
        ): Upload() {
            override val snapshot: UploadSnapshot
                get() = UploadSnapshot(ios)
        }
        actual class Stream: Upload() {
            init { throw UnsupportedOperationException("Stream upload not supported on this platform") }
            override val snapshot: UploadSnapshot = UploadSnapshot(null)
            override val controller: FIRStorageTaskManagementProtocol? = null
        }

    }

    actual sealed class Download : FirebaseStorageTask() {
        actual class Data internal constructor(
            val ios: FIRStorageDownloadTask,
            override val controller: FIRStorageTaskManagementProtocol? = ios
        ): Download()
        actual class File internal constructor(
            val ios: FIRStorageDownloadTask,
            override val controller: FIRStorageTaskManagementProtocol? = ios
        ) : Download() {
            actual val snapshot: DownloadSnapshot
                get() = DownloadSnapshot(ios)

            actual open class DownloadSnapshot internal constructor(
                override val ios: FIRStorageDownloadTask
            ): FirebaseStorageSnapshotBase(ios) {
                actual val totalBytes: Long
                    get() = ios.snapshot.progress?.totalUnitCount ?: -1L
                actual val bytesDownloaded: Long
                    get() = ios.snapshot.progress?.completedUnitCount ?: -1L
            }
        }

        actual class Stream : Download() {
            init { throw UnsupportedOperationException("Stream download not supported on this platform") }
            override val controller: FIRStorageTaskManagementProtocol? = null
            actual val snapshot: StreamDownloadSnapshot = StreamDownloadSnapshot()

            actual class StreamDownloadSnapshot:
                FirebaseStorageSnapshotBase(null) {
                actual val totalBytes: Long
                    get() = -1L
                actual val bytesDownloaded: Long
                    get() = -1L
                actual val stream: Any?
                    get() = null
            }
        }
    }
}


actual sealed class FirebaseStorageJob(
    val scope: CoroutineScope,
){
    actual abstract val task: FirebaseStorageTask
    actual abstract val jobHandler: JobHandler

    actual val job: Job
        get() = scope.launch(Dispatchers.Main) {
            task.controller?.enqueue()
        }.apply {
            invokeOnCompletion { jobHandler.removeFromList(this@FirebaseStorageJob) }
        }

    actual fun start(): Boolean {
        jobHandler.addToList(this)
        return job.start()
    }

    actual fun cancel() = task.controller?.cancel() ?: job.cancel()

    actual fun pause(): Boolean = task.controller?.pause().run { true }

    actual fun resume(): Boolean = task.controller?.resume().run { true }
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



actual class FirebaseStorageException(message: String, val code: StorageExceptionCode) : FirebaseException(message)

actual val FirebaseStorageException.code: StorageExceptionCode
    get() = code

actual enum class StorageExceptionCode {
    OBJECT_NOT_FOUND,
    BUCKET_NOT_FOUND,
    PROJECT_NOT_FOUND,
    QUOTA_EXCEEDED,
    NOT_AUTHENTICATED,
    NOT_AUTHORIZED,
    RETRY_LIMIT_EXCEEDED,
    INVALID_CHECKSUM,
    CANCELED,
    UNKNOWN
}

fun NSError.toException() = when(domain) {
    FIRStorageErrorDomain -> when(code) {
        FIRStorageErrorCodeObjectNotFound -> StorageExceptionCode.OBJECT_NOT_FOUND
        FIRStorageErrorCodeBucketNotFound -> StorageExceptionCode.BUCKET_NOT_FOUND
        FIRStorageErrorCodeProjectNotFound -> StorageExceptionCode.PROJECT_NOT_FOUND
        FIRStorageErrorCodeQuotaExceeded -> StorageExceptionCode.QUOTA_EXCEEDED
        FIRStorageErrorCodeUnauthenticated -> StorageExceptionCode.NOT_AUTHENTICATED
        FIRStorageErrorCodeUnauthorized -> StorageExceptionCode.NOT_AUTHORIZED
        FIRStorageErrorCodeRetryLimitExceeded -> StorageExceptionCode.RETRY_LIMIT_EXCEEDED
        FIRStorageErrorCodeNonMatchingChecksum -> StorageExceptionCode.INVALID_CHECKSUM
        FIRStorageErrorCodeCancelled -> StorageExceptionCode.CANCELED
        else -> StorageExceptionCode.UNKNOWN
    }
    else -> StorageExceptionCode.UNKNOWN
}.let { FirebaseStorageException(description!!, it) }



suspend inline fun <reified T> awaitResult(function: (callback: (T?, NSError?) -> Unit) -> Unit): T {
    val job = CompletableDeferred<T?>()
    function { result, error ->
        if(error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as T
}

suspend inline fun <T> await(function: (callback: (NSError?) -> Unit) -> T): T {
    val job = CompletableDeferred<Unit>()
    val result = function { error ->
        if(error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
    return result
}