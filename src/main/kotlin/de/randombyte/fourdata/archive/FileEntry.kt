package de.randombyte.fourdata.archive

import kotlinx.serialization.Serializable

@Serializable
class FileEntry(val path: String, val md5Sum: String) {
    fun isSamePath(other: FileEntry) = path == other.path
    fun isSameMd5Sum(other: FileEntry) = md5Sum == other.md5Sum
}
