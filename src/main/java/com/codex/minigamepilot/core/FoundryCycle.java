package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * The one next step through a Giants' Foundry sword: a commission from Kovac, the best moulds you
 * own in the jig, 28 bars in the crucible, pour, pick up the preform, then each section with the
 * tool its stage asks for at the heat that tool needs, the lava pool or the waterfall in between,
 * and the sword handed back to Kovac.
 *
 * <p>Plain Java over a snapshot of the game, so every turn can be tested. Facts (OSRS wiki,
 * Giants' Foundry and Giants' Foundry/Strategies):
 * <ul>
 * <li>the trip hammer needs a hot sword and each use gives 2% progress and takes 2.5% heat; the
 * grindstone needs a medium heat, gives 1% and adds 1.5%; the polishing wheel needs it cold,
 * gives 1% and takes 1.7%; the wrong tool or heat costs 10 quality;</li>
 * <li>the lava pool heats (Heat-preform, or Dunk-preform faster) and the waterfall cools
 * (Cool-preform, or Quench-preform faster); "use them to get close, then switch to normal
 * heat/cool to fine-tune";</li>
 * <li>a sweet spot shows for about 10 ticks after a machine works: clicking it then gives a
 * large progress gain and a point of quality back;</li>
 * <li>picking up the preform needs a bucket of water carried, or ice gloves or smiths gloves (i)
 * worn; preform storage keeps it with its progress and heat;</li>
 * <li>a sword too damaged to work on is still handed to Kovac.</li>
 * </ul>
 *
 * <p>Heat and progress are read from the game on a 0-1,000 scale and the HUD's three heat bands
 * sit at a sixth, a half and five sixths of the bar, as wide as the HUD draws them: both as the
 * Easy Giants' Foundry plugin reads them (a cross-check, not a source). How far off a band is
 * worth the fast option is the Pilot's own rule ({@link #FAST_GAP}), not a game fact.
 */
public final class FoundryCycle
{
	/** Heat and progress are out of this. */
	public static final int SCALE = 1000;
	/** Further than this from the band, dunk or quench; nearer, heat or cool to fine-tune. A fifth of the bar. */
	public static final int FAST_GAP = 200;

	/** The three machines, the heat each needs, and what each use does (OSRS wiki, per mille). */
	public enum Tool
	{
		HAMMER("Trip hammer", "the trip hammer", "Hammer", "hot", 5, 20, -25),
		GRIND("Grindstone", "the grindstone", "Grind", "medium", 3, 10, 15),
		POLISH("Polishing wheel", "the polishing wheel", "Polish", "cold", 1, 10, -17);

		private final String title;
		private final String the;
		private final String verb;
		private final String heatWord;
		/** Where its band's middle sits, in sixths of the bar. */
		private final int sixths;
		private final int progress;
		private final int heat;

		Tool(String title, String the, String verb, String heatWord, int sixths, int progress, int heat)
		{
			this.title = title;
			this.the = the;
			this.verb = verb;
			this.heatWord = heatWord;
			this.sixths = sixths;
			this.progress = progress;
			this.heat = heat;
		}

		public String getTitle()
		{
			return title;
		}

		public String the()
		{
			return the;
		}

		public String getVerb()
		{
			return verb;
		}

		/** "hot", "medium" or "cold". */
		public String heatWord()
		{
			return heatWord;
		}

		/** Progress a use gives, per mille. */
		public int progress()
		{
			return progress;
		}

		/** Heat a use adds (or, below 0, takes), per mille. */
		public int heat()
		{
			return heat;
		}

		/** The middle of its band, per mille. */
		public int centre()
		{
			return sixths * SCALE / 6;
		}

		public Step.Target target()
		{
			switch (this)
			{
				case HAMMER:
					return Step.Target.TRIP_HAMMER;
				case GRIND:
					return Step.Target.GRINDSTONE;
				default:
					return Step.Target.POLISHING_WHEEL;
			}
		}
	}

