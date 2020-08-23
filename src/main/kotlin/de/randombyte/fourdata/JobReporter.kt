package de.randombyte.fourdata

import de.randombyte.fourdata.archive.Archive
import de.randombyte.fourdata.archive.FileEntry

abstract class JobReporter {
    companion object {
        const val UNKNOWN_TOTAL = -1
    }

    private var ended = false

    protected abstract fun onProgress(current: Int, total: Int)
    protected abstract fun onEnd(result: Result)

    fun progress(current: Int, total: Int) = onProgress(current, total)

    fun end(result: Result) {
        if (ended) throw IllegalStateException("This job already ended!")
        ended = true
        onEnd(result)
    }

    sealed class Result {
        object Success : Result()
        class Error(val message: String) : Result()
        object StoppedByUser : Result()
        object ArchiveAlreadyExists : Result()
        object ArchiveNotFound : Result()

        sealed class TypedResult<T>(val value: T) : Result() {
            sealed class MessageResult(message: String) : TypedResult<String>(message) {
                class GenericError(message: String) : MessageResult(message)
                class FileNotFound(message: String) : MessageResult(message)
                class CanNotReadArchiveEntry(message: String) : MessageResult(message)
            }

            class FileEntries(val entries: List<FileEntry>) : TypedResult<List<FileEntry>>(entries)

            class ArchiveCreated(val archive: Archive) : TypedResult<Archive>(archive)
        }
    }
}