# Sound assets

Drop your Stark Industries system-diagnostic sound effects here. They are
resolved **by filename at runtime**, so the project compiles and runs without
them (it just plays silently) — add them whenever you're ready.

| Cue (`HudSound`) | Filename (no extension) | Engine     | Suggested sound                         |
|------------------|-------------------------|------------|-----------------------------------------|
| `POWER_UP`       | `jarvis_power_up`       | ExoPlayer  | Sustained arc-reactor power-up hum (~2s)|
| `RING_CHIME`     | `jarvis_ring_chime`     | SoundPool  | Short metallic chime                    |
| `TELEMETRY_BEEP` | `jarvis_beep`           | SoundPool  | Crisp UI beep (per telemetry line)      |
| `ONLINE`         | `jarvis_online`         | SoundPool  | Confirmation tone ("systems online")    |

## Format notes

- Use `.ogg` (preferred), `.mp3`, or `.wav`.
- Android resource names must be **lowercase**, digits, and underscores only.
- Keep SoundPool SFX short (< ~2s) and small — SoundPool decodes them fully into
  memory for zero-latency triggering.
- Example: `app/src/main/res/raw/jarvis_beep.ogg`
