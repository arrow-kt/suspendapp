# Module suspendapp [![Maven Central](https://img.shields.io/maven-central/v/io.arrow-kt/suspendapp?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.arrow-kt/suspendapp)

> [!WARNING]
> Development of SuspendApp has moved to the [main Arrow repository](https://github.com/arrow-kt/arrow)

```kotlin
dependencies {
  implementation("io.arrow-kt:suspendapp:_")
}
```

## Rationale

When writing software we need to deal with the lifecycle of the application such as termination signals, and sending
correct exit codes. This is important, so we correctly interact with the OS our application runs on.
This requires a lot of platform-specific code, SuspendApp solves that problem by leveraging Kotlin MPP using KotlinX Coroutines, and Structured Concurrency. 
See [Simple Example below](#simple-example).

Currently supported targets:

- JVM
- MacOsX64 & MacosArm64
- NodeJS
- Windows (MingwX64)
- Linux

SuspendApp currently does not support any mobile or browser targets because it does not make sense to have such
application behavior on such platforms. If you have a use-case for this please open a ticket!

Let's see some simple examples that more clearly demonstrate the rationale for SuspendApp.

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

[Arrow Fx Coroutines Resource](https://apidocs.arrow-kt.io/arrow-fx-coroutines/arrow.fx.coroutines/-resource/index.html)
allows for modeling resources within the `suspend` world,
and properly takes into account structured concurrency and cancellation.
This means that when a `CoroutineScope` gets cancelled, then any `suspend finalizer` will _back pressure_ `Job#join`.
And thus when you call `cancelAndJoin` on a `CoroutineScope` it will properly await the `finalizers` to have finished
running.

With `SuspendApp` this means that if someone sends a terminal signal such as `SIGINT` or `SIGTERM` to the `App`
then it will run all the `suspend finalizers` before closing the `App`.

```kotlin
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
```

In the example above we have a `Resource` that during _acquisition_ will print `Creating some resource`,
when the `Resource` needs to be closed, _release_, we print the `ExitCase` with which the `Resource` was closed, and
then
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

## SuspendApp with Ktor

There are some cases where it is convenient to gracefully shutdown a `Ktor` server. Basically, it is about giving some
time to the server to finish some pending processing before turning it off.

Kubernetes is a good example of this need. When we're working with Kubernetes we often need to
support [Graceful Shutdown](https://cloud.google.com/blog/products/containers-kubernetes/kubernetes-best-practices-terminating-with-grace)
.
Kubernetes sends `SIGTERM` to our _Pod_ to signal it needs to gracefully shutdown.
However, there is an issue which doesn't allow us to immediately shutdown when we receive `SIGTERM` from Kubernetes.

Our pod can still receive traffic **after** `SIGTERM`, so we need to apply additional back-pressure to delay graceful
shutdown.
More information on this can be found in this blog by [Phil Pearl](https://philpearl.github.io/post/k8s_ingress/),
and on [learnk8s.io](https://learnk8s.io/graceful-shutdown).

The module `suspendapp-ktor` provides a `server` constructor that lifts the Ktor `ApplicationEngine` in to a `Resource`,
representing the _Engine_ running an `Application`(i.e `Netty`) while supporting auto-reload.
Check the official Ktor documentation to learn more about [watchPaths](https://ktor.io/docs/auto-reload.html).

When our `release` function of our `ApplicationEngine` is called, there is a `wait` period before the beginning of the
stop
process (defaulted to `30.seconds`), this gives Kubernetes enough time to do all its network management before we shut
down.
Two more parameters are available: `grace` which set the number of seconds during which already inflight requests are
allowed to continue before the shutdown process begins, and `timeout` which set the number of seconds after which the
server will be forceably shutdown. In the case that `ktor` server is set in
[development mode](https://ktor.io/docs/development-mode.html), the `wait` period is ignored.

Given this `Resource` definition of a Ktor server, with support for gracefully shutting down for K8S we can define
a `SuspendApp`.

```kotlin
fun main() = SuspendApp {
  resourceScope {
    server(Netty) {
      routing {
        get("/ping") {
          call.respond("pong")
        }
      }
    }
    awaitCancellation()
  }
}
```

We also use `awaitCancellation` here to _await_ `SIGTERM`, `SIGINT` or other shutdown hooks,
and we let the `server` `Resource` back-pressure closing the application for K8s.

## SuspendApp with Kafka

Gracefully shutting down is also often needed with other applications, beside K8S.
It can be useful in all kinds of applications that need to execute some code before getting shutdown.

Kafka for example, when streaming _records_ from Kafka we need to _commit_ (acknowledge) the offset of the _records_
we've processed.
The official recommendation for doing this is committing offsets in batches, so we typically don't send the commit event
to Kafka for every processed record.
Instead, we commit the offset every 5 seconds (or every x records, 5s is default).

Imagine the application getting stopped after 4,5 seconds, either by `ctrl+c` or `K8S` or another type of
containerization.
We could've processed thousands, or tens of thousands of events.
If we don't commit these offsets before shutting down we'd have to re-process all the events.

We can easily prevent this with `SuspendApp`, and [kotlin-kafka](https://github.com/nomisRev/kotlin-kafka)
or [reactor-kafka](https://github.com/reactor/reactor-kafka).
Both these high-level Kafka libraries guarantee committing offsets upon termination of the stream, this includes
cancellation!
In the example below, all calls to `acknowledge` will be committed to Kafka before the `SuspendApp` terminates when
receiving `SIGTERM` or `SIGINT`.

```kotlin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.apache.kafka.common.serialization.StringDeserializer
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import arrow.continuations.SuspendApp

fun main() = SuspendApp {
  val settings: ReceiverSettings<Nothing, String> = ReceiverSettings(
    bootstrapServers = kafka.bootstrapServers,
    groupId = "group-id",
    valueDeserializer = StringDeserializer()
  )
  KafkaReceiver(settings)
    .receive(topicName)
    .map { record ->
      println("${record.key()} -> ${record.value()}")
      record.offset.acknowledge()
    }.collect()
}
```

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
./gradlew compileProductionExecutableKotlinJs
node example/build/compileSync/js/main/productionExecutable/kotlin/suspendapp-example.js

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
./gradlew linkReleaseExecutableMacosArm64
build/bin/native/releaseExecutable/YourAppName.kexe

App Started! Waiting until asked to shutdown.
^CCleaning up App... will take 10 seconds...
Done cleaning up. Will release app to exit
```
