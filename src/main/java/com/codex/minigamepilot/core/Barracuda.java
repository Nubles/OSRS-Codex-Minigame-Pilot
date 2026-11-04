package com.codex.minigamepilot.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of the Barracuda Trials, Sailing's timed courses, as the OSRS wiki gives them
 * (Barracuda Trials; The Tempor Tantrum; The Jubbly Jive; The Gwenith Glide; and each rank's
 * reward page: Stormy key, Barrel stand, Ralph's fabric roll, Fetid key, Captured wind mote,
 * Gurtob's fabric roll, Serrated key, Heart of Ithell, Gwyna's fabric roll), quoted beside each.
 * Ids are RuneLite's gameval names.
 *
 * <p>What a varbit or interface line holds is inferred from its RuneLite name; where the Plugin
 * Hub's Barracuda Trials plugins read it the same way, that is said, and it stays unconfirmed.
 * Nothing here places a checkpoint or a route: the wiki gives none in words, so where the next
 * checkpoint lies is learnt from your own runs.
 */
public final class Barracuda
{
	/** "Only skiffs may be used in these trials." (Barracuda Trials): so a course is a trial and a rank, never a boat. */
	public static final String BOAT = "skiff";

	/** The three ranks: "three ranks of difficulty (Swordfish, Shark, and Marlin)" (Barracuda Trials). */
	public enum Rank
	{
		SWORDFISH("Swordfish", 's'),
		SHARK("Shark", 'k'),
		MARLIN("Marlin", 'm');

		private final String title;
		final char code;

		Rank(String title, char code)
		{
			this.title = title;
			this.code = code;
		}

		public String getTitle()
		{
			return title;
		}

		public char code()
		{
			return code;
		}

		public static Rank byCode(char c)
		{
			for (Rank r : values())
			{
				if (r.code == c)
				{
					return r;
				}
			}
			return null;
		}
	}

	/** The three trials, in the order their Sailing levels open them. */
	public enum Trial
	{
		/**
		 * Level 30, Rum-dashed Ralph, the Storm Tempor. Targets from each reward's page:
		 * Stormy key "requiring a completion time of 1:48.00", Barrel stand "2:51.00", Ralph's
		 * fabric roll "4:30.00". Bonus experience from the trial's table: 1,000, 2,000, 3,000.
		 */
		TEMPOR_TANTRUM("The Tempor Tantrum", "Tempor Tantrum", 't', 30, "Rum-dashed Ralph", "the Storm Tempor",
			new int[]{180, 285, 450},
			new String[]{"Stormy key", "Barrel stand and Whirlpool surprise", "Ralph's fabric roll"},
			new int[]{1_000, 2_000, 3_000},
			new int[]{NpcID.SAILING_BT_TRIAL_MASTER_1_PARENT, NpcID.SAILING_BT_TRIAL_MASTER_1_1OP, NpcID.SAILING_BT_TRIAL_MASTER_1_3OP, NpcID.SAILING_BT_TRIAL_MASTER_1_0OP},
			new int[]{VarbitID.SAILING_BT_TEMPOR_TANTRUM_SWORDFISH_FIRST, VarbitID.SAILING_BT_TEMPOR_TANTRUM_SHARK_FIRST, VarbitID.SAILING_BT_TEMPOR_TANTRUM_MARLIN_FIRST},
			VarbitID.SAILING_BT_TEMPOR_TANTRUM_MASTER_STATE, VarPlayerID.SAILING_BT_TRIAL_TEMPOR_TANTRUM_COMPLETED, null),
		/**
		 * Level 55, Gurtob, Backwater. Fetid key "2:03.00", Captured wind mote "3:00.00", Gurtob's
		 * fabric roll "5:21.00". "a one time bonus of 5,000", "7,500", "10,000" Sailing experience.
		 */
		JUBBLY_JIVE("The Jubbly Jive", "Jubbly Jive", 'j', 55, "Gurtob", "Backwater",
			new int[]{205, 300, 535},
			new String[]{"Fetid key", "Captured wind mote", "Gurtob's fabric roll"},
			new int[]{5_000, 7_500, 10_000},
			new int[]{NpcID.SAILING_BT_TRIAL_MASTER_2_PARENT, NpcID.SAILING_BT_TRIAL_MASTER_2_1OP, NpcID.SAILING_BT_TRIAL_MASTER_2_3OP, NpcID.SAILING_BT_TRIAL_MASTER_2_0OP,
				NpcID.SAILING_BT_TRIAL_MASTER_2_ASSISTANT_PARENT, NpcID.SAILING_BT_TRIAL_MASTER_2_ASSISTANT_1OP, NpcID.SAILING_BT_TRIAL_MASTER_2_ASSISTANT_0OP},
			new int[]{VarbitID.SAILING_BT_JUBBLY_JIVE_SWORDFISH_FIRST, VarbitID.SAILING_BT_JUBBLY_JIVE_SHARK_FIRST, VarbitID.SAILING_BT_JUBBLY_JIVE_MARLIN_FIRST},
			VarbitID.SAILING_BT_JUBBLY_JIVE_MASTER_STATE, VarPlayerID.SAILING_BT_TRIAL_JUBBLY_JIVE_COMPLETED, null),
		/**
		 * Level 72 and Regicide, Gwyna, Porth Gwenith. Serrated key "2:00.00", Heart of Ithell
		 * "3:42.00", Gwyna's fabric roll "6:09.00". The bonus experience is not in the wiki's text,
		 * so none is shown.
		 */
		GWENITH_GLIDE("The Gwenith Glide", "Gwenith Glide", 'g', 72, "Gwyna", "Porth Gwenith",
			new int[]{200, 370, 615},
			new String[]{"Serrated key", "Heart of Ithell", "Gwyna's fabric roll"},
			new int[]{-1, -1, -1},
			new int[]{NpcID.SAILING_BT_TRIAL_MASTER_3_PARENT, NpcID.SAILING_BT_TRIAL_MASTER_3_1OP, NpcID.SAILING_BT_TRIAL_MASTER_3_3OP, NpcID.SAILING_BT_TRIAL_MASTER_3_0OP},
			new int[]{VarbitID.SAILING_BT_GWENITH_GLIDE_SWORDFISH_FIRST, VarbitID.SAILING_BT_GWENITH_GLIDE_SHARK_FIRST, VarbitID.SAILING_BT_GWENITH_GLIDE_MARLIN_FIRST},
			VarbitID.SAILING_BT_GWENITH_GLIDE_MASTER_STATE, VarPlayerID.SAILING_BT_TRIAL_GWENITH_GLIDE_COMPLETED, "Regicide");

