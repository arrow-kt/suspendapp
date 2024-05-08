package io.arrow.suspendapp.example

import arrow.continuations.SuspendApp
import arrow.fx.coroutines.resource
import arrow.fx.coroutines.resourceScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

fun main() = SuspendApp {
  resourceScope {
    install({ println("Creating some resource") }) { _, exitCase ->
      println("ExitCase: $exitCase")
      println("Shutting down will take 10 seconds")
      delay(10_000)
      println("Shutdown finished")
    }
    println("Application running with acquired resources.")
    awaitCancellation()
  }
}
