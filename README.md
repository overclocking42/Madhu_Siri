# Madhu-Siri

Madhu-Siri is a production-style Android app built with Kotlin, Jetpack Compose, Firebase, and Google Maps to help farmers and beekeepers coordinate spray safety inside a 2 km radius.

The app is ready to install and use as APK, just download, install and use [App](https://github.com/overclocking42/Madhu_Siri/blob/main/app/release/app-release.apk) and for more info about this project, reviewing, cloning, modifying on your version and run on your own device, check these [documents](https://github.com/overclocking42/Madhu_Siri/tree/main/docs)

## What the app does

- Farmers create spray alerts before pesticide use
- Beekeepers pin hives, store bee-out timings, and receive nearby alerts
- The app blocks unsafe spray windows that overlap protected bee activity time
- Both roles get local map visibility for nearby hives and spray zones

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM + Repository pattern
- Firebase Auth
- Cloud Firestore
- Firebase Cloud Messaging
- Google Maps Compose

## Open in Android Studio

1. Clone the repository.
2. Open the project folder in Android Studio.
3. Let Gradle sync automatically.
4. Add local config values before running:
   - `local.properties` must contain `sdk.dir`
   - `local.properties` should also contain `MAPS_API_KEY=your_maps_key`
5. Run the `app` configuration on an emulator or real device with Google Play Services.

The repository already includes:

- app source code
- Gradle dependency configuration
- Firebase app config in `app/google-services.json`

The repository does not include:

- your release keystore
- a reusable public Google Maps billing key
- server-side notification fan-out

## Instant reviewer install

If you commit the built release APK to the repository, reviewers can install it immediately without opening Android Studio.

Recommended APK path in the repo:

- `app/build/outputs/apk/release/app-release.apk`

Suggested reviewer flow:

1. Download the repository zip or clone it.
2. Open the APK from the release output folder.
3. Install it on an Android device.
4. Open the app and test the main flows directly.

This is useful for internship review and demos because it removes local build setup for the reviewer.

## Local setup files

Local-only files are intentionally ignored by git:

- `local.properties`
- `keystore.properties`
- `app/keys/`

That keeps your Maps key and signing key out of GitHub.

## Google Sign-In

Google Sign-In is available on both login and create-account flows.

For local debug testing, the Firebase project must include your debug SHA fingerprint.

For release builds, Firebase must also include your release SHA fingerprint.

## Release builds

Release builds require a valid local keystore. If `keystore.properties` or the keystore file is missing, debug builds still work, but release packaging will not.

See:

- [Clone and Setup Guide](/Users/joyboy/AndroidStudioProjects/Madhu_Siri/docs/CLONE_AND_SETUP.md)
- [App Guide](/Users/joyboy/AndroidStudioProjects/Madhu_Siri/docs/MADHU_SIRI_APP_GUIDE.md)
- [Release Notes](/Users/joyboy/AndroidStudioProjects/Madhu_Siri/docs/PLAY_STORE_RELEASE.md)

## Production note

The app is in a strong demo and internship state, but one important production gap remains:

- reliable push delivery while the app is fully closed still needs secure server-side FCM fan-out, such as Firebase Cloud Functions

## Security note

Do not push these local files to GitHub:

- `local.properties`
- `keystore.properties`
- `app/keys/madhu-siri-release.jks`

It is fine to share a built APK for reviewers. Sharing the APK does not expose your private signing key file.

If you previously exposed a Maps key or release signing key in a public repo or chat, rotate them before real deployment.
