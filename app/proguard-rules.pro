# Keep Compose runtime metadata used by the compiler plugin.
-keepclassmembers class androidx.compose.** { *; }

# Lottie uses reflection on its model classes.
-keep class com.airbnb.lottie.** { *; }

# Media3 / ExoPlayer.
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Keep our entry points referenced from the manifest.
-keep class com.stark.jarvis.boot.BootCompletedReceiver { *; }
-keep class com.stark.jarvis.service.JarvisOverlayService { *; }
-keep class com.stark.jarvis.MainActivity { *; }
