package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Which open Guardians of the Rift altar to craft at.
 *
 * <p>The rule is the OSRS wiki's (Guardians of the Rift/Strategies): "For even points, prioritise
 * crafting at the blood and death altars, followed by fire and earth. When none of these are
 * active, craft at whichever type you have the fewest points for." An altar you cannot use (its
 * Runecraft level or its quest) is never picked, however good it would be.
 */
public final class AltarPick
{
	/** The choice, and a line of why for the plaque and the panel. */
	public static final class Choice
	{
		public final Altar altar;
		public final String why;
		/** Each open altar, with whether you can use it and why not. */
		public final List<Option> open;

		Choice(Altar altar, String why, List<Option> open)
		{
			this.altar = altar;
			this.why = why;
			this.open = Collections.unmodifiableList(open);
		}
	}

	/** An open altar as the panel lists it. */
	public static final class Option
	{
		public final Altar altar;
		/** Null when you can use it, else what stops you: "Needs 77 Runecraft". */
		public final String locked;

		Option(Altar altar, String locked)
		{
			this.altar = altar;
			this.locked = locked;
		}
	}

	private AltarPick()
	{
	}

	/**
	 * @param elemental the open elemental altar, or null when none is known
	 * @param catalytic the open catalytic altar, or null
	 * @param runecraft your Runecraft level (unboosted: a boost does not open an altar here)
	 * @param questDone whether a quest, by name, is done
	 * @param points your points of each type as best known, or null when not known
	 */
	public static Choice choose(Altar elemental, Altar catalytic, int runecraft, Predicate<String> questDone, RiftPoints points)
	{
		List<Option> open = new ArrayList<>();
		List<Altar> usable = new ArrayList<>();
		for (Altar a : new Altar[]{elemental, catalytic})
		{
			if (a == null)
			{
				continue;
			}
			String lock = lock(a, runecraft, questDone);
			open.add(new Option(a, lock));
			if (lock == null)
			{
				usable.add(a);
			}
		}
		if (open.isEmpty())
		{
			return new Choice(null, "No altar is open yet", open);
		}
		if (usable.isEmpty())
		{
			StringBuilder b = new StringBuilder();
			for (Option o : open)
			{
				b.append(b.length() == 0 ? "" : "; ").append(o.altar.rune()).append(" n").append(o.locked.substring(1));
			}
			return new Choice(null, "Neither open altar is yours yet: " + b, open);
		}
		if (usable.size() == 1)
		{
			Altar a = usable.get(0);
			Altar other = usable.get(0) == elemental ? catalytic : elemental;
			String why = other == null ? "The only altar open" : "The " + other.rune().toLowerCase() + " altar is not yours yet";
			return new Choice(a, why + cell(a), open);
		}
		Altar e = elemental;
		Altar c = catalytic;
		int re = rank(e);
		int rc = rank(c);
		if (re != rc)
		{
			Altar a = re < rc ? e : c;
			String why = rank(a) == 0 ? "Blood and death come first" : "Fire and earth come next";
			return new Choice(a, why + cell(a), open);
		}
		// Neither is blood, death, fire or earth: keep the points even.
		if (points != null && points.known())
		{
			double pe = points.elemental();
			double pc = points.catalytic();
			if (Math.abs(pe - pc) >= 0.5)
			{
				Altar a = pe < pc ? e : c;
				Altar.Alignment low = a.alignment();
				// The figures are on the panel, banked and this game's apart, as the game reports them.
				return new Choice(a, "Fewer " + low.word() + " points" + cell(a), open);
			}
			Altar a = e.level() >= c.level() ? e : c;
			return new Choice(a, "Points even, so the stronger cell" + cell(a), open);
		}
		Altar a = e.level() >= c.level() ? e : c;
		return new Choice(a, "Points not known yet, so the stronger cell" + cell(a), open);
	}

	/** What stops you using an altar, or null when you can. */
	static String lock(Altar a, int runecraft, Predicate<String> questDone)
	{
		if (runecraft < a.level())
		{
			return "Needs " + a.level() + " Runecraft";
		}
		if (a.quest() != null && (questDone == null || !questDone.test(a.quest())))
		{
			return "Needs " + a.quest();
		}
		return null;
	}

	/** Blood and death first, then fire and earth, then the rest (wiki strategy). */
	static int rank(Altar a)
	{
		switch (a)
		{
			case BLOOD:
			case DEATH:
				return 0;
			case FIRE:
			case EARTH:
				return 1;
			default:
				return 2;
		}
	}

	private static String cell(Altar a)
	{
		return " · " + a.cell().word() + " cell";
	}
}
