package com.codex.minigamepilot.core;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * The facts of stealing valuables, the Thieving activity in Civitas illa Fortis: its level, the
 * three houses and their owners, the urchins' distractions of the wealthy citizens, the house keys,
 * the time an owner is away, the valuables and what Oriana pays for them, and the experience.
 *
 * <p>Figures are from the OSRS wiki (Stealing valuables; Wealthy citizen; House keys; Valuables;
 * Locked Door (stealing valuables); Window (stealing valuables); Chest, Wardrobe and Jewellery case
 * (stealing valuables); Oriana; Lavinia; Victor (Civitas illa Fortis); Caius); ids are RuneLite's
 * gameval names. Where each house stands the wiki gives only in words, so the Pilot learns each
 * door's place from the game when it comes into view.
 */
public final class Houses
{
	/** "requiring level 50 Thieving and completion of Children of the Sun to participate." */
	public static final int LEVEL = 50;
	/** "gaining 45 Thieving experience per stolen valuable." */
	public static final int XP_EACH = 45;
	/** "a one-time bonus of 14 valuables and 630 Thieving experience." */
	public static final int SHINE_VALUABLES = 14;
	public static final int SHINE_XP = 630;
	/** "The arrow will disappear after around 7 seconds". */
	public static final int SHINE_SECONDS = 7;
	/** "The homeowner will return 180–190 seconds after they left the house". */
	public static final int AWAY_MIN = 180;
	public static final int AWAY_MAX = 190;
	/** "players may loot each object for up to 50-60 seconds without additional input". */
	public static final int LOOT_MIN = 50;
	public static final int LOOT_MAX = 60;
	/** "causing them to lose around 90% of the valuables stolen from that house." */
	public static final int CAUGHT_LOSS_PERCENT = 90;
	/** Oriana: "for 55 coins each", "increased to 65 for players who have earned the Gladiator rank". */
	public static final int ORIANA_PAYS = 55;
	public static final int ORIANA_PAYS_GLADIATOR = 65;
	/** "Players can also pay 15 valuables to several urchins around this area" to learn the vacant house. */
	public static final int URCHIN_FEE = 15;
	/** "Every 84-99 seconds, three street urchins ... will simultaneously distract one each for approximately 20 seconds". */
	public static final int DISTRACT_EVERY_MIN = 84;
	public static final int DISTRACT_EVERY_MAX = 99;
	public static final int DISTRACT_FOR = 20;
	/** Wealthy citizen's infobox: "Thieving XP" "96 xp", and coins 85 a pick. */
	public static final int PICKPOCKET_XP = 96;
	public static final int PICKPOCKET_COINS = 85;
	/** Wealthy citizen's drop table: House keys "~5/85". */
	public static final int KEY_IN = 85;
	public static final int KEY_CHANCE = 5;
	/** Wealthy citizen's infobox: "Stun (status) Timer" "4s", "Damage" "3". */
	public static final int STUN_SECONDS = 4;
	public static final int STUN_DAMAGE = 3;
	/** "about 1,600–2,300 valuables from 18–19 houses per hour". */
	public static final int WIKI_VALUABLES_MIN = 1600;
	public static final int WIKI_VALUABLES_MAX = 2300;
	/** "about 90,000–125,000 coins per hour". */
	public static final int WIKI_COINS_MIN = 90_000;
	public static final int WIKI_COINS_MAX = 125_000;
	/** The wiki's burgling experience table: Thieving level, then experience an hour. */
	static final int[][] WIKI_XP = {{50, 72_000}, {60, 80_000}, {70, 93_000}, {80, 95_000}, {90, 100_000}, {99, 105_000}};

	public static final int KEY = ItemID.VARLAMORE_THIEVING_HOUSE_KEY;
	public static final int VALUABLES = ItemID.VARLAMORE_THIEVING_HOUSE_VALUABLE;
	/** "A very sturdy, locked wooden door": only while the house is vacant. */
	public static final int DOOR_LOCKED = ObjectID.VARLAMORE_THIEVING_HOUSE_DOOR_LOCKED;
	/** The door while the owner is home ("it will instead be an unlocked door"); gameval calls it _DOOR_OPEN. */
	public static final int DOOR = ObjectID.VARLAMORE_THIEVING_HOUSE_DOOR_OPEN;
	/** The window: 52000 in the wiki's infobox (open), 52001 the other state, and RuneLite's multiloc. */
	public static final int WINDOW_OPEN = ObjectID.VARLAMORE_THIEVING_HOUSE_WINDOW_OPEN;
	public static final int WINDOW_CLOSED = ObjectID.VARLAMORE_THIEVING_HOUSE_WINDOW_CLOSED;
	public static final int WINDOW_MULTI = ObjectID.VARLAMORE_THIEVING_HOUSE_WINDOW_MULTI;
	public static final int[] WINDOWS = {WINDOW_OPEN, WINDOW_CLOSED, WINDOW_MULTI};
	/**
	 * "chests, wardrobes, and jewellery cases which players may search": every chest, wardrobe and
	 * jewellery case RuneLite names for the houses (the wiki's infoboxes give 52008, 52010 and 52011).
	 */
	public static final int[] LOOT = {ObjectID.VARLAMORE_THIEVING_HOUSE_CHEST, ObjectID.VARLAMORE_THIEVING_HOUSE_CHEST01,
		ObjectID.VARLAMORE_THIEVING_HOUSE_CHEST02, ObjectID.VARLAMORE_THIEVING_HOUSE_CHEST03, ObjectID.VARLAMORE_THIEVING_HOUSE_CHEST04,
		ObjectID.VARLAMORE_THIEVING_HOUSE_WARDROBE, ObjectID.VARLAMORE_THIEVING_HOUSE_WARDROBE01, ObjectID.VARLAMORE_THIEVING_HOUSE_WARDROBE02,
		ObjectID.VARLAMORE_THIEVING_HOUSE_JEWELLERY_CASE01};
	public static final int[] CITIZENS = {NpcID.VARLAMORE_WEALTHY_CITIZEN_A, NpcID.VARLAMORE_WEALTHY_CITIZEN_B,
		NpcID.VARLAMORE_WEALTHY_CITIZEN_C, NpcID.VARLAMORE_WEALTHY_CITIZEN_D};
	/** Leo, Julia and Aurelia, who distract the citizens. */
	public static final int[] DISTRACTERS = {NpcID.VARLAMORE_THIEVING_STREET_URCHIN_A, NpcID.VARLAMORE_THIEVING_STREET_URCHIN_B,
		NpcID.VARLAMORE_THIEVING_STREET_URCHIN_C};
	/** Rufus, Ava, Lucia and Oli, who tell you which house is vacant. */
	public static final int[] TELLERS = {NpcID.VARLAMORE_THIEVING_HOUSE_URCHIN_A, NpcID.VARLAMORE_THIEVING_HOUSE_URCHIN_B,
		NpcID.VARLAMORE_THIEVING_HOUSE_URCHIN_C, NpcID.VARLAMORE_THIEVING_HOUSE_URCHIN_D};
	/** Oriana, a "Dodgy Character the first time you meet". */
	public static final int[] ORIANA = {NpcID.VARLAMORE_DODGY_CHARACTER, NpcID.VARLAMORE_DODGY_CHARACTER_MET};

