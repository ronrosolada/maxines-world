# Maxine's World — release minification rules.
#
# ── Hilt ViewModel multibinding vs R8 horizontal class merging ─────────────
# Every @HiltViewModel must survive as its own runtime class: the generated
# Dagger component keys the ViewModel multibinding map by Class object. If R8
# horizontally merges two ViewModels into one class, the map builder receives
# the same key twice and the app crashes on first composition:
#   java.lang.IllegalArgumentException: Multiple entries with same key
# Pin every ViewModel subclass so it cannot be merged away or removed,
# while still allowing renaming (allowobfuscation) and member shrinking.
-keep,allowobfuscation class * extends androidx.lifecycle.ViewModel
