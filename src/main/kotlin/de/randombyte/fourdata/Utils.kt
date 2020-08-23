package de.randombyte.fourdata

import java.io.File

fun File.pathWithoutRoot(root: File) = canonicalPath.removePrefix(root.canonicalPath)
