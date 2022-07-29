package arrow.continuations.unsafe

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

expect object Unsafe {
  fun onShutdown(block: suspend () -> Unit): () -> Unit

  fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  )
}
