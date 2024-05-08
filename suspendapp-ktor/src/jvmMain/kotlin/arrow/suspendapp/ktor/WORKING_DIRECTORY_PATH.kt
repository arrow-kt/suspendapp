package arrow.suspendapp.ktor

import java.io.File

internal actual val WORKING_DIRECTORY_PATH: String
  get() = File(".").canonicalPath
