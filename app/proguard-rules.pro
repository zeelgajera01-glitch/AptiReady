# R8 / ProGuard Keep Rules for AptiRise

# Room Database Entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.aptirise.app.data.local.db.** { *; }

# Domain Data Models
-keep class com.aptirise.app.data.model.** { *; }

# Restored fragment names are mapped from the former namespace after an upgrade.
-keepnames class com.aptirise.app.** extends androidx.fragment.app.Fragment

# ViewBinding Inflation Methods
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}


# Firebase Auth & Cloud Firestore
-keep class com.google.firebase.** { *; }
