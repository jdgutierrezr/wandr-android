# 3. Architectural Design

This document describes the architecture of the Wandr Android app (Kotlin, Jetpack Compose). The backend is Supabase and is documented in the `ISIS3510-Moviles-Group32` repository.

## Architecture - Overall System Structure and Components

### Diagram 1: Layers and external systems

```mermaid
flowchart TB
    subgraph APP["Wandr Android app"]
        direction TB
        subgraph UI["VIEW · UI layer (Jetpack Compose)"]
            Screens["Screens and navigation<br/>ui/feature, ui/navigation"]
            DS["Design system<br/>ui/theme, ui/components"]
        end

        subgraph PRES["VIEWMODEL · Presentation layer"]
            VM["ViewModels<br/>one per screen, expose StateFlow‹UiState›"]
        end

        subgraph MODEL["MODEL"]
            direction TB
            subgraph DOMAIN["Domain models"]
                Models["Quest, Place, Event, UserProfile…<br/>domain/model"]
            end

            subgraph DATA["Data layer"]
                Repos["Repositories<br/>data/repository"]
                Remote["Remote data sources + DTOs<br/>data/remote"]
                Local["Room database + DAOs<br/>data/local"]
                Mappers["Mappers<br/>data/mapper"]
            end
        end

        subgraph CORE["Core · cross-cutting"]
            DI["Dependency injection (Hilt)"]
            Bus["Event bus"]
            Strategy["Fetch strategies"]
            Device["Connectivity and location"]
        end
    end

    subgraph SB["Supabase (backend)"]
        Auth["Auth"]
        API["PostgREST: tables + RPC functions"]
        Storage["Storage: quest photos"]
        DB[("PostgreSQL + RLS")]
    end

    subgraph PHONE["Device"]
        GPS["GPS / network location"]
        Net["Network state"]
        SQLite[("SQLite file")]
    end

    Screens --> DS
    Screens -->|"observe state, send actions"| VM
    VM --> Repos
    VM --> Models
    Repos --> Remote
    Repos --> Local
    Repos --> Mappers
    Repos --> Strategy
    Repos --> Bus
    Remote -->|"HTTPS"| Auth & API & Storage
    Auth & API & Storage --> DB
    Local --> SQLite
    Device --> GPS & Net
    VM --> Device
```

**Rationale:** The app follows MVVM, and its three parts are layers with one-way dependencies: View → ViewModel → Model. Each one only knows the one below it.

- **View:** the Compose screens, the navigation graph and the design system. It draws the state and reports what the user does.
- **ViewModel:** one per screen. It holds the screen's state (`UiState`) and decides what to ask the Model for.
- **Model:** the domain models plus the data layer (repositories, Supabase data sources, Room). It owns the data and the access to it.

- **Why this separation:** a screen never talks to Supabase or Room. It only renders the state of its ViewModel. This lets the team build screens and data access in parallel, and lets us test the logic without a device (70 unit tests run on the JVM).
- **Why MVVM:** it is the pattern Google recommends for Compose. The ViewModel survives screen rotation and holds the state, so the UI is a pure function of that state.
- **Why a data layer with two sources:** the quality scenarios require the app to keep working without a connection (QS1) and to keep quest progress (QS8). The data layer hides whether the data comes from Supabase or from the local Room cache.
- **Why Supabase as the whole backend:** it already provides login, a REST API over PostgreSQL, storage and row-level security. The team does not need to run its own server. Business rules that must not be bypassed, such as XP, badges and event capacity, run in the database as RPC functions.
- **Why a `core` module:** injection, events, connectivity and location are used by several features. Keeping them in one place avoids duplicating them in every feature.

### Diagram 2: MVVM components per screen

