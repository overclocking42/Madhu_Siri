# Madhu-Siri Release And Production Notes

The app is ready to install and use as APK, just download, install and use [App](https://github.com/overclocking42/Madhu_Siri/blob/main/app/release/app-release.apk)


## Release signing

Release signing is local-only.

Required local files:

- `keystore.properties`
- `app/keys/madhu-siri-release.jks`

These are ignored by git and should stay on your machine only.

The app now checks whether the configured keystore file actually exists before applying release signing. That prevents fresh clones from failing immediately just because local signing files are absent.

## Inspect a local release keystore

If your keystore uses `PKCS12`, inspect it with:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
"$JAVA_HOME/bin/keytool" -list -v -storetype PKCS12 -keystore app/keys/madhu-siri-release.jks -alias madhu-siri-release
```

If `keytool` reports `Unrecognized keystore format` or low-level DER parsing errors, the keystore file is corrupted and must be replaced.

## Debug and release fingerprints

### Debug

- SHA-1: `52:40:B9:3B:1C:AB:EF:C9:5B:75:CF:3F:9D:87:BB:4F:62:DC:B9:25`
- SHA-256: `CF:5C:E3:05:1E:17:74:8A:C0:62:65:23:BE:6A:4F:34:AB:4A:B9:33:EC:04:4A:AF:18:1F:80:22:FB:28:B0:24`

### Release

- SHA-1: `79:A8:22:3D:58:BF:C0:CC:17:28:DB:2A:7A:5E:7D:6C:16:03:5E:E5`
- SHA-256: `83:02:5E:D2:44:8A:41:5C:C4:DD:75:90:C2:75:F0:D9:AD:AF:EF:40:28:85:AA:7B:21:71:AB:C4:9A:9E:76:BF`

These values belong to the current regenerated local release keystore.

## Google Sign-In setup

For Google Sign-In to work correctly:

1. Add the debug SHA-1 and SHA-256 in Firebase while testing locally.
2. Add the release SHA-1 and SHA-256 before release distribution.
3. If Play App Signing is enabled, also add the Play App Signing SHA values.
4. Download the updated `google-services.json` and place it in:
   - `app/google-services.json`

## Maps key setup

The Maps key is intentionally local-only now.

Put this in `local.properties`:

```properties
MAPS_API_KEY=your_google_maps_api_key
```

Do not commit a live billing-enabled Maps key to GitHub.

## Build status expectations

- `debug` builds should work after Gradle sync plus local Maps key setup
- `release` builds also require a valid local keystore

## Sharing a built APK in the repo

For reviewer convenience, you can keep a generated release APK in the repository so people can install it immediately without building.

Recommended path:

- `app/build/outputs/apk/release/app-release.apk`

This is acceptable for demos and internship review. It does not reveal your private signing keystore.

It does mean reviewers are using the exact Firebase project and client configuration baked into that APK, so only do this if that shared project usage is intentional.

## Production gaps still remaining

### Background push delivery

The app currently supports app-side notification records and active-app notifications. Reliable delivery while the app is fully closed still needs server-side FCM fan-out.

### Backend rule enforcement

The core business rules should eventually be enforced server-side too:

- 2 km radius alert targeting
- duplicate spray alert blocking
- bee-safe timing validation

### Security rules

Firestore rules still need a full production audit before public launch.

### Identity hygiene

The package id is still:

- `com.example.madhu_siri`

Replace it with a real production package name before Play Store release.
