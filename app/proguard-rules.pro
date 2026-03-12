# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Gson
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }

# Kotlin Reflection (for Gson)
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Material 3
-keep class androidx.compose.material3.** { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# Android Lifecycle
-keep class androidx.lifecycle.** { *; }

# Gson / Data Models
# Prevent R8 from stripping fields used in JSON serialization
-keep class com.example.timecard.data.model.** { *; }
-keepclassmembers class com.example.timecard.data.model.** { <fields>; }

# ViewModels and UI
-keep class com.example.timecard.ui.** { *; }

# General
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

-dontwarn android.media.**
-keep class android.media.** { *; }
