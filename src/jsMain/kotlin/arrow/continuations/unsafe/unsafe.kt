package arrow.continuations.unsafe

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.js.Promise
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise

actual object Unsafe {
  actual fun onShutdown(context: CoroutineContext, block: suspend () -> Unit): () -> Unit {
    suspend fun run(code: Int): Result<Unit> =
      runCatching {
          block()
          exit(code)
        }
        .onFailure {
          it.printStackTrace()
          exit(-1)
        }

    onSignal(context, "SIGTERM") { run(143) }
    onSignal(context, "SIGINT") { run(130) }
    return { /* Nothing to unregister */}
  }

  actual fun runCoroutineScope(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
  ) {
    val innerJob = Job()
    val innerScope = CoroutineScope(innerJob)
    suspend {
        // An infinite heartbeat to keep main alive.
        // The tick is fast enough that  it isn't silently discarded, as longer ticks are,
        // but slow enough that we don't interrupt often.
        // https://stackoverflow.com/questions/23622051/how-to-forcibly-keep-a-node-js-process-from-terminating
        val keepAlive: Job =
          innerScope.launch {
            while (isActive) {
              // Schedule a `no-op tick` by returning to main every hour with no actual work but
              // just looping.
              delay(1.hours)
            }
          }

        block(innerScope)
        keepAlive.cancelAndJoin()
      }
      .startCoroutine(Continuation(context) {})
  }

  fun onSignal(context: CoroutineContext, signal: String, handle: suspend () -> Unit) {
    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("UNUSED_VARIABLE")
    val provide: () -> Promise<Unit> = { GlobalScope.promise(context) { handle() } }
    js("process.on(signal, function() {\n" + "  provide()\n" + "});")
  }

  actual fun exit(code: Int) {
    runCatching { js("process.exit(i)") }
    throw RuntimeException(
      "process.exit($code) returned normally, while it was supposed to halt NodeJS."
    )
  }
}
