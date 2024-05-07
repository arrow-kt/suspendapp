package arrow.continuations.unsafe

import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.signal

@SharedImmutable private val SIGNAL: CompletableDeferred<Int> = CompletableDeferred()

@SharedImmutable private val BACKPRESSURE: CompletableDeferred<Int> = CompletableDeferred()

@SharedImmutable
@OptIn(ExperimentalForeignApi::class)
private val SignalHandler =
  staticCFunction<Int, Unit> { code ->
    SIGNAL.complete(code)
    val finalCode: Int = runBlocking { BACKPRESSURE.await() }
    exitProcess(finalCode)
  }

actual object Unsafe {
  @OptIn(DelicateCoroutinesApi::class, ExperimentalForeignApi::class)
  actual fun onShutdown(context: CoroutineContext, block: suspend () -> Unit): () -> Unit {
    GlobalScope.launch(context) {
      val code: Int = SIGNAL.await()
      val res: Result<Unit> = runCatching { block() }
      BACKPRESSURE.complete(res.fold({ code }, { -1 }))
    }

    signal(SIGTERM, SignalHandler)
    signal(SIGINT, SignalHandler)
    return { /* Nothing to unregister */}
  }

  actual fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  ): Unit = runBlocking(context, block)

  actual fun exit(code: Int) {
    exitProcess(code)
  }
}
