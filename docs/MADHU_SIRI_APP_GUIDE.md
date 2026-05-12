# Madhu-Siri

Madhu-Siri is a real-time bee-farmer coordination app built to prevent honeybee deaths during pesticide spraying. Beekeepers pin hive locations and define bee-out timings. Farmers create spray alerts only when the selected time is safe for nearby hives within a 2 km radius.

The app is ready to install and use as APK, just download, install and use [App](/Madhu_Siri/app/release/app-release.apk)


## Problem

Farmers often spray chemicals near bee colonies without a timely warning.

This creates three risks:

1. Bees die while foraging.
2. Beekeepers do not know when to close hives.
3. Farmers do not know whether spraying is safe at the selected time.

Madhu-Siri solves this by coordinating both roles around location, time, and alerting.

## Core Workflow

### Farmer workflow

1. Register or log in as a farmer.
2. Open the `Spray` tab.
3. Choose a spray type, chemical name, duration, and planned start time.
4. Pick the spray location manually on the map or by current GPS.
5. The app checks all hives within 2 km.
6. If the chosen time overlaps with the bee-out safety rule, the alert is blocked and safe alternative times are suggested.
7. If the time is safe, the spray alert is saved and nearby beekeepers receive a notification record.
8. The spray marker stays visible through the spray duration plus the extra 4-hour bee-safety window, then disappears automatically from the live map.

### Beekeeper workflow

1. Register or log in as a beekeeper.
2. Open the `Map` tab.
3. Add a hive location by map pin or current GPS.
4. Enter bee-out timings for that hive.
5. The hive becomes visible in real time to nearby farmers.
6. Nearby spray alerts within 2 km appear on the dashboard and map.
7. The beekeeper can review spray details, remaining safe-wait time, and contact info.
8. The beekeeper can maintain health and honey logs from the dashboard.

## Safety Rule

Madhu-Siri does not allow a farmer to create a spray alert if the chosen start time overlaps with:

- the bee-out window, or
- the 4-hour protection window before bees are released.

If a conflict exists:

- the spray alert is not created,
- the farmer sees a clear warning,
- the app suggests the next safe spray times until the next day.

## Live Map Rules

- Hive markers are persistent until edited or removed by the owner.
- Spray markers and red danger circles are temporary.
- Spray markers remain visible only until the spray duration ends and the additional 4-hour waiting window is complete.
- Once no spray markers remain in a nearby area, beekeepers can visually confirm that the danger window has passed.

## Dashboard Logic

### Farmer dashboard

The dashboard focuses on the farmer’s relevant working area:

- nearby visible spray zones within 2 km of the farmer’s latest spray reference point,
- nearby hive markers within 2 km of that same area,
- spray history for the farmer account.

### Beekeeper dashboard

The dashboard focuses on relevant local protection data:

- nearby spray alerts within 2 km of the beekeeper’s hives,
- beekeeper hives currently inside an alert radius,
- notification history for nearby spray alerts,
- hive health and honey logs.

## Notifications

Current implementation supports:

- Firestore notification records in the `notifications` collection,
- local on-device notifications when the beekeeper app is active and permission is granted.

Important limitation:

True server-side background FCM fan-out for a fully closed app still requires a backend sender such as a Firebase Cloud Function or another secure server component.

## Auth and User Preferences

The app supports:

- splash screen,
- login,
- registration,
- Google Sign-In,
- role selection,
- persistent session,
- language selection,
- theme preference selection.

Supported theme modes:

- System
- Light
- Dark

Supported app languages:

- English
- Kannada
- Hindi
- Telugu
- Tamil

## Local Secrets And Billing Safety

These files should stay local and must not be committed:

- `local.properties`
- `keystore.properties`
- `app/keys/`

Reason:

- `local.properties` may contain your Google Maps billing key
- `keystore.properties` points to your release signing setup
- `app/keys/` contains your private release signing key

