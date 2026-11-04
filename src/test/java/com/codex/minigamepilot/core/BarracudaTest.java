package com.codex.minigamepilot.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Barracuda.Rank;
import com.codex.minigamepilot.core.Barracuda.Trial;
import org.junit.Test;

/** The Barracuda Trials' facts against the wiki's pages, and their ids against RuneLite's gameval. */
public class BarracudaTest
{
	private static int ticks(int seconds)
	{
		assertEquals(0, seconds * 10 % 6);
		return seconds * 10 / 6;
	}

	@Test
	public void levelsAndMasters()
	{
		// Barracuda Trials: "30 | The Tempor Tantrum | Rum-dashed Ralph", "55 | The Jubbly Jive | Gurtob",
		// "72 | The Gwenith Glide | Gwyna" with Regicide.
		assertEquals(30, Trial.TEMPOR_TANTRUM.level());
		assertEquals(55, Trial.JUBBLY_JIVE.level());
		assertEquals(72, Trial.GWENITH_GLIDE.level());
		assertEquals("Rum-dashed Ralph", Trial.TEMPOR_TANTRUM.master());
		assertEquals("Gurtob", Trial.JUBBLY_JIVE.master());
		assertEquals("Gwyna", Trial.GWENITH_GLIDE.master());
		assertEquals("Regicide", Trial.GWENITH_GLIDE.quest());
		assertNull(Trial.TEMPOR_TANTRUM.quest());
		assertEquals("skiff", Barracuda.BOAT);
	}

	@Test
	public void rankTimes()
	{
		// Each reward's page: "requiring a completion time of ..."
		assertEquals(ticks(108), Trial.TEMPOR_TANTRUM.targetTicks(Rank.SWORDFISH));   // Stormy key 1:48.00
		assertEquals(ticks(171), Trial.TEMPOR_TANTRUM.targetTicks(Rank.SHARK));       // Barrel stand 2:51.00
		assertEquals(ticks(270), Trial.TEMPOR_TANTRUM.targetTicks(Rank.MARLIN));      // Ralph's fabric roll 4:30.00
		assertEquals(ticks(123), Trial.JUBBLY_JIVE.targetTicks(Rank.SWORDFISH));      // Fetid key 2:03.00
		assertEquals(ticks(180), Trial.JUBBLY_JIVE.targetTicks(Rank.SHARK));          // Captured wind mote 3:00.00
		assertEquals(ticks(321), Trial.JUBBLY_JIVE.targetTicks(Rank.MARLIN));         // Gurtob's fabric roll 5:21.00
		assertEquals(ticks(120), Trial.GWENITH_GLIDE.targetTicks(Rank.SWORDFISH));    // Serrated key 2:00.00
		assertEquals(ticks(222), Trial.GWENITH_GLIDE.targetTicks(Rank.SHARK));        // Heart of Ithell 3:42.00
		assertEquals(ticks(369), Trial.GWENITH_GLIDE.targetTicks(Rank.MARLIN));       // Gwyna's fabric roll 6:09.00
	}

	@Test
	public void rewardsAndBonus()
	{
		assertEquals("Stormy key", Trial.TEMPOR_TANTRUM.reward(Rank.SWORDFISH));
		assertEquals("Barrel stand and Whirlpool surprise", Trial.TEMPOR_TANTRUM.reward(Rank.SHARK));
		assertEquals("Ralph's fabric roll", Trial.TEMPOR_TANTRUM.reward(Rank.MARLIN));
		assertEquals("Fetid key", Trial.JUBBLY_JIVE.reward(Rank.SWORDFISH));
		assertEquals("Captured wind mote", Trial.JUBBLY_JIVE.reward(Rank.SHARK));
		assertEquals("Gurtob's fabric roll", Trial.JUBBLY_JIVE.reward(Rank.MARLIN));
		assertEquals("Serrated key", Trial.GWENITH_GLIDE.reward(Rank.SWORDFISH));
		assertEquals("Heart of Ithell", Trial.GWENITH_GLIDE.reward(Rank.SHARK));
		assertEquals("Gwyna's fabric roll", Trial.GWENITH_GLIDE.reward(Rank.MARLIN));
		// The Tempor Tantrum's table: Bonus XP 1,000, 2,000, 3,000.
		assertEquals(1_000, Trial.TEMPOR_TANTRUM.bonusXp(Rank.SWORDFISH));
		assertEquals(2_000, Trial.TEMPOR_TANTRUM.bonusXp(Rank.SHARK));
		assertEquals(3_000, Trial.TEMPOR_TANTRUM.bonusXp(Rank.MARLIN));
		// The Jubbly Jive: "a one time bonus of 5,000", "7,500", "10,000" Sailing experience.
		assertEquals(5_000, Trial.JUBBLY_JIVE.bonusXp(Rank.SWORDFISH));
		assertEquals(7_500, Trial.JUBBLY_JIVE.bonusXp(Rank.SHARK));
		assertEquals(10_000, Trial.JUBBLY_JIVE.bonusXp(Rank.MARLIN));
		// The Gwenith Glide's bonus is not in the wiki's text: none is claimed.
		for (Rank k : Rank.values())
		{
			assertEquals(-1, Trial.GWENITH_GLIDE.bonusXp(k));
		}
		// "If the trial area is left, the player has 10 seconds to return": 16.7 ticks, rounded up.
		assertEquals(17, Barracuda.RETURN_TICKS);
	}

	@Test
	public void runeliteIds()
	{
		// NpcID: SAILING_BT_TRIAL_MASTER_1_* Rum-dashed Ralph 15094-15097, _2_* Gurtob 15098-15101 and
		// Ros 15102-15104, _3_* Gwyna 15105-15108.
		assertArrayEquals(new int[]{15094, 15095, 15096, 15097}, Trial.TEMPOR_TANTRUM.npcs());
		assertArrayEquals(new int[]{15098, 15099, 15100, 15101, 15102, 15103, 15104}, Trial.JUBBLY_JIVE.npcs());
		assertArrayEquals(new int[]{15105, 15106, 15107, 15108}, Trial.GWENITH_GLIDE.npcs());
		// VarbitID: SAILING_BT_IN_TRIAL 18410; _TEMPOR_TANTRUM_MASTER_STATE 18427, _SWORDFISH_FIRST 18429 ...
		assertEquals(18410, Barracuda.IN_TRIAL);
		assertEquals(18427, Trial.TEMPOR_TANTRUM.masterStateVarbit());
		assertEquals(18432, Trial.JUBBLY_JIVE.masterStateVarbit());
		assertEquals(18437, Trial.GWENITH_GLIDE.masterStateVarbit());
		assertEquals(18429, Trial.TEMPOR_TANTRUM.firstVarbit(Rank.SWORDFISH));
		assertEquals(18436, Trial.JUBBLY_JIVE.firstVarbit(Rank.MARLIN));
		assertEquals(18440, Trial.GWENITH_GLIDE.firstVarbit(Rank.SHARK));
		// VarPlayerID: SAILING_BT_TRIAL_TEMPOR_TANTRUM_COMPLETED 4998, _JUBBLY_JIVE_ 4999, _GWENITH_GLIDE_ 5000.
		assertEquals(4998, Trial.TEMPOR_TANTRUM.completedVarp());
		assertEquals(4999, Trial.JUBBLY_JIVE.completedVarp());
		assertEquals(5000, Trial.GWENITH_GLIDE.completedVarp());
		// InterfaceID.SailingBtHud: group 931, BT_TITLE 8, BT_CURRENT_TIME 16, BT_RANK_TIME 17,
		// BT_TRACKER_PROGRESS 24, BT_OPTIONAL_PROGRESS 25.
		assertEquals((931 << 16) | 8, Barracuda.HUD_TITLE);
		assertEquals((931 << 16) | 16, Barracuda.HUD_TIME);
		assertEquals((931 << 16) | 17, Barracuda.HUD_RANK_TIME);
		assertEquals((931 << 16) | 24, Barracuda.HUD_PROGRESS);
		assertEquals((931 << 16) | 25, Barracuda.HUD_OPTIONAL);
	}

	@Test
	public void readsTheTrialsText()
	{
		assertEquals(180, Barracuda.ticks("1:48.00"));
		assertEquals(180, Barracuda.ticks("<col=ffffff>1:48</col>"));
		assertEquals(10_830, Barracuda.centis("1:48.3"));
		assertEquals(-1, Barracuda.ticks("Time"));
		assertEquals(-1, Barracuda.ticks(null));
		assertEquals(-1, Barracuda.ticks("1:75"));
		assertArrayEquals(new int[]{3, 20}, Barracuda.progress("Crates: 3/20"));
		assertArrayEquals(new int[]{4, 23}, Barracuda.progress("1 / 3<br>3/20"));
		assertArrayEquals(new int[]{-1, -1}, Barracuda.progress("5/3"));
		assertArrayEquals(new int[]{-1, -1}, Barracuda.progress(null));
		assertEquals(Trial.JUBBLY_JIVE, Trial.named("You prepare to begin the Jubbly Jive."));
		assertEquals(Trial.GWENITH_GLIDE, Trial.named("<col=ff0000>The Gwenith Glide</col>"));
		assertNull(Trial.named("Sailing"));
		assertEquals(Rank.SHARK, Trial.TEMPOR_TANTRUM.rankFor(285));
		assertNull(Trial.TEMPOR_TANTRUM.rankFor(300));
		assertEquals("1:48", Barracuda.target(180));
		assertEquals("1:42.6", Barracuda.clock(171));
		assertEquals("1.2s behind", Barracuda.gap(2));
		assertEquals("6.0s ahead", Barracuda.gap(-10));
		assertEquals("level", Barracuda.gap(0));
		assertTrue(Barracuda.STARTING.startsWith("You prepare"));
	}
}
