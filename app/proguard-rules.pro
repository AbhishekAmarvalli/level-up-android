# Level Up release rules. The release build does not enable minification, but these
# rules keep Room, CameraX, Glance and Play Services intact if it is turned on later.

-keep class androidx.room.** { *; }
-keep class androidx.camera.** { *; }
-keep class androidx.glance.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
