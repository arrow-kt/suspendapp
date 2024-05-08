import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE

plugins {
  kotlin("multiplatform")
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
    val commonMain by getting {
      dependencies {
        implementation(project.rootProject)
        implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0")
      }
    }

    val jvmMain by getting
    val jsMain by getting
    val mingwX64Main by getting
    val linuxX64Main by getting
    val macosArm64Main by getting
    val macosX64Main by getting
  }
}
