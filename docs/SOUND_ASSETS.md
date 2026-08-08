# Sound assets

Drop your Stark Industries system-diagnostic sound effects into
`app/src/main/res/raw/`. They are resolved **by filename at runtime**, so the
project compiles and runs without them (it just plays silently) — add them
whenever you're ready.

> **Do not put this file (or any `.md`/uppercase-named file) in `res/raw/`.**
> Android compiles every file in `res/raw/` as a resource, and resource
> filenames must be lowercase `a–z`, `0–9`, or `_` only. That's why this doc
> lives in `docs/` instead.

| Cue (`HudSound`) | Filename (no extension) | Engine     | Suggested sound                          |
|------------------|-------------------------|------------|------------------------------------------|
| `POWER_UP`       | `jarvis_power_up`       | ExoPlayer  | Sustained arc-reactor power-up hum (~2s) |
| `RING_CHIME`     | `jarvis_ring_chime`     | SoundPool  | Short metallic chime                     |
| `TELEMETRY_BEEP` | `jarvis_beep`           | SoundPool  | Crisp UI beep (per telemetry line)       |
| `ONLINE`         | `jarvis_online`         | SoundPool  | Confirmation tone ("systems online")     |

## Format notes

- Use `.ogg` (preferred), `.mp3`, or `.wav`.
- Android resource names must be **lowercase**, digits, and underscores only.
- Keep SoundPool SFX short (< ~2s) and small — SoundPool decodes them fully into
  memory for zero-latency triggering.
- Example: `app/src/main/res/raw/jarvis_beep.ogg`
