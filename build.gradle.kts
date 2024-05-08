import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.binaryCompatibilityValidator)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

repositories {
  mavenCentral()
}

allprojects {
  group = property("projects.group").toString()
  extra.set("dokka.outputDirectory", rootDir.resolve("docs"))
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
        api(libs.coroutines)
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
      linuxX64Main.dependsOn(this)
      macosArm64Main.dependsOn(this)
      macosX64Main.dependsOn(this)
      mingwX64Main.dependsOn(this)
    }
  }
}

tasks {
  withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs"))
    moduleName.set("suspendapp")
    dokkaSourceSets {
      named("commonMain") {
        includes.from("README.md")
        perPackageOption {
          matchingRegex.set(".*\\.unsafe.*")
          suppress.set(true)
        }
        externalDocumentationLink("https://kotlinlang.org/api/kotlinx.coroutines/")
        sourceLink {
          localDirectory.set(file("src/commonMain/kotlin"))
          remoteUrl.set(uri("https://github.com/arrow-kt/suspendapp/tree/main/src/commonMain/kotlin").toURL())
          remoteLineSuffix.set("#L")
        }
      }
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }
  
  register<Delete>("cleanDocs") {
    val folder = file("docs").also { it.mkdir() }
    val docsContent = folder.listFiles().filter { it != folder }
    delete(docsContent)
  }
}

apiValidation {
  ignoredProjects.add("example")
}
