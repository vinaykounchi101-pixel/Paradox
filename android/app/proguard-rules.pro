# ProGuard rules for Paradox
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.paradox.finance.data.model.** { *; }
-keep class com.paradox.finance.data.local.entity.** { *; }
