# Add project specific ProGuard / R8 rules here.

# ===================================================================
# Anti-Decompilation & Aggressive Code Obfuscation
# ===================================================================
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontpreverify
-repackageclasses ''

# Remove Source File names, Line Numbers, and Local Variable Tables from DEX
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable

# Aggressive Overloading of Obfuscated Names
-overloadaggressively

# ===================================================================
# Keep Rules for External Libraries (Billing, Firebase, AdMob)
# ===================================================================

# Keep Google Play Billing
-keep class com.android.billingclient.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Keep androidx.startup and WorkManager
-keep class androidx.startup.** { *; }
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-dontwarn android.app.job.JobScheduler
