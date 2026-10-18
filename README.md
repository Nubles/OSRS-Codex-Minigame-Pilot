# Codex Minigame Pilot

**One clear next step for Motherlode Mine, Blast Furnace and Mahogany Homes.** The Pilot watches the minigame you are in and always shows the single thing to do next: what to click, lit in gold in the world, the path there drawn on the ground and the minimap, and a line on why. You still do every click. It only reads the game and draws.

It is part of the **Codex** suite, alongside [Codex Navigator](https://github.com/Nubles/OSRS-Codex-Navigator), [Codex Guardian](https://github.com/Nubles/OSRS-Codex-Guardian), [Codex Gear Roadmap](https://github.com/Nubles/OSRS-Codex-Gear-Roadmap), [Codex Session Chronicle](https://github.com/Nubles/OSRS-Codex-Session-Chronicle), [Codex Ledger](https://github.com/Nubles/OSRS-Codex-Ledger) and [Codex Quartermaster](https://github.com/Nubles/OSRS-Codex-Quartermaster), and shares their look: warm ink, aged gold and the game's own fonts.

<p align="center">
  <img src="docs/screenshots/motherlode.png" width="225" alt="Motherlode Mine">
  <img src="docs/screenshots/furnace.png" width="225" alt="Blast Furnace">
  <img src="docs/screenshots/homes.png" width="225" alt="Mahogany Homes">
</p>

## What it does

### Motherlode Mine
- **Every load sized to fill the sack.** With 89 pay-dirt in the sack it says to mine 19, not 27 the hopper cannot take. Pay-dirt still washing through the wheel is counted as in flight, so the sack is never overfilled by a load that has not landed yet.
- **Fill, then empty.** It remembers whether you are filling the sack or emptying it, so after banking it sends you back to the sack, not the veins, until the sack is empty.
- **The nearest vein on foot.** Distances are walked over the game's own collision map, so a vein around a corner beats one behind a wall. Veins with another player standing at them count as further away. The next vein after this one is quietly outlined too.
- **Rockfalls and struts.** When the quickest way runs through a rockfall, the step becomes mining it. When both struts are broken and pay-dirt is held up, the step is hammering one.
- **Either level.** It tells the upper and lower levels apart and sends you to the ladder when what you need is on the other.

### Blast Furnace
- **Coal trips worked out ahead.** At the bank it compares the coal in the furnace, plus a full coal bag, with what a load of ore will burn, and calls a coal trip only when the ore would otherwise sit unsmelted. Steel with a coal bag never needs one; runite gets three to each ore trip.
- **The whole round.** Deposit bars, fill the coal bag, withdraw coal or ore (ringed in the bank), load the belt, empty the coal bag and load again, put on ice gloves while the bars smelt, take them, bank. Gold asks for goldsmith gauntlets before the ore goes on the belt.
- **Coffer watch.** It asks you to top up the coffer before it runs dry, and the panel shows how many minutes of smelting are left.
- **Any bar.** Iron, steel, mithril, adamantite, runite, gold and silver, picked from the ore you carry, the ore in the furnace or the ore in your bank, or chosen in the settings.

### Mahogany Homes
- **From contract to points.** Which contractor to ask and for which tier, the planks and steel bars to bring (counted from the job's own furniture, plank sack included), the way to the client's house with a hint arrow and a world map marker, the teleport you carry that gets there quickest, then each piece of furniture in walking order.
- **Floor by floor.** When the rest of the work, or the client, is upstairs, the step is the ladder.
- **A checklist.** Every piece of furniture with what it needs (repair, remove, build) and what it costs, ticked off as you go.

<p align="center">
  <img src="docs/screenshots/motherlode-sack.png" width="225" alt="Emptying the sack">
  <img src="docs/screenshots/furnace-wait.png" width="225" alt="Waiting for bars">
  <img src="docs/screenshots/homes-travel.png" width="225" alt="Travelling to a client">
</p>

## In the game view

- The thing to click is filled and outlined in gold, with a small plate naming the action (Deposit, Take, Build).
- The walk there is a trail of teal beads on the ground that brightens toward the end, and the same path on the minimap.
- Items a step names are ringed in the inventory, the worn equipment and the open bank.
- A slim plaque holds the step and its line of why.

<p align="center">
  <img src="docs/screenshots/plaque.png" alt="Plaque">
  <img src="docs/screenshots/plaque-furnace.png" alt="Plaque at the Blast Furnace">
  <img src="docs/screenshots/plaque-homes.png" alt="Plaque at a Mahogany Homes job">
</p>

## The panel

The helm at the top holds the next action as one large gilt word and the full instruction under it, and flares when the step changes. Under it, the minigame's loop with your place in it, its gauges (the sack with pay-dirt in flight, the furnace's coal against what the next load needs, the planks the job still takes), the furniture checklist, and the session so far: pay-dirt, ore, bars, contracts, points and experience, with hourly pace. Away from the minigames it lists all three and what is waiting at each, such as an open contract.

## Pace coach

Every time the loop comes back to its start (a load of pay-dirt in the hopper, a trip to the furnace, a finished contract) the round is timed and compared with your best round of the same kind. The panel shows the time, how far it was over or under your best, and the one thing that cost the most: ticks spent standing still with something to do (the bank, dialogue and menus don't count) or tiles walked beyond the shortest way. Your bests are kept per account, so they carry over to the next session. Rounds with a break in them are left out.

<p align="center">
  <img src="docs/screenshots/pace.png" width="225" alt="Pace coach">
</p>

## Field notes

An optional setting, **Keep field notes**, writes a plain record of what the Pilot saw and decided to `.runelite/codex-minigame-pilot/` while you are in a minigame: each step it chose, any target it could not find, the minigame's varbits, game messages, your clicks, objects and characters nearby (with whether the Pilot was tracking them), and each timed round. It is for checking the guidance against real play. It is off unless you turn it on, it stays on your computer, and nothing is sent anywhere.

## Rules posture

Codex Minigame Pilot is guidance only. It reads what the game already shows you (your inventory, worn items, the bank when open, the minigames' own counters, and objects and characters in view) and draws on top. It does not click, type, change menus or send anything over the network. Field notes, when turned on, are only written to a local file. It gives no combat or boss advice.

## Settings

Each minigame can be turned off. The Blast Furnace bar can be fixed instead of worked out, and the coffer warning level set. The plaque, ground path, minimap path, action labels, quieter marks for what comes next, hint arrow and world map marker can each be turned off. Field notes are under their own section, off by default.

## Development

```bash
./gradlew build   # compile and run tests
./gradlew run     # launch RuneLite with the plugin loaded
```

The planners and the walking-distance search (`core/`) are plain Java with unit tests. `pilot/` reads the game and picks targets, and `ui/` and `overlay/` draw.

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
