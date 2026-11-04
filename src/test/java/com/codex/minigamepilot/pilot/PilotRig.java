package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.ui.View;
import com.codex.replay.FakeGame;
import com.codex.replay.Replay;
import com.codex.replay.Rig;
import com.google.gson.Gson;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.api.GameState;
import net.runelite.api.TileObject;

/**
 * Runs the Minigame Pilot's pilots on a replay, wired as the plugin wires them: the same
 * object, varbit, chat and game-state events, its settings, and its saved per-account state.
 *
 * <p>Commands it adds: {@code setting NAME on|off} (motherlode, furnace, homes, upperHopper, rift, tickCoach),
 * {@code stormload N} (Tempoross's "Load what you hold at storm"; setting names tempoross and temporossCook too),
 * {@code foundry stages TOOL...} and {@code foundry bonus on|off} (the Giants' Foundry HUD's stage icons and sweet spot),
 * {@code fragments N} (fragments before the first Guardians of the Rift craft), {@code quest QUEST done|not}
 * (by RuneLite's {@code Quest} name), {@code hud elemental|catalytic ALTAR|none} (the rune icon the
 * Guardians of the Rift HUD shows for an open altar; the HUD component itself is a {@code widget}),
 * {@code saved KEY VALUE} to seed the account's saved state, and {@code profile NAME} for an
 * account login. Readings each tick: {@code activity}, {@code step} (its title), {@code verb},
 * {@code target}, {@code waiting}, {@code place}, {@code object} (the script's name for the
 * object lit up, else its id), {@code route} (tiles to walk), and {@code saved.KEY} for every
 * saved key.
 */
final class PilotRig implements Rig
{
	private final FakeGame game;
	private final Map<String, String> store = new TreeMap<>();
	private final Map<View.Activity, String> bests = new EnumMap<>(View.Activity.class);
	private final Map<String, Boolean> settings = new HashMap<>();
	private final Pilots pilots;
	/** The last tick's guidance, for screenshots of a scripted moment. */
	Flight last;
	/** Quests the script has said are done; FakeGame cannot run the game's quest script. */
	private final java.util.Set<net.runelite.api.Quest> quests = java.util.EnumSet.noneOf(net.runelite.api.Quest.class);
	/** Sprites shown on HUD components by id; FakeGame's widgets have no sprites. */
	private final Map<Integer, Integer> sprites = new HashMap<>();
	/** The "Load what you hold at storm" setting for Tempoross. */
	private int stormLoad = 80;
	/** The Giants' Foundry HUD as the script sets it. */
	private com.codex.minigamepilot.core.FoundryCycle.Tool[] foundryStages = new com.codex.minigamepilot.core.FoundryCycle.Tool[0];
	private boolean foundryBonus;
	PilotRig(FakeGame game)
	{
		this.game = game;
		settings.put("motherlode", true);
		settings.put("furnace", true);
		settings.put("homes", true);
		settings.put("upperHopper", false);
		settings.put("tickCoach", true);
		settings.put("rift", true);
		settings.put("tempoross", true);
		settings.put("temporossCook", true);
		settings.put("foundry", true);
		pilots = new Pilots(game.client(), null, c -> { }, new FieldNotes(() -> null, new Gson()),
			bests::get, bests::put, id -> 0, store::get, store::put, () -> null);
		pilots.profile("replay");
		pilots.riftReaders(q -> quests.contains(q), c -> sprites.getOrDefault(c, -1));
		pilots.foundryHud(new FoundryPilot.Hud()
		{
			@Override
			public com.codex.minigamepilot.core.FoundryCycle.Tool[] stages(net.runelite.api.Client client)
			{
				return foundryStages.clone();
			}

			@Override
			public boolean bonus(net.runelite.api.Client client)
			{
				return foundryBonus;
			}
		});
	}

