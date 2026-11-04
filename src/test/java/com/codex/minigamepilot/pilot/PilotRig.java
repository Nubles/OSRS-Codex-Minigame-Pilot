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
 * Runs the Minigame Pilot's three pilots on a replay, wired as the plugin wires them: the same
 * object, varbit, chat and game-state events, its settings, and its saved per-account state.
 *
 * <p>Commands it adds: {@code setting NAME on|off} (motherlode, furnace, homes, upperHopper),
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
	PilotRig(FakeGame game)
	{
		this.game = game;
		settings.put("motherlode", true);
		settings.put("furnace", true);
		settings.put("homes", true);
		settings.put("upperHopper", false);
		settings.put("tickCoach", true);
		pilots = new Pilots(game.client(), null, c -> { }, new FieldNotes(() -> null, new Gson()),
			bests::get, bests::put, id -> 0, store::get, store::put, () -> null);
		pilots.profile("replay");
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
				return true;
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
