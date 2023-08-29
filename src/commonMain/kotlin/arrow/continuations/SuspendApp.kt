package arrow.continuations

import arrow.continuations.unsafe.Unsafe
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * An unsafe blocking edge that wires the [CoroutineScope] (and structured concurrency) to the
 * [SuspendApp], such that the [CoroutineScope] gets cancelled when the `App` is requested to
 * gracefully shutdown. => `SIGTERM` & `SIGINT` on Native & NodeJS and a ShutdownHook for JVM.
 *
 * It applies backpressure to the process such that they can gracefully shutdown.
 *
 * @param context the [CoroutineContext] where [block] will execute. Use [EmptyCoroutineContext] to
 *   create an `CoroutineDispatcher` for the main thread and run there instead.
 * @param timeout the maximum backpressure time that can be applied to the process. This emulates a
 *   `SIGKILL` command, and after the [timeout] is passed the App will forcefully shut down
 *   regardless of finalizers.
 * @param block the lambda of the actual application.
 */
fun SuspendApp(
  context: CoroutineContext = Dispatchers.Default,
  timeout: Duration = Duration.INFINITE,
  block: suspend CoroutineScope.() -> Unit,
): Unit =
  Unsafe.runCoroutineScope(context) {
    val job: Job = launch(context = context, start = CoroutineStart.LAZY) { block() }
    val unregister: () -> Unit = Unsafe.onShutdown { withTimeout(timeout) { job.cancelAndJoin() } }
    job.start()
    job.join()
    unregister()
  }
