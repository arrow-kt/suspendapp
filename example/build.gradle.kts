plugins {
  kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
  // TODO fix setup for Main-Class
  // jvm()
  js(IR) {
    nodejs {
      binaries.executable()
    }
  }
  
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
  
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project.rootProject)
        implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")
      }
    }
    
    // val jvmMain by getting
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
