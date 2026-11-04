package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Totems.Animal;
import com.codex.minigamepilot.core.Totems.Wood;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Works out the one next step at Vale Totems from what the game shows: build a totem at an empty
 * site with the best logs you carry, carve the three spirits the site wants, decorate it with four
 * fletched items of its wood (fletching them first when you carry logs and a knife), claim the
 * offerings the ents left, then on to the next site, your own loop order first.
 *
 * <p>Facts are from the OSRS wiki (see {@link Totems}). It only reads; every click is yours.
 */
public final class TotemCycle
{
	/** The stages of one totem, for the loop strip. */
	public static final String[] LOOP = {"Build", "Carve", "Decorate", "Claim"};

	private TotemCycle()
	{
	}

	/** Where a site's totem stands. */
	public enum State
	{
		/** No totem: one can be built. */
		EMPTY,
		/** Built, with spirits still to carve. */
		CARVE,
		/** Carved, with decorations still to add. */
		DECORATE,
		/** Carved and decorated: the ents are visiting it. */
		DONE
	}

	/** One totem site as the game shows it. */
	public static final class Site
	{
		/** 1 to 8, as the game numbers them. */
		public final int number;
		public boolean built;
		/** The totem's wood, or null when not built or not known. */
		public Wood wood;
		/** The three spirits the site wants, null where not known. */
		public final Animal[] wanted = new Animal[Totems.CARVINGS];
		/** The spirit carved into each of the totem's three tiers, null where none is yet. */
		public final Animal[] carved = new Animal[Totems.CARVINGS];
		/** The game's own word that the carving is finished. */
		public boolean carvedFlag;
		public int decorations;
		/** Offerings waiting to be claimed at the site. */
		public int offerings;
		/** Tiles from you, or -1 when the site's place is not known. */
		public int distance = -1;

		public Site(int number)
		{
			this.number = number;
		}

		public int carvings()
		{
			int n = 0;
			for (Animal a : carved)
			{
				n += a == null ? 0 : 1;
			}
			return n;
		}

		public boolean carvingDone()
		{
			return carvedFlag || carvings() >= Totems.CARVINGS;
		}

		public State state()
		{
			if (!built)
			{
				return State.EMPTY;
			}
			if (!carvingDone())
			{
				return State.CARVE;
			}
			return decorations >= Totems.DECORATIONS ? State.DONE : State.DECORATE;
		}

		/** The spirits the site wants that are not carved yet, in the game's order. */
		public List<Animal> toCarve()
		{
			List<Animal> out = new ArrayList<>();
			for (Animal a : wanted)
			{
				if (a != null && !isCarved(a) && !out.contains(a))
				{
					out.add(a);
				}
			}
			return out;
		}

		public boolean isCarved(Animal a)
		{
			for (Animal c : carved)
			{
				if (c == a)
				{
					return true;
				}
			}
			return false;
		}

		/** Whether all three spirits it wants are known. */
		public boolean wantsKnown()
		{
			for (Animal a : wanted)
			{
				if (a == null)
				{
					return false;
				}
			}
			return true;
		}
	}

	/** Everything a decision needs, read from the game. */
	public static final class Snapshot
	{
		public final Site[] sites = new Site[Totems.SITES];
		/** The site you stand at (index 0 to 7), or -1. */
		public int here = -1;
		/** Your Fletching level, boosts included: "temporary Fletching skill boosts do work". */
		public int level;
		/** The wood you build with, or null for the best your level and your logs allow. */
		public Wood choice;
		/** Logs and decorations carried, by {@link Wood#ordinal()}. */
		public final int[] logs = new int[Wood.values().length];
		public final int[] decorations = new int[Wood.values().length];
		public boolean knife;
		/** Claim each site's offerings as you pass it, rather than only when they block a build. */
		public boolean claimAsYouPass = true;
		/** The site you usually go to after this one (index), or -1 before your loop is known. */
		public int usualNext = -1;

		public Snapshot()
		{
			for (int i = 0; i < sites.length; i++)
			{
				sites[i] = new Site(i + 1);
			}
		}
	}

	/** What to do and where. */
	public static final class Decision
	{
		public final Step step;
		/** The site the step is about (index 0 to 7), or -1. */
		public final int site;
		/** The spirit to carve now, for the carving choices, or null. */
		public final Animal carve;
		/** The wood the step's items are of, or null. */
		public final Wood wood;

		Decision(Step step, int site, Animal carve, Wood wood)
		{
			this.step = step;
			this.site = site;
			this.carve = carve;
			this.wood = wood;
		}
	}

	/** The wood to build with: your choice if your level allows it, else the best you can use and carry. */
	public static Wood buildWood(Snapshot s)
	{
		if (s.choice != null)
		{
			return s.choice.level() <= s.level ? s.choice : null;
		}
		Wood best = null;
		for (Wood w : Wood.values())
		{
			if (w.level() <= s.level && s.logs[w.ordinal()] > 0)
			{
				best = w;
			}
		}
		return best;
	}

	/** The wood to bank for: your choice, or the best your level allows. */
	public static Wood bankWood(Snapshot s)
	{
		return s.choice != null && s.choice.level() <= s.level ? s.choice : Wood.best(s.level);
	}

