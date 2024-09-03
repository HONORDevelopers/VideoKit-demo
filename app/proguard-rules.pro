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

-ignorewarnings
-optimizationpasses 5

-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, *Annotation*, *JavascriptInterface*

-printconfiguration ./build/outputs/mapping/full-config.txt

-keep class **.BuildConfig {*;}

-keepclassmembers enum *

#Android support
-keep class android.support.** {*;}
-dontwarn android.support.**
-keep interface android.support.** { *; }
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

#Androidx
-keep class androidx.** {*;}
-keep interface androidx.** {*;}
-keep class * extends androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

#Parcelable class
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

#Serializable class
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#Annotation class
-keep,allowobfuscation interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# log TAG
-keep class * {
    private static final java.lang.String TAG;
}

-keep class com.hihonor.heartstudykit.api.** { *; }
-keep class com.hihonor.heartstudykit.notify.** { *; }