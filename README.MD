# Module suspendapp

# Rationale

When building applications that require graceful shutdown it typically requires us to write a bunch of platform specific code.
This library aims to solve that problem leveraging for Kotlin MPP using KotlinX Coroutines, and Structured Concurrency.

Currently supported targets:
 - JVM
 - MacOsX64 & MacosArm64
 - NodeJS
 - Windows (MingwX64)
 - Linux

## Running and testing apps

Just `./gradlew build` the project, and launch the created binaries as shown in the sections belows.

If you see `App Started! Waiting until asked to shutdown.` try pressing `ctrl+C` to signal interruption (`SIGINT`) to the process.
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

### Node App

```text
./gradlew build
node build/js/packages/YourAppName/kotlin/YourAppName.js

App Started! Waiting until asked to shutdown.
^CClosing resources..................... Done!
```

### Native App

```text
./gradlew build
build/bin/native/releaseExecutable/YourAppName.kexe

App Started! Waiting until asked to shutdown.
^CClosing resources..................... Done!
```
