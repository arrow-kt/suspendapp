package io.arrow.suspendapp.example

import arrow.fx.coroutines.resource
import arrow.suspendapp.SuspendApp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

fun main() = SuspendApp {
  resource {
      install({ println("Creating some resource") }) { _, exitCase ->
        println("ExitCase: $exitCase")
        println("Shutting down will take 10 seconds")
        delay(10_000)
        println("Shutdown finished")
      }
    }
    .use {
      println("Application running with acquired resources.")
      awaitCancellation()
    }
}