```mermaid
flowchart LR
    subgraph VIEW["VIEW · Compose screens"]
        direction TB
        SplashS["SplashScreen"]
        LoginS["LoginScreen"]
        SignUpS["SignUpScreen"]
        HomeS["HomeScreen<br/>(Discovery Engine)"]
        ExploreS["ExploreScreen"]
        MapS["MapScreen"]
        DetailS["QuestDetailScreen"]
        ActiveS["ActiveQuestScreen"]
        ActiveListS["ActiveQuestsScreen<br/>(Saved tab)"]
        DropoffS["QuestDropoffScreen<br/>(debug builds)"]
        ProfileS["ProfileScreen"]
    end

    subgraph VMS["VIEWMODEL"]
        direction TB
        SessionVM["SessionViewModel"]
        LoginVM["LoginViewModel"]
        SignUpVM["SignUpViewModel"]
        HomeVM["HomeViewModel"]
        ExploreVM["ExploreViewModel"]
        MapVM["MapViewModel"]
        DetailVM["QuestDetailViewModel"]
        ActiveVM["ActiveQuestViewModel"]
        DropoffVM["QuestDropoffViewModel"]
        ProfileVM["ProfileViewModel"]
    end

    subgraph MODEL["MODEL · Repositories"]
        direction TB
        AuthR["AuthRepository"]
        ProfileR["ProfileRepository"]
        QuestR["QuestRepository"]
        StorageR["StorageRepository"]
        PlaceR["PlaceRepository"]
        EventR["EventRepository"]
        AnalyticsR["AnalyticsRepository"]
    end

    LocP["LocationProvider<br/>(device GPS)"]
    MapA["WandrMap<br/>(GoogleMapAdapter)"]
    Room[("Room cache")]
    SB["Supabase"]

    SplashS --> SessionVM
    LoginS --> LoginVM
    SignUpS --> SignUpVM
    HomeS --> HomeVM
    ExploreS --> ExploreVM
    MapS --> MapVM
    MapS --> MapA
    DetailS --> DetailVM
    ActiveS --> ActiveVM
    ActiveListS --> ActiveVM
    DropoffS --> DropoffVM
    ProfileS --> ProfileVM

    SessionVM --> AuthR
    LoginVM --> AuthR
    SignUpVM --> AuthR
    HomeVM --> QuestR
    ExploreVM --> EventR & QuestR
    MapVM --> QuestR & PlaceR
    DetailVM --> QuestR
    ActiveVM --> QuestR & StorageR & AnalyticsR
    DropoffVM --> AnalyticsR
    ProfileVM --> ProfileR & AuthR

    HomeVM & ExploreVM & MapVM & ActiveVM --> LocP

    ProfileR & QuestR & PlaceR & EventR --> Room
    MODEL --> SB
```

**Rationale:** This diagram shows the MVVM rule applied to every feature: one View, one ViewModel, and only the repositories that screen needs.

- **One ViewModel per screen:** each screen has its own state and its own reasons to change. A shared "god" ViewModel would make every screen depend on every repository. The Saved tab and the Active Quest screen share `ActiveQuestViewModel` because both show the same data: the quests in progress.
- **ViewModels never talk to each other:** when two screens care about the same thing, they share a repository (the data) or hear the same event (see Diagram 6). For example, `ProfileViewModel` updates because `CacheInvalidator` refreshes `ProfileRepository` after a quest.
- **Repositories are split by domain, not by screen:** `QuestRepository` serves five screens (Home, Explore, Map, Quest Details, Active Quest). The logic for quests exists once.
- **Only four repositories use the Room cache:** profile, quests, places and events are what the user must see offline (QS1, QS8). Auth, photo storage and the drop-off analytics always need the network.
- **Location is a device service, not a repository:** `LocationProvider` reads the GPS. Only the four screens that need the user's position depend on it.
- **The map is behind an interface too:** `MapScreen` draws a `WandrMap`, and `GoogleMapAdapter` is the only class that imports Google Maps.

### Diagram 3: Components of the data layer

This diagram zooms into one repository of Diagram 2. It uses quests as the example. The other domains (profile, places, events) follow the same shape.

```mermaid
flowchart TB
    VM["VIEWMODEL<br/>e.g. HomeViewModel"]

    subgraph DATA["MODEL · Data layer"]
        RepoI["«interface» QuestRepository"]
        Impl["QuestRepositoryImpl"]
        Strat["FetchStrategy<br/>NetworkFirst · CacheFirst · CacheOnly"]
        Mapper["Mappers<br/>DTO ↔ Entity ↔ Model"]
        Remote["QuestRemoteDataSource<br/>(Facade over supabase-kt)"]
        DTO["DTOs"]
        DAO["QuestDao"]
        Room[("Room<br/>local cache")]
    end

    subgraph EVT["Event subscribers"]
        Bus["AppEventBus"]
        Inv["CacheInvalidator"]
    end

    subgraph SB["Supabase"]
        API["Auth · PostgREST · Storage"]
    end

    VM -->|"calls"| RepoI
    RepoI -.->|"Flow of models"| VM
    RepoI -.->|"Hilt injects"| Impl
    Impl --> Strat
    Impl --> Mapper
    Strat --> DAO
    Strat --> Remote
    DAO <--> Room
    Remote --> DTO
    Remote --> API

    Impl -->|"QuestCompleted"| Bus
    Bus --> Inv
    Bus -->|"domain events"| VM
    Inv -->|"refreshes"| Profile["ProfileRepository"]
```