	public static Decision next(Snapshot s)
	{
		Wood best = Wood.best(s.level);
		if (best == null)
		{
			return new Decision(Step.waiting("Vale Totems needs 20 Fletching",
				"An oak totem is the first, at 20 Fletching; temporary boosts count", Step.Target.NONE), -1, null, null);
		}
		if (s.choice != null && s.choice.level() > s.level)
		{
			return new Decision(Step.waiting(s.choice.getTitle() + " totems need " + s.choice.level() + " Fletching",
				"Your Fletching is " + s.level + ": pick a wood your level can build under Vale totem wood", Step.Target.NONE), -1, null, null);
		}
		if (s.here >= 0)
		{
			Decision d = atSite(s, s.sites[s.here], s.here);
			if (d != null)
			{
				return d;
			}
		}
		return onward(s);
	}

	/** What to do at the site you stand at, or null when it is done with for now. */
	private static Decision atSite(Snapshot s, Site h, int i)
	{
		switch (h.state())
		{
			case EMPTY:
			{
				if (h.offerings >= Totems.OFFERINGS_LIMIT)
				{
					return new Decision(Step.of("Claim", "Claim the offerings",
						limitHere() + ": no new totem can be built here until you take some", Step.Target.OFFERINGS), i, null, null);
				}
				Wood w = buildWood(s);
				if (w != null && s.logs[w.ordinal()] > 0)
				{
					return new Decision(Step.of("Build", "Build " + article(w) + " " + lower(w) + " totem",
						"Site " + h.number + " is empty; then carve the three spirits by it", Step.Target.TOTEM).withItems(w.log()), i, null, w);
				}
				if (claimHere(s, h))
				{
					return claim(h, i);
				}
				return null;
			}
			case CARVE:
			{
				List<Animal> left = h.toCarve();
				if (left.isEmpty())
				{
					return new Decision(Step.of("Carve", "Carve the totem",
						"Carve the three spirits that stand by this site", Step.Target.TOTEM), i, null, h.wood);
				}
				Animal a = left.get(0);
				String detail = "This site's spirits: " + names(h.wanted) + ". Each wrong carving costs "
					+ Totems.WRONG_CARVING_COST + "% of the decoration experience";
				if (left.size() == 1 && h.carvings() == Totems.CARVINGS - 1)
				{
					detail = "The last carving: for the most offerings, finish it once a visiting ent's animation ends";
				}
				return new Decision(Step.of("Carve", "Carve the " + a.getTitle().toLowerCase(Locale.ROOT), detail, Step.Target.TOTEM), i, a, h.wood);
			}
			case DECORATE:
				return decorate(s, h, i);
			default:
				return claimHere(s, h) ? claim(h, i) : null;
		}
	}

	private static String limitHere()
	{
		return exact(Totems.OFFERINGS_LIMIT) + " offerings here";
	}

	private static boolean claimHere(Snapshot s, Site h)
	{
		return h.offerings >= Totems.OFFERINGS_LIMIT || s.claimAsYouPass && h.offerings > 0;
	}

	private static Decision claim(Site h, int i)
	{
		return new Decision(Step.of("Claim", "Claim the offerings",
			exact(h.offerings) + " offerings here, " + exact(Totems.points(h.offerings)) + (Totems.points(h.offerings) == 1 ? " research point's worth" : " research points' worth"),
			Step.Target.OFFERINGS), i, null, null);
	}

	private static Decision decorate(Snapshot s, Site h, int i)
	{
		int left = Totems.DECORATIONS - h.decorations;
		String count = "(" + h.decorations + " of " + Totems.DECORATIONS + ")";
		Wood w = h.wood;
		if (w == null)
		{
			// The totem's wood is not known: any decorations you carry are worth a try.
			for (Wood x : Wood.values())
			{
				if (s.decorations[x.ordinal()] > 0)
				{
					w = x;
					break;
				}
			}
			if (w == null)
			{
				return new Decision(Step.of("Decorate", "Decorate the totem " + count,
					"Four fletched items of the totem's own wood", Step.Target.TOTEM), i, null, null);
			}
		}
		if (s.decorations[w.ordinal()] > 0)
		{
			int have = s.decorations[w.ordinal()];
			String detail = have >= left ? "Any fletched " + lower(w) + " item; " + left + " more " + (left == 1 ? "finishes" : "finish") + " it"
				: "You carry " + have + " of the " + left + " still needed" + (s.logs[w.ordinal()] > 0 && s.knife ? "; fletch your logs for the rest" : "");
			return new Decision(Step.of("Decorate", "Decorate the totem " + count, detail, Step.Target.TOTEM).withItems(w.decorations()), i, null, w);
		}
		if (s.logs[w.ordinal()] > 0 && s.knife)
		{
			return new Decision(Step.items("Fletch", "Fletch your " + lower(w) + " logs",
				"Any fletched " + lower(w) + " item decorates this totem; " + left + " still needed", w.log()), i, null, w);
		}
		return new Decision(Step.of("Bank", "Bank for " + lower(w) + " decorations",
			left + " fletched " + lower(w) + (left == 1 ? " item finishes" : " items finish") + " the totem at site " + h.number
				+ (s.logs[w.ordinal()] > 0 ? "; or bring a knife for your logs" : ""), Step.Target.BANK), i, null, w);
	}

