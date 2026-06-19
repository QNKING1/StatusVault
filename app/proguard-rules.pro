# StatusVault ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
    <init>();
}
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * {
    <methods>;
}
-dontwarn androidx.room.paging.**

# Hilt
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * {
    <init>();
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keepclassmembers class * extends dagger.hilt.internal.GeneratedComponent {
    <init>();
}

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# General
-keepattributes Signature
-keepattributes Exceptions
-keepattributes EnclosingMethod
-keepattributes InnerClasses
