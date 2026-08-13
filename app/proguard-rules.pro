# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Restu\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep Google Play Billing
-keep class com.android.billingclient.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