	/** On to the next site with work, your usual next one first, else the nearest. */
	private static Decision onward(Snapshot s)
	{
		int go = pickNext(s);
		if (go < 0)
		{
			if (s.here >= 0 && s.sites[s.here].offerings > 0)
			{
				return claim(s.sites[s.here], s.here);
			}
			int known = 0;
			for (Site x : s.sites)
			{
				known += x.distance >= 0 ? 1 : 0;
			}
			return new Decision(Step.waiting(known == 0 ? "Head to a totem site" : "Wait for a totem to fall",
				known == 0 ? "The Pilot learns where each site is as you come near it"
					: "Every site you know of has a finished totem; each ent visit wears one down", Step.Target.TOTEM), -1, null, null);
		}
		Site t = s.sites[go];
		Wood w = buildWood(s);
		boolean hasLogs = w != null && s.logs[w.ordinal()] > 0;
		if (t.state() == State.EMPTY && !hasLogs && t.offerings < Totems.OFFERINGS_LIMIT)
		{
			Wood b = bankWood(s);
			return new Decision(Step.of("Bank", "Bank for " + lower(b) + " logs",
				"Each totem takes a log to build and four fletched " + lower(b) + " items to decorate", Step.Target.BANK), go, null, b);
		}
		return new Decision(Step.of("Go", "Go to site " + t.number, "Then " + there(s, t, w), Step.Target.TOTEM), go, null, t.wood);
	}

	/** What waits at a site, as a sentence's end. */
	private static String there(Snapshot s, Site t, Wood w)
	{
		switch (t.state())
		{
			case EMPTY:
				if (t.offerings >= Totems.OFFERINGS_LIMIT)
				{
					return "claim its offerings: " + limitHere();
				}
				return "build " + (w == null ? "a" : article(w) + " " + lower(w)) + " totem";
			case CARVE:
			{
				List<Animal> left = t.toCarve();
				return left.isEmpty() ? "carve it" : "carve " + names(left.toArray(new Animal[0]));
			}
			case DECORATE:
				return "decorate it (" + t.decorations + " of " + Totems.DECORATIONS + ")";
			default:
				return "claim its offerings";
		}
	}

	/** The next site to go to (index), or -1 with none to go to. */
	static int pickNext(Snapshot s)
	{
		if (s.usualNext >= 0 && s.usualNext != s.here && hasWork(s, s.sites[s.usualNext]))
		{
			return s.usualNext;
		}
		int best = -1;
		for (int i = 0; i < s.sites.length; i++)
		{
			Site x = s.sites[i];
			if (i == s.here || x.distance < 0 || !hasWork(s, x))
			{
				continue;
			}
			if (best < 0 || x.distance < s.sites[best].distance)
			{
				best = i;
			}
		}
		return best;
	}

	private static boolean hasWork(Snapshot s, Site x)
	{
		return x.state() != State.DONE || x.offerings >= Totems.OFFERINGS_LIMIT;
	}

	/** The stage a step belongs to, for the loop strip. */
	public static int stage(Step step)
	{
		if (step == null)
		{
			return -1;
		}
		switch (step.getVerb())
		{
			case "Carve":
				return 1;
			case "Decorate":
			case "Fletch":
				return 2;
			case "Claim":
			case "Wait":
				return 3;
			default:
				return 0;
		}
	}

	/** What the rest of a loop takes: logs to build the empty sites and decorations for every unfinished totem. */
	public static int[] needs(Snapshot s)
	{
		int logs = 0;
		int decorations = 0;
		for (Site x : s.sites)
		{
			switch (x.state())
			{
				case EMPTY:
					logs++;
					decorations += Totems.DECORATIONS;
					break;
				case CARVE:
					decorations += Totems.DECORATIONS;
					break;
				case DECORATE:
					decorations += Totems.DECORATIONS - x.decorations;
					break;
				default:
					break;
			}
		}
		return new int[]{logs, decorations};
	}

	/** "buffalo, eagle and scorpion"; unknown spirits are left out. */
	public static String names(Animal[] animals)
	{
		List<String> n = new ArrayList<>();
		for (Animal a : animals)
		{
			if (a != null)
			{
				n.add(a.getTitle().toLowerCase(Locale.ROOT));
			}
		}
		if (n.isEmpty())
		{
			return "not yet seen";
		}
		if (n.size() == 1)
		{
			return n.get(0);
		}
		return String.join(", ", n.subList(0, n.size() - 1)) + " and " + n.get(n.size() - 1);
	}

	/** "15,000": every digit, for a line that is read rather than glanced at. */
	static String exact(long n)
	{
		return String.format(Locale.ROOT, "%,d", n);
	}

	static String lower(Wood w)
	{
		return w.getTitle().toLowerCase(Locale.ROOT);
	}

	static String article(Wood w)
	{
		return w == Wood.OAK ? "an" : "a";
	}
}
