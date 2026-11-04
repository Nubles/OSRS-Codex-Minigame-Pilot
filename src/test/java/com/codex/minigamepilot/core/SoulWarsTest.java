package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The wiki's Soul Wars facts, each as the wiki or the game's own scripts give it. */
public class SoulWarsTest
{
	@Test
	public void requirementsAndZeal()
	{
		// "Players require a total level of 500 and a combat level of 40 to play Soul Wars."
		assertEquals(500, SoulWars.TOTAL_LEVEL);
		assertEquals(40, SoulWars.COMBAT_LEVEL);
		// "up to 40 zeal" a loss, "up to 60 zeal" a win, "up to 50 zeal" a draw.
		assertEquals(40, SoulWars.LOSS_MAX);
		assertEquals(60, SoulWars.WIN_MAX);
		assertEquals(50, SoulWars.DRAW_MAX);
		assertEquals(15, SoulWars.GAME_MINUTES);
	}

	@Test
	public void experiencePerTokenFollowsTheShopsScript()
	{
		// The wiki's table: 35-42 gives 60 for melee, 54 for Magic and Ranged, 28 for Prayer; 98-99 gives 480, 432, 224.
		assertEquals(60, SoulWars.ZealSkill.ATTACK.xpPerToken(35));
		assertEquals(60, SoulWars.ZealSkill.HITPOINTS.xpPerToken(42));
		assertEquals(54, SoulWars.ZealSkill.MAGIC.xpPerToken(35));
		assertEquals(28, SoulWars.ZealSkill.PRAYER.xpPerToken(42));
		assertEquals(480, SoulWars.ZealSkill.STRENGTH.xpPerToken(98));
		assertEquals(480, SoulWars.ZealSkill.DEFENCE.xpPerToken(99));
		assertEquals(432, SoulWars.ZealSkill.RANGED.xpPerToken(99));
		assertEquals(224, SoulWars.ZealSkill.PRAYER.xpPerToken(99));
		// "Players must have at least level 30 in the chosen skill"; the script gives 0 below it.
		assertEquals(30, SoulWars.ZealSkill.ATTACK.xpPerToken(30));
		assertEquals(0, SoulWars.ZealSkill.ATTACK.xpPerToken(29));
		assertEquals(14, SoulWars.ZealSkill.PRAYER.xpPerToken(34));
		// floor(70^2 / 600) = 8.
		assertEquals(240, SoulWars.ZealSkill.ATTACK.xpPerToken(70));
	}

	@Test
	public void tokensToALevelAreBoughtALevelAtATime()
	{
		// 98 to 99 Attack: 13,034,431 - 11,805,606 = 1,228,825 xp at 480 a token.
		assertEquals(2_561, SoulWars.tokensTo(SoulWars.ZealSkill.ATTACK, Goals.xpFor(98), Goals.xpFor(99)));
		// Below 30 zeal buys nothing.
		assertEquals(-1, SoulWars.tokensTo(SoulWars.ZealSkill.ATTACK, Goals.xpFor(20), Goals.xpFor(31)));
		assertEquals(0, SoulWars.tokensTo(SoulWars.ZealSkill.PRAYER, Goals.xpFor(50), Goals.xpFor(50)));
		// Two levels: each at its own rate. 70 -> 71 at 240 a token, 71 -> 72 at floor(71^2/600)=8 -> 240.
		long a = SoulWars.tokensTo(SoulWars.ZealSkill.ATTACK, Goals.xpFor(70), Goals.xpFor(71));
		long b = SoulWars.tokensTo(SoulWars.ZealSkill.ATTACK, Goals.xpFor(70), Goals.xpFor(72));
		assertEquals((Goals.xpFor(71) - Goals.xpFor(70) + 239) / 240, a);
		assertTrue(b > a);
	}

	@Test
	public void rewardsAtNomadsPrices()
	{
		assertEquals(2_500, SoulWars.Reward.SOUL_CAPE.cost(false));
		assertEquals(250, SoulWars.Reward.ECTOPLASMATOR.cost(false));
		assertEquals(30, SoulWars.Reward.SPOILS.cost(false));
		assertEquals(500, SoulWars.Reward.SLAYER_HELMET.cost(false));
		assertEquals(320, SoulWars.Reward.SALVE.cost(false));
		assertEquals(300, SoulWars.Reward.SUFFERING.cost(false));
		assertEquals(260, SoulWars.Reward.RING.cost(false));
		assertEquals(200, SoulWars.Reward.GRANITE_RING.cost(false));
		// "The cost of imbuing items is halved if you have claimed the hard-tier Combat Achievements rewards."
		assertEquals(250, SoulWars.Reward.SLAYER_HELMET.cost(true));
		assertEquals(100, SoulWars.Reward.GRANITE_RING.cost(true));
		assertEquals(2_500, SoulWars.Reward.SOUL_CAPE.cost(true));
		// Lil' Creator: 1/400 a Spoils of War at 30 each, 12,000 on average.
		assertEquals(12_000, SoulWars.Reward.LIL_CREATOR.cost(false));
	}

	@Test
	public void petChance()
	{
		assertEquals(0, SoulWars.petChance(0), 1e-9);
		assertEquals(1 / 400.0, SoulWars.petChance(1), 1e-9);
		assertEquals(277, SoulWars.spoilsFor(0.5));
		assertTrue(SoulWars.petChance(277) >= 0.5 && SoulWars.petChance(276) < 0.5);
		assertEquals(0.632, SoulWars.petChance(400), 0.001);
	}

	@Test
	public void teams()
	{
		assertEquals(SoulWars.Team.BLUE, SoulWars.Team.byCape(SoulWars.BLUE_CAPE));
		assertEquals(SoulWars.Team.RED, SoulWars.Team.byCape(SoulWars.RED_CAPE));
		assertNull(SoulWars.Team.byCape(0));
		assertEquals(SoulWars.Team.BLUE, SoulWars.Team.byVarbit(1));
		assertEquals(SoulWars.Team.RED, SoulWars.Team.byVarbit(2));
		assertNull(SoulWars.Team.byVarbit(0));
		assertTrue(SoulWars.inArena(8749) && SoulWars.inArena(8493) && SoulWars.inArena(9005));
		assertTrue(!SoulWars.inArena(8748));
	}
}
