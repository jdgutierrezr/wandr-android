# Assets

Original design files for Wandr. Keep the high-resolution sources here; the app uses exported copies under `app/src/main/res/`.

| Folder | Contents | Where it goes in the app |
|---|---|---|
| `icon/` | App icon (ideally a 1024×1024 PNG or SVG) | Android Studio → *New → Image Asset* generates `mipmap-*/ic_launcher*` |
| `mascot/` | Wandr bear mascot | Copy a PNG/WebP to `app/src/main/res/drawable/` (e.g. `wandr_bear.png`) and use it with `painterResource(R.drawable.wandr_bear)` |

Android resource names must be lowercase, with only letters, numbers and underscores.
