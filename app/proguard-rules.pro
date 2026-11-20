# ── Lia AI ProGuard rules ────────────────────────────────────────────────────

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp + Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# org.json — used for Gemini API payloads in JVM unit tests
-keep class org.json.** { *; }

# Compose — Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# DataStore
-keep class androidx.datastore.** { *; }

# Accessibility service subclass must survive shrinking
-keep public class * extends android.accessibilityservice.AccessibilityService

# FileProvider
-keep class androidx.core.content.FileProvider { *; }

# Keep enum names used in DataStore serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# WebView JavaScript bridge methods must not be renamed
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