	@Override
	public boolean command(String verb, List<String> args)
	{
		switch (verb)
		{
			case "setting":
				if (!settings.containsKey(args.get(0)))
				{
					throw new IllegalArgumentException("No setting " + args.get(0) + ", only " + settings.keySet());
				}
				settings.put(args.get(0), "on".equals(args.get(1)));
				pilots.upperHopper(settings.get("upperHopper"));
				pilots.tickCoach(settings.get("tickCoach"));
				pilots.rift(settings.get("rift"));
				pilots.tempoross(settings.get("tempoross"), settings.get("temporossCook"), stormLoad);
				pilots.foundry(settings.get("foundry"));
				return true;
			case "foundry":
			{
				// foundry stages HAMMER GRIND ... | foundry bonus on|off: the HUD's stage icons and
				// sweet spot, which FakeGame's widgets cannot draw.
				if ("stages".equals(args.get(0)))
				{
					foundryStages = new com.codex.minigamepilot.core.FoundryCycle.Tool[args.size() - 1];
					for (int i = 1; i < args.size(); i++)
					{
						foundryStages[i - 1] = com.codex.minigamepilot.core.FoundryCycle.Tool.valueOf(args.get(i));
					}
				}
				else if ("bonus".equals(args.get(0)))
				{
					foundryBonus = "on".equals(args.get(1));
				}
				else
				{
					throw new IllegalArgumentException("foundry stages TOOL... | foundry bonus on|off");
				}
				return true;
			}
			case "stormload":
				stormLoad = Integer.parseInt(args.get(0));
				pilots.tempoross(settings.get("tempoross"), settings.get("temporossCook"), stormLoad);
				return true;
			case "fragments":
				pilots.riftFragments(Integer.parseInt(args.get(0)));
				return true;
			case "quest":
			{
				net.runelite.api.Quest q = net.runelite.api.Quest.valueOf(args.get(0));
				if ("done".equals(args.get(1)))
				{
					quests.add(q);
				}
				else
				{
					quests.remove(q);
				}
				// The Pilot asks about a quest not yet done only now and then; a script changes it now.
				pilots.riftReaders(x -> quests.contains(x), c -> sprites.getOrDefault(c, -1));
				return true;
			}
			case "hud":
			{
				// hud elemental|catalytic ALTAR|none: the rune icon a GotR HUD portal component shows.
				int component = "elemental".equals(args.get(0)) ? net.runelite.api.gameval.InterfaceID.GotrHud.ELEMENTAL_PORTAL
					: net.runelite.api.gameval.InterfaceID.GotrHud.CATALYTIC_PORTAL;
				if ("none".equals(args.get(1)))
				{
					sprites.remove(component);
				}
				else
				{
					com.codex.minigamepilot.core.Altar a = com.codex.minigamepilot.core.Altar.valueOf(args.get(1));
					sprites.put(component, net.runelite.api.gameval.SpriteID.IconRune32x32._0 + a.ordinal());
				}
				return true;
			}
			case "saved":
				store.put(args.get(0), args.get(1));
				return true;
			case "profile":
				pilots.profile(args.get(0));
				return true;
			default:
				return false;
		}
	}

	@Override
	public void spawned(TileObject object)
	{
		pilots.scene().add(object);
	}

	@Override
	public void despawned(TileObject object)
	{
		pilots.scene().remove(object);
	}

	@Override
	public void varbit(int id, int value)
	{
		pilots.varbit(id, value);
	}

	@Override
	public void chat(String type, String message)
	{
		pilots.chat(type, message);
	}

	@Override
	public void gameState(GameState state)
	{
		switch (state)
		{
			case LOADING:
				pilots.clearScene();
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				pilots.relogged(Replay.START + game.tickCount() * Replay.TICK_MS);
				break;
			default:
				break;
		}
	}

	@Override
	public Map<String, String> tick(long now)
	{
		Flight f = pilots.fly(settings.get("motherlode"), settings.get("furnace"), settings.get("homes"), null, 0, now);
		last = f;
		Map<String, String> r = new TreeMap<>();
		r.put("activity", f.activity.name());
		if (f.step != null)
		{
			r.put("step", f.step.getTitle());
			r.put("verb", f.step.getVerb());
			r.put("target", f.step.getTarget().name());
			r.put("waiting", String.valueOf(f.step.isWaiting()));
		}
		if (f.view.place() != null)
		{
			r.put("place", f.view.place());
		}
		if (f.object != null)
		{
			r.put("object", game.nameOf(f.object));
		}
		r.put("route", String.valueOf(f.route.size()));
		if (f.npc != null)
		{
			r.put("npc", f.npc.getName());
		}
		if (f.step != null)
		{
			r.put("detail", f.step.getDetail());
		}
		r.putAll(f.view.readings());
		if (!f.marks.isEmpty())
		{
			r.put("marks", String.join(", ", f.marks));
		}
		View.TickCard t = f.view.ticks();
		if (t != null)
		{
			r.put("ticks.last", String.valueOf(t.getLastTicks()));
			r.put("ticks.foot", String.valueOf(t.getFootTicks()));
			r.put("ticks.floor", String.valueOf(t.getFloorTicks()));
			r.put("ticks.leak", t.getLeak());
			r.put("ticks.loops", String.valueOf(t.getLoops()));
			r.put("ticks.perHour", String.valueOf(t.getPerHour()));
		}
		store.forEach((k, v) -> r.put("saved." + k, v));
		return r;
	}
}
