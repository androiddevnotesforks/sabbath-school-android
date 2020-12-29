package dependencies

object Dependencies {
    const val MATERIAL = "com.google.android.material:material:${Versions.MATERIAL}"

    const val HILT = "com.google.dagger:hilt-android:${Versions.HILT}"
    const val HILT_COMPILER = "com.google.dagger:hilt-android-compiler:${Versions.HILT}"

    const val TIMBER = "com.jakewharton.timber:timber:${Versions.TIMBER}"

    const val RETROFIT = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT}"
    const val RETROFIT_CONVERTER = "com.squareup.retrofit2:converter-gson:${Versions.RETROFIT}"

    object AndroidX {
        const val CORE = "androidx.core:core-ktx:${Versions.AndroidX.CORE}"
        const val APPCOMPAT = "androidx.appcompat:appcompat:${Versions.AndroidX.APPCOMPAT}"
        const val RECYCLER_VIEW = "androidx.recyclerview:recyclerview:${Versions.AndroidX.RECYCLER_VIEW}"
        const val NAVIGATION_FRAGMENT = "androidx.navigation:navigation-fragment-ktx:${Versions.AndroidX.NAVIGATION}"
        const val NAVIGATION_UI = "androidx.navigation:navigation-ui-ktx:${Versions.AndroidX.NAVIGATION}"
        const val LIFECYCLE_EXTENSIONS = "androidx.lifecycle:lifecycle-extensions:${Versions.AndroidX.LIFECYCLE}"
        const val LIFECYCLE_VIEWMODEL = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.AndroidX.LIFECYCLE}"
        const val LIFECYCLE_LIVEDATA = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.AndroidX.LIFECYCLE}"
        const val FRAGMENT_KTX = "androidx.fragment:fragment-ktx:${Versions.AndroidX.FRAGMENT}"
        const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.AndroidX.CONSTRAINT_LAYOUT}"
        const val START_UP = "androidx.startup:startup-runtime:${Versions.AndroidX.START_UP}"
        const val HILT_COMPILER = "androidx.hilt:hilt-compiler:${Versions.AndroidX.HILT}"
        const val HILT_VIEWMODEL = "androidx.hilt:hilt-lifecycle-viewmodel:${Versions.AndroidX.HILT}"
    }

    object Firebase {
        const val BOM = "com.google.firebase:firebase-bom:${Versions.FIREBASE_BOM}"
        const val CORE = "com.google.firebase:firebase-core"
        const val ANALYTICS = "com.google.firebase:firebase-analytics-ktx"
        const val AUTH = "com.google.firebase:firebase-auth-ktx"
        const val DATABASE = "com.google.firebase:firebase-database-ktx"
        const val STORAGE = "com.google.firebase:firebase-storage-ktx"
        const val MESSAGING = "com.google.firebase:firebase-messaging-ktx"
        const val CRASHLYTICS = "com.google.firebase:firebase-crashlytics-ktx"
    }

    object Kotlin {
        const val KOTLIN = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}"
        const val COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}"
    }

}