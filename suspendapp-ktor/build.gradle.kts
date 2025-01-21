import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

repositories {
    mavenCentral()
}

kotlin {
  // We set up custom targets rather than use Arrow Gradle Config for Kotlin,
  // Since we don't support all targets but only subset where having this behavior makes sense.
  jvm {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_1_8
    }
  }
  linuxX64()
  macosArm64()
  macosX64()
  
  sourceSets {
    commonMain  {
      dependencies {
        api(libs.arrow.fx)
        api(libs.ktor.core)
      }
    }
  }
}
