package com.codex.minigamepilot.core;

import java.util.Locale;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * The facts of stealing artefacts, the Thieving activity in Port Piscarilius: its level, the six
 * houses and their drawers, the artefacts, the experience and coins a delivery pays, and the
 * Kourend &amp; Kebos diary task it counts toward.
 *
 * <p>Figures are from the OSRS wiki (Stealing artefacts; Captain Khaled; Drawers (stealing
 * artefacts); Stolen pendant; Kourend &amp; Kebos Diary); ids are RuneLite's gameval names. Which
 * value of {@code PISCARILIUS_BURGLARY_TARGET} names which house, and where each house stands,
 * the wiki does not give: they are as the Stealing Artefacts plugin (pajlads, MIT) has them, so
 * the Pilot treats them as unconfirmed, checks the house against Captain Khaled's own words when
 * he gives a task, and learns each drawer's place when it comes into view.
 */
public final class Artefacts
{
	/** "requiring level 49 Thieving to participate". */
	public static final int LEVEL = 49;
	/** "Successfully picking the lock grants 750 Thieving experience." */
	public static final int LOCKPICK_XP = 750;
	/** Delivering "yields additional Thieving experience equal to 40 times the current Thieving level". */
	public static final int DELIVERY_XP_PER_LEVEL = 40;
	/** "500–1,000 coins" a delivery. */
	public static final int COINS_MIN = 500;
	public static final int COINS_MAX = 1000;
	/** "the maximum number of artefacts stolen per hour from approximately 48 to 55" with a teleport after each hand-in. */
	public static final int WIKI_PER_HOUR = 48;
	public static final int WIKI_PER_HOUR_TELEPORT = 55;

	/** Stolen pendant, garnet ring, circlet, family heirloom and jewelry box. */
	public static final int[] ITEMS = {ItemID.PISCARILIUS_STOLEN_PENDANT, ItemID.PISCARILIUS_STOLEN_RING,
		ItemID.PISCARILIUS_STOLEN_CIRCLET, ItemID.PISCARILIUS_STOLEN_HEIRLOOM, ItemID.PISCARILIUS_STOLEN_BOX};
	/** "A lockpick or hair clip is required to steal artefacts". */
	public static final int[] PICKS = {ItemID.LOCKPICK, ItemID.KR_HAIRCLIP};
	/** Kharedst's memoirs and the Book of the Dead, which teleport "to the centre of Piscarilius". */
	public static final int[] TELEPORTS = {ItemID.VEOS_KHAREDSTS_MEMOIRS, ItemID.BOOK_OF_THE_DEAD};
	public static final int[] KHALED = {NpcID.PISCARILIUS_CAPTAIN_KHALED_DEFAULT, NpcID.PISCARILIUS_CAPTAIN_KHALED_TASK,
		NpcID.PISCARILIUS_CAPTAIN_KHALED_MULTI};
	/** The "constantly moving patrolmen and patrolwomen", by RuneLite's names. */
	public static final int[] PATROLS = {NpcID.PISCARILIUS_PATROLMAN_NW_1, NpcID.PISCARILIUS_PATROLMAN_NW_2,
		NpcID.PISCARILIUS_PATROLMAN_N_1, NpcID.PISCARILIUS_PATROLMAN_N_2, NpcID.PISCARILIUS_PATROLMAN_N_3,
		NpcID.PISCARILIUS_PATROLMAN_S_1, NpcID.PISCARILIUS_PATROLMAN_SW_1, NpcID.PISCARILIUS_PATROLMAN_IDLE};
	public static final int LADDER_UP = ObjectID.PISCARILIUS_LADDER_BOTTOM;
	public static final int[] LADDER_DOWN = {ObjectID.PISCARILIUS_LADDER_TOP_01, ObjectID.PISCARILIUS_LADDER_TOP_02};

	/** The task varbit's values after the six houses. Unconfirmed (source: Stealing Artefacts plugin). */
	public static final int TASK_NONE = 0;
	public static final int TASK_FAILED = 7;
	public static final int TASK_DELIVER = 8;

	/** Port Piscarilius, roughly. Unconfirmed (source: Stealing Artefacts plugin's isInPisc). */
	private static final int MIN_X = 1739;
	private static final int MAX_X = 1860;
	private static final int MIN_Y = 3675;
	private static final int MAX_Y = 3803;

	private Artefacts()
	{
	}

	/**
	 * The six houses, in the order RuneLite names their drawers ({@code PISCARILIUS_BURGLARY_DRAWERS_N},
	 * {@code _SE}, {@code _S}, {@code _SW}, {@code _W}, {@code _NW}), which is also the order of the
	 * task varbit's values 1 to 6 in the Stealing Artefacts plugin. The wiki: "The artefact is
	 * located in a drawer on the ground floor in the northern house, and upstairs in the others",
	 * and the average round trip to Captain Khaled for each.
	 */
	public enum House
	{
		NORTH("North house", 1, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_N, false, 1768, 3750, 0, 0, 60, 50),
		SOUTH_EAST("South-east house", 2, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_SE, true, 1775, 3733, 1776, 3730, 65, 55),
		SOUTH("South house", 3, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_S, true, 1765, 3732, 1768, 3733, 70, 60),
		SOUTH_WEST("South-west house", 4, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_SW, true, 1749, 3734, 1749, 3730, 90, 80),
		WEST("West house", 5, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_W, true, 1750, 3749, 1751, 3751, 80, 70),
		NORTH_WEST("North-west house", 6, ObjectID.PISCARILIUS_BURGLARY_DRAWERS_NW, true, 1750, 3761, 1750, 3756, 85, 75);

