package arrow.continuations

import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.*

@Deprecated(
  "SuspendApp was moved to arrow.suspendapp",
  ReplaceWith("SuspendApp(context, timeout, block)", "arrow.suspendapp.SuspendApp"),
)
fun SuspendApp(
  context: CoroutineContext = Dispatchers.Default,
  timeout: Duration = Duration.INFINITE,
  block: suspend CoroutineScope.() -> Unit,
): Unit = arrow.suspendapp.SuspendApp(context = context, timeout = timeout, block = block)
