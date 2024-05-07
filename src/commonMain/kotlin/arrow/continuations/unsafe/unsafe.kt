package arrow.continuations.unsafe

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

expect object Unsafe {
  /**
   * Installs a shutdown hook:
   * - JVM: Regular shutdown hook. Exit with -1, or 0
   * - JS: NodeJS Signal Hook for SIGINT, and SIGTERM
   * - Native: Signal Hook for SIGINT, and SIGTERM
   *
   * @param context parameter used to run the blocking operation, for `runBlocking` in JS/Native,
   *   and GlobalScope.promise for JS.
   */
  fun onShutdown(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
  ): () -> Unit

  /**
   * Make sure to call exitProcess within [block], when encountering an [Throwable] print the trace
   * and exit with -1. If successful exit normally with 0.
   */
  fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  )

  fun exit(code: Int)
}
