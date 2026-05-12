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

# Firestore reflection needs model classes and their default constructors/fields.
-keep class com.example.madhu_siri.data.model.User { *; }
-keep class com.example.madhu_siri.data.model.Hive { *; }
-keep class com.example.madhu_siri.data.model.SprayEvent { *; }
-keep class com.example.madhu_siri.data.model.HealthLog { *; }
-keep class com.example.madhu_siri.data.model.NotificationAlert { *; }

# Keep Kotlin metadata used by some reflection-based libraries.
-keep class kotlin.Metadata { *; }
