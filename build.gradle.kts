@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  kotlin("multiplatform") version "1.7.10"
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.binaryCompatibilityValidator)
  alias(libs.plugins.arrowGradleConfig.nexus)
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
  js(IR) {
    nodejs()
  }
  
  linuxX64()
  mingwX64()
  macosArm64()
  macosX64()
  
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.coroutines)
      }
    }
    
    val jvmMain by getting
    val jsMain by getting
    val mingwX64Main by getting
    val linuxX64Main by getting
    val macosArm64Main by getting
    val macosX64Main by getting
    
    create("nativeMain") {
      dependsOn(commonMain)
      mingwX64Main.dependsOn(this)
      linuxX64Main.dependsOn(this)
      macosArm64Main.dependsOn(this)
      macosX64Main.dependsOn(this)
    }
  }
}
