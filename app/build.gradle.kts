import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("kotlin-kapt")
	id("io.sentry.android.gradle") version "4.11.0"
	id("com.google.devtools.ksp")
	id("com.google.gms.google-services")
	id("com.github.ben-manes.versions") version "0.48.0"
}

val keyStore = Properties()
keyStore.load(FileInputStream(file("$rootDir/keystore/keystore.config")))
val mainVersionCode: String by project
val majorVersionName: String by project
val minorVersionName: String by project
val patchVersionName: String by project
val releaseLabel: String by project
val debugLabel: String by project
val sentryDSN: String by project
val sentryAuthToken: String by project
val bitmovinStagingKey: String by project
val bitmovinProductionKey: String by project
val bitmovinAnalyticsStagingKey: String by project
val bitmovinAnalyticsProductionKey: String by project
val pubnubSubscribeKey: String by project
val pubnubPublishKey: String by project

fun generateDebugVersionSuffix(): String {
	return ".$debugLabel" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(".MMdd"))
}

android {
	signingConfigs {
		getByName(debugLabel) {
			storeFile = file("$rootDir/keystore/veeps")
			storePassword = keyStore["password"].toString()
			keyPassword = keyStore["password"].toString()
			keyAlias = keyStore["alias"].toString()
		}
		create(releaseLabel) {
			storeFile = file("$rootDir/keystore/veeps")
			storePassword = keyStore["password"].toString()
			keyPassword = keyStore["password"].toString()
			keyAlias = keyStore["alias"].toString()
		}
	}
	namespace = "com.veeps.app"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.veeps.app"
		minSdk = 24
		targetSdk = 35
		versionCode = mainVersionCode.toInt()
		versionName = "$majorVersionName.$minorVersionName.$patchVersionName"
		signingConfig = signingConfigs.getByName(releaseLabel)
		testFunctionalTest = true
		testHandleProfiling = true
	}

	buildTypes {
		getByName(releaseLabel) {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName(releaseLabel)
		}
		getByName(debugLabel) {
			versionNameSuffix = generateDebugVersionSuffix()
			signingConfig = signingConfigs.getByName(debugLabel)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	/*kapt {
		correctErrorTypes = true
	}*/

	dependenciesInfo {
		includeInApk = false
		includeInBundle = false
	}

	buildFeatures {
		viewBinding = true
		dataBinding = true
		buildConfig = true
	}

	flavorDimensions += listOf("environment")
	productFlavors {
		create("stage") {
			dimension = "environment"
			versionCode = mainVersionCode.toInt()
			signingConfig = signingConfigs.getByName(debugLabel)
			buildConfigField("Boolean", "isProduction", "false")
			manifestPlaceholders["SentryDSN"] = sentryDSN
			manifestPlaceholders["BitmovinKey"] = bitmovinStagingKey
			buildConfigField("String", "bitmovinAnalyticsKey", "\"$bitmovinAnalyticsStagingKey\"")
			buildConfigField("String", "pubnubSubscribeKey", "\"$pubnubSubscribeKey\"")
			buildConfigField("String", "pubnubPublishKey", "\"$pubnubPublishKey\"")
		}
		create("production") {
			dimension = "environment"
			versionCode = mainVersionCode.toInt()
			signingConfig = signingConfigs.getByName(releaseLabel)
			buildConfigField("Boolean", "isProduction", "true")
			manifestPlaceholders["SentryDSN"] = sentryDSN
			manifestPlaceholders["BitmovinKey"] = bitmovinProductionKey
			buildConfigField("String", "bitmovinAnalyticsKey", "\"$bitmovinAnalyticsProductionKey\"")
			buildConfigField("String", "pubnubSubscribeKey", "\"$pubnubSubscribeKey\"")
			buildConfigField("String", "pubnubPublishKey", "\"$pubnubPublishKey\"")
		}
	}
}

dependencies {

	/* Amazon IAP */
	implementation(libs.amazon.appstore)

	/* Android Core */
	implementation(libs.core)
	implementation(libs.leanback)
	implementation(libs.leanback.grid)
	implementation(libs.palette)
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.activity)
	implementation(libs.material)
	implementation(libs.fragment)
	implementation(libs.constraintlayout)
	implementation(libs.lifecycle.viewmodel)
	implementation(libs.lifecycle.livedata)
	implementation(libs.lifecycle.common.java8)
	implementation(libs.androidx.work.runtime)
	implementation(libs.dpadrecyclerview)
	implementation(libs.androidx.ui.unit.android)

	/* Calligraphy for Fonts */
	implementation(libs.calligraphy3)
	implementation(libs.viewpump) /* DO NOT UPDATE - Latest version v2.1.1 is incompatible with Calligraphy3 and causes an Init() method error */

	/* Retrofit */
	implementation(libs.gson)
	implementation(libs.retrofit)
	implementation(libs.converter.scalars)
	implementation(libs.converter.gson)
	implementation(libs.okhttp)
	implementation(libs.logging.interceptor)

	/* Joda Time */
	implementation(libs.android.joda)

	/* Blur */
	implementation(libs.blurView)

	/* PubNub */
	implementation(libs.pubnub)

	/* Lottie Animation */
	implementation(libs.lottie)

	/* Markdown Parser */
	implementation(libs.markwon.core)

	/* Firebase */
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.analytics)

	/* Bitmovin */
	implementation(libs.bitmovin.player)

	/* Glide */
	implementation(libs.glide)
	ksp(libs.glide.ksp)
	implementation(libs.glide.recyclerview.integration)

	/* JWT */
	implementation(libs.jjwt.api)
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.orgjson)

}

sentry {
	org.set("veeps")
	projectName.set("firetv")
	includeProguardMapping.set(true)
	autoUploadProguardMapping.set(true)
	authToken.set(sentryAuthToken)
	includeSourceContext.set(true)
	tracingInstrumentation {
		enabled.set(true)
		features.set(
			setOf(
				InstrumentationFeature.DATABASE,
				InstrumentationFeature.FILE_IO,
				InstrumentationFeature.OKHTTP,
				InstrumentationFeature.COMPOSE
			)
		)
		logcat {
			enabled.set(true)
			minLevel.set(LogcatLevel.VERBOSE)
		}
	}
}
