# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Koin internal structures
-keep class org.koin.** { *; }
-dontwarn org.koin.**

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keep class com.luna.budgetapp.data.** { *; }
-keep class com.luna.budgetapp.di.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes *Annotation*,EnclosingMethod,InnerClasses

# Ignore missing SLF4J logger bindings introduced by third-party libraries
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
