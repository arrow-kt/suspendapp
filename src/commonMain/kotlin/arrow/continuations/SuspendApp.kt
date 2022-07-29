package arrow.continuations

import arrow.continuations.unsafe.Unsafe
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * # Module SuspendApp
 *
 * # Rationale When building applications that require graceful shutdown it typically requires us to
 * write a bunch of platform specific code. This library offers a common API to write such
 * applications targeting the JVM, Native or NodeJS.
 *
 * ## SuspendApp
 *
 * SuspendApp allows for building applications that leverage `CoroutineScope`, and structured
 * concurrency as a way of reasoning about resource-safety. When we're writing apps that need to be
 * able to _gracefully shutdown_, then you want your resource handlers to run on SIGTERM, SIGINT or
 * JVM Shutdown.
 *
 * Let's look at some examples.
 *
 * ```kotlin
 * import arrow.continuations.SuspendApp
 * import kotlinx.coroutines.CancellationException
 * import kotlinx.coroutines.NonCancellable
 * import kotlinx.coroutines.delay
 * import kotlinx.coroutines.withContext
 *
 * fun main() = SuspendApp {
 *   try {
 *     println("App Started!  Waiting until asked to shutdown.")
 *     while (true) {
 *       delay(2_500)
 *       println("Ping")
 *     }
 *   } catch (e: CancellationException) {
 *     println("Cleaning up App... will take 10 seconds...")
 *     withContext(NonCancellable) { delay(10_000) }
 *     println("Done cleaning up. Will release app to exit")
 *   }
 * }
 * ```
 * ```text
 * App Started! Waiting until asked to shutdown.
 * Ping
 * Closing resources..................... Done!
 *
 * Process finished with exit code 130 (interrupted by signal 2: SIGINT)
 * ```
 *
 * # SuspendApp with Arrow Fx Resource TODO
 *
 * ## SuspendApp with Arrow Fx Resource, Ktor & K8s TODO
 *
 * ## SuspendApp with Kotlin-Kafka TODO
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
