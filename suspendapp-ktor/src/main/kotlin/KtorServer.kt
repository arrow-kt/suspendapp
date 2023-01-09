package arrow.continuations.ktor

import arrow.fx.coroutines.Resource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Ktor [ApplicationEngine] as a [Resource]. This [Resource] will gracefully shut down the server
 * When we need to shut down a Ktor service we need to properly take into account a _grace_ period
 * where we still handle requests instead of immediately cancelling any in-flight requests.
 *
 * @param factory Application engine for processing the requests
 *
 * @param port Server listening port. Default is set to 80
 *
 * @param host Host address. Default is set to "0.0.0.0"
 *
 * @param configure Ktor server configuration parameters.
 *
 * @param preWait preWait a duration to wait before beginning the stop process. During this time,
 * requests will continue to be accepted. This setting is useful to allow time for the container to
 * be removed from the load balancer. This is disabled when `io.ktor.development=true`.
 *
 * @param grace grace a duration during which already inflight requests are allowed to continue
 * before the shutdown process begins.
 *
 * @param timeout timeout a duration after which the server will be forceably shutdown.
 *
 * @param module Represents configured and running web application, capable of handling requests.
 */
suspend fun <
  TEngine : ApplicationEngine,
  TConfiguration : ApplicationEngine.Configuration> ResourceScope.server(
  factory: ApplicationEngineFactory<TEngine, TConfiguration>,
  port: Int = 80,
  host: String = "0.0.0.0",
  configure: TConfiguration.() -> Unit = {},
  preWait: Duration = 30.seconds,
  grace: Duration = 500.milliseconds,
  timeout: Duration = 500.milliseconds,
  module: suspend Application.() -> Unit = {}
): ApplicationEngine =
  Resource(
    acquire = {
      embeddedServer(factory, host = host, port = port, configure = configure) {}.apply {
        module(application)
        start()
      }
    },
    release = { engine, _ ->
      if (!engine.environment.developmentMode) {
        engine.environment.log.info(
          "prewait delay of ${preWait.inWholeMilliseconds}ms, turn it off using io.ktor.development=true"
        )
        delay(preWait.inWholeMilliseconds)
      }
      engine.environment.log.info("Shutting down HTTP server...")
      engine.stop(grace.inWholeMilliseconds, timeout.inWholeMicroseconds)
      engine.environment.log.info("HTTP server shutdown!")
    }
  )
