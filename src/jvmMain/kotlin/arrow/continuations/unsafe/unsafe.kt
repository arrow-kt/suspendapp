package arrow.continuations.unsafe

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual object Unsafe {
  actual fun onShutdown(block: suspend () -> Unit): () -> Unit {
    val isShutdown = AtomicBoolean(false)
    // ON JS and Native we return 143 and 130 on SIGTERM and SIGINT
    val hook =
      Thread(
        {
          runBlocking {
            isShutdown.set(true)
            runCatching { block() }.onFailure {
              // Change to default error handler lambda
              it.printStackTrace()
            }
          }
        },
        "Shutdown hook"
      )
    Runtime.getRuntime().addShutdownHook(hook)
    return {
      if (!isShutdown.get()) {
        Runtime.getRuntime().removeShutdownHook(hook)
      }
    }
  }

  actual fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  ): Unit = runBlocking(context, block)
}