		private final String title;
		/** How the game's lines name it, without "The". */
		private final String shortName;
		final char code;
		private final int level;
		private final String master;
		private final String place;
		/** Each rank's target, in game ticks (0.6 seconds). */
		private final int[] targetTicks;
		private final String[] rewards;
		private final int[] bonusXp;
		private final int[] npcs;
		private final int[] firsts;
		private final int masterState;
		private final int completed;
		private final String quest;

		Trial(String title, String shortName, char code, int level, String master, String place, int[] targetTicks, String[] rewards,
			int[] bonusXp, int[] npcs, int[] firsts, int masterState, int completed, String quest)
		{
			this.title = title;
			this.shortName = shortName;
			this.code = code;
			this.level = level;
			this.master = master;
			this.place = place;
			this.targetTicks = targetTicks;
			this.rewards = rewards;
			this.bonusXp = bonusXp;
			this.npcs = npcs;
			this.firsts = firsts;
			this.masterState = masterState;
			this.completed = completed;
			this.quest = quest;
		}

		public String getTitle()
		{
			return title;
		}

		public String shortName()
		{
			return shortName;
		}

		/** "the Tempor Tantrum", for the middle of a sentence. */
		public String inSentence()
		{
			return "the " + shortName;
		}

		public char code()
		{
			return code;
		}

		/** "The level requirement is not boostable." (Barracuda Trials) */
		public int level()
		{
			return level;
		}

		public String master()
		{
			return master;
		}

		public String place()
		{
			return place;
		}

		/** The quest the trial also needs, or null. */
		public String quest()
		{
			return quest;
		}

		/** The rank's target time in ticks: every one of the nine is a whole number of ticks. */
		public int targetTicks(Rank r)
		{
			return targetTicks[r.ordinal()];
		}

		public String reward(Rank r)
		{
			return rewards[r.ordinal()];
		}

		/** The one-time bonus Sailing experience for the rank, or -1 where the wiki's text gives none. */
		public int bonusXp(Rank r)
		{
			return bonusXp[r.ordinal()];
		}

		public boolean isMaster(int npcId)
		{
			for (int id : npcs)
			{
				if (id == npcId)
				{
					return true;
				}
			}
			return false;
		}

		public int[] npcs()
		{
			return npcs.clone();
		}

		/**
		 * The rank's first completion, by RuneLite's name {@code SAILING_BT_<TRIAL>_<RANK>_FIRST}:
		 * unconfirmed, inferred from the name ("The first completion of each tier awards additional
		 * bonus Sailing experience").
		 */
		public int firstVarbit(Rank r)
		{
			return firsts[r.ordinal()];
		}

		/**
		 * In this trial, by RuneLite's name {@code SAILING_BT_<TRIAL>_MASTER_STATE}: unconfirmed; the
		 * Hub's Barracuda Trials Pathfinder and Barracuda Ghost Ship both read 2 as a trial under way.
		 */
		public int masterStateVarbit()
		{
			return masterState;
		}

		/** Completions, by RuneLite's name {@code SAILING_BT_TRIAL_<TRIAL>_COMPLETED}: unconfirmed, inferred from the name. */
		public int completedVarp()
		{
			return completed;
		}

		public static Trial byCode(char c)
		{
			for (Trial t : values())
			{
				if (t.code == c)
				{
					return t;
				}
			}
			return null;
		}

