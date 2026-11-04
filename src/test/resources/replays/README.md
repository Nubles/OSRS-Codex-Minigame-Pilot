# Replays

Each `.replay` file here is a scripted run through a minigame, tick by tick, played against the
real pilot code in CI by `ReplayTest`. The game client is a stand-in (`com.codex.replay.FakeGame`)
that the script drives: where the player stands, the ground, skills, varbits, what is carried,
and the objects that spawn and despawn. The Pilot reads it through RuneLite's own `Client`
interface, so its code runs unchanged.

A script says what the Pilot must show (`expect`), must never show (`never`) and must always
show (`always`). A failure names the file, line, tick and tile, and lists every reading that tick.

```
include /replays/scenes/motherlode.scene   # a shared layout
skill MINING 40
at 3740 5650
never target = LADDER                      # checked on every tick from here on
remove lowVein1
tick 5
expect step = "Wait for a vein"
```

The full command list is at the top of `src/test/java/com/codex/replay/Replay.java`; the Minigame
Pilot's own commands (`setting`, `saved`, `profile`) and readings are in `PilotRig.java`.

Rules for new scripts:

- Write the game facts a script relies on (levels, unlocks, ids) from the OSRS wiki or RuneLite's
  source, and cite the page in a comment. Ids are written by their RuneLite names.
- Scenes are schematic (the shape that matters, not the real map) and say so.
- Every player-reported bug gets a replay named after what went wrong, with the issue number in
  its first comment, before the fix is merged.