**Rationale:** Each component has one job, so a change in one place does not spread.

- **Repository interface:** ViewModels depend on `QuestRepository`, not on its implementation. Tests replace it with a fake, and Hilt hands out the implementation without the ViewModel noticing.
- **Remote data source:** it is the only place that imports `supabase-kt`. If the SDK or an endpoint changes, only this class changes.
- **DTOs and mappers:** DTOs copy the backend's JSON exactly. Mappers translate them to Room entities and to domain models. A renamed column is fixed in one mapper instead of in every screen.
- **Room as the single source of truth:** reads always come from Room. The network only refreshes Room. This gives one code path for online and offline, and the UI updates by itself when the cache changes.
- **Event bus:** refreshing the profile after a quest is a side effect. It lives in a subscriber, so the quest repository does not need to know about profiles.

## Architecture Design - Component Interaction and Communication
| Pattern / style | Where | Why |
| --- | --- | --- |
| **MVVM** | `ui/feature/**/XViewModel.kt` | Screens only render a `UiState` and call functions. All the logic can be tested without Android UI. |
| **Repository** | `data/repository/` | One interface per domain. ViewModels depend on the interface, never on Supabase or Room. |
| **DTO** | `data/remote/dto/` | Each class mirrors a Supabase response exactly (`@SerialName("snake_case")`). Postgres enums stay as `String`, so a new backend value cannot break decoding. |
| **DAO** | `data/local/dao/` | All local SQL lives in Room DAOs. Queries return `Flow`, and multi-step writes use `@Transaction`. |
| **Mapper (Adapter)** | `data/mapper/` | Converts DTO → Entity → domain model. If a column changes, only the mapper changes. |
| **Facade** | `data/remote/datasource/*RemoteDataSource` | Hides the `supabase-kt` query builder behind simple methods such as `nearbyQuests(lat, lng, radius)`. Only these classes import Supabase. |
| **Adapter** | `ui/components/map/`, `core/location/` | `WandrMap` and `LocationProvider` are the interfaces the app expects. `GoogleMapAdapter` and `FusedLocationProvider` translate them to the Google SDKs, so changing the map provider does not touch any screen. |
| **Strategy** | `core/strategy/` | `NetworkFirst`, `CacheFirst` and `CacheOnly` are interchangeable. `StrategySelector` switches to `CacheOnly` when there is no connection. Repositories do not repeat this logic. |
| **Observer** | Room `Flow` → Repository → ViewModel | When Room changes, every screen that shows that data updates automatically. |
| **Event-based** | `core/event/AppEventBus.kt` | Publishers (`QuestRepositoryImpl`, `AuthRepository`, `SessionViewModel`) and subscribers (`CacheInvalidator`, `ProfileViewModel`) do not know each other. |
| **Singleton + Dependency Injection** | `core/di/` (Hilt) | One `SupabaseClient` (one session) and one `WandrDatabase` for the whole app. Tests pass in fakes. |
| **Layers + single source of truth** | Remote → Room → Repository | Reads always come from Room, so the app shows saved data offline (QS1) and keeps quest progress (QS8). |

### Diagram 4: Reading data (nearby quests)

```mermaid
sequenceDiagram
    box View
        participant UI as HomeScreen
    end
    box ViewModel
        participant VM as HomeViewModel
    end
    box Model
        participant R as QuestRepositoryImpl
        participant S as FetchStrategy
        participant DB as Room
    end
    participant API as Supabase

    UI->>VM: observes uiState
    VM->>R: nearbyQuests(location)
    R->>S: select(policy).fetch(local, refresh)
    alt online (NetworkFirst)
        S->>API: nearby_quests RPC + quest_tags
        API-->>S: DTOs (JSON)
        S->>DB: mappers → entities, replaceNearby()
    else offline (CacheOnly)
        Note over S: skips the network
    end
    DB-->>VM: Flow of quests (isStale = true when offline)
    VM-->>UI: new UiState
```

