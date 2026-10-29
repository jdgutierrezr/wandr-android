# Assets

Original design files for Wandr. Keep the high-resolution sources here; the app uses exported copies under `app/src/main/res/`.

| Folder | Contents | Where it goes in the app |
|---|---|---|
| `icon/` | `wandr_icon.png`: 1024×1024 app icon (the bear, cropped square) | Already applied: adaptive foreground in `drawable-nodpi/ic_launcher_foreground.png`, background color `ic_launcher_background`, legacy `mipmap-*/ic_launcher*.webp` |
| `mascot/` | Wandr bear: `wandr_bear.jpeg` (logo, source of the app icon), `wandr_bear_map.png` and `wandr_bear_coffee.png` (full size, transparent) | The two PNGs are used in the app as `drawable-nodpi/wandr_bear_map.webp` (Map, no quests nearby) and `wandr_bear_coffee.webp` (Saved, no quests in progress): 640 px wide, with the green chroma-key residue removed. Use them with `painterResource(R.drawable.wandr_bear_map)` |

Android resource names must be lowercase, with only letters, numbers and underscores.