	/** The game's messages, as the wiki quotes them. */
	public static final String DISTRACTED = "You notice an urchin distract a wealthy citizen nearby.";
	public static final String OWNER_BACK = "You hear someone outside spot the homeowner coming back!";
	public static final String STILL_NEAR = "You notice the owner is still near the door and decide not to enter.";
	public static final String ESCORTED = "You are escorted out of the house as the owner has locked the door.";
	public static final String SHINE = "You notice something shine somewhere else in the house.";
	public static final String NO_SPACE = "You need at least one free inventory space to steal from that.";

	private Houses()
	{
	}

	/** The three houses, by their owners. */
	public enum House
	{
		LAVINIA("Lavinia", "her", NpcID.VARLAMORE_THIEVING_HOUSE_OWNER_1, "directly south-west of the bazaar"),
		VICTOR("Victor", "his", NpcID.VARLAMORE_THIEVING_HOUSE_OWNER_2, "directly south-west of the western bank"),
		CAIUS("Caius", "his", NpcID.VARLAMORE_THIEVING_HOUSE_OWNER_3, "directly north-west of the western bank");

		private final String owner;
		private final String their;
		private final int npc;
		private final String where;

		House(String owner, String their, int npc, String where)
		{
			this.owner = owner;
			this.their = their;
			this.npc = npc;
			this.where = where;
		}

		public String owner()
		{
			return owner;
		}

		/** "her" or "his". */
		public String their()
		{
			return their;
		}

		public int npc()
		{
			return npc;
		}

		/** Where the wiki says it stands, after "The house". */
		public String where()
		{
			return where;
		}

		/** "Lavinia's house"; Caius' with the wiki's apostrophe. */
		public String title()
		{
			return possessive() + " house";
		}

		/** "Lavinia's", "Caius'". */
		public String possessive()
		{
			return owner + (owner.endsWith("s") ? "'" : "'s");
		}

		public String letter()
		{
			return owner.substring(0, 1);
		}

		public static House byOwner(int npcId)
		{
			for (House h : values())
			{
				if (h.npc == npcId)
				{
					return h;
				}
			}
			return null;
		}
	}

	/** Experience from this many valuables, at 45 each. */
	public static long xpFor(long valuables)
	{
		return Math.max(0, valuables) * XP_EACH;
	}

	/** Valuables for this much experience more, at 45 each. */
	public static long valuablesFor(long xp)
	{
		return xp <= 0 ? 0 : (xp + XP_EACH - 1) / XP_EACH;
	}

	/** The wiki's burgling experience an hour at a level: its table, straight between rows. */
	public static int wikiXpPerHour(int level)
	{
		int l = Math.max(LEVEL, Math.min(99, level));
		for (int i = 1; i < WIKI_XP.length; i++)
		{
			int[] a = WIKI_XP[i - 1];
			int[] b = WIKI_XP[i];
			if (l <= b[0])
			{
				return a[1] + (int) Math.round((b[1] - a[1]) * (l - a[0]) / (double) (b[0] - a[0]));
			}
		}
		return WIKI_XP[WIKI_XP.length - 1][1];
	}

	/** What Oriana pays for a valuable: 55, or 65 once she has paid that. */
	public static int rate(int learned)
	{
		return learned == ORIANA_PAYS_GLADIATOR ? ORIANA_PAYS_GLADIATOR : ORIANA_PAYS;
	}

	public static boolean isCitizen(int id)
	{
		return contains(CITIZENS, id);
	}

	public static boolean isDistracter(int id)
	{
		return contains(DISTRACTERS, id);
	}

	public static boolean isTeller(int id)
	{
		return contains(TELLERS, id);
	}

	public static boolean isOriana(int id)
	{
		return contains(ORIANA, id);
	}

	public static boolean isLoot(int id)
	{
		return contains(LOOT, id);
	}

	public static boolean isWindow(int id)
	{
		return contains(WINDOWS, id);
	}

	public static boolean isDoor(int id)
	{
		return id == DOOR_LOCKED || id == DOOR;
	}

	/** "M:SS" for seconds. */
	public static String clock(int seconds)
	{
		int s = Math.max(0, seconds);
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
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
