plugins {
    id("java-library")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // 因為你是 .kts 檔，要用括號和雙引號
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}