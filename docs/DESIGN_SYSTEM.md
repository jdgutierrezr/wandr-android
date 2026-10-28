# Wandr Design System

Wandr's own design system is built on Material 3 and styled after the "Fluent green" prototype. It has two parts:
- **Tokens** (`ui/theme/`): colors, type, shapes and spacing.
- **Components** (`ui/components/`): reusable composables.

Screens combine these components. They should not style Material widgets directly.

Components with a `@Preview` (for example the buttons and the text fields) can be seen in Android Studio's *Split* / *Design* view.

## Tokens

| Token | Where | Use it as |
| --- | --- | --- |
| Brand palette | `Color.kt` | `MaterialTheme.colorScheme.primary` (Forest Green), `.secondary` (Sage), `.tertiary` (Sand), `.background` (Cream) |
| Extra colors | `Tokens.kt` → `WandrColors` | `WandrTheme.colors.textSecondary`, `.border`, `.successContainer`, `.locked`, `.danger`, `.heroGradient` |
| Type scale (Poppins) | `Type.kt` | `MaterialTheme.typography.headlineSmall`, `.titleMedium`, `.bodyMedium`, `.labelSmall`… |
| Shapes | `Tokens.kt` → `WandrShapes` | `MaterialTheme.shapes.medium` (cards, fields, buttons), `.large`, `.extraLarge` |
| Spacing | `Tokens.kt` → `WandrSpacing` | `WandrTheme.spacing.lg` (16 dp), `.screen` (20 dp side margin)… |

Rules:
- **Colors:** never write a hex color in a screen. If a color is missing, add a token.
- **Surfaces:** cards are white (`surface`) on a cream background (`background`) with a 1 dp `border`.
- **Text:** main text uses `onSurface`; addresses, hints and descriptions use `WandrTheme.colors.textSecondary`.

## Components

| Component | File | Mockup |
| --- | --- | --- |
| `WandrCard`, `IconCircle` | `WandrCard.kt` | Every card, icon circles |
| `PrimaryButton`, `SecondaryButton`, `DangerButton`, `LinkButton` | `WandrButtons.kt` | "Accept Quest", "Give me another one", "Need Help / SOS", "Skip for now" |
| `WandrTextField`, `PasswordTextField` | `WandrTextFields.kt` | Login and Sign up |
| `StatusBadge`, `XpBadge`, `WandrChip` | `Badges.kt` | "Completed", "+50 XP", category and interest filters |
| `SectionLabel`, `SectionHeader` | `SectionHeader.kt` | "OBJECTIVES", "Achievements 2/8 … View All" |
| `Avatar` | `Avatar.kt` | User photo or initials |
| `WandrTopBar`, `WandrBottomBar`, `MainTab` | `Bars.kt` | Top bar with streak, bottom bar (Home, Explore, Map, Saved, Profile) |
| `QuestCard`, `QuestRow`, `ObjectiveItem`, `RewardRow`, `CoverImage`, `MetaText` | `QuestComponents.kt` | Upcoming Quests, map card, Quest Objectives, Rewards |
| `StatCard`, `MilestoneCard`, `WandrProgressBar`, `AchievementCard` | `ProgressComponents.kt` | Side Quests (profile) |
| `WeekDaysRow`, `WeeklyQuestsChart`, `WeekComparisonMessage` | `StreakComponents.kt` | Profile: this week's active days, quests per week and the comparison with last week (BQ4) |
| `DropoffBars`, `DropoffBar` | `DropoffComponents.kt` | Abandonment funnel: abandons per step (BQ8) |
| `AuthLayout` | `AuthLayout.kt` | Login and Sign up frame |

## Adding a component

1. Check the table above. If something close already exists, add a parameter instead of copying it.
2. Create the component in `ui/components/`:
   - **Stateless:** the data comes in as parameters and events go out as lambdas.
   - **`modifier: Modifier = Modifier`:** it is the first optional parameter.
   - **Tokens only:** use `MaterialTheme` / `WandrTheme` values, no loose colors or sizes.
3. Add a `@Preview` so the team can see it.
4. Add it to the table above.
