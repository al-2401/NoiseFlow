# kotlinx.serialization keeps its serializers on the companion object.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.noiseflow.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class app.noiseflow.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The DSP is reflection-free; nothing else needs keeping.
