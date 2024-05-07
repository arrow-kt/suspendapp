package arrow.continuations.unsafe

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import sun.misc.Signal
import sun.misc.SignalHandler

actual object Unsafe {
  actual fun onShutdown(context: CoroutineContext, block: suspend () -> Unit): () -> Unit {
    val isShutdown = AtomicBoolean(false)
    // ON JS and Native we return 143 and 130 on SIGTERM and SIGINT
    val hook =
      Thread(
        {
          if (!isShutdown.getAndSet(true)) {
            runBlocking(context) {
              runCatching {
                  block()
                  exitProcess(0)
                }
                .onFailure {
                  // Change to default error handler lambda
                  it.printStackTrace()
                  exitProcess(-1)
                }
            }
          }
        },
        "Arrow-kt SuspendApp JVM ShutdownHook"
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

  fun onSignal(context: CoroutineContext, signal: String, handle: suspend () -> Unit) =
    try {
      var handler: SignalHandler? = null
      handler =
        Signal.handle(Signal(signal)) { s ->
          runBlocking(context) { handle() }
          if (handler != SignalHandler.SIG_DFL && handler != SignalHandler.SIG_IGN) {
            handler?.handle(s)
          }
        }
    } catch (e: Throwable) {
      // TODO: Currently ignore all exceptions here
    }

  actual fun exit(code: Int) {
    exitProcess(code)
  }
}
