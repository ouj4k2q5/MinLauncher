# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers so that stack traces from release builds can be read after
# retracing them with the mapping.txt that the release workflow archives.
-keepattributes SourceFile,LineNumberTable

# Line numbers are enough to locate the frame; the original file name adds nothing and
# would leak the source layout, so replace it with a placeholder.
-renamesourcefileattribute SourceFile
