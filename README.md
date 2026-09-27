# Wandr

Wandr app developed for Android using Kotlin.

## Problem

People often fall into a routine and struggle to find interesting things to do nearby. Finding something new means searching across social media, review sites and word of mouth and comparing options by hand. Because of that friction, many people end up doing nothing or repeating the same activities.

## Solution

Wandr uses the user's location to show popular activities in nearby places, in one simple and accessible feed. Users can pick a plan based on their interests and location without jumping between platforms or deciding from scratch.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Supabase (database and auth)
- Min SDK 24

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
│   └── ui/theme/            Colors, typography and theme
├── res/                     Resources (images, fonts, strings, icons)
└── AndroidManifest.xml      App configuration and permissions
assets/                      Original design files (icon, mascot)
```

## Getting started

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or device.
