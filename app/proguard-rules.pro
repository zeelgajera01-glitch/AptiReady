# R8 / ProGuard Keep Rules for AptiRise

# Room Database Entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.aptiready.data.local.db.** { *; }

# Domain Data Models
-keep class com.example.aptiready.data.model.** { *; }

# ViewBinding Inflation Methods
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# Google Mobile Ads SDK & UMP
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }

# Firebase Auth & Cloud Firestore
-keep class com.google.firebase.** { *; }