import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

repositories {
  mavenCentral()
}

kotlin {
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    mainRun {
      mainClass.set("io.arrow.suspendapp.example.MaintKt")
    }
    withJava()
  }
  js(IR) {
    nodejs()
    binaries.executable()
  }

  listOf(
    linuxX64(),
    mingwX64(),
    macosArm64(),
    macosX64()
  ).forEach { target ->
    target.binaries.executable(listOf(RELEASE)) {
      entryPoint = "io.arrow.suspendapp.example.main"
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project.rootProject)
        implementation(libs.arrow.fx)
      }
    }
  }
}
