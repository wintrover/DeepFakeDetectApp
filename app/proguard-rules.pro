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
# PyTorch Core 라이브러리 보호
-keep class org.pytorch.** { *; }
-keepclassmembers class org.pytorch.** { *; }

# PyTorch 관련 네이티브 메서드 보호
-keepclasseswithmembers class * {
    native <methods>;
}
-printmapping mapping.txt
-printconfiguration configuration.txt


## 네이티브 라이브러리 파일 보호
#-keepresourcefiles lib/armeabi-v7a/*.so
#-keepresourcefiles lib/arm64-v8a/*.so
#-keepresourcefiles lib/x86/*.so
#-keepresourcefiles lib/x86_64/*.so