	/** What the mould jig shows. */
	public enum Jig
	{
		UNKNOWN,
		EMPTY,
		SET,
		/** Metal poured and still cooling. */
		COOLING,
		COOLED
	}

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** The commission's two words, or null with none. */
		public Mould.Style first;
		public Mould.Style second;
		public Jig jig = Jig.UNKNOWN;
		/** Bars' worth in the crucible, of 28. */
		public int crucible;
		/** The crucible's metal score so far. */
		public int metal;
		/** Metal bars carried, to add to it. */
		public int barsCarried;
		/** The preform is in your hands, or in storage. */
		public boolean holding;
		public boolean stored;
		/** A bucket of water carried or ice gloves (or smiths gloves (i)) worn. */
		public boolean canPickUp = true;
		/** 0-1,000. */
		public int progress;
		public int heat;
		/** The sword's stages, in order, as its HUD shows them; empty when not read. */
		public Tool[] stages = new Tool[0];
		/** Half a heat band's width, per mille, or -1 when the HUD's bands are not read. */
		public int halfBand = -1;
		/** A sweet spot is up now. */
		public boolean bonus;
		/** The game said the sword is too damaged to keep working. */
		public boolean ruined;
		/** The best moulds you own for this commission, or null. */
		public MouldPick.Pick pick;
	}

	/** Stages of the loop as the panel shows them. */
	public static final String[] LOOP = {"Commission", "Mould", "Pour", "Refine", "Hand in"};

	private FoundryCycle()
	{
	}

	/** The stage the sword's progress is in, or -1 with none read. */
	public static int stageIndex(Snapshot s)
	{
		int n = s.stages.length;
		if (n == 0)
		{
			return -1;
		}
		int i = Math.max(0, Math.min(SCALE, s.progress)) * n / SCALE;
		return Math.min(n - 1, i);
	}

	/** The tool the sword's stage needs now, or null. */
	public static Tool tool(Snapshot s)
	{
		int i = stageIndex(s);
		return i < 0 ? null : s.stages[i];
	}

	/** The band a tool needs, per mille: {low, high}, or null when the bands are not read. */
	public static int[] band(Tool tool, int halfBand)
	{
		if (tool == null || halfBand < 0)
		{
			return null;
		}
		return new int[]{tool.centre() - halfBand, tool.centre() + halfBand};
	}

	/** Uses of the tool before its own heat change takes the sword out of its band; 0 when out already. */
	public static int usesInBand(Tool tool, int heat, int halfBand)
	{
		int[] band = band(tool, halfBand);
		if (band == null)
		{
			return 0;
		}
		int n = 0;
		int h = heat;
		while (h >= band[0] && h <= band[1] && n < 100)
		{
			n++;
			h += tool.heat();
		}
		return n;
	}

	/** Uses of the tool to finish the stage under way, without sweet spots or the uniform. */
	public static int usesToFinish(Snapshot s)
	{
		Tool tool = tool(s);
		if (tool == null)
		{
			return 0;
		}
		int n = s.stages.length;
		int end = (stageIndex(s) + 1) * SCALE / n;
		int left = Math.max(0, end - s.progress);
		return (left + tool.progress() - 1) / tool.progress();
	}

	/** The commission as words, "Narrow Heavy", or null. */
	public static String commission(Snapshot s)
	{
		return s.first == null || s.second == null ? null : s.first.getTitle() + " " + s.second.getTitle();
	}

	public static Step next(Snapshot s)
	{
		if (s.holding || s.stored)
		{
			return sword(s);
		}
		if (s.jig == Jig.COOLED)
		{
			if (!s.canPickUp)
			{
				return Step.of("Take", "Pick up the preform",
					"It is too hot to hold: carry a bucket of water or wear ice gloves", Step.Target.MOULD_JIG);
			}
			return Step.of("Pick-up", "Pick up the preform", "Cooled in the jig and ready to work", Step.Target.MOULD_JIG);
		}
		if (s.jig == Jig.COOLING)
		{
			return Step.waiting("Let the metal cool", "Then pick up the preform from the jig", Step.Target.MOULD_JIG);
		}
		if (s.first == null)
		{
			return Step.of("Commission", "Take a commission from Kovac",
				"He asks for two styles; the moulds are picked to match them", Step.Target.KOVAC);
		}
		String words = commission(s);
		if (s.jig == Jig.EMPTY)
		{
			if (s.pick != null)
			{
				return Step.of("Setup", "Set the moulds",
					s.pick.names() + " (" + s.pick.getScore() + ")", Step.Target.MOULD_JIG);
			}
			return Step.of("Setup", "Set the moulds", "For " + words, Step.Target.MOULD_JIG);
		}
		if (s.crucible < Alloy.CRUCIBLE)
		{
			int need = Alloy.CRUCIBLE - s.crucible;
			if (s.barsCarried > 0)
			{
				return Step.of("Fill", "Fill the crucible",
					s.crucible + " of " + Alloy.CRUCIBLE + " bars in" + (s.metal > 0 ? " · metal score " + s.metal : "")
						+ " · " + need + " to go", Step.Target.CRUCIBLE);
			}
			return Step.of("Bank", "Take bars from the bank chest",
				need + (need == 1 ? " bar's" : " bars'") + " worth to fill the crucible", Step.Target.BANK_CHEST);
		}
		if (s.jig == Jig.SET)
		{
			return Step.of("Pour", "Pour the crucible", "Full: metal score " + s.metal
				+ (s.pick != null ? ", quality up to " + (s.metal + s.pick.getScore()) : ""), Step.Target.CRUCIBLE);
		}
		return Step.of("Setup", "Set the moulds", "The crucible is full; set the jig to pour", Step.Target.MOULD_JIG);
	}

	private static Step sword(Snapshot s)
	{
		if (s.ruined)
		{
			return Step.of("Hand-in", "Hand the sword to Kovac", "Too damaged to keep working: hand it in and start again", Step.Target.KOVAC);
		}
		if (s.progress >= SCALE)
		{
			return Step.of("Hand-in", "Hand the sword to Kovac", "Finished: Kovac pays experience, coins and reputation", Step.Target.KOVAC);
		}
		if (!s.holding)
		{
			return Step.of("Take", "Take the preform from storage", "It kept its progress and heat", Step.Target.PREFORM_STORAGE);
		}
		Tool tool = tool(s);
		if (tool == null)
		{
			return Step.of("Work", "Work the preform", "The HUD shows which machine each stage needs", Step.Target.NONE);
		}
		int[] band = band(tool, s.halfBand);
		String stage = "Stage " + (stageIndex(s) + 1) + " of " + s.stages.length;
		if (band == null)
		{
			return Step.of(tool.getVerb(), "Use " + tool.the(), stage + ": it needs the sword " + tool.heatWord(), tool.target());
		}
		if (s.heat >= band[0] && s.heat <= band[1])
		{
			if (s.bonus)
			{
				return Step.of("Bonus", "Use " + tool.the() + " now",
					"Sweet spot: a big step of progress and a point of quality back", tool.target());
			}
			int uses = usesInBand(tool, s.heat, s.halfBand);
			int finish = usesToFinish(s);
			String why = stage + " · heat for " + uses + (uses == 1 ? " use" : " uses") + ", " + finish + " to finish";
			Tool after = nextTool(s);
			if (after != null && finish <= uses)
			{
				why = stage + " · " + finish + " to finish, then " + after.getVerb().toLowerCase(Locale.ROOT) + " (" + after.heatWord() + ")";
			}
			return Step.of(tool.getVerb(), "Use " + tool.the(), why, tool.target());
		}
		boolean cold = s.heat < band[0];
		int gap = cold ? band[0] - s.heat : s.heat - band[1];
		boolean fast = gap > FAST_GAP;
		// Where to stop: the side of the band the tool will spend from.
		String aim = tool.heat() < 0 ? "the top" : "the bottom";
		String why = tool.getVerb() + " at " + pct(band[0]).replace("%", "") + "-" + pct(band[1]) + ", now " + pct(s.heat)
			+ ": stop near " + aim;
		if (cold)
		{
			return Step.of(fast ? "Dunk" : "Heat", fast ? "Dunk the preform in the lava" : "Heat the preform at the lava pool", why,
				Step.Target.LAVA_POOL);
		}
		return Step.of(fast ? "Quench" : "Cool", fast ? "Quench the preform in the waterfall" : "Cool the preform at the waterfall", why,
			Step.Target.WATERFALL);
	}

	/** The stage after the one under way, if its tool differs, or null. */
	public static Tool nextTool(Snapshot s)
	{
		int i = stageIndex(s);
		if (i < 0 || i + 1 >= s.stages.length)
		{
			return null;
		}
		Tool next = s.stages[i + 1];
		return next == s.stages[i] ? null : next;
	}

	static String pct(int perMille)
	{
		return String.format(Locale.ROOT, "%d%%", Math.round(perMille / 10.0));
	}

	/** Where a step sits in the loop, or -1 for one that keeps the stage it broke into. */
	public static int stage(Step step)
	{
		switch (step.getTarget())
		{
			case KOVAC:
				return step.getVerb().startsWith("Hand") ? 4 : 0;
			case MOULD_JIG:
				return step.getTitle().startsWith("Set") ? 1 : step.isWaiting() ? 2 : 3;
			case CRUCIBLE:
			case BANK_CHEST:
				return 2;
			case TRIP_HAMMER:
			case GRINDSTONE:
			case POLISHING_WHEEL:
			case LAVA_POOL:
			case WATERFALL:
			case PREFORM_STORAGE:
				return 3;
			default:
				return -1;
		}
	}
}
