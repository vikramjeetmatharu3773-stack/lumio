# Media3, Room, Glance and Coil ship consumer rules. Add app-specific keeps here only if release testing shows crashes.
-keep class app.lumio.player.data.db.** { *; }
-keepclassmembers enum app.lumio.player.** { *; }
