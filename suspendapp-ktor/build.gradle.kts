@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.kotlin.multiplatform.get().pluginId)

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
    compilations.all {
      kotlinOptions.jvmTarget = "1.8"
    }
  }
  linuxX64()
  macosArm64()
  macosX64()
  
  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.arrow.fx)
        api(libs.ktor.core)
      }
    }
    
    val jvmMain by getting
    val linuxX64Main by getting
    val macosArm64Main by getting
    val macosX64Main by getting
    
    create("nativeMain") {
      dependsOn(commonMain)
      linuxX64Main.dependsOn(this)
      macosArm64Main.dependsOn(this)
      macosX64Main.dependsOn(this)
    }
  }
}
