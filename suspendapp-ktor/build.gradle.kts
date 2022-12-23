@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.jvm.get().pluginId)

  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

repositories {
    mavenCentral()
}

dependencies{
  implementation(libs.coroutines)
  implementation(libs.arrow.fx)
  implementation(libs.ktor.core)
  implementation(libs.ktor.netty)
}