		/** The trial a line of the game's text names, or null. */
		public static Trial named(String text)
		{
			if (text == null)
			{
				return null;
			}
			String t = text.replaceAll("<[^>]*>", "").toLowerCase(java.util.Locale.ROOT);
			for (Trial x : values())
			{
				if (t.contains(x.shortName.toLowerCase(java.util.Locale.ROOT)))
				{
					return x;
				}
			}
			return null;
		}

		/** The rank whose target is this time, read off the trial's own interface, or null. */
		public Rank rankFor(int ticks)
		{
			for (Rank r : Rank.values())
			{
				if (targetTicks(r) == ticks)
				{
					return r;
				}
			}
			return null;
		}
	}

	/** Value of a trial's master state while a trial is under way (see {@link Trial#masterStateVarbit}): unconfirmed. */
	public static final int MASTER_STATE_IN_TRIAL = 2;
	/** In a trial, by RuneLite's name {@code SAILING_BT_IN_TRIAL}: unconfirmed, inferred from the name. */
	public static final int IN_TRIAL = VarbitID.SAILING_BT_IN_TRIAL;

	/** The trial's interface: its title, the time so far, the rank's time, and the objectives' progress. */
	public static final int HUD_TITLE = InterfaceID.SailingBtHud.BT_TITLE;
	public static final int HUD_TIME = InterfaceID.SailingBtHud.BT_CURRENT_TIME;
	public static final int HUD_RANK_TIME = InterfaceID.SailingBtHud.BT_RANK_TIME;
	public static final int HUD_PROGRESS = InterfaceID.SailingBtHud.BT_TRACKER_PROGRESS;
	public static final int HUD_OPTIONAL = InterfaceID.SailingBtHud.BT_OPTIONAL_PROGRESS;

	/**
	 * "If the trial area is left, the player has 10 seconds to return or their progress will be
	 * lost." (The Tempor Tantrum) In ticks.
	 */
	public static final int RETURN_TICKS = 17;

	/** The start and the end, as the game says them: unconfirmed; the Hub's Barracuda Ghost Ship plugin waits for these. */
	public static final String STARTING = "You prepare to begin the";
	public static final String COMPLETED = "You have completed the";

	private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,2}))?");
	private static final Pattern COUNT = Pattern.compile("(\\d{1,4})\\s*/\\s*(\\d{1,4})");

	private Barracuda()
	{
	}

	/** A time as the trial's interface writes it ("2:51", "2:51.00", tagged or not), in ticks rounded to the nearest; -1 when there is none. */
	public static int ticks(String text)
	{
		int cs = centis(text);
		return cs < 0 ? -1 : (int) Math.round(cs / 60.0);
	}

	/** A time as the interface writes it, in hundredths of a second, or -1. */
	public static int centis(String text)
	{
		if (text == null)
		{
			return -1;
		}
		Matcher m = TIME.matcher(text.replaceAll("<[^>]*>", ""));
		if (!m.find())
		{
			return -1;
		}
		int min = Integer.parseInt(m.group(1));
		int sec = Integer.parseInt(m.group(2));
		if (sec >= 60 || min > 59)
		{
			return -1;
		}
		int frac = 0;
		if (m.group(3) != null)
		{
			frac = Integer.parseInt(m.group(3));
			frac = m.group(3).length() == 1 ? frac * 10 : frac;
		}
		return (min * 60 + sec) * 100 + frac;
	}

	/**
	 * The objectives done in a line of the interface: every "done / of" in it summed, as
	 * {done, of}; {-1, -1} when there is none. Objectives only ever count up within a run, so a
	 * rise is a checkpoint passed.
	 */
	public static int[] progress(String text)
	{
		if (text == null)
		{
			return new int[]{-1, -1};
		}
		Matcher m = COUNT.matcher(text.replaceAll("<[^>]*>", " "));
		int done = 0;
		int of = 0;
		boolean any = false;
		while (m.find())
		{
			int a = Integer.parseInt(m.group(1));
			int b = Integer.parseInt(m.group(2));
			if (a > b || b == 0)
			{
				continue;
			}
			any = true;
			done += a;
			of += b;
		}
		return any ? new int[]{done, of} : new int[]{-1, -1};
	}

	/** "1:48.0": ticks as minutes, seconds and tenths. */
	public static String clock(int ticks)
	{
		if (ticks < 0)
		{
			return "-";
		}
		int tenths = ticks * 6;
		int s = tenths / 10;
		return s / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60) + "." + tenths % 10;
	}

	/** "1:48" when whole, as the wiki writes the targets. */
	public static String target(int ticks)
	{
		int tenths = ticks * 6;
		if (tenths % 10 != 0)
		{
			return clock(ticks);
		}
		int s = tenths / 10;
		return s / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60);
	}

	/** "1.2s behind", "0.6s ahead" or "level": a gap in ticks against a ghost (positive is slower). */
	public static String gap(int ticks)
	{
		if (ticks == 0)
		{
			return "level";
		}
		return seconds(Math.abs(ticks)) + (ticks > 0 ? " behind" : " ahead");
	}

	/** "1.2s": ticks as seconds and tenths. */
	public static String seconds(int ticks)
	{
		int tenths = Math.abs(ticks) * 6;
		return tenths / 10 + "." + tenths % 10 + "s";
	}
}