		private final String title;
		private final int task;
		private final int drawers;
		private final boolean upstairs;
		private final int x;
		private final int y;
		private final int ladderX;
		private final int ladderY;
		private final int wikiSeconds;
		private final int wikiSecondsTeleport;

		House(String title, int task, int drawers, boolean upstairs, int x, int y, int ladderX, int ladderY, int wikiSeconds, int wikiSecondsTeleport)
		{
			this.title = title;
			this.task = task;
			this.drawers = drawers;
			this.upstairs = upstairs;
			this.x = x;
			this.y = y;
			this.ladderX = ladderX;
			this.ladderY = ladderY;
			this.wikiSeconds = wikiSeconds;
			this.wikiSecondsTeleport = wikiSecondsTeleport;
		}

		public String getTitle()
		{
			return title;
		}

		/** "the north-west house" and so on, for a sentence. */
		public String inSentence()
		{
			return "the " + title.toLowerCase(Locale.ROOT);
		}

		/** The task varbit's value for it. Unconfirmed (source: Stealing Artefacts plugin). */
		public int task()
		{
			return task;
		}

		public int drawers()
		{
			return drawers;
		}

		/** The drawer is upstairs, by a ladder. */
		public boolean upstairs()
		{
			return upstairs;
		}

		/** Near the drawer, on its floor. Unconfirmed (source: Stealing Artefacts plugin); the Pilot uses the drawer itself once seen. */
		public int x()
		{
			return x;
		}

		public int y()
		{
			return y;
		}

		/** Near the house's ladder up, or 0 for the north house. Unconfirmed (source: Stealing Artefacts plugin). */
		public int ladderX()
		{
			return ladderX;
		}

		public int ladderY()
		{
			return ladderY;
		}

		/** The wiki's average round trip to Captain Khaled, in seconds, walking. */
		public int wikiSeconds()
		{
			return wikiSeconds;
		}

		/** The same with Kharedst's memoirs or the Book of the Dead after each hand-in. */
		public int wikiSecondsTeleport()
		{
			return wikiSecondsTeleport;
		}

		/** The house for a value of the task varbit, or null for none (or failed, or deliver). */
		public static House byTask(int value)
		{
			for (House h : values())
			{
				if (h.task == value)
				{
					return h;
				}
			}
			return null;
		}

		public static House byDrawers(int id)
		{
			for (House h : values())
			{
				if (h.drawers == id)
				{
					return h;
				}
			}
			return null;
		}

		/**
		 * The house Captain Khaled names in "It can be found in the [location].", read loosely
		 * ("north-western", "North West", "northwest"), or null when he names none.
		 */
		public static House heard(String text)
		{
			if (text == null)
			{
				return null;
			}
			String t = text.toLowerCase(Locale.ROOT).replace(' ', ' ');
			int at = t.indexOf("found in the");
			if (at < 0)
			{
				return null;
			}
			t = t.substring(at + "found in the".length()).replaceAll("[^a-z]", "");
			if (t.startsWith("northwest"))
			{
				return NORTH_WEST;
			}
			if (t.startsWith("southwest"))
			{
				return SOUTH_WEST;
			}
			if (t.startsWith("southeast"))
			{
				return SOUTH_EAST;
			}
			if (t.startsWith("north"))
			{
				return NORTH;
			}
			if (t.startsWith("south"))
			{
				return SOUTH;
			}
			if (t.startsWith("west"))
			{
				return WEST;
			}
			return null;
		}
	}

	/** Thieving experience a delivery pays at this level: 40 times it. */
	public static int deliveryXp(int level)
	{
		return DELIVERY_XP_PER_LEVEL * Math.max(1, Math.min(99, level));
	}

	/** An artefact from the drawer to Captain Khaled: the lock, then the delivery. */
	public static int xpEach(int level)
	{
		return LOCKPICK_XP + deliveryXp(level);
	}

	/** The wiki's experience an hour at a level: 48 artefacts an hour walking, 55 with a teleport after each. */
	public static int wikiXpPerHour(int level, boolean teleport)
	{
		return xpEach(level) * (teleport ? WIKI_PER_HOUR_TELEPORT : WIKI_PER_HOUR);
	}

	/** In Port Piscarilius, roughly. Unconfirmed (source: Stealing Artefacts plugin). */
	public static boolean inPort(int x, int y)
	{
		return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y;
	}

	public static boolean isArtefact(int id)
	{
		return contains(ITEMS, id);
	}

	public static boolean isPatrol(int id)
	{
		return contains(PATROLS, id);
	}

	public static boolean isKhaled(int id)
	{
		return contains(KHALED, id);
	}

	static boolean contains(int[] ids, int id)
	{
		for (int i : ids)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}
}