**Rationale:** This flow answers one quality scenario.

- **Offline (QS1):** the `StrategySelector` picks `CacheOnly` when there is no connection. The ViewModel receives the saved quests with `isStale = true` and the screen shows "No internet connection. Showing saved activities".
- **Reactive UI:** the ViewModel does not ask "did it finish?". It observes a `Flow` from Room, so any later change in the cache reaches the screen without extra code.

### Diagram 5: Writing data (checking a quest step)

```mermaid
sequenceDiagram
    box View
        participant UI as ActiveQuestScreen
    end
    box ViewModel
        participant VM as ActiveQuestViewModel
        participant P as ProfileViewModel
    end
    box Model
        participant St as StorageRepository
        participant R as QuestRepositoryImpl
        participant DB as Room
    end
    participant API as Supabase
    participant Bus as AppEventBus
    participant Inv as CacheInvalidator

    UI->>VM: completeObjective(quest, step, photo)
    VM->>St: uploadQuestPhoto() (only if the step needs a photo)
    St->>API: Storage upload to quest-photos/<user>/<quest>/<step>.jpg
    VM->>R: completeObjective(quest, step, photoPath)
    R->>API: complete_objective RPC
    API-->>R: progress, XP, level, new badges
    R->>API: download active quests
    R->>DB: replaceActive()
    opt it was the last step
        R->>Bus: QuestCompleted
        Bus->>Inv: QuestCompleted
        Inv->>API: download profile, badges, history
        Inv->>DB: update cache
        DB-->>P: Flow emits the new XP and badges
        Bus->>P: QuestCompleted (show celebration)
    end
    R-->>VM: Result‹ObjectiveResult›
    VM-->>UI: new UiState (progress, XP, badges)
```

**Rationale:**

- **Rules on the server:** XP, level, streak and badges are computed inside the `complete_objective` RPC. The app cannot grant itself XP, even if it is modified, because the database rejects direct writes to those columns.
- **Writes return a `Result`:** every failure is mapped to a readable `AppError`. Backend messages such as "Quest is not in progress" reach the user as they are. Writes need a connection. Offline, they fail with "No internet connection".
- **Decoupled side effects:** the quest repository only publishes `QuestCompleted`. The `CacheInvalidator` refreshes the profile, and `ProfileViewModel` updates because it observes Room. No component calls another feature directly.

### Diagram 6: Event-based communication

```mermaid
flowchart LR
    subgraph PUB["Publishers"]
        QR["QuestRepositoryImpl"]
        AR["AuthRepository"]
        SVM["SessionViewModel"]
    end

    Bus(["AppEventBus<br/>SharedFlow‹AppEvent›"])

    subgraph SUB["Subscribers"]
        CI["CacheInvalidator<br/>refreshes profile, clears cache on sign out"]
        PVM["ProfileViewModel<br/>shows the celebration"]
    end

    QR -->|"QuestCompleted"| Bus
    AR -->|"SignedOut"| Bus
    SVM -->|"SignedOut"| Bus

    Bus --> CI
    Bus --> PVM
```

**Rationale:** Some things must happen "whenever X happens", wherever X comes from. A direct call would force every publisher to know every interested component.

- **Loose coupling:** a publisher does not know who listens. Adding a new reaction, for example a push notification when a quest is completed, means adding one subscriber and changing no existing code.
- **App-wide subscriber:** `CacheInvalidator` starts in `WandrApplication` and lives as long as the app.
- **Limit of the bus:** it is only used for notifications. Data still travels through repositories and Room, so there is always one place to read the current state.

## Design Patterns and Tactics

### Design patterns

