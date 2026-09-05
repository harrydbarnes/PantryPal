plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ValueSource to fetch Git Hash
abstract class GitHashValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        return try {
            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = output
            }
            output.toString().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

// ValueSource to fetch Build Date
abstract class BuildDateValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
         val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
         return LocalDate.now(ZoneOffset.UTC).format(formatter)
    }
}

val gitHashProvider = providers.of(GitHashValueSource::class) {}
val buildDateProvider = providers.of(BuildDateValueSource::class) {}
val buildMetadataProvider = gitHashProvider.zip(buildDateProvider) { gitHash, buildDate ->
    mapOf(
        "GIT_HASH" to BuildConfigField<Serializable>("String", "\"$gitHash\"", null),
        "BUILD_DATE" to BuildConfigField<Serializable>("String", "\"$buildDate\"", null),
    )
}

android {
    namespace = "com.example.pantrypal"
    compileSdk = 36

    val releaseSigningKeystore = providers.environmentVariable("PANTRYPAL_RELEASE_KEYSTORE")
    val releaseSigningStorePassword = providers.environmentVariable("PANTRYPAL_RELEASE_STORE_PASSWORD")
    val releaseSigningKeyAlias = providers.environmentVariable("PANTRYPAL_RELEASE_KEY_ALIAS")
    val releaseSigningKeyPassword = providers.environmentVariable("PANTRYPAL_RELEASE_KEY_PASSWORD")
    val releaseSigningValues = listOf(
        releaseSigningKeystore,
        releaseSigningStorePassword,
        releaseSigningKeyAlias,
        releaseSigningKeyPassword,
    )
    val releaseSigningConfigured = releaseSigningValues.any { it.isPresent }

    if (releaseSigningConfigured && releaseSigningValues.any { !it.isPresent }) {
        throw GradleException(
            "PANTRYPAL_RELEASE_KEYSTORE, PANTRYPAL_RELEASE_STORE_PASSWORD, " +
                "PANTRYPAL_RELEASE_KEY_ALIAS, and PANTRYPAL_RELEASE_KEY_PASSWORD must be provided together."
        )
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseSigningKeystore.get())
                storePassword = releaseSigningStorePassword.get()
                keyAlias = releaseSigningKeyAlias.get()
                keyPassword = releaseSigningKeyPassword.get()
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.pantrypal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    // Keep Git/date metadata lazy through AGP's variant-scoped MapProperty so
    // configuration-cache builds do not force either provider during setup.
    onVariants { variant ->
        variant.buildConfigFields?.putAll(buildMetadataProvider)
    }

    // ML Kit's native libraries previously needed this only to silence debug
    // packaging strip warnings. Keep the diagnostic symbols out of release APKs.
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.packaging.jniLibs.keepDebugSymbols.add("**/libbarhopper_v3.so")
        variant.packaging.jniLibs.keepDebugSymbols.add("**/libimage_processing_util_jni.so")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // CameraX
    val cameraxVersion = "1.6.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // MLKit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Keep the OCR model out of the base APK. Google Play services downloads it when needed.
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Battery-efficient OS geofencing for optional nearby shopping nudges
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Gson for backup
    implementation("com.google.code.gson:gson:2.14.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
