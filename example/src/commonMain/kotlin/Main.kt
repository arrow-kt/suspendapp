import arrow.continuations.SuspendApp
import arrow.fx.coroutines.Resource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

fun main() = SuspendApp {
  Resource(
      acquire = { println("Creating some resource") },
      release = { _, exitCase ->
        println("ExitCase: $exitCase")
        println("Shutting down will take 10 seconds")
        delay(10_000)
        println("Shutdown finished")
      }
    )
    .use {
      println("Application running with acquired resources.")
      awaitCancellation()
    }
}