The checked-in Firebase config file is not the same thing as a private signing key, but Firestore and Auth still need proper backend-side security and project restrictions.

## Data Model

### Firestore collections

- `users`
- `hives`
- `spray_events`
- `notifications`
- `health_logs`

### Key fields

#### users

- `uid`
- `fullName`
- `email`
- `phoneNumber`
- `role`
- `preferredLanguage`
- `themePreference`
- `fcmToken`
- `createdAt`

#### hives

- `id`
- `name`
- `lat`
- `lng`
- `ownerId`
- `notes`
- `contactNumber`
- `activeStartTime`
- `activeEndTime`
- `createdAt`

#### spray_events

- `id`
- `farmerId`
- `sprayType`
- `chemicalName`
- `durationHours`
- `scheduledAt`
- `lat`
- `lng`
- `radiusKm`
- `notes`
- `createdAt`

#### notifications

- `id`
- `recipientUserId`
- `sprayEventId`
- `title`
- `body`
- `timestamp`

#### health_logs

- `id`
- `ownerId`
- `hiveId`
- `hiveName`
- `logType`
- `status`
- `metricValue`
- `metricUnit`
- `notes`
- `createdAt`

## Architecture

Madhu-Siri uses MVVM with a repository layer.

### Frontend

- Kotlin
- Jetpack Compose
- Material 3
- Google Maps Compose

### State management

- `StateFlow`
- `MutableStateFlow`

### ViewModels

- `AuthViewModel`
- `MainViewModel`

### Repository

- `FirebaseRepository`
- `AppSettingsRepository`

### Utilities

- `BeeTimingUtil`
- `HaversineUtil`
- `NotificationHelper`

## Package Responsibilities

### `data/model`

Contains core app models such as:

- user
- hive
- spray event
- health log
- notification

### `data/repository`

Handles:

- Firebase Auth
- Firestore reads and writes
- FCM token sync
- user settings persistence

### `viewmodel`

Handles:

- session state
- dashboard state
- spray submission state
- profile updates
- history clearing

### `ui/screens`

Contains screens for:

- auth
- dashboards
- map
- spray alert
- tips
- profile

### `ui/components`

Reusable Compose building blocks such as:

- section cards
- inline messages
- empty states
- headers

## Important Business Rules

1. All proximity checks use a 2 km radius.
2. Duplicate spray alerts from the same farmer are blocked for 5 minutes.
3. A spray time is invalid if it overlaps with bee-out time or the 4-hour pre-release safety window.
4. Spray markers disappear automatically after the full safety window ends.
5. Hive markers remain until the beekeeper changes them.
6. Old notification and farmer history records can be cleared up to yesterday to avoid stale clutter.

## Known Limitations

1. Background push delivery for a fully closed app still needs server-side FCM fan-out.
2. Farmer dashboard relevance is based on the latest spray reference area because continuous background farmer location tracking is intentionally not used.
3. Multilingual support is implemented for the main app flow and is designed to be extended further as more UI strings are centralized.

## How To Use The App

### First-time setup

1. Open the app.
2. Create an account.
3. Choose your role.
4. Choose your app language.
5. Allow location permission.
6. If you are a beekeeper, allow notification permission.

### If you are a beekeeper

1. Go to `Map`.
2. Add your hive.
3. Enter bee-out timings.
4. Check `Dashboard` for nearby spray alerts.
5. Use `Profile` to update contact number, language, and theme.

### If you are a farmer

1. Open `Spray`.
2. Select the field location.
3. Choose spray type, chemical name, duration, and start time.
4. If the time is unsafe, pick one of the safe suggestions.
5. Send the alert only when the app confirms it is safe.

## Design Direction

The visual system uses a honey-and-nature palette with:

- warm honey gold accents,
- green field tones,
- Material 3 surfaces,
- dark mode,
- light mode,
- map-first interaction for live local coordination.