| Pattern | Where | Problem it solves |
| --- | --- | --- |
| **MVVM** | `ui/feature/**/XViewModel.kt` | Screens only render a `UiState` and call functions. All the logic is testable without Android UI. |
| **Repository** | `data/repository/` | One interface per domain. ViewModels never depend on Supabase or Room. |
| **DTO** | `data/remote/dto/` | Each class mirrors a Supabase response exactly. Postgres enums stay as `String`, so a new backend value cannot break decoding. |
| **DAO** | `data/local/dao/` | All local SQL lives in Room DAOs. Queries return `Flow`, and multi-step writes use `@Transaction`. |
| **Mapper (Adapter)** | `data/mapper/` | Converts DTO → Entity → domain model. If a column changes, only the mapper changes. |
| **Adapter** | `ui/components/map/`, `core/location/` | `WandrMap` and `LocationProvider` are the interfaces the app expects. `GoogleMapAdapter` and `FusedLocationProvider` translate them to the Google SDKs, so changing the map provider does not touch any screen. |
| **Facade** | `data/remote/datasource/` | Hides the `supabase-kt` query builder behind simple methods such as `nearbyQuests(lat, lng, radius)`. |
| **Strategy** | `core/strategy/` | `NetworkFirst`, `CacheFirst` and `CacheOnly` are interchangeable. `StrategySelector` picks one for each request. |
| **Observer** | Room `Flow` → Repository → ViewModel → Compose | When data changes, every screen that shows it updates automatically. |
| **Publish-subscribe** | `core/event/AppEventBus.kt` | Publishers and subscribers do not know each other. |
| **Singleton + Dependency Injection** | `core/di/` (Hilt) | One `SupabaseClient` (one session) and one database for the whole app. Tests pass in fakes. |

### Tactics

| Quality attribute | Tactic | Where | Scenario |
| --- | --- | --- | --- |
| Availability | Local cache with fallback: if the network fails, show saved data marked as stale | `FetchStrategy`, Room | QS1 |
| Availability | Detect the fault: observe connectivity and switch strategy before calling the network | `ConnectivityObserver`, `StrategySelector` | QS1 |
| Availability | Degraded mode for location: current fix → last known → center of Bogotá | `LocationProvider` | QS11 |
| Reliability | Persist state: quest progress is saved in Room after every step | `QuestDao` | QS8 |
| Performance | Reduce requests: Quest Details reads the active quests from the cache first (`CacheFirst`) | `QuestDetailViewModel`, `CacheFirst` | - |
| Energy | Location updates only after moving 50 m | `LocationProvider` | QS5 |
| Security | Authorize on the server: row-level security and RPC functions; the app only holds a publishable key | Supabase | QS6 |
| Security | Limit exposure: the cache is cleared on sign out; photos go to a private bucket, in a per-user folder | `CacheInvalidator`, `storage.sql` | QS6 |
| Testability | Depend on interfaces and inject fakes | Repositories, Hilt | - |
| Modifiability | Isolate the backend SDK behind facades and mappers | `data/remote`, `data/mapper` | - |

**Rationale:** The patterns and tactics were chosen from the quality scenarios and business questions of Sprint 1, not for their own sake.

- **Offline and persistence (QS1, QS8)** drive the largest decisions: Room as the single source of truth, the Strategy pattern to choose between cache and network, and the Observer pattern so the UI follows the cache.
- **Modifiability** matters because the backend is still changing. DTOs, mappers and facades confine a backend change to the data layer.
- **Security** is enforced by the backend. The app is treated as untrusted: it can only do what row-level security and the RPC functions allow.

One limit of the current design:
- **Writes need a connection.** Completing a step offline fails with a message; it is not queued and retried. Scenario QS10 (eventual connectivity) is not covered yet.

## Appendix

### Packages

```
com.kotlin.wandr
├── WandrApplication.kt      Starts the event bus subscriber
├── core/
│   ├── di/                  Hilt modules (Supabase, Room, repositories, device services)
│   ├── error/               AppError, ErrorMapper, safeCall
│   ├── event/               AppEvent, AppEventBus, CacheInvalidator
│   ├── location/            LocationProvider
│   ├── network/             ConnectivityObserver
│   └── strategy/            FetchStrategy, StrategySelector, Resource
├── data/
│   ├── local/               WandrDatabase, dao/, entity/
│   ├── mapper/
│   ├── remote/              dto/, datasource/
│   └── repository/          Interfaces + implementations
├── domain/model/            Models used by the ViewModels and the UI
└── ui/
    ├── theme/               Design tokens (see DESIGN_SYSTEM.md)
    ├── components/          Reusable composables, including map/ (WandrMap, GoogleMapAdapter)
    ├── navigation/          Navigation graph
    └── feature/             session, auth, home, events (Explore), map, quest, analytics, profile
```

