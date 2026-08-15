# NanoHTTPD - server internals accessed via reflection in some paths
-keep class fi.iki.elonen.** { *; }

# ZXing - QR generation/scanning
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# Room - entities and DAOs must survive obfuscation for schema/reflection
-keep class com.xcloak.airflux.data.database.entity.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**