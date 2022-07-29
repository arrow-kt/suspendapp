package arrow.continuations.unsafe

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

expect object Unsafe {
  fun onShutdown(block: suspend () -> Unit): () -> Unit
  
  fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  )
}
