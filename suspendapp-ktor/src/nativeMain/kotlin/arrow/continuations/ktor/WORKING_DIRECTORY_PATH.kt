package arrow.continuations.ktor

import kotlinx.cinterop.*
import platform.posix.*

internal actual val WORKING_DIRECTORY_PATH: String
  get() = memScoped {
    val result = allocArray<ByteVar>(512)
    getcwd(result, 512)
    result.toKString()
  }
