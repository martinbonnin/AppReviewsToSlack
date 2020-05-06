buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.cloud.tools:appengine-gradle-plugin:2.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    }
}

repositories {
    maven {
        url = uri("https://maven-central.storage.googleapis.com")
    }
    jcenter()
    mavenCentral()
}

apply(plugin = "kotlin")
apply(plugin = "war")
apply(plugin = "com.google.cloud.tools.appengine-standard")
apply(plugin = "kotlin-kapt")

dependencies {
    add("providedCompile", "javax.servlet:javax.servlet-api:3.1.0")
    add("compile", "org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    add("compile", "com.google.cloud:google-cloud-datastore:1.14.0")
    add("compile", "com.google.cloud:google-cloud-translate:1.53.0")
    add("compile", "com.squareup.moshi:moshi:1.8.0")
    add("compile", "org.apache.commons:commons-lang3:3.0")
    add("kapt", "com.squareup.moshi:moshi-kotlin-codegen:1.8.0")

    add("testCompile", "junit:junit:4.12")
}

configure<com.google.cloud.tools.gradle.appengine.standard.AppEngineStandardExtension> {
    deploy {
        projectId = "api-8968624424481748993-986289"
        version = "GCLOUD_CONFIG"
        stopPreviousVersion = true
        promote = true
    }
}

group = "net.mbonnin.googlePlayToSlack"
version = "1.0-SNAPSHOT"

//sourceCompatibility = 1.7
//targetCompatibility = 1.7

/*compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}*/
