package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Assault.Reward;
import com.codex.minigamepilot.core.Assault.Role;
import org.junit.Test;

/** Barbarian Assault's figures against the OSRS wiki and the game's own client scripts. */
public class AssaultTest
{
	@Test
	public void wikiFigures()
	{
		// "battle ten waves of monsters"; "On Wave 10, the Penance Queen will spawn."
		assertEquals(10, Assault.WAVES);
		// "each player gets 80 points for their role plus 5 points in each of the other 3 roles"
		assertEquals(80, Assault.QUEEN_ROLE_POINTS);
		assertEquals(5, Assault.QUEEN_OTHER_POINTS);
		// "Points cap at 5,000 in each role."
		assertEquals(5000, Assault.CAP);
		// "10% more points (rounded down) if they have completed the Hard Kandarin Diary."
		assertEquals(10, Assault.KANDARIN_BONUS_PERCENT);
		// Granite body: "for 95,000 coins".
		assertEquals(95_000, Assault.GRANITE_BODY_COINS);
	}

	@Test
	public void levelCosts()
	{
		// The wiki's level table: 200, 300, 400, 500 honour points for levels 2 to 5, in every role.
		assertEquals(0, Assault.levelCost(1));
		assertEquals(200, Assault.levelCost(2));
		assertEquals(300, Assault.levelCost(3));
		assertEquals(400, Assault.levelCost(4));
		assertEquals(500, Assault.levelCost(5));
		assertEquals(0, Assault.levelCost(6));
		assertEquals(1400, Assault.toMastered(1));
		assertEquals(900, Assault.toMastered(3));
		assertEquals(0, Assault.toMastered(5));
		// The game's script: "level up to <rolelevel + 1>" below 5, "Mastered" from 5; a fresh account reads 0.
		assertEquals(1, Assault.level(0));
		assertEquals(3, Assault.level(3));
		assertEquals(5, Assault.level(5));
	}

	@Test
	public void pointsFromTwoVarbits()
	{
		// script1371: "%barbassault_points_attacker_extra * 512 + %barbassault_points_attacker_base"
		assertEquals(0, Assault.points(0, 0));
		assertEquals(511, Assault.points(511, 0));
		assertEquals(512, Assault.points(0, 1));
		assertEquals(4_999, Assault.points(4999 - 9 * 512, 9));
	}

	@Test
	public void rewardPrices()
	{
		// The wiki's armour table, "Price sold at", each role, and its "Queen" column.
		assertEquals(375, Reward.FIGHTER_TORSO.price());
		assertTrue(Reward.FIGHTER_TORSO.queen());
		assertEquals(375, Reward.PENANCE_SKIRT.price());
		assertTrue(Reward.PENANCE_SKIRT.queen());
		for (Reward hat : new Reward[]{Reward.FIGHTER_HAT, Reward.RANGER_HAT, Reward.RUNNER_HAT, Reward.HEALER_HAT})
		{
			assertEquals(275, hat.price());
			assertTrue(hat.queen());
		}
		assertEquals(100, Reward.RUNNER_BOOTS.price());
		assertFalse(Reward.RUNNER_BOOTS.queen());
		assertEquals(150, Reward.PENANCE_GLOVES.price());
		assertFalse(Reward.PENANCE_GLOVES.queen());
		// Granite body: coins, "the only reward ... not obtained with honour points", and a queen kill.
		assertEquals(0, Reward.GRANITE_BODY.price());
		assertTrue(Reward.GRANITE_BODY.queen());
		// The wiki: "For all items, one would need 2,100 points in every single one of the four different roles".
		int all = 0;
		for (Reward r : Reward.values())
		{
			all += r.price();
		}
		assertEquals(2100, all);
		// The level goal costs a role what its levels to 5 cost.
		assertEquals(1400, Reward.ROLE_LEVELS.cost(1));
		assertEquals(500, Reward.ROLE_LEVELS.cost(4));
		assertEquals(375, Reward.FIGHTER_TORSO.cost(2));
	}

	@Test
	public void waveLine()
	{
		// RuneLite's plugin: a GAMEMESSAGE starting "---- Wave:", the number its third word.
		assertEquals(1, Assault.waveOf("---- Wave: 1 ----"));
		assertEquals(10, Assault.waveOf("---- Wave: 10 ----"));
		assertEquals(-1, Assault.waveOf("---- Wave: 11 ----"));
		assertEquals(-1, Assault.waveOf("---- Wave: ----"));
		assertEquals(-1, Assault.waveOf("Wave 1 duration: 1:00"));
		assertEquals(-1, Assault.waveOf(null));
	}

	@Test
	public void roles()
	{
		assertEquals(4, Role.values().length);
		for (Role r : Role.values())
		{
			assertEquals(r, Role.of(r.code()));
		}
		assertEquals(null, Role.of('x'));
	}
}
