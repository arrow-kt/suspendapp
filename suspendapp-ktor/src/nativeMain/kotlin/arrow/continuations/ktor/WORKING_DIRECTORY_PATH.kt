package arrow.continuations.ktor

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual val WORKING_DIRECTORY_PATH: String
  get() = memScoped {
    val result = allocArray<ByteVar>(512)
    getcwd(result, 512u)
    result.toKString()
  }
