# Sew Pattern Gemini (Android)

Android Java prototype: **offline parametric schematic pattern** + optional **Gemini photo garment analysis** + actual API connectivity indication.

## Features
- Optional photo selection from Android document picker.
- Native Android offline schematic drafting for dress, skirt, top; user-entered body dimensions.
- Front and back diagram pieces, seam-allowance *indication*, arrows, notches, CUT labels.
- PDF export to Android document storage.
- Gemini `gemini-2.5-flash` via Google Generative Language API to describe garment details from a photo.
- CONNECTED indicator only after successful API response; errors and offline mode remain separate.

## Significant limitations
- This is **an early functional prototype**, not accurate ready-to-cut industrial sewing patterns.
- The generated PDF is a **schematic, NOT 1:1**, and dashed seam allowance is illustrative. Do not use it to cut material.
- Offline mode doesn't infer patterns from photos. It uses simple built-in templates with user dimensions currently appearing as annotations, not scaled geometric construction. There is no computer vision or geometric unfolding yet.
- Gemini only describes photo construction; it does not automatically turn AI suggestions into cutting geometry.
- Gemini key is stored in app-private SharedPreferences (not encrypted); for production prefer Android Keystore-based encryption or an authenticated server proxy. Never commit a key to GitHub.

## Build
1. Create a new GitHub repository or use Android Studio to open this folder.
2. Push files to branch `main` (including `.github/workflows/android.yml`).
3. GitHub Actions > `Android APK` > Run workflow (or push to main).
4. Download `SewPatternGemini-debug-apk` artifact after a successful run.

Requires Android SDK 35, JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3. Workflow installs them. Android Studio may offer to install these dependencies locally.

## Gemini API key
Create an API key in [Google AI Studio](https://aistudio.google.com/apikey). Paste it in the app. Check connection requires a real successful model call (not just an internet check). Gemini usage may incur fees or quotas.

## Privacy
Gemini image analysis transmits a resized version of the photo to Google's Gemini API. Offline drafting does not upload the photo.
