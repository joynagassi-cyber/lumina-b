import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// --- Configuration de signature ---------------------------------------------
// Priorité : variables d'environnement (CI / shell) > keystore.properties (local, ignoré
// par Git) > valeur par défaut. Compatible avec l'injection AI Studio (mêmes noms de
// signingConfigs : "release" et "debugConfig").
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { load(it) }
  }
}

fun signingValue(envKey: String, propertyKey: String): String? =
  System.getenv(envKey)?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }

val appEnvProperties = Properties().apply {
  val envFile = rootProject.file(".env")
  if (envFile.exists()) envFile.inputStream().use { load(it) }
}
fun appConfigValue(name: String, fallback: String): String =
  System.getenv(name)?.takeIf { it.isNotBlank() }
    ?: appEnvProperties.getProperty(name)?.takeIf { it.isNotBlank() }
    ?: fallback
fun buildConfigString(value: String): String =
  "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseUrl = appConfigValue("SUPABASE_URL", "https://ybfebchvsgbgpeesrqld.supabase.co")
val supabasePublishableKey = appConfigValue(
  "SUPABASE_PUBLISHABLE_KEY",
  "sb_publishable_U71rafe7hFkDcF6P-uFjgQ_WDGaHMY8"
)

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.applet.hfkylu"
    // Android 7.0 (24) par défaut : l'APK couvre Android 7 -> 16, donc Android 12 (31).
    // Plancher plus élevé possible sans toucher au code : MIN_SDK=31 ./gradlew assembleRelease
    minSdk = System.getenv("MIN_SDK")?.toIntOrNull() ?: 24
    targetSdk = 36
    // Surchargeables par l'environnement (utile en CI) :
    //   VERSION_CODE=42 VERSION_NAME=1.2.3 ./gradlew assembleRelease
    versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
    versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0"
    buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", buildConfigString(supabasePublishableKey))

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = signingValue("KEYSTORE_PATH", "storeFile") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = signingValue("STORE_PASSWORD", "storePassword")
      keyAlias = signingValue("KEY_ALIAS", "keyAlias") ?: "upload"
      keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
      // PKCS12 (Android Studio moderne) ou JKS : laisser vide pour détection automatique.
      signingValue("KEY_STORE_TYPE", "storeType")?.let { type -> storeType = type }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
  testLogging {
    events("failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
