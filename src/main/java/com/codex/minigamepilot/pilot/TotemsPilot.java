package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Figures;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.TotemCycle;
import com.codex.minigamepilot.core.TotemCycle.Site;
import com.codex.minigamepilot.core.TotemLog;
import com.codex.minigamepilot.core.Totems;
import com.codex.minigamepilot.core.Totems.Animal;
import com.codex.minigamepilot.core.Totems.Wood;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Flies Vale Totems: the next totem site and the logs to bring, the build, the three spirits each
 * totem wants (ringed in the game's carving choices), the four decorations of its wood, the
 * offerings to claim, and the ent trails near you; with your research points towards a reward and
 * every totem kept per account. It only reads the game (varbits, a varp, what you carry, the
 * objects around you and the carving choices on screen) and draws.
 *
 * <p>Ids are RuneLite's gameval names: the eight {@code ENT_TOTEMS_SITE_n_BASE} and
 * {@code _OFFERINGS} objects, the {@code ENT_TOTEMS_TRAIL_PART_0} and {@code _1} trail objects, the
 * eighteen {@code ENT_TOTEMS_SITE_n_*} varbits of each site, the {@code ENT_TOTEMS_RESEARCH_POINTS}
 * varp and the {@code ENT_TOTEMS_ANIMAL_ICON_*} items. What the site varbits' values mean (the
 * base 0 for no totem, a tier's spirit plus 9 once carved, the three wanted spirits 1 to 5, the
 * decorations 0 to 4 and the offerings waiting) is as the Totem Fletching plugin by TheRealGuru
 * (BSD-2) reads them: a cross-check, not a source. The totem's wood is read from the object the
 * site shows ({@code ENT_TOTEMS_*_OAK} and so on) before the base varbit's number.
 */
public final class TotemsPilot
{
	static final int[] BASES = {ObjectID.ENT_TOTEMS_SITE_1_BASE, ObjectID.ENT_TOTEMS_SITE_2_BASE, ObjectID.ENT_TOTEMS_SITE_3_BASE,
		ObjectID.ENT_TOTEMS_SITE_4_BASE, ObjectID.ENT_TOTEMS_SITE_5_BASE, ObjectID.ENT_TOTEMS_SITE_6_BASE,
		ObjectID.ENT_TOTEMS_SITE_7_BASE, ObjectID.ENT_TOTEMS_SITE_8_BASE};
	static final int[] OFFERINGS = {ObjectID.ENT_TOTEMS_SITE_1_OFFERINGS, ObjectID.ENT_TOTEMS_SITE_2_OFFERINGS,
		ObjectID.ENT_TOTEMS_SITE_3_OFFERINGS, ObjectID.ENT_TOTEMS_SITE_4_OFFERINGS, ObjectID.ENT_TOTEMS_SITE_5_OFFERINGS,
		ObjectID.ENT_TOTEMS_SITE_6_OFFERINGS, ObjectID.ENT_TOTEMS_SITE_7_OFFERINGS, ObjectID.ENT_TOTEMS_SITE_8_OFFERINGS};
	static final int[] TRAILS = {ObjectID.ENT_TOTEMS_TRAIL_PART_0, ObjectID.ENT_TOTEMS_TRAIL_PART_1};
	/** The first of each site's eighteen varbits; the rest follow in the same order for every site. */
	static final int[] SITE_VARBITS = {VarbitID.ENT_TOTEMS_SITE_1_BASE, VarbitID.ENT_TOTEMS_SITE_2_BASE,
		VarbitID.ENT_TOTEMS_SITE_3_BASE, VarbitID.ENT_TOTEMS_SITE_4_BASE, VarbitID.ENT_TOTEMS_SITE_5_BASE,
		VarbitID.ENT_TOTEMS_SITE_6_BASE, VarbitID.ENT_TOTEMS_SITE_7_BASE, VarbitID.ENT_TOTEMS_SITE_8_BASE};
	/** Offsets from a site's first varbit, as {@code ENT_TOTEMS_SITE_1_*} lies from {@code ENT_TOTEMS_SITE_1_BASE}. */
	static final int BASE = 0;
	static final int BASE_CARVED = VarbitID.ENT_TOTEMS_SITE_1_BASE_CARVED - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int LOW = VarbitID.ENT_TOTEMS_SITE_1_LOW - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int MID = VarbitID.ENT_TOTEMS_SITE_1_MID - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int TOP = VarbitID.ENT_TOTEMS_SITE_1_TOP - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int DECORATIONS = VarbitID.ENT_TOTEMS_SITE_1_DECORATIONS - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int ANIMAL_1 = VarbitID.ENT_TOTEMS_SITE_1_ANIMAL_1 - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int POINTS = VarbitID.ENT_TOTEMS_SITE_1_POINTS - VarbitID.ENT_TOTEMS_SITE_1_BASE;
	static final int TRAIL_BUFF = VarbitID.ENT_TOTEMS_TRAIL_XP_BUFF_ACTIONS;
	static final int RESEARCH = VarPlayerID.ENT_TOTEMS_RESEARCH_POINTS;
	static final int VALE_OFFERINGS = ItemID.ENT_TOTEMS_LOOT;
	static final int[] KNIVES = {ItemID.KNIFE, ItemID.FLETCHING_KNIFE};
	/** The carving choices: the make-how-many box's first five options. */
	static final int[] CHOICES = {InterfaceID.Skillmulti.A, InterfaceID.Skillmulti.B, InterfaceID.Skillmulti.C,
		InterfaceID.Skillmulti.D, InterfaceID.Skillmulti.E};
	/** Within this many tiles of a site's totem you are at it. */
	static final int AT_SITE = 10;
	/** Ent trails within this many tiles are marked. */
	static final int TRAIL_REACH = 15;
	static final String LOG_KEY = "totems.kept";
	static final String SITES_KEY = "totems.sites";
	static final String LOOP_KEY = "totems.loop";
	/** The trail bonus lasts this many build and carving actions: "the next 4 build/carving actions". */
	static final int TRAIL_ACTIONS = 4;

