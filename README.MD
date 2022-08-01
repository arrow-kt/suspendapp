# Module suspendapp

[![Maven Central](https://img.shields.io/maven-central/v/io.arrow-kt/suspendapp?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.arrow-kt/suspendapp)

```kotlin
dependencies {
  implementation("io.arrow-kt:suspendapp:_")
}
```

## Rationale

When building applications that require graceful shutdown it typically requires us to write a bunch of platform specific
code.
This library aims to solve that problem leveraging for Kotlin MPP using KotlinX Coroutines, and Structured Concurrency.

Currently supported targets:

- JVM
- MacOsX64 & MacosArm64
- NodeJS
- Windows (MingwX64)
- Linux

SuspendApp currently does not support any mobile or browser targets because it does not make sense to have such
application behavior on such platforms. If you have a use-case for this please open a ticket!

Let's see some simple example that more clearly demonstrate the rationale for SuspendApp.

## Simple example

If you see `App Started! Waiting until asked to shutdown.` try pressing `ctrl+C` to signal interruption (`SIGINT`) to
the process.
You can also use `ps -ax` to find the `PID` and call `kill PID` to send a `SIGTERM` event to the process.

```kotlin
import arrow.continuations.SuspendApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun main() = SuspendApp {
  try {
    println("App Started!  Waiting until asked to shutdown.")
    while (true) {
      delay(2_500)
      println("Ping")
    }
  } catch (e: CancellationException) {
    println("Cleaning up App... will take 10 seconds...")
    withContext(NonCancellable) { delay(10_000) }
    println("Done cleaning up. Will release app to exit")
  }
}
```

Note: since our `CoroutineScope` is cancelled we need to run our `delay` in `NonCancelable`.

## SuspendApp Arrow's Resource

[Arrow Fx Coroutines Resource](https://arrow-kt.io/docs/apidocs/arrow-fx-coroutines/arrow.fx.coroutines/-resource/)
allows for modeling resources within the `suspend` world,
and properly takes into account structured concurrency and cancellation.
This means that when a `CoroutineScope` gets cancelled, then any `suspend finalizer` will _back pressure_ `Job#join`.
And thus when you call `cancelAndJoin` on a `CoroutineScope` it will properly await the `finalizers` to have finished
running.

With `SuspendApp` this means that if someone sends a terminal signal such as `SIGINT` or `SIGTERM` to the `App`
then it will run all the `suspend finalizers` before closing the `App`.

```kotlin
fun main() = SuspendApp {
  Resource(
    acquire = { println("Creating some resource") },
    release = { _, exitCase ->
      println("ExitCase: $exitCase")
      println("Shutting down will take 10 seconds")
      delay(10_000)
      println("Shutdown finished")
    }
  ).use {
    println("Application running with acquired resources.")
    awaitCancellation()
  }
}
```

In the example above we have a `Resource` that during _acquisition_ will print `Creating some resource`,
when the `Resource` needs to be closed, _release_, we print the `ExitCase` with which the `Resource` was closed, and then
we wait for 10 seconds. The `Resource` already takes care of calling `release` on a `NonCancelable` context.

We consume the `Resource` until our `App` is cancelled by calling `awaitCancellation` from KotlinX Coroutines.
That gives us the following output, if you press `ctrl+c` in the terminal.

```text
Creating some resource
Application running with acquired resources.
^CExitCase: Cancelled(exception=kotlinx.coroutines.JobCancellationException: LazyStandaloneCoroutine was cancelled; job=LazyStandaloneCoroutine{Cancelling}@f7470010)
Shutting down will take 10 seconds
Shutdown finished
```

You can find this example in the `example` module, currently setup for NodeJS and native targets.

## SuspendApp with Ktor on Kubernetes

## SuspendApp with Kafka

## Running SuspendApp applications on different platforms

A small tutorial on how you can configure and run SuspendApp on the different platforms.
For more details on Kotlin Multiplatform configuration consult the official documentation [here]().
Just `./gradlew build` the project, and launch the created binaries as shown in the sections belows.

### Node App

Make sure you configure your NodeJS app to be executable.

```kotlin
js(IR) {
  nodejs {
    binaries.executable()
  }
}
```

You can run your NodeJS app with the following `node` command,
and if you press `ctrl+c` within the first 2500ms you will see the following output.

```text
node build/js/packages/YourAppName/kotlin/YourAppName.js

App Started! Waiting until asked to shutdown.
^CCleaning up App... will take 10 seconds...
Done cleaning up. Will release app to exit
```

### Native App

Make sure you configure your Native app(s) to be executable.

```kotlin
linuxX64 {
  binaries.executable()
}
mingwX64 {
  binaries.executable()
}
macosArm64 {
  binaries.executable()
}
macosX64 {
  binaries.executable()
}
```

You can run your Native app with the following command,
and if you press `ctrl+c` within the first 2500ms you will see the following output.

```text
./gradlew build
build/bin/native/releaseExecutable/YourAppName.kexe

App Started! Waiting until asked to shutdown.
^CCleaning up App... will take 10 seconds...
Done cleaning up. Will release app to exit
```
