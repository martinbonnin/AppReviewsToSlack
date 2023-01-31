buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.cloud.tools:appengine-gradle-plugin:2.4.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
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
    add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    add("implementation", "com.google.cloud:google-cloud-datastore:2.13.3")
    add("implementation", "com.google.cloud:google-cloud-translate:2.7.0")
    add("implementation", "com.google.apis:google-api-services-androidpublisher:v3-rev20230123-2.0.0")
    add("implementation", "com.squareup.moshi:moshi:1.14.0")
    add("implementation", "org.apache.commons:commons-lang3:3.0")
    add("kapt", "com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    add("testImplementation", "junit:junit:4.12")
}

configure<com.google.cloud.tools.gradle.appengine.standard.AppEngineStandardExtension> {
    deploy {
        projectId = "app-engine-google-project-id"
        version = "GCLOUD_CONFIG"
        stopPreviousVersion = true
        promote = true
    }
}

group = "net.mbonnin.googlePlayToSlack"
version = "1.1"