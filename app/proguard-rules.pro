-keep class app.musique.core.SessionCallback { *; }
-keepclassmembers class * implements app.musique.core.SessionCallback { *; }
-keep class app.musique.core.spirc.SpircInitializationCallback { *; }
-keep class app.musique.core.spirc.SpircBufferCallback { *; }
-keep class app.musique.core.spirc.SpircDeviceCallback { *; }

-keep class app.musique.playback.callbacks.PlayerEventCallback { *; }
-keepclassmembers class * implements app.musique.playback.callbacks.PlayerEventCallback { *; }
-keep class app.musique.playback.AudioEngine { *; }

# These names/signatures are resolved by JNI, including callback implementations.
-keep class app.musique.core.jam.JamNative { *; }
-keep interface app.musique.core.jam.JamCallback { *; }
-keep interface app.musique.core.jam.JamRequestCallback { *; }
-keepclassmembers class * implements app.musique.core.jam.JamCallback { *; }
-keepclassmembers class * implements app.musique.core.jam.JamRequestCallback { *; }
