// ★★★ 這一行一定要加在最上面，不然 JavaCompile 會找不到 ★★★
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

// ★ 這是最穩定的 Kotlin DSL 寫法，強制編譯器用 UTF-8
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}