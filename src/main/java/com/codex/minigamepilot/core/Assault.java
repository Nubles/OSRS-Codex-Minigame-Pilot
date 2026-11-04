package com.codex.minigamepilot.core;

import java.util.Locale;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of Barbarian Assault, at the Barbarian Outpost: its four roles and their honour
 * points, the role levels and what each costs, the waves of a game, and Commander Connad's
 * rewards with their prices in each role.
 *
 * <p>Figures are from the OSRS wiki (Barbarian Assault; Honour points; Fighter torso; Granite
 * body), quoted beside each. Ids are RuneLite's gameval names. How a role's points are kept in
 * two varbits, and how a role's level is kept, is read from the game's own client scripts as
 * RuneLite publishes them (runelite/cs2-scripts: scripts 1367, 1371 to 1374); the wave's chat
 * line and the role's horn interface are as RuneLite's own Barbarian Assault plugin reads them.
 *
 * <p>Nothing here is about the fight itself: the calls, the eggs and the healers are left to
 * RuneLite's Barbarian Assault plugin.
 */
public final class Assault
{
	/** "battle ten waves of monsters"; "On Wave 10, the Penance Queen will spawn." */
	public static final int WAVES = 10;
	/** "For completing wave 10, each player gets 80 points for their role plus 5 points in each of the other 3 roles". */
	public static final int QUEEN_ROLE_POINTS = 80;
	public static final int QUEEN_OTHER_POINTS = 5;
	/** "Points cap at 5,000 in each role." */
	public static final int CAP = 5_000;
	/** "Players may upgrade the level in a particular role from level 1 up to level 5." */
	public static final int MAX_LEVEL = 5;
	/** The wiki's level table, by level: 200, 300, 400 and 500 honour points for levels 2 to 5, the same in every role. */
	private static final int[] LEVEL_COST = {0, 0, 200, 300, 400, 500};
	/** "Players will get 10% more points (rounded down) if they have completed the Hard Kandarin Diary." */
	public static final int KANDARIN_BONUS_PERCENT = 10;
	/** The granite body: "for 95,000 coins"; "the only reward ... that is not obtained with honour points". */
	public static final int GRANITE_BODY_COINS = 95_000;
	/**
	 * A role's points are two varbits: the game's own script reads
	 * {@code %barbassault_points_attacker_extra * 512 + %barbassault_points_attacker_base}.
	 */
	public static final int EXTRA_UNIT = 512;

	/** Commander Connad, who keeps the rewards shop: "Players can access their rewards by speaking to Commander Connad". */
	public static final int CONNAD = NpcID.BARBASSAULT_BARBARIAN_COMMANDER;
	/** The table in the lobby where a team's leader takes the recruitment scroll. */
	public static final int SCROLL_TABLE = ObjectID.BARBASSAULT_SCROLL_TABLE;
	/** The interface shown when a wave is complete, with its points. */
	public static final int WAVE_COMPLETE = InterfaceID.BarbassaultWavecomplete.ROOT_MODEL0;
	/** Each wave starts with this game message, "---- Wave: 3 ----" (as RuneLite's plugin reads it). */
	static final String WAVE_LINE = "---- Wave:";

	private Assault()
	{
	}

	/** The four roles: "There are four roles in total: attacker, defender, healer, and collector." */
	public enum Role
	{
		ATTACKER("Attacker", 'A', VarbitID.BARBASSAULT_POINTS_ATTACKER_BASE, VarbitID.BARBASSAULT_POINTS_ATTACKER_EXTRA,
			VarbitID.BARBASSULT_ROLELEVEL_ATT, InterfaceID.BarbassaultOverAtt.BARBASSAULT_ATTACKER_HORN_TEXT),
		COLLECTOR("Collector", 'C', VarbitID.BARBASSAULT_POINTS_COLLECTOR_BASE, VarbitID.BARBASSAULT_POINTS_COLLECTOR_EXTRA,
			VarbitID.BARBASSULT_ROLELEVEL_COL, InterfaceID.BarbassaultOverCol.BARBASSAULT_COLLECTOR_HORN_TEXT),
		DEFENDER("Defender", 'D', VarbitID.BARBASSAULT_POINTS_DEFENDER_BASE, VarbitID.BARBASSAULT_POINTS_DEFENDER_EXTRA,
			VarbitID.BARBASSULT_ROLELEVEL_DEF, InterfaceID.BarbassaultOverDef.BARBASSAULT_DEFENDER_HORN_TEXT),
		HEALER("Healer", 'H', VarbitID.BARBASSAULT_POINTS_HEALER_BASE, VarbitID.BARBASSAULT_POINTS_HEALER_EXTRA,
			VarbitID.BARBASSULT_ROLELEVEL_HEAL, InterfaceID.BarbassaultOverHeal.BARBASSAULT_HEALER_HORN_TEXT);

		private final String title;
		private final char code;
		private final int base;
		private final int extra;
		private final int level;
		private final int horn;

		Role(String title, char code, int base, int extra, int level, int horn)
		{
			this.title = title;
			this.code = code;
			this.base = base;
			this.extra = extra;
			this.level = level;
			this.horn = horn;
		}

		public String getTitle()
		{
			return title;
		}

		public String lower()
		{
			return title.toLowerCase(Locale.ROOT);
		}

		public char code()
		{
			return code;
		}

		/** The varbit with the low part of this role's points. */
		public int baseVarbit()
		{
			return base;
		}

		/** The varbit with the high part, in units of 512. */
		public int extraVarbit()
		{
			return extra;
		}

