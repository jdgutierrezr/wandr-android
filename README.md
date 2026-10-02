# Wandr

Wandr app developed for Android using Kotlin.

## Problem

People often fall into a routine and struggle to find interesting things to do nearby. Finding something new means searching across social media, review sites and word of mouth and comparing options by hand. Because of that friction, many people end up doing nothing or repeating the same activities.

## Solution

Wandr uses the user's location to show popular activities in nearby places, in one simple and accessible feed. Users can pick a plan based on their interests and location without jumping between platforms or deciding from scratch.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- MVVM + Repository, Hilt, Room (offline cache)
- Supabase (database, auth and storage) through [supabase-kt](https://github.com/supabase-community/supabase-kt)
- Min SDK 24

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the layers, the design patterns and how a request flows,
and [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) for the tokens and UI components.
The backend is documented in the `ISIS3510-Moviles-Group32` repository.

## Design

- **Font:** Poppins
- **Palette:**

| Name | Hex |
|---|---|
| Forest Green | `#163820` |
| Sage | `#5A7B62` |
| Sand | `#BFA78A` |
| Taupe | `#9A876F` |
| Cream | `#FCFAF6` |

## Project structure

```
app/src/main/
├── java/com/kotlin/wandr/   Kotlin source code
│   ├── MainActivity.kt      App entry point
│   ├── core/                Cross-cutting code: DI, events, errors, strategies, location, telemetry
│   ├── data/                DTOs, Supabase data sources, Room, mappers, repositories
│   ├── domain/model/        Models used by the ViewModels and the UI
│   └── ui/                  Theme and one ViewModel per screen
├── res/                     Resources (images, fonts, strings, icons)
└── AndroidManifest.xml      App configuration and permissions
app/schemas/                 Room schema history (generated, keep it in git)
assets/                      Original design files (icon, mascot)
docs/                        Architecture documentation
```

## Getting started

1. Add the Supabase keys and a Google Maps key to `local.properties` in the project root. This file is not committed.

   ```properties
   SUPABASE_URL=https://cugtwwqqxczwtkfkubrf.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_YPjvNLSqznQitYTinqvabg_p8V5e8-i
   MAPS_API_KEY=<your key>
   ```

   The Maps key is created in Google Cloud Console with **Maps SDK for Android** enabled. Without it the map screen stays blank.

2. Open the project in Android Studio and let Gradle sync.
3. Run the `app` configuration on an emulator or device.

In Supabase Auth, **Confirm email** must be off. Otherwise, sign up cannot create the user's profile.

## Tests

| Command | What it runs |
| --- | --- |
| `./gradlew testDebugUnitTest` | Unit tests: strategies, mappers, the telemetry decorator, the event bus and every ViewModel |
| `WANDR_SUPABASE_SMOKE=1 ./gradlew testDebugUnitTest --tests "*SupabaseReadOnlySmokeTest*"` | Decodes real Supabase responses with a test user. Read only |
| `./gradlew connectedDebugAndroidTest` | Room DAO tests and `SupabaseIntegrationTest`. **The integration test changes data**, so reset the database afterwards |
