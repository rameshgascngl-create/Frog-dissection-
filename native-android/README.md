# Frog Dissection — Pure Kotlin / Jetpack Compose branch

**This is a separate pure-native Android implementation**. It does not launch `index.html` and contains no WebView. The older `android-app/` HTML/WebView application remains unchanged until native acceptance.

The corpus in `app/src/main/assets/frog_corpus.json` was extracted from the repository's validated HTML sources on the branch creation checkpoint; it preserves **31 organs**, **26 viva MCQs**, **six guided practical steps**, descriptions in English/Tamil, spotter notes and six sex-specific/organ-layer hotspot sets. Photographs are copied into the APK by GitHub Actions.

Native features implemented so far: Material 3 navigation, five anatomical layers, separate male/female urogenital plates, an interactive photo overlay, native pinch zoom/pan, marker calibration in app-local preferences, tool-state exercises, bilingual anatomy reader, 26-MCQ assessment, photographic spotter quiz and glossary search.

**Academic limitations:** Existing photographic marker positions are provisional and must not be described as medically/anatomically approved. The native exercises record virtual dissection state; they do not actually deform or cut a photograph. AVIF specimen decoding is supported by targeting Android 12+ (API 31+); the previous hybrid APK supported older Android releases. Photo reuse rights remain an independent release blocker. Installation on physical devices, accessibility acceptance and persistent-state recovery still need verification. Keep the old HTML app as a fallback until feature acceptance.

To build locally: install Android SDK Platform 35 and build-tools 35.0.0, ensure Gradle 8.9 and JDK 17, copy the six named JPG/AVIF plates into `app/src/main/assets/`, then run `gradle :app:testDebugUnitTest :app:assembleDebug` from `native-android`.

The branch-level GitHub Actions workflow is `.github/workflows/native-compose-apk.yml`.
