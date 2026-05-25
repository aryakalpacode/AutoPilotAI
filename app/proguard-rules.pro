# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class com.autopilot.agent.data.remote.dto.** { *; }
-keep class com.autopilot.agent.data.local.entity.** { *; }

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jsoup
-keep class org.jsoup.** { *; }
