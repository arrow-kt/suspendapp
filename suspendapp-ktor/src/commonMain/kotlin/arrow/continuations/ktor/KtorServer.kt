package arrow.continuations.ktor

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.ResourceScope
import arrow.suspendapp.ktor.WORKING_DIRECTORY_PATH
import arrow.suspendapp.ktor.server as kserver
import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Ktor [ApplicationEngine] as a [Resource]. This [Resource] will gracefully shut down the server
 * When we need to shut down a Ktor service we need to properly take into account a _grace_ period
 * where we still handle requests instead of immediately cancelling any in-flight requests.
 *
 * @param factory Application engine for processing the requests
 * @param port Server listening port. Default is set to 80
 * @param host Host address. Default is set to "0.0.0.0"
 * @param watchPaths specifies path substrings that will be watched for automatic reloading
 * @param configure Ktor server configuration parameters. Only this function is taken into account
 *   for auto-reload.
 * @param preWait preWait a duration to wait before beginning the stop process. During this time,
 *   requests will continue to be accepted. This setting is useful to allow time for the container
 *   to be removed from the load balancer. This is disabled when `io.ktor.development=true`.
 * @param grace grace a duration during which already inflight requests are allowed to continue
 *   before the shutdown process begins.
 * @param timeout timeout a duration after which the server will be forceably shutdown.
 * @param module Represents configured and running web application, capable of handling requests.
 */
suspend fun <
  TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> ResourceScope
  .server(
  factory: ApplicationEngineFactory<TEngine, TConfiguration>,
  port: Int = 80,
  host: String = "0.0.0.0",
  watchPaths: List<String> = listOf(WORKING_DIRECTORY_PATH),
  configure: TConfiguration.() -> Unit = {},
  preWait: Duration = 30.seconds,
  grace: Duration = 500.milliseconds,
  timeout: Duration = 500.milliseconds,
  module: Application.() -> Unit = {}
): ApplicationEngine =
  kserver(factory, port, host, watchPaths, configure, preWait, grace, timeout, module)

/**
 * Ktor [ApplicationEngine] as a [Resource]. This [Resource] will gracefully shut down the server
 * When we need to shut down a Ktor service we need to properly take into account a _grace_ period
 * where we still handle requests instead of immediately cancelling any in-flight requests.
 *
 * @param factory Application engine for processing the requests
 * @param environment definition of the environment where the engine will run
 * @param preWait preWait a duration to wait before beginning the stop process. During this time,
 *   requests will continue to be accepted. This setting is useful to allow time for the container
 *   to be removed from the load balancer. This is disabled when `io.ktor.development=true`.
 * @param grace grace a duration during which already inflight requests are allowed to continue
 *   before the shutdown process begins.
 * @param timeout timeout a duration after which the server will be forceably shutdown.
 */
suspend fun <
  TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> ResourceScope
  .server(
  factory: ApplicationEngineFactory<TEngine, TConfiguration>,
  environment: ApplicationEngineEnvironment,
  configure: TConfiguration.() -> Unit = {},
  preWait: Duration = 30.seconds,
  grace: Duration = 500.milliseconds,
  timeout: Duration = 500.milliseconds
): ApplicationEngine = kserver(factory, environment, configure, preWait, grace, timeout)