		/** The varbit with this role's level: the game's script shows "level up to" it plus one, and "Mastered" from 5. */
		public int levelVarbit()
		{
			return level;
		}

		/** The text of the role's horn interface, shown in a wave while you play this role. */
		public int horn()
		{
			return horn;
		}

		public static Role of(char c)
		{
			for (Role r : values())
			{
				if (r.code == c)
				{
					return r;
				}
			}
			return null;
		}
	}

	/** A role's points from its two varbits, as the game's script adds them. */
	public static int points(int base, int extra)
	{
		return Math.max(0, extra) * EXTRA_UNIT + Math.max(0, base);
	}

	/** A role's level from its varbit: 1 to 5 (an account that never played reads 0, which is level 1). */
	public static int level(int varbit)
	{
		return Math.max(1, Math.min(MAX_LEVEL, varbit));
	}

	/** Honour points to raise a role from {@code level - 1} to {@code level}: 200 to 500, or 0 out of range. */
	public static int levelCost(int level)
	{
		return level >= 2 && level <= MAX_LEVEL ? LEVEL_COST[level] : 0;
	}

	/** Honour points still to spend in a role to bring it from {@code level} to 5. */
	public static int toMastered(int level)
	{
		int n = 0;
		for (int l = Math.max(1, level) + 1; l <= MAX_LEVEL; l++)
		{
			n += LEVEL_COST[l];
		}
		return n;
	}

	/** The wave a "---- Wave: N ----" message starts, or -1 for any other message. */
	public static int waveOf(String message)
	{
		if (message == null || !message.startsWith(WAVE_LINE))
		{
			return -1;
		}
		String rest = message.substring(WAVE_LINE.length()).replace("-", " ").trim();
		int end = 0;
		while (end < rest.length() && Character.isDigit(rest.charAt(end)))
		{
			end++;
		}
		if (end == 0 || end > 2)
		{
			return -1;
		}
		int n = Integer.parseInt(rest.substring(0, end));
		return n >= 1 && n <= WAVES ? n : -1;
	}

	/**
	 * Commander Connad's rewards. Each costs its price "each role" ("Points from all roles are
	 * required"), and the wiki's table marks those that also need a Penance Queen kill. The level
	 * goal is every role to level 5, at the level table's prices.
	 */
	public enum Reward
	{
		FIGHTER_TORSO("Fighter torso", 375, true, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO_BROKEN,
			ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO_TROUVER, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO_TROUVER_BROKEN,
			ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO_TROUVER_MANGLED),
		PENANCE_SKIRT("Penance skirt", 375, true, ItemID.BARBASSAULT_PENANCE_RANGER_LEGS, ItemID.BARBASSAULT_PENANCE_RANGER_LEGS_BROKEN,
			ItemID.BARBASSAULT_PENANCE_RANGER_LEGS_TROUVER),
		FIGHTER_HAT("Fighter hat", 275, true, ItemID.BARBASSAULT_PENANCE_FIGHTER_HAT, ItemID.BARBASSAULT_PENANCE_FIGHTER_HAT_BROKEN,
			ItemID.BARBASSAULT_PENANCE_FIGHTER_HAT_TROUVER),
		RANGER_HAT("Ranger hat", 275, true, ItemID.BARBASSAULT_PENANCE_RANGER_HAT, ItemID.BARBASSAULT_PENANCE_RANGER_HAT_BROKEN,
			ItemID.BARBASSAULT_PENANCE_RANGER_HAT_TROUVER),
		RUNNER_HAT("Runner hat", 275, true, ItemID.BARBASSAULT_PENANCE_RUNNER_HAT, ItemID.BARBASSAULT_PENANCE_RUNNER_HAT_BROKEN,
			ItemID.BARBASSAULT_PENANCE_RUNNER_HAT_TROUVER),
		HEALER_HAT("Healer hat", 275, true, ItemID.BARBASSAULT_PENANCE_HEALER_HAT, ItemID.BARBASSAULT_PENANCE_HEALER_HAT_BROKEN,
			ItemID.BARBASSAULT_PENANCE_HEALER_HAT_TROUVER),
		RUNNER_BOOTS("Runner boots", 100, false, ItemID.BARBASSAULT_PENANCE_RUNNER_BOOTS),
		PENANCE_GLOVES("Penance gloves", 150, false, ItemID.BARBASSAULT_PENANCE_GLOVES, ItemID.BARBASSAULT_PENANCE_GLOVES_WORN),
		GRANITE_BODY("Granite body", 0, true, ItemID.GRANITE_BODY),
		ROLE_LEVELS("Level 5 in every role", 0, false);

		private final String title;
		private final int price;
		private final boolean queen;
		private final int[] items;

		Reward(String title, int price, boolean queen, int... items)
		{
			this.title = title;
			this.price = price;
			this.queen = queen;
			this.items = items;
		}

		public String getTitle()
		{
			return title;
		}

		/** Honour points in each of the four roles; 0 for the granite body (coins) and the level goal (by level). */
		public int price()
		{
			return price;
		}

		/** Whether it also needs a Penance Queen kill: "Certain items require the player to kill the Penance Queen." */
		public boolean queen()
		{
			return queen;
		}

		/** The items that show it owned, broken and locked copies too; none for the level goal. */
		public int[] items()
		{
			return items.clone();
		}

		/** Points still to find in a role at this level for this reward: its price, or the levels left to 5. */
		public int cost(int level)
		{
			return this == ROLE_LEVELS ? toMastered(level) : price;
		}
	}
}
