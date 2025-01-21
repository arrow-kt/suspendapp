@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

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
    compilerOptions {
      jvmTarget = JvmTarget.JVM_1_8
    }
  }
  js {
    nodejs()
  }
  wasmJs {
    nodejs()
    d8()
  }

  linuxArm64()
  linuxX64()
  mingwX64()
  macosArm64()
  macosX64()
  
  sourceSets {
    applyDefaultHierarchyTemplate()

    commonMain {
      dependencies {
        api(libs.coroutines)
      }
    }
  }
}

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs"))
  }
  moduleName.set("suspendapp")
  dokkaSourceSets {
    named("commonMain") {
      includes.from("README.md")
      perPackageOption {
        matchingRegex.set(".*\\.unsafe.*")
        suppress.set(true)
      }
      // externalDocumentationLink("https://kotlinlang.org/api/kotlinx.coroutines/")
      sourceLink {
        localDirectory.set(file("src/commonMain/kotlin"))
        remoteUrl.set(URI("https://github.com/arrow-kt/suspendapp/tree/main/src/commonMain/kotlin"))
        remoteLineSuffix.set("#L")
      }
    }
  }
}

tasks {
  register<Delete>("cleanDocs") {
    val folder = file("docs").also { it.mkdir() }
    val docsContent = folder.listFiles().filter { it != folder }
    delete(docsContent)
  }
}

apiValidation {
  ignoredProjects.add("example")
}