## ViewModel per screen

| Screen | ViewModel | Repositories |
| --- | --- | --- |
| Splash / session gate | `SessionViewModel` + `SplashScreen` | Auth (`sessionState`) |
| Log in | `LoginViewModel` | Auth |
| Sign up | `SignUpViewModel` | Auth |
| Home (Discovery Engine) | `HomeViewModel` | Quest + LocationProvider |
| Explore | `ExploreViewModel` | Event, Quest + LocationProvider |
| Map | `MapViewModel` | Quest, Place + LocationProvider |
| Quest Details | `QuestDetailViewModel` + `QuestDetailScreen` | Quest |
| Active Quest / Navigation | `ActiveQuestViewModel` + `ActiveQuestScreen` | Quest, Storage, Analytics + LocationProvider |
| My Quests (Saved tab) | `ActiveQuestViewModel` + `ActiveQuestsScreen` | Quest |
| Abandonment Funnel (BQ8, internal, debug builds only) | `QuestDropoffViewModel` + `QuestDropoffScreen` | Analytics (`get_quest_dropoff` RPC) |
| Profile / Side Quests | `ProfileViewModel` + `ProfileScreen` | Profile (+ `get_streak_summary` RPC for BQ4), Auth |

Every `UiState` has an `errorMessage` that is ready to show, plus an `onErrorShown()` function. Screens that can show cached data also have `isShowingSavedData`.

## BQ8: where users abandon quests

*"In which step do users most frequently abandon a quest without finishing it?"* (Type 3)

```mermaid
flowchart LR
    QR["QuestRepository<br/>abandon_quest keeps the checked steps"]
    QC[("quest_completions +<br/>quest_objective_completions")]
    RPC["RPC get_quest_dropoff"]
    AR["AnalyticsRepository"]
    Screen["QuestDropoffScreen<br/>Abandonment funnel"]
    Hint["ActiveQuestScreen<br/>drop-off hint"]

    QR --> QC --> RPC --> AR
    AR --> Screen
    AR --> Hint
```

- **Answer (for the team):** `get_quest_dropoff()` reads every abandoned quest and the last step that was checked, and the
  Abandonment Funnel screen shows the quest + step with most abandons and the totals per step. BQ8 is Type 3 (features
  analysis), so this screen is an internal tool: it is only reachable from Profile in **debug builds**.
- **For the user:** the same data powers the drop-off hint in Active Quest (smart feature). A failure is silent: no data or no
  connection simply means no hint.
- **Progress persistence:** active quests and checked steps live in Room (`activeQuests`), so the tracker keeps the
  progress offline and after the app is closed.

## Session gate (authentication)

The app starts on a splash screen instead of Login. `SessionViewModel` observes `AuthRepository.sessionState`:

| Supabase status | `SessionState` | What the app does |
| --- | --- | --- |
| `Initializing` | `Loading` | Splash keeps waiting for the saved session |
| `Authenticated` | `SignedIn` | Splash opens Home: no password needed |
| `RefreshFailure` | `Reconnecting` | The user stays in the app (usually offline, or back from the camera) |
| `NotAuthenticated` | `SignedOut` | Splash opens Login; anywhere else the app goes back to Login |

When a session that existed ends (logout, expired or revoked token), `SessionViewModel` publishes
`AppEvent.SignedOut`, so `CacheInvalidator` clears the previous user's cached data.

## Live distance in Active Quest (context-aware + GPS)

While Active Quest is visible, `ActiveQuestViewModel.trackDistanceTo()` follows `LocationProvider.locationUpdates()`
(every 5 s or 10 m) and calculates the straight-line distance to the quest's place on the phone
(`GeoPoint.distanceMetersTo`, haversine). No maps service is called.

- Within 100 m the screen shows "You've arrived!" with the next step.
- A remembered position (`LAST_KNOWN`) is shown as approximate; without permission or a fix (`FALLBACK`) the
  live distance is hidden.
- The GPS stops when the screen is paused or closed (`LifecycleResumeEffect`), to save battery (QS5).

