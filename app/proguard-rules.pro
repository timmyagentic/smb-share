-dontwarn org.bouncycastle.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.security.auth.kerberos.**
# SMBJ references lightweight BC primitives directly; let R8 remove unused algorithms.
# SMBJ's event bus uses runtime Handler annotations. Expression filters are unused.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @interface net.engio.mbassy.listener.**
-keepclassmembers class * { @net.engio.mbassy.listener.Handler <methods>; }
-dontwarn javax.el.**
# MBassador instantiates dispatchers/factories by reflected constructors.
-keep class net.engio.mbassy.** { *; }
