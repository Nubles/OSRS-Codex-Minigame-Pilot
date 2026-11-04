package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Alloy;
import com.codex.minigamepilot.core.FoundryCycle;
import com.codex.minigamepilot.core.Mould;
import com.codex.minigamepilot.core.MouldPick;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Swords;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Flies the Giants' Foundry: the one next step through each sword, the tool its stage needs at
 * the heat that tool needs, the best moulds you own for the commission, and your swords with
 * their reputation and experience an hour. It only reads the game (varbits, the HUD, the mould
 * jig's list, what you carry and the objects around you) and draws.
 *
 * <p>Ids are RuneLite's gameval names. The heat, progress, crucible, commission and mould unlock
 * varbits are {@code VarbitID.GIANTS_FOUNDRY_*}; reputation is
 * {@code VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS}. How the HUD shows a stage's tool (the
 * {@code GiantsFoundryToolIcon} sprites, hammer, grindstone, polishing wheel in that order on the
 * completion bar's children), the sweet spot (a gold first child on the sweet-spot layer), the
 * heat band width (the grindstone band against the whole bar), the commission varbits' values
 * and a locked mould's grey text are as the Easy Giants' Foundry plugin reads them: a
 * cross-check, not a source.
 */
public final class FoundryPilot
{
	static final int KOVAC = NpcID.GIANTS_FOUNDRY_KOVAC;
	static final int KOVAC_MULTI = NpcID.GIANTS_FOUNDRY_KOVAC_MULTI;
	static final int TRIP_HAMMER = ObjectID.GIANTS_FOUNDRY_TRIP_HAMMER;
	static final int GRINDSTONE = ObjectID.GIANTS_FOUNDRY_GRINDSTONE;
	static final int POLISHING_WHEEL = ObjectID.GIANTS_FOUNDRY_POLISHING_WHEEL;
	static final int LAVA_POOL = ObjectID.GIANTS_FOUNDRY_LAVA_POOL;
	static final int WATERFALL = ObjectID.GIANTS_FOUNDRY_WATERFALL;
	static final int CHEST = ObjectID.GIANTS_FOUNDRY_CHEST;
	static final int[] CRUCIBLES = {ObjectID.GIANTS_FOUNDRY_CRUCIBLE_MULTI, ObjectID.GIANTS_FOUNDRY_CRUCIBLE_EMPTY,
		ObjectID.GIANTS_FOUNDRY_CRUCIBLE_PARTIAL, ObjectID.GIANTS_FOUNDRY_CRUCIBLE_FULL};
	static final int[] JIGS = {ObjectID.GIANTS_FOUNDRY_MOULD_JIG, ObjectID.GIANTS_FOUNDRY_JIG_EMPTY, ObjectID.GIANTS_FOUNDRY_JIG_MOULD,
		ObjectID.GIANTS_FOUNDRY_JIG_MOULD_FILLED, ObjectID.GIANTS_FOUNDRY_JIG_MOULD_COOLING, ObjectID.GIANTS_FOUNDRY_JIG_MOULD_COOLED};
	static final int[] STORAGE = {ObjectID.GIANTS_FOUNDRY_PREFORM_STORAGE, ObjectID.GIANTS_FOUNDRY_PREFORM_STORAGE_EMPTY,
		ObjectID.GIANTS_FOUNDRY_PREFORM_STORAGE_FULL};
	static final int PREFORM = ItemID.GIANTS_FOUNDRY_PREFORM;
	static final int[] BARS = {ItemID.BRONZE_BAR, ItemID.IRON_BAR, ItemID.STEEL_BAR, ItemID.MITHRIL_BAR, ItemID.ADAMANTITE_BAR,
		ItemID.RUNITE_BAR};
	/** Either lets you lift a hot preform worn (OSRS wiki, Giants' Foundry). */
	static final int[] GLOVES = {ItemID.ICE_GLOVES, ItemID.SMITHING_UNIFORM_GLOVES_ICE};
	static final int[] COUNTS = {VarbitID.GIANTS_FOUNDRY_BRONZE_COUNT, VarbitID.GIANTS_FOUNDRY_IRON_COUNT,
		VarbitID.GIANTS_FOUNDRY_STEEL_COUNT, VarbitID.GIANTS_FOUNDRY_MITHRIL_COUNT, VarbitID.GIANTS_FOUNDRY_ADAMANT_COUNT,
		VarbitID.GIANTS_FOUNDRY_RUNE_COUNT};
	/** One varbit a bought mould: five a part. */
	static final int[][] UNLOCKS = {
		{VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_7, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_8,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_9, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_10,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_RICASSO_11},
		{VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_7, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_8,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_9, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_10,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_BLADE_11},
		{VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_7, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_8,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_9, VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_10,
			VarbitID.GIANTS_FOUNDRY_UNLOCKED_MOULD_TIP_11}};
	/** A mould the jig lists in this grey cannot be used (the Easy Giants' Foundry plugin). */
	static final int LOCKED_TEXT = 0x9f9f9f;
	/** The sweet spot's gold (the Easy Giants' Foundry plugin). */
	static final int SWEET_SPOT = 0xfcd703;
	static final String FINISHED = "You have successfully finished the sword";
	static final String TOO_DAMAGED = "This sword is too damaged to keep working";
	static final String SWORDS_KEY = "foundry.swords";
	static final String MOULDS_KEY = "foundry.moulds";
	static final String REP_KEY = "foundry.rep";

	/** Reads the parts of the HUD the replays cannot draw: the stage icons and the sweet spot. */
	interface Hud
	{
		FoundryCycle.Tool[] stages(Client client);

		boolean bonus(Client client);
	}

	/** The game's own HUD. */
	static final Hud GAME_HUD = new Hud()
	{
		@Override
		public FoundryCycle.Tool[] stages(Client client)
		{
			Widget bar = client.getWidget(InterfaceID.GiantsFoundryHud.COMPLETION_BAR);
			if (bar == null || bar.isHidden())
			{
				return new FoundryCycle.Tool[0];
			}
			List<FoundryCycle.Tool> out = new ArrayList<>();
			Widget[] children = bar.getChildren();
			if (children != null)
			{
				for (Widget c : children)
				{
					FoundryCycle.Tool t = c == null ? null : toolOfSprite(c.getSpriteId());
					if (t != null)
					{
						out.add(t);
					}
				}
			}
			return out.toArray(new FoundryCycle.Tool[0]);
		}

		@Override
		public boolean bonus(Client client)
		{
			Widget w = client.getWidget(InterfaceID.GiantsFoundryHud.SWEETSPOT_LAYER);
			if (w == null || w.isHidden() || w.getChildren() == null || w.getChildren().length == 0)
			{
				return false;
			}
			Widget first = w.getChild(0);
			return first != null && first.getTextColor() == SWEET_SPOT;
		}
	};

	static FoundryCycle.Tool toolOfSprite(int sprite)
	{
		if (sprite == SpriteID.GiantsFoundryToolIcon._0)
		{
			return FoundryCycle.Tool.HAMMER;
		}
		if (sprite == SpriteID.GiantsFoundryToolIcon._1)
		{
			return FoundryCycle.Tool.GRIND;
		}
		if (sprite == SpriteID.GiantsFoundryToolIcon._2)
		{
			return FoundryCycle.Tool.POLISH;
		}
		return null;
	}

	private final Session session = new Session(Skill.SMITHING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private Hud hud = GAME_HUD;
	private Swords swords;
	/** Moulds the jig has listed as yours, learnt and kept per account. */
	private Set<Mould> learnt;

	/** The sword's stages, read once a preform is in hand. */
	private FoundryCycle.Tool[] stages = new FoundryCycle.Tool[0];
	private int halfBand = -1;
	private boolean ruined;
	private boolean finished;
	/** The tick the commission was seen given, or -1. */
	private int commissionTick = -1;
	private boolean hadCommission;
	private int lastRep = -1;
	private int lastXp = -1;
	/** A hand-in whose experience has not shown yet: its reputation, and ticks to wait. */
	private int pendingQuality = -1;
	private int pendingTicks;
	private int pendingWait;
	/** Smithing experience two ticks before a hand-in, to count Kovac's from; and a tick before the last. */
	private int baseXp = -1;
	private int olderXp = -1;
	/** The mould score the jig showed as set for this sword, or -1. */
	private int mouldSet = -1;
	/** The metal score of the crucible as it was poured for this sword, or 0. */
	private int poured;
	private int handedIn;
	private int lastStage;
	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setHud(Hud hud)
	{
		this.hud = hud == null ? GAME_HUD : hud;
	}

	static boolean isWatched(int id)
	{
		return id == TRIP_HAMMER || id == GRINDSTONE || id == POLISHING_WHEEL || id == LAVA_POOL || id == WATERFALL || id == CHEST
			|| StormPilot.contains(CRUCIBLES, id) || StormPilot.contains(JIGS, id) || StormPilot.contains(STORAGE, id);
	}

	private Swords swords()
	{
		if (swords == null)
		{
			swords = Swords.decode(load == null ? null : load.apply(SWORDS_KEY));
		}
		return swords;
	}

	private Set<Mould> learnt()
	{
		if (learnt == null)
		{
			learnt = decodeMoulds(load == null ? null : load.apply(MOULDS_KEY));
		}
		return learnt;
	}

	static Set<Mould> decodeMoulds(String text)
	{
		Set<Mould> out = EnumSet.noneOf(Mould.class);
		if (text == null)
		{
			return out;
		}
		for (String part : text.split(","))
		{
			try
			{
				out.add(Mould.valueOf(part.trim()));
			}
			catch (IllegalArgumentException e)
			{
				// A mould this version does not know: left out.
			}
		}
		return out;
	}

	static String encodeMoulds(Collection<Mould> moulds)
	{
		StringBuilder b = new StringBuilder();
		for (Mould m : moulds)
		{
			if (!m.isDefault())
			{
				if (b.length() > 0)
				{
					b.append(',');
				}
				b.append(m.name());
			}
		}
		return b.toString();
	}

	/** At the foundry: its machines are in the loaded scene. */
	boolean active(Client client, SceneIndex scene)
	{
		return client.getLocalPlayer() != null && (!scene.get(TRIP_HAMMER).isEmpty() || !scene.get(CRUCIBLES).isEmpty()
			|| !scene.get(JIGS).isEmpty());
	}

	/** A game message, tags removed. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		if (message.contains(TOO_DAMAGED))
		{
			ruined = true;
		}
		if (message.contains(FINISHED))
		{
			finished = true;
		}
	}

	void left()
	{
		stages = new FoundryCycle.Tool[0];
	}

	void newAccount()
	{
		session.startOver();
		swords = null;
		learnt = null;
		commissionTick = -1;
		hadCommission = false;
		lastRep = -1;
		lastXp = -1;
		olderXp = -1;
		pendingQuality = -1;
		poured = 0;
		handedIn = 0;
		mouldSet = -1;
	}

	/** A running count for the pace coach: swords handed in. */
	int units()
	{
		return handedIn;
	}

	/** Moulds you own of a part, and whether that is known for sure. */
	static final class Owned
	{
		final Set<Mould> moulds;
		final boolean sure;

		Owned(Set<Mould> moulds, boolean sure)
		{
			this.moulds = moulds;
			this.sure = sure;
		}
	}

	/**
	 * The moulds you own: the defaults, the ones the jig has listed as yours, and, for a part
	 * whose bought count the game says is 0 or all five, exactly that. A part is sure when the
	 * moulds learnt for it match the count the game keeps.
	 */
	Owned owned(Client client, int level)
	{
		Set<Mould> out = MouldPick.defaults();
		boolean sure = true;
		for (Mould.Part part : Mould.Part.values())
		{
			int bought = 0;
			for (int varbit : UNLOCKS[part.ordinal()])
			{
				bought += client.getVarbitValue(varbit) > 0 ? 1 : 0;
			}
			List<Mould> known = new ArrayList<>();
			List<Mould> all = new ArrayList<>();
			for (Mould m : Mould.values())
			{
				if (m.getPart() == part && !m.isDefault())
				{
					all.add(m);
					if (learnt().contains(m))
					{
						known.add(m);
					}
				}
			}
			if (bought >= all.size())
			{
				out.addAll(all);
			}
			else if (bought > 0)
			{
				out.addAll(known);
				sure &= known.size() == bought;
			}
		}
		return new Owned(out, sure);
	}

	/** Learns from the mould jig's list which moulds you can set. */
	private void learnMoulds(Client client)
	{
		Widget list = client.getWidget(InterfaceID.GiantsFoundryMould.CONTENT);
		if (list == null || list.isHidden())
		{
			return;
		}
		boolean changed = false;
		for (Widget[] kids : new Widget[][]{list.getDynamicChildren(), list.getChildren()})
		{
			if (kids == null)
			{
				continue;
			}
			for (Widget c : kids)
			{
				Mould m = c == null ? null : Mould.ofTitle(c.getText());
				if (m == null || m.isDefault())
				{
					continue;
				}
				boolean yours = c.getTextColor() != LOCKED_TEXT;
				if (yours && learnt().add(m) || !yours && learnt().remove(m))
				{
					changed = true;
				}
			}
		}
		if (changed && save != null)
		{
			save.accept(MOULDS_KEY, encodeMoulds(learnt()));
		}
	}

	/** The mould score the open jig shows for what is set, or -1. */
	static int jigScore(Client client)
	{
		int a = lastNumber(client.getWidget(InterfaceID.GiantsFoundryMould.KEYVAL1));
		int b = lastNumber(client.getWidget(InterfaceID.GiantsFoundryMould.KEYVAL2));
		return a < 0 || b < 0 ? -1 : a + b;
	}

	private static int lastNumber(Widget w)
	{
		if (w == null || w.isHidden() || w.getText() == null)
		{
			return -1;
		}
		String t = w.getText().replaceAll("<[^>]*>", "").trim();
		int space = t.lastIndexOf(' ');
		try
		{
			return Integer.parseInt(space < 0 ? t : t.substring(space + 1));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/** A commission word as the open jig shows it, or null. */
	private static Mould.Style jigWord(Client client, int component)
	{
		Widget w = client.getWidget(component);
		if (w == null || w.isHidden() || w.getText() == null)
		{
			return null;
		}
		return Mould.Style.ofWord(w.getText().replaceAll("<[^>]*>", "").replace(":", ""));
	}

	/** Half a heat band, per mille, from the HUD: the grindstone band against the whole bar. */
	static int halfBand(Client client)
	{
		Widget bar = client.getWidget(InterfaceID.GiantsFoundryHud.TEMPERATURE_BAR_RECT1);
		Widget med = client.getWidget(InterfaceID.GiantsFoundryHud.GRIND_BAR);
		if (bar == null || med == null || bar.isHidden() || bar.getWidth() <= 0 || med.getWidth() <= 0)
		{
			return -1;
		}
		return (int) Math.round(med.getWidth() * (double) FoundryCycle.SCALE / bar.getWidth() / 2);
	}

	/** What a multiloc shows now, or the id itself. */
	private static int shownId(Client client, int id)
	{
		ObjectComposition def = client.getObjectDefinition(id);
		if (def != null && def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			if (now != null)
			{
				return now.getId();
			}
		}
		return id;
	}

	static FoundryCycle.Jig jig(int shown)
	{
		if (shown == ObjectID.GIANTS_FOUNDRY_JIG_EMPTY)
		{
			return FoundryCycle.Jig.EMPTY;
		}
		if (shown == ObjectID.GIANTS_FOUNDRY_JIG_MOULD)
		{
			return FoundryCycle.Jig.SET;
		}
		if (shown == ObjectID.GIANTS_FOUNDRY_JIG_MOULD_FILLED || shown == ObjectID.GIANTS_FOUNDRY_JIG_MOULD_COOLING)
		{
			return FoundryCycle.Jig.COOLING;
		}
		if (shown == ObjectID.GIANTS_FOUNDRY_JIG_MOULD_COOLED)
		{
			return FoundryCycle.Jig.COOLED;
		}
		return FoundryCycle.Jig.UNKNOWN;
	}

	/** Every tick, at the foundry or not: a hand-in's reputation and experience into your swords. */
	void settle(Client client, long now)
	{
		int rep = client.getVarpValue(VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS);
		int xp = client.getSkillExperience(Skill.SMITHING);
		if (client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			// Skills and varps have not arrived: nothing read now is a change.
			return;
		}
		int tick = client.getTickCount();
		if (pendingQuality >= 0 && --pendingWait <= 0)
		{
			// Kovac's experience lands about the tick his reputation does; the few ticks either
			// side are counted, as no machine works while you talk to him.
			int gained = baseXp >= 0 && xp > baseXp ? xp - baseXp : 0;
			swords().add(new Swords.Sword(now, pendingTicks, pendingQuality, gained));
			persist();
			pendingQuality = -1;
		}
		else if (pendingQuality < 0 && lastRep >= 0 && rep > lastRep)
		{
			// Reputation only rises at a hand-in, and by the sword's final quality.
			pendingQuality = rep - lastRep;
			pendingTicks = commissionTick >= 0 ? tick - commissionTick : 0;
			pendingWait = 2;
			baseXp = olderXp >= 0 ? olderXp : lastXp;
			handedIn++;
			commissionTick = -1;
			mouldSet = -1;
			poured = 0;
			ruined = false;
			finished = false;
		}
		if (rep != lastRep && save != null && rep >= 0)
		{
			save.accept(REP_KEY, Integer.toString(rep));
		}
		lastRep = rep;
		olderXp = lastXp;
		lastXp = xp;
	}

	private void persist()
	{
		if (save != null)
		{
			save.accept(SWORDS_KEY, swords().encode());
		}
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		int tick = client.getTickCount();
		int level = client.getRealSkillLevel(Skill.SMITHING);
		learnMoulds(client);

		FoundryCycle.Snapshot s = new FoundryCycle.Snapshot();
		lastState = s;
		s.first = Mould.Style.ofVarbit(client.getVarbitValue(VarbitID.GIANTS_FOUNDRY_COMMISSION_WORD_1));
		s.second = Mould.Style.ofVarbit(client.getVarbitValue(VarbitID.GIANTS_FOUNDRY_COMMISSION_WORD_2));
		Mould.Style w1 = jigWord(client, InterfaceID.GiantsFoundryMould.KEYWORD1);
		Mould.Style w2 = jigWord(client, InterfaceID.GiantsFoundryMould.KEYWORD2);
		if (w1 != null && w2 != null)
		{
			s.first = w1;
			s.second = w2;
		}
		boolean commission = s.first != null && s.second != null;
		if (commission && !hadCommission)
		{
			commissionTick = tick;
			mouldSet = -1;
		}
		hadCommission = commission;
		int shown = jigScore(client);
		if (shown >= 0)
		{
			mouldSet = shown;
		}

		int[] counts = new int[COUNTS.length];
		for (int i = 0; i < COUNTS.length; i++)
		{
			counts[i] = client.getVarbitValue(COUNTS[i]);
		}
		s.crucible = Alloy.bars(counts);
		s.metal = Alloy.metalScore(counts);
		if (s.crucible >= Alloy.CRUCIBLE)
		{
			poured = s.metal;
		}
		s.barsCarried = pack.count(BARS);
		Collection<TileObject> jigs = scene.get(JIGS);
		s.jig = jigs.isEmpty() ? FoundryCycle.Jig.UNKNOWN : jig(shownId(client, jigs.iterator().next().getId()));
		s.holding = pack.wears(PREFORM);
		s.stored = !s.holding && client.getVarbitValue(VarbitID.GIANTS_FOUNDRY_PREFORM_STORED) > 0;
		s.canPickUp = pack.carries(ItemID.BUCKET_WATER) || pack.wears(GLOVES);
		s.progress = client.getVarbitValue(VarbitID.GIANTS_FOUNDRY_PREFORM_COMPLETION);
		s.heat = client.getVarbitValue(VarbitID.GIANTS_FOUNDRY_PREFORM_TEMPERATURE);
		if (s.holding)
		{
			if (stages.length == 0)
			{
				stages = hud.stages(client);
			}
			int band = halfBand(client);
			if (band > 0)
			{
				halfBand = band;
			}
			s.bonus = hud.bonus(client);
		}
		else if (!s.stored)
		{
			stages = new FoundryCycle.Tool[0];
			halfBand = -1;
			if (s.jig != FoundryCycle.Jig.COOLED)
			{
				ruined = false;
				finished = false;
			}
		}
		s.stages = stages;
		s.halfBand = halfBand;
		s.ruined = ruined;
		if (finished && s.holding)
		{
			s.progress = Math.max(s.progress, FoundryCycle.SCALE);
		}
		Owned owned = owned(client, level);
		s.pick = commission ? MouldPick.best(s.first, s.second, owned.moulds) : null;

		Step step = FoundryCycle.next(s);
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = route(client, scene, ground, step, at);
		if (step.getTarget() == Step.Target.MOULD_JIG && s.pick != null && s.jig == FoundryCycle.Jig.EMPTY)
		{
			for (Mould.Part p : Mould.Part.values())
			{
				if (s.pick.get(p) != null)
				{
					flight.marks.add(s.pick.get(p).getTitle());
				}
			}
		}
		if (step.getTarget() == Step.Target.CRUCIBLE && s.barsCarried > 0)
		{
			step = step.withItems(BARS);
			Flight withBars = new Flight(View.Activity.FOUNDRY, step, null);
			withBars.object = flight.object;
			withBars.route = flight.route;
			withBars.marks.addAll(flight.marks);
			flight = withBars;
		}

		int stage = FoundryCycle.stage(step);
		if (stage < 0)
		{
			stage = lastStage;
		}
		lastStage = stage;
		View view = new View(true, View.Activity.FOUNDRY, step, place(s), FoundryCycle.LOOP, stage);
		Flight out = new Flight(View.Activity.FOUNDRY, step, view);
		out.object = flight.object;
		out.npc = flight.npc;
		out.route = flight.route;
		out.marks.addAll(flight.marks);
		dress(view, s, owned, level, client, now);
		return out;
	}

	private Flight route(Client client, SceneIndex scene, Ground ground, Step step, WorldPoint at)
	{
		Flight flight = new Flight(View.Activity.FOUNDRY, step, null);
		Collection<? extends TileObject> things;
		switch (step.getTarget())
		{
			case KOVAC:
			{
				List<NPC> who = new ArrayList<>();
				for (NPC n : client.getTopLevelWorldView().npcs())
				{
					if (n != null && (n.getId() == KOVAC || n.getId() == KOVAC_MULTI))
					{
						who.add(n);
					}
				}
				if (who.isEmpty())
				{
					return flight;
				}
				Ground.Way<NPC> way = ground == null ? null : ground.nearestNpc(who);
				if (way != null)
				{
					return flight.to(way);
				}
				flight.npc = who.get(0);
				return flight;
			}
			case MOULD_JIG:
				things = scene.get(JIGS);
				break;
			case CRUCIBLE:
				things = scene.get(CRUCIBLES);
				break;
			case BANK_CHEST:
				things = scene.get(CHEST);
				break;
			case TRIP_HAMMER:
				things = scene.get(TRIP_HAMMER);
				break;
			case GRINDSTONE:
				things = scene.get(GRINDSTONE);
				break;
			case POLISHING_WHEEL:
				things = scene.get(POLISHING_WHEEL);
				break;
			case LAVA_POOL:
				things = scene.get(LAVA_POOL);
				break;
			case WATERFALL:
				things = scene.get(WATERFALL);
				break;
			case PREFORM_STORAGE:
				things = scene.get(STORAGE);
				break;
			default:
				return flight;
		}
		if (things.isEmpty())
		{
			return flight;
		}
		Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
		if (way != null)
		{
			return flight.to(way);
		}
		TileObject best = null;
		for (TileObject o : things)
		{
			if (best == null || o.getWorldLocation().distanceTo(at) < best.getWorldLocation().distanceTo(at))
			{
				best = o;
			}
		}
		flight.object = best;
		return flight;
	}

	private static String place(FoundryCycle.Snapshot s)
	{
		String words = FoundryCycle.commission(s);
		return words == null ? "Giants' Foundry" : words;
	}

	/** The sword's gauges and stages, the moulds, your reputation and swords. */
	private void dress(View view, FoundryCycle.Snapshot s, Owned owned, int level, Client client, long now)
	{
		boolean working = s.holding && s.progress < FoundryCycle.SCALE;
		FoundryCycle.Tool tool = FoundryCycle.tool(s);
		if (working)
		{
			int[] band = FoundryCycle.band(tool, s.halfBand);
			view.gauge(new View.Gauge("Heat", s.heat / 10, 0, 100,
				band == null ? (tool == null ? "The HUD shows the bands" : tool.getTitle() + " needs it " + tool.heatWord())
					: tool.getTitle() + ": " + Math.round(band[0] / 10.0) + "-" + Math.round(band[1] / 10.0) + "%", heatTone(s, band)));
			view.gauge(new View.Gauge("Progress", s.progress / 10, 0, 100,
				s.stages.length == 0 ? "Sword" : "Stage " + (FoundryCycle.stageIndex(s) + 1) + " of " + s.stages.length, Theme.GOLD));
		}
		else if (!s.holding && !s.stored)
		{
			view.gauge(new View.Gauge("Crucible", s.crucible, 0, Alloy.CRUCIBLE,
				s.metal > 0 ? "Metal score " + s.metal + " · " + Alloy.sections(s.metal) + " sections" : "28 bars' worth", Theme.EMBER));
		}

		if (s.holding && s.stages.length > 0)
		{
			List<View.Line> lines = new ArrayList<>();
			int at = FoundryCycle.stageIndex(s);
			for (int i = 0; i < s.stages.length; i++)
			{
				FoundryCycle.Tool t = s.stages[i];
				View.Line.Mark mark = s.progress >= FoundryCycle.SCALE || i < at ? View.Line.Mark.DONE
					: i == at ? View.Line.Mark.NEXT : View.Line.Mark.TODO;
				lines.add(new View.Line(t.getTitle(), t.heatWord(), mark));
			}
			int spots = Alloy.sweetSpots(s.stages.length);
			view.lines("The sword", s.stages.length + " sections · " + spots + " sweet spots", lines);
		}

		if (s.first != null && s.second != null && s.pick != null && !s.holding && !s.stored)
		{
			view.pick(mouldCard(s, owned, level));
		}

		int rep = client.getVarpValue(VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS);
		if (s.pick != null || mouldSet >= 0)
		{
			int mould = mouldSet >= 0 ? mouldSet : s.pick.getScore();
			int metal = s.metal > 0 ? s.metal : poured;
			// Without the metal poured (a preform picked up before the Pilot saw the crucible) there is no figure to give.
			if (metal > 0)
			{
				int quality = metal + mould;
				view.stat(new View.Stat(Integer.toString(quality), "Quality up to",
					"About " + Session.format(Alloy.xp(quality)) + " xp", Theme.GOLD_BRIGHT));
			}
		}
		view.stat(new View.Stat(Session.format(rep), "Reputation", "Kovac's shop", Theme.TEAL));
		Swords g = swords();
		double repHour = g.repPerHour();
		if (repHour >= 0)
		{
			view.stat(new View.Stat(Session.format(Math.round(repHour)), "Rep an hour",
				"Over " + g.count() + (g.count() == 1 ? " sword" : " swords"), Theme.GOLD));
		}
		view.stat(new View.Stat(Session.format(handedIn), "Swords", session.perHour(handedIn, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Smithing xp", session.perHour(xp, now), Theme.GREEN));

		if (!(s.holding && s.stages.length > 0) && g.count() > 0)
		{
			List<View.Line> lines = new ArrayList<>();
			List<Swords.Sword> all = g.all();
			Swords.Sword best = g.best();
			for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
			{
				Swords.Sword sw = all.get(i);
				lines.add(new View.Line("Quality " + sw.quality + (sw.ticks > 0 ? " · " + StormPilot.clock(sw.ticks) : ""),
					Session.format(sw.xp) + " xp", sw == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
			}
			view.lines("Your swords", g.count() == 1 ? "1 kept" : g.count() + " kept", lines);
		}
	}

	private static java.awt.Color heatTone(FoundryCycle.Snapshot s, int[] band)
	{
		if (band == null)
		{
			return Theme.EMBER;
		}
		if (s.heat >= band[0] && s.heat <= band[1])
		{
			return Theme.GREEN;
		}
		return s.heat < band[0] ? Theme.TEAL : Theme.BLOOD;
	}

	/** The moulds for this commission, part by part, and the one worth buying next. */
	private View.PickCard mouldCard(FoundryCycle.Snapshot s, Owned owned, int level)
	{
		List<View.Pick> rows = new ArrayList<>();
		for (Mould.Part part : Mould.Part.values())
		{
			Mould m = s.pick.get(part);
			if (m == null)
			{
				continue;
			}
			int score = m.score(s.first, s.second);
			rows.add(new View.Pick(m.getTitle(), "+" + score,
				part.getTitle() + " · " + s.first.getTitle() + " " + m.score(s.first) + ", " + s.second.getTitle() + " " + m.score(s.second),
				true, true, score / 24.0, false));
		}
		String note;
		MouldPick.Buy buy = owned.sure ? MouldPick.next(level, owned.moulds, s.first, s.second) : null;
		if (buy != null)
		{
			rows.add(new View.Pick(buy.mould.getTitle(), buy.here > 0 ? "+" + buy.here : Session.format(buy.mould.getRep()),
				"Buy next · " + Session.format(buy.mould.getRep()) + " rep · " + String.format(Locale.ROOT, "+%.1f", buy.average) + " on average",
				true, false, 0, false));
			note = "Of the moulds at your level, " + buy.mould.getTitle() + " adds most to your average mould score for its reputation.";
		}
		else if (!owned.sure)
		{
			note = "Open the mould jig's tabs once so the Pilot can see every mould you have bought.";
		}
		else
		{
			note = "The best you own for this commission.";
		}
		return new View.PickCard("Moulds", s.pick.getScore() + " for " + FoundryCycle.commission(s), note, rows);
	}

	/** Brief for the panel away from the minigame. */
	View.Brief brief()
	{
		Swords g = swords();
		String status = null;
		Swords.Sword last = g.last();
		if (last != null)
		{
			status = "Last sword: quality " + last.quality + ", " + Session.format(last.xp) + " Smithing xp"
				+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.FOUNDRY, status,
			"Below the Giants' Plateau, after Sleeping Giants. Bring 28 bars' worth of metal and a bucket of water or ice gloves.");
		Swords.Sword best = g.best();
		double repHour = g.repPerHour();
		if (best != null && repHour >= 0)
		{
			b.best("Best sword quality " + best.quality + "; " + Session.format(Math.round(repHour)) + " reputation and "
				+ Session.format(Math.round(g.xpPerHour())) + " xp an hour over " + g.count() + (g.count() == 1 ? " sword." : " swords."));
		}
		return b;
	}
}
