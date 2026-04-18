# Moonstone ProGuard Rules

# Keep KleinLisp classes
-keep class net.sourceforge.kleinlisp.** { *; }

# Keep GUI runtime classes that may be accessed reflectively
-keep class net.sourceforge.moonstone.runtime.** { *; }
-keep class net.sourceforge.moonstone.components.** { *; }