	private final Session session = new Session(Skill.FLETCHING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private Wood choice;
	private boolean claimAsYouPass = true;
	private Totems.Reward goal;
	private TotemLog log;

	/** Where each site stands, learned from the scene; null where not seen yet. */
	private WorldPoint[] places;
	/** The site you went to after each one last time (index), -1 where not known. */
	private int[] loop;
	/** The site you last did something at (index), or -1. */
	private int lastWorked = -1;

	/** Each site as last read; -1 before it has been. */
	private final int[] lastDecor = new int[Totems.SITES];
	private final int[] lastOffer = new int[Totems.SITES];
	private final int[] lastCarved = new int[Totems.SITES];
	private final boolean[] lastBuilt = new boolean[Totems.SITES];
	private boolean baseRead;
	/** When the last totem was finished, or when you arrived; -1 when away. */
	private int sinceTick = -1;
	private int sinceXp = -1;
	/** Offerings the ents have left at your sites since the last totem was finished. */
	private int sinceOfferings;
	private int finished;
	private int sessionOfferings;

	Object lastState;

	TotemsPilot()
	{
		java.util.Arrays.fill(lastDecor, -1);
		java.util.Arrays.fill(lastOffer, -1);
		java.util.Arrays.fill(lastCarved, -1);
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void settings(Wood choice, boolean claimAsYouPass, Totems.Reward goal)
	{
		this.choice = choice;
		this.claimAsYouPass = claimAsYouPass;
		this.goal = goal;
	}

	static boolean isWatched(int id)
	{
		return StormPilot.contains(BASES, id) || StormPilot.contains(OFFERINGS, id) || StormPilot.contains(TRAILS, id);
	}

	/** In Auburn Valley: a totem site or its offerings is in the loaded scene. */
	boolean active(Client client, SceneIndex scene)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		for (int i = 0; i < Totems.SITES; i++)
		{
			if (!scene.get(BASES[i]).isEmpty() || !scene.get(OFFERINGS[i]).isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	void left()
	{
		sinceTick = -1;
		sinceXp = -1;
		sinceOfferings = 0;
		baseRead = false;
		java.util.Arrays.fill(lastDecor, -1);
		java.util.Arrays.fill(lastOffer, -1);
		java.util.Arrays.fill(lastCarved, -1);
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		places = null;
		loop = null;
		lastWorked = -1;
		finished = 0;
		sessionOfferings = 0;
		left();
	}

	/** A running count for the pace coach: totems finished. */
	int units()
	{
		return finished;
	}

	private TotemLog log()
	{
		if (log == null)
		{
			log = TotemLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private WorldPoint[] places()
	{
		if (places == null)
		{
			places = decodePlaces(load == null ? null : load.apply(SITES_KEY));
		}
		return places;
	}

	private int[] loop()
	{
		if (loop == null)
		{
			loop = decodeLoop(load == null ? null : load.apply(LOOP_KEY));
		}
		return loop;
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		learnPlaces(scene);

		TotemCycle.Snapshot s = new TotemCycle.Snapshot();
		lastState = s;
		s.level = client.getBoostedSkillLevel(Skill.FLETCHING);
		s.choice = choice;
		s.claimAsYouPass = claimAsYouPass;
		for (Wood w : Wood.values())
		{
			s.logs[w.ordinal()] = pack.count(w.log());
			s.decorations[w.ordinal()] = pack.count(w.decorations());
		}
		s.knife = pack.carries(KNIVES) || pack.wears(KNIVES);
		WorldPoint[] where = places();
		int nearest = -1;
		for (int i = 0; i < Totems.SITES; i++)
		{
			read(client, scene, s.sites[i], i);
			if (where[i] != null && where[i].getPlane() == at.getPlane())
			{
				s.sites[i].distance = where[i].distanceTo2D(at);
				if (s.sites[i].distance <= AT_SITE && (nearest < 0 || s.sites[i].distance < s.sites[nearest].distance))
				{
					nearest = i;
				}
			}
		}
		s.here = nearest;
		settle(client, s, now);
		int usual = s.here >= 0 ? loop()[s.here] : lastWorked >= 0 ? loop()[lastWorked] : -1;
		s.usualNext = usual;

		TotemCycle.Decision d = TotemCycle.next(s);
		Step step = d.step;
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = route(scene, ground, step, d, at, where);
		int stage = TotemCycle.stage(step);
		View view = new View(true, View.Activity.TOTEMS, step, place(s), TotemCycle.LOOP, Math.max(stage, 0));
		Flight out = new Flight(View.Activity.TOTEMS, step, view);
		out.object = flight.object;
		out.route = flight.route;
		out.destination = flight.destination;
		carvings(client, s, out);
		trails(client, scene, at, out);
		dress(view, s, d, client, pack, now);
		return out;
	}

	/** One site from its varbits and the object it shows. */
	private static void read(Client client, SceneIndex scene, Site site, int i)
	{
		int v = SITE_VARBITS[i];
		int base = client.getVarbitValue(v + BASE);
		site.built = base != 0;
		site.carvedFlag = client.getVarbitValue(v + BASE_CARVED) == 1;
		site.carved[0] = Animal.ofCarved(client.getVarbitValue(v + LOW));
		site.carved[1] = Animal.ofCarved(client.getVarbitValue(v + MID));
		site.carved[2] = Animal.ofCarved(client.getVarbitValue(v + TOP));
		site.decorations = Math.max(0, Math.min(Totems.DECORATIONS, client.getVarbitValue(v + DECORATIONS)));
		for (int k = 0; k < Totems.CARVINGS; k++)
		{
			site.wanted[k] = Animal.ofVarbit(client.getVarbitValue(v + ANIMAL_1 + k));
		}
		site.offerings = Math.max(0, client.getVarbitValue(v + POINTS));
		if (site.built)
		{
			Wood shown = woodShown(client, scene.get(BASES[i]));
			site.wood = shown != null ? shown : Wood.ofVarbit(base);
		}
	}

	/** The wood a site's totem object shows, from the name of the object it stands as, or null. */
	static Wood woodShown(Client client, Collection<TileObject> objects)
	{
		for (TileObject o : objects)
		{
			ObjectComposition def = client.getObjectDefinition(o.getId());
			ObjectComposition shown = def == null ? null : def.getImpostorIds() == null ? def : def.getImpostor();
			Wood w = shown == null ? null : woodOf(shown.getId());
			if (w != null)
			{
				return w;
			}
		}
		return null;
	}

	/** The wood of one of the totem objects ({@code ENT_TOTEMS_BASE_OAK}, {@code _CARVED_BASE_OAK} and so on), or null. */
	static Wood woodOf(int id)
	{
		int[][] byWood = {
			{ObjectID.ENT_TOTEMS_BASE_OAK, ObjectID.ENT_TOTEMS_CARVED_BASE_OAK, ObjectID.ENT_TOTEMS_LOW_OAK, ObjectID.ENT_TOTEMS_MID_OAK, ObjectID.ENT_TOTEMS_TOP_OAK},
			{ObjectID.ENT_TOTEMS_BASE_WILLOW, ObjectID.ENT_TOTEMS_CARVED_BASE_WILLOW, ObjectID.ENT_TOTEMS_LOW_WILLOW, ObjectID.ENT_TOTEMS_MID_WILLOW, ObjectID.ENT_TOTEMS_TOP_WILLOW},
			{ObjectID.ENT_TOTEMS_BASE_MAPLE, ObjectID.ENT_TOTEMS_CARVED_BASE_MAPLE, ObjectID.ENT_TOTEMS_LOW_MAPLE, ObjectID.ENT_TOTEMS_MID_MAPLE, ObjectID.ENT_TOTEMS_TOP_MAPLE},
			{ObjectID.ENT_TOTEMS_BASE_YEW, ObjectID.ENT_TOTEMS_CARVED_BASE_YEW, ObjectID.ENT_TOTEMS_LOW_YEW, ObjectID.ENT_TOTEMS_MID_YEW, ObjectID.ENT_TOTEMS_TOP_YEW},
			{ObjectID.ENT_TOTEMS_BASE_MAGIC, ObjectID.ENT_TOTEMS_CARVED_BASE_MAGIC, ObjectID.ENT_TOTEMS_LOW_MAGIC, ObjectID.ENT_TOTEMS_MID_MAGIC, ObjectID.ENT_TOTEMS_TOP_MAGIC},
			{ObjectID.ENT_TOTEMS_BASE_REDWOOD, ObjectID.ENT_TOTEMS_CARVED_BASE_REDWOOD, ObjectID.ENT_TOTEMS_LOW_REDWOOD, ObjectID.ENT_TOTEMS_MID_REDWOOD, ObjectID.ENT_TOTEMS_TOP_REDWOOD}};
		for (Wood w : Wood.values())
		{
			if (StormPilot.contains(byWood[w.ordinal()], id))
			{
				return w;
			}
		}
		return null;
	}

	/** Notes where each site's totem stands as it comes into the scene, kept per account. */
	private void learnPlaces(SceneIndex scene)
	{
		WorldPoint[] where = places();
		boolean changed = false;
		for (int i = 0; i < Totems.SITES; i++)
		{
			for (TileObject o : scene.get(BASES[i]))
			{
				WorldPoint p = o.getWorldLocation();
				if (p != null && !p.equals(where[i]))
				{
					where[i] = p;
					changed = true;
				}
				break;
			}
		}
		if (changed && save != null)
		{
			save.accept(SITES_KEY, encodePlaces(where));
		}
	}

	/**
	 * Every tick at the valley: a site's fourth decoration finishes a totem, which is kept with the
	 * experience, offerings and time since the last; offerings rising are the ents' visits, and
	 * falling are a claim. Whatever you change at a site teaches the Pilot your loop order.
	 */
	private void settle(Client client, TotemCycle.Snapshot s, long now)
	{
		if (client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			// Skills and varbits have not arrived: nothing read now is a change.
			return;
		}
		int tick = client.getTickCount();
		int xp = client.getSkillExperience(Skill.FLETCHING);
		if (sinceTick < 0)
		{
			sinceTick = tick;
			sinceXp = xp;
		}
		for (int i = 0; i < Totems.SITES; i++)
		{
			Site x = s.sites[i];
			int carved = x.carvings();
			if (baseRead)
			{
				boolean worked = false;
				if (x.offerings > lastOffer[i])
				{
					sinceOfferings += x.offerings - lastOffer[i];
					sessionOfferings += x.offerings - lastOffer[i];
				}
				else if (x.offerings < lastOffer[i])
				{
					// Claimed.
					worked = true;
				}
				worked |= x.built && !lastBuilt[i] || carved > lastCarved[i] || x.decorations > lastDecor[i];
				if (lastDecor[i] < Totems.DECORATIONS && x.decorations >= Totems.DECORATIONS && x.built)
				{
					finish(x, tick, xp, now);
				}
				if (worked)
				{
					worked(i);
				}
			}
			lastDecor[i] = x.decorations;
			lastOffer[i] = x.offerings;
			lastCarved[i] = carved;
			lastBuilt[i] = x.built;
		}
		baseRead = true;
	}

	private void finish(Site x, int tick, int xp, long now)
	{
		int ticks = sinceTick >= 0 ? tick - sinceTick : 0;
		int gained = sinceXp >= 0 && xp > sinceXp ? xp - sinceXp : 0;
		log().add(new TotemLog.Totem(now, ticks, x.wood, gained, sinceOfferings));
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		finished++;
		sinceTick = tick;
		sinceXp = xp;
		sinceOfferings = 0;
	}

	/** You did something at a site: after the last one you worked at, this is where you went. */
	private void worked(int i)
	{
		int[] l = loop();
		if (lastWorked >= 0 && lastWorked != i && l[lastWorked] != i)
		{
			l[lastWorked] = i;
			if (save != null)
			{
				save.accept(LOOP_KEY, encodeLoop(l));
			}
		}
		lastWorked = i;
	}

	private static Flight route(SceneIndex scene, Ground ground, Step step, TotemCycle.Decision d, WorldPoint at, WorldPoint[] where)
	{
		Flight flight = new Flight(View.Activity.TOTEMS, step, null);
		Collection<? extends TileObject> things;
		switch (step.getTarget())
		{
			case TOTEM:
				things = d.site < 0 ? Collections.emptySet() : scene.get(BASES[d.site]);
				break;
			case OFFERINGS:
				things = d.site < 0 ? Collections.emptySet() : scene.get(OFFERINGS[d.site]);
				break;
			case BANK:
				things = scene.banks();
				break;
			default:
				return flight;
		}
		if (things.isEmpty())
		{
			if (step.getTarget() != Step.Target.BANK && d.site >= 0 && where[d.site] != null)
			{
				// Beyond the loaded scene: head for where the site was last seen.
				flight.destination = where[d.site];
			}
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

	/**
	 * The carving choices, when they are on screen: each one whose icon is a spirit the totem
	 * here still wants is ringed. Without icons to read, the choices are taken in the game's
	 * order of the spirits, as the Totem Fletching plugin reads them, but only when the box asks
	 * about carving.
	 */
	private static void carvings(Client client, TotemCycle.Snapshot s, Flight out)
	{
		if (s.here < 0)
		{
			return;
		}
		List<Animal> want = s.sites[s.here].toCarve();
		if (want.isEmpty() || s.sites[s.here].state() != TotemCycle.State.CARVE)
		{
			return;
		}
		Animal[] shown = new Animal[CHOICES.length];
		boolean any = false;
		boolean open = false;
		for (int k = 0; k < CHOICES.length; k++)
		{
			Widget w = client.getWidget(CHOICES[k]);
			if (w == null || w.isHidden())
			{
				continue;
			}
			open = true;
			shown[k] = iconIn(w);
			any |= shown[k] != null;
		}
		if (!open)
		{
			return;
		}
		if (!any)
		{
			Widget title = client.getWidget(InterfaceID.Skillmulti.TITLE);
			String text = title == null || title.getText() == null ? "" : title.getText().toLowerCase(Locale.ROOT);
			if (!text.contains("carve"))
			{
				return;
			}
			for (int k = 0; k < CHOICES.length; k++)
			{
				shown[k] = client.getWidget(CHOICES[k]) == null ? null : Animal.values()[k];
			}
		}
		for (int k = 0; k < CHOICES.length; k++)
		{
			if (shown[k] != null && want.contains(shown[k]))
			{
				out.widgets.add(CHOICES[k]);
				out.marks.add(shown[k].getTitle());
			}
		}
	}

	/** The spirit an option's icon shows, on the option or one of its parts, or null. */
	static Animal iconIn(Widget w)
	{
		Animal a = Animal.ofIcon(w.getItemId());
		if (a != null)
		{
			return a;
		}
		for (Widget[] kids : new Widget[][]{w.getDynamicChildren(), w.getChildren()})
		{
			if (kids == null)
			{
				continue;
			}
			for (Widget k : kids)
			{
				a = k == null ? null : Animal.ofIcon(k.getItemId());
				if (a != null)
				{
					return a;
				}
			}
		}
		return null;
	}

	/** Ent trails near you, marked while no trail bonus is under way. */
	private static void trails(Client client, SceneIndex scene, WorldPoint at, Flight out)
	{
		if (client.getVarbitValue(TRAIL_BUFF) > 0)
		{
			return;
		}
		for (TileObject o : scene.get(TRAILS))
		{
			WorldPoint p = o.getWorldLocation();
			if (p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= TRAIL_REACH)
			{
				out.others.add(o);
			}
		}
	}

	/** "Site 3 · maple totem", or the valley when you are at no site. */
	private static String place(TotemCycle.Snapshot s)
	{
		if (s.here < 0)
		{
			return "Auburn Valley";
		}
		Site h = s.sites[s.here];
		return "Site " + h.number + " · " + (!h.built ? "empty" : h.wood == null ? "a totem" : h.wood.getTitle().toLowerCase(Locale.ROOT) + " totem");
	}

	/** The site here, the eight sites, your session, research points towards a reward, and your totems. */
	private void dress(View view, TotemCycle.Snapshot s, TotemCycle.Decision d, Client client, Pack pack, long now)
	{
		if (s.here >= 0)
		{
			Site h = s.sites[s.here];
			view.gauge(new View.Gauge("Offerings", h.offerings, 0, Totems.OFFERINGS_LIMIT,
				h.offerings >= Totems.OFFERINGS_LIMIT ? "Full: claim before building here"
					: Figures.count(Totems.points(h.offerings)) + " research points' worth", Theme.GOLD));
			if (h.built)
			{
				view.gauge(new View.Gauge("Decorations", h.decorations, 0, Totems.DECORATIONS,
					h.state() == TotemCycle.State.CARVE ? h.carvings() + " of 3 spirits carved"
						: h.decorations >= Totems.DECORATIONS ? "Finished: the ents are visiting"
						: (Totems.DECORATIONS - h.decorations) + " more of " + (h.wood == null ? "its wood" : h.wood.getTitle().toLowerCase(Locale.ROOT)),
					Theme.GREEN));
			}
		}
		int buff = client.getVarbitValue(TRAIL_BUFF);
		if (buff > 0)
		{
			view.gauge(new View.Gauge("Trail bonus", Math.min(buff, TRAIL_ACTIONS), 0, TRAIL_ACTIONS,
				"+" + Totems.TRAIL_BONUS + "% xp on builds and carvings", Theme.TEAL));
		}

		List<View.Line> lines = new ArrayList<>();
		for (int i = 0; i < Totems.SITES; i++)
		{
			Site x = s.sites[i];
			View.Line.Mark mark;
			if (i == d.site)
			{
				mark = View.Line.Mark.NEXT;
			}
			else if (x.state() == TotemCycle.State.DONE)
			{
				mark = View.Line.Mark.DONE;
			}
			else if (x.distance < 0 && !x.built)
			{
				mark = View.Line.Mark.IDLE;
			}
			else
			{
				mark = View.Line.Mark.TODO;
			}
			lines.add(new View.Line(siteText(x), siteTrailing(x), mark));
		}
		int[] need = TotemCycle.needs(s);
		Wood w = TotemCycle.buildWood(s);
		if (w == null)
		{
			w = TotemCycle.bankWood(s);
		}
		String bring = need[0] == 0 && need[1] == 0 ? "Every totem stands"
			: "Loop: " + need[0] + (need[0] == 1 ? " log" : " logs") + ", " + need[1] + " decorations";
		view.lines("The sites", bring, lines);

		int points = Math.max(0, client.getVarpValue(RESEARCH));
		long carried = pack.count(VALE_OFFERINGS);
		long waiting = 0;
		for (Site x : s.sites)
		{
			waiting += x.offerings;
		}
		view.stat(new View.Stat(Session.format(finished), "Totems", session.perHour(finished, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Fletching xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(Session.format(waiting + carried), "Offerings",
			waiting > 0 && carried > 0 ? Session.format(waiting) + " at sites" : waiting > 0 ? "At your sites" : carried > 0 ? "Carried" : "",
			Theme.GOLD_BRIGHT));
		long extra = Totems.points(waiting + carried);
		view.stat(new View.Stat(Session.format(points), "Research points", extra > 0 ? "+" + Session.format(extra) + " in offerings" : "",
			Theme.TEAL));

		if (goal != null)
		{
			view.goal(goalCard(points, waiting + carried));
		}
		TotemLog g = log();
		if (g.count() > 0)
		{
			view.supply(historyCard(g));
		}
	}

	private static String siteText(Site x)
	{
		return "Site " + x.number + (x.built && x.wood != null ? " · " + x.wood.getTitle().toLowerCase(Locale.ROOT) : "");
	}

	private static String siteTrailing(Site x)
	{
		String off = x.offerings > 0 ? " · " + MixologyPilot.compact(x.offerings) : "";
		switch (x.state())
		{
			case EMPTY:
				return (x.offerings >= Totems.OFFERINGS_LIMIT ? "Full" : "Empty") + off;
			case CARVE:
				return x.carvings() + "/3 carved" + off;
			case DECORATE:
				return x.decorations + "/4 decorated" + off;
			default:
				return "Finished" + off;
		}
	}

	/** Your research points against the reward saved for, offerings counted at a hundred a point. */
	private View.GoalCard goalCard(int points, long offerings)
	{
		long have = Math.min(goal.points(), (long) points + Totems.points(offerings));
		boolean done = points >= goal.points();
		String note;
		if (done)
		{
			note = "You can buy it from Ranulph now.";
		}
		else if (have >= goal.points())
		{
			note = "Rummage your offerings for the last of the points";
		}
		else
		{
			double hours = log().hoursTo(goal, points, offerings);
			note = hours > 0 ? "About " + MixologyPilot.clockHours(hours) + " at your pace" : "Its time shows after a totem or two";
		}
		List<View.Line> pieces = new ArrayList<>();
		pieces.add(new View.Line("Research points", Session.format(points), points >= goal.points() ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		pieces.add(new View.Line("In offerings", "+" + Session.format(Totems.points(offerings)), View.Line.Mark.IDLE));
		return new View.GoalCard(View.Activity.TOTEMS, goal.getTitle(), have, goal.points(), "points", note, pieces, done);
	}

	/** Your last totems, the quickest, and experience and offerings an hour across them. */
	private static View.Supply historyCard(TotemLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<TotemLog.Totem> all = g.all();
		TotemLog.Totem best = g.quickest();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			TotemLog.Totem t = all.get(i);
			lines.add(new View.Line((t.wood == null ? "Totem" : t.wood.getTitle() + " totem") + (t.ticks > 0 ? " · " + StormPilot.clock(t.ticks) : ""),
				Session.format(t.xp) + " xp", t == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Quickest totem: " + StormPilot.clock(best.ticks);
		double xpHour = g.xpPerHour();
		double offHour = g.offeringsPerHour();
		String detail = xpHour < 0 ? null : Session.format(Math.round(xpHour)) + " Fletching xp and " + Session.format(Math.round(offHour))
			+ " offerings an hour over your timed totems";
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your totems");
	}

	/** Brief for the panel away from the valley. */
	View.Brief brief()
	{
		TotemLog g = log();
		String status = null;
		TotemLog.Totem last = g.last();
		if (last != null)
		{
			status = "Last totem: " + (last.wood == null ? "a totem" : last.wood.getTitle().toLowerCase(Locale.ROOT) + " totem") + ", "
				+ Session.format(last.xp) + " Fletching xp" + (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.TOTEMS, status,
			"In Auburn Valley, Varlamore, after the Vale Totems miniquest, from 20 Fletching. Bring logs and a knife, or fletched items of one wood.");
		double hour = g.xpPerHour();
		if (hour >= 0)
		{
			b.best(Session.format(Math.round(hour)) + " xp and " + Session.format(Math.round(g.pointsPerHour())) + " research points an hour over "
				+ g.count() + (g.count() == 1 ? " totem." : " totems."));
		}
		return b;
	}

	/** "x,y,plane;..." for the eight sites, "-" where not seen. */
	static String encodePlaces(WorldPoint[] where)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < Totems.SITES; i++)
		{
			if (i > 0)
			{
				b.append(';');
			}
			WorldPoint p = where[i];
			b.append(p == null ? "-" : p.getX() + "," + p.getY() + "," + p.getPlane());
		}
		return b.toString();
	}

	static WorldPoint[] decodePlaces(String text)
	{
		WorldPoint[] out = new WorldPoint[Totems.SITES];
		if (text == null)
		{
			return out;
		}
		String[] parts = text.split(";");
		for (int i = 0; i < Math.min(parts.length, Totems.SITES); i++)
		{
			String[] f = parts[i].split(",");
			if (f.length != 3)
			{
				continue;
			}
			try
			{
				int x = Integer.parseInt(f[0].trim());
				int y = Integer.parseInt(f[1].trim());
				int p = Integer.parseInt(f[2].trim());
				if (x > 0 && y > 0 && p >= 0 && p <= 3)
				{
					out[i] = new WorldPoint(x, y, p);
				}
			}
			catch (NumberFormatException e)
			{
				// Not seen.
			}
		}
		return out;
	}

	/** "1,2,-1,..." the site after each one, by number, -1 where not known. */
	static String encodeLoop(int[] loop)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < Totems.SITES; i++)
		{
			if (i > 0)
			{
				b.append(',');
			}
			b.append(loop[i] < 0 ? -1 : loop[i] + 1);
		}
		return b.toString();
	}

	static int[] decodeLoop(String text)
	{
		int[] out = new int[Totems.SITES];
		java.util.Arrays.fill(out, -1);
		if (text == null)
		{
			return out;
		}
		String[] parts = text.split(",");
		for (int i = 0; i < Math.min(parts.length, Totems.SITES); i++)
		{
			try
			{
				int n = Integer.parseInt(parts[i].trim());
				out[i] = n >= 1 && n <= Totems.SITES && n - 1 != i ? n - 1 : -1;
			}
			catch (NumberFormatException e)
			{
				// Not known.
			}
		}
		return out;
	}
}
