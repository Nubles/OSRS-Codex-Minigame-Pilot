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
 * {@code totems wood WOOD|BEST} and {@code totems reward REWARD|none} (Vale Totems; setting names valeTotems and totemsClaim),
 * {@code tithe plants PLAN} and {@code tithe reward REWARD|none} (the Tithe Farm, by {@code Tithe.Plan} and {@code Tithe.Reward}
 * names; setting name titheFarm), {@code artefacts level N} (the Thieving level to work toward when stealing artefacts;
 * setting names stealingArtefacts and artefactsPatrols), {@code valuables level N} (the same for stealing valuables;
 * setting names stealingValuables and valuablesOwners),
 * {@code mixology plan ALL|PER_PASTE}, {@code mixology reward REWARD|none} and {@code mixology cue SPOTANIM} (a Mastering
 * Mixology setting, or a spot animation appearing, by its {@code SpotanimID} name; setting names mixology and mixologyCues),
 * {@code bank ITEM COUNT} (the bank as last seen), {@code teleport "Name" "Source" X Y SECONDS carried|banked},
 * {@code teleport pace S} and {@code teleport none} (Codex Wayfinder's shared atlas),
 * {@code fragments N} (fragments before the first Guardians of the Rift craft), {@code quest QUEST done|not}
 * (by RuneLite's {@code Quest} name), {@code hud elemental|catalytic ALTAR|none} (the rune icon the
 * Guardians of the Rift HUD shows for an open altar; the HUD component itself is a {@code widget}),
 * {@code saved KEY VALUE} to seed the account's saved state, and {@code profile NAME} for an
 * account login. Readings each tick: {@code activity}, {@code step} (its title), {@code verb},
 * {@code target}, {@code waiting}, {@code place}, {@code object} (the script's name for the
 * object lit up, else its id), {@code route} (tiles to walk), {@code destination} (a place beyond the scene, "X Y"), {@code others} (objects marked quietly), {@code lowPlanks} (the last low-planks line for the chat), and {@code saved.KEY} for every
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
	/** The Mastering Mixology settings as the script sets them. */
	private com.codex.minigamepilot.core.MixCycle.Plan mixPlan = com.codex.minigamepilot.core.MixCycle.Plan.ALL;
	private com.codex.minigamepilot.core.Mixology.Reward mixReward;
	/** The Vale Totems settings as the script sets them. */
	private com.codex.minigamepilot.core.Totems.Wood totemsWood;
	private com.codex.minigamepilot.core.Totems.Reward totemsReward;
	/** The Tithe Farm settings as the script sets them. */
	private com.codex.minigamepilot.core.Tithe.Plan tithePlan = com.codex.minigamepilot.core.Tithe.Plan.TWENTY;
	private com.codex.minigamepilot.core.Tithe.Reward titheReward;
	/** The Thieving level stealing artefacts works toward, as the script sets it. */
	private int artefactsLevel;
	/** The Thieving level stealing valuables works toward, as the script sets it. */
	private int valuablesLevel;
	/** The bank as last seen, set by {@code bank ITEM COUNT}; null until the script sets one. */
	private com.codex.minigamepilot.core.BankStock bank;
	private final Map<Integer, Long> bankItems = new HashMap<>();
	/** Codex Wayfinder's atlas as the script sets it with {@code teleport}; null without Wayfinder. */
	private List<com.codex.minigamepilot.core.WayfinderAtlas.Teleport> teleports;
	private double walkPace = 0.3;
	/** The last low-planks line said in the chat. */
	private String lowPlanks;
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
		settings.put("mixology", true);
		settings.put("mixologyCues", true);
		settings.put("valeTotems", true);
		settings.put("totemsClaim", true);
		settings.put("titheFarm", true);
		settings.put("stealingArtefacts", true);
		settings.put("artefactsPatrols", true);
		settings.put("stealingValuables", true);
		settings.put("valuablesOwners", true);
		pilots = new Pilots(game.client(), null, c -> { }, new FieldNotes(() -> null, new Gson()),
			bests::get, bests::put, game::price, store::get, store::put, () -> bank);
		pilots.atlas(() -> teleports == null ? null : com.codex.minigamepilot.core.WayfinderAtlas.of(walkPace, teleports));
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
				mixology();
				totems();
				tithe();
				artefacts();
				valuables();
				return true;
			case "valuables":
				// valuables level N
				if (!"level".equals(args.get(0)))
				{
					throw new IllegalArgumentException("valuables level N");
				}
				valuablesLevel = Integer.parseInt(args.get(1));
				valuables();
				return true;
			case "artefacts":
				// artefacts level N
				if (!"level".equals(args.get(0)))
				{
					throw new IllegalArgumentException("artefacts level N");
				}
				artefactsLevel = Integer.parseInt(args.get(1));
				artefacts();
				return true;
			case "tithe":
				// tithe plants PLAN | tithe reward REWARD|none
				if ("plants".equals(args.get(0)))
				{
					tithePlan = com.codex.minigamepilot.core.Tithe.Plan.valueOf(args.get(1));
				}
				else if ("reward".equals(args.get(0)))
				{
					titheReward = "none".equals(args.get(1)) ? null : com.codex.minigamepilot.core.Tithe.Reward.valueOf(args.get(1));
				}
				else
				{
					throw new IllegalArgumentException("tithe plants PLAN | tithe reward REWARD|none");
				}
				tithe();
				return true;
			case "totems":
				// totems wood WOOD|BEST | totems reward REWARD|none
				if ("wood".equals(args.get(0)))
				{
					totemsWood = "BEST".equals(args.get(1)) ? null : com.codex.minigamepilot.core.Totems.Wood.valueOf(args.get(1));
				}
				else if ("reward".equals(args.get(0)))
				{
					totemsReward = "none".equals(args.get(1)) ? null : com.codex.minigamepilot.core.Totems.Reward.valueOf(args.get(1));
				}
				else
				{
					throw new IllegalArgumentException("totems wood WOOD|BEST | totems reward REWARD|none");
				}
				totems();
				return true;
			case "mixology":
				// mixology plan PLAN | reward REWARD|none | cue SPOTANIM
				if ("plan".equals(args.get(0)))
				{
					mixPlan = com.codex.minigamepilot.core.MixCycle.Plan.valueOf(args.get(1));
				}
				else if ("reward".equals(args.get(0)))
				{
					mixReward = "none".equals(args.get(1)) ? null : com.codex.minigamepilot.core.Mixology.Reward.valueOf(args.get(1));
				}
				else if ("cue".equals(args.get(0)))
				{
					try
					{
						pilots.spotanim(net.runelite.api.gameval.SpotanimID.class.getField(args.get(1)).getInt(null));
					}
					catch (ReflectiveOperationException e)
					{
						throw new IllegalArgumentException("No SpotanimID." + args.get(1));
					}
					return true;
				}
				else
				{
					throw new IllegalArgumentException("mixology plan PLAN | reward REWARD|none | cue SPOTANIM");
				}
				mixology();
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
			case "bank":
			{
				// bank ITEM COUNT: the bank as the Pilot last saw it (by ItemID name or number).
				int id = itemId(args.get(0));
				bankItems.put(id, Long.parseLong(args.get(1)));
				bank = com.codex.minigamepilot.core.BankStock.seen(new HashMap<>(bankItems), Replay.START);
				return true;
			}
			case "teleport":
			{
				// teleport "Name" "Source" X Y SECONDS carried|banked | teleport pace S | teleport none
				if ("none".equals(args.get(0)))
				{
					teleports = null;
					return true;
				}
				if ("pace".equals(args.get(0)))
				{
					walkPace = Double.parseDouble(args.get(1));
					return true;
				}
				if (teleports == null)
				{
					teleports = new java.util.ArrayList<>();
				}
				teleports.add(new com.codex.minigamepilot.core.WayfinderAtlas.Teleport(args.get(0), args.get(1),
					Integer.parseInt(args.get(2)), Integer.parseInt(args.get(3)), 0, Integer.parseInt(args.get(4)), "carried".equals(args.get(5))));
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

	private static int itemId(String name)
	{
		try
		{
			return Integer.parseInt(name);
		}
		catch (NumberFormatException e)
		{
			try
			{
				return net.runelite.api.gameval.ItemID.class.getField(name).getInt(null);
			}
			catch (ReflectiveOperationException x)
			{
				throw new IllegalArgumentException("No ItemID." + name);
			}
		}
	}

	private void valuables()
	{
		pilots.valuables(settings.get("stealingValuables"), settings.get("valuablesOwners"), valuablesLevel);
	}

	private void artefacts()
	{
		pilots.artefacts(settings.get("stealingArtefacts"), settings.get("artefactsPatrols"), artefactsLevel);
	}

	private void tithe()
	{
		pilots.tithe(settings.get("titheFarm"), tithePlan, titheReward);
	}

	private void totems()
	{
		pilots.totems(settings.get("valeTotems"), totemsWood, settings.get("totemsClaim"), totemsReward);
	}

	private void mixology()
	{
		pilots.mixology(settings.get("mixology"), mixPlan, mixReward, settings.get("mixologyCues"));
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
		if (f.destination != null)
		{
			r.put("destination", f.destination.getX() + " " + f.destination.getY());
		}
		if (!f.others.isEmpty())
		{
			StringBuilder o = new StringBuilder();
			for (TileObject x : f.others)
			{
				o.append(o.length() == 0 ? "" : ", ").append(game.nameOf(x));
			}
			r.put("others", o.toString());
		}
		if (f.npc != null)
		{
			r.put("npc", f.npc.getName());
		}
		if (!f.wary.isEmpty())
		{
			r.put("wary", String.valueOf(f.wary.size()));
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
		String low = pilots.takeLowPlanks();
		if (low != null)
		{
			lowPlanks = low;
		}
		if (lowPlanks != null)
		{
			r.put("lowPlanks", lowPlanks);
		}
		store.forEach((k, v) -> r.put("saved." + k, v));
		return r;
	}
}
