package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class GoalTest
{
	private static Goals.Input owning(Integer... ids)
	{
		Set<Integer> set = new HashSet<>(Arrays.asList(ids));
		Goals.Input in = new Goals.Input();
		in.have = set::contains;
		in.skill = "Mining";
		return in;
	}

	@Test
	public void experienceTableMatchesTheGame()
	{
		assertEquals(0, Goals.xpFor(1));
		assertEquals(83, Goals.xpFor(2));
		assertEquals(13_034_431, Goals.xpFor(99));
		assertEquals(737_627, Goals.xpFor(70));
		for (int level = 1; level <= 99; level++)
		{
			assertEquals(net.runelite.api.Experience.getXpForLevel(level), Goals.xpFor(level));
		}
		assertEquals(69, Goals.levelFor(Goals.xpFor(69)));
		assertEquals(68, Goals.levelFor(Goals.xpFor(69) - 1));
		assertEquals(99, Goals.levelFor(200_000_000));
	}

	@Test
	public void nextRewardSkipsTheKitOnceItIsAllOwned()
	{
		Goals.Input in = owning(12013, 12014, 12015, 25555);
		in.balance = 30;
		Goal g = Goals.motherlode(Goals.Choice.NEXT_REWARD, in);
		assertEquals("Coal bag", g.getTitle());
		assertEquals(100, g.getNeed());
		assertEquals(70, g.toGo());
	}

	@Test
	public void kitCountsOnlyThePiecesStillMissing()
	{
		// The helmet is in the bank; the gilded boots count as boots.
		Goals.Input in = owning(12013, 25555);
		in.balance = 64;
		in.currencyPerHour = 12;
		Goal g = Goals.motherlode(Goals.Choice.NEXT_REWARD, in);
		assertEquals("Prospector kit", g.getTitle());
		assertEquals(110, g.getNeed());
		assertEquals(64, g.getHave());
		assertEquals(46, g.toGo());
		assertEquals(4, g.getPieces().size());
		assertTrue(g.getPieces().get(0).isOwned());
		assertFalse(g.getPieces().get(1).isOwned());
		assertEquals(230, g.minutesLeft());
		assertEquals("46 nuggets to go · about 3h 50m", g.describe());
	}

	@Test
	public void everythingOwnedBecomesTheNextLevel()
	{
		Goals.Input in = owning(12013, 12014, 12015, 12016, 12019, 24481);
		in.ownedOtherwise = EnumSet.of(Goal.Reward.BIGGER_SACK);
		in.xp = Goals.xpFor(80) + 1000;
		Goal g = Goals.motherlode(Goals.Choice.NEXT_REWARD, in);
		assertEquals("Mining level 81", g.getTitle());
		assertEquals(Goals.xpFor(81) - Goals.xpFor(80), g.getNeed());
		assertEquals(1000, g.getHave());
	}

	@Test
	public void homesGoalCountsContractsAtYourTier()
	{
		Goals.Input in = owning();
		in.balance = 100;
		in.currencyPerRound = 5;
		in.roundName = "expert contract";
		Goal g = Goals.homes(Goals.Choice.CARPENTERS_OUTFIT, in);
		assertEquals("Carpenter's outfit", g.getTitle());
		assertEquals(2000, g.getNeed());
		assertEquals(380, g.roundsLeft());
		assertEquals("1,900 points to go · 380 expert contracts", g.describe());
	}

	@Test
	public void enoughInHandIsDone()
	{
		Goals.Input in = owning();
		in.balance = 400;
		Goal g = Goals.homes(Goals.Choice.PLANK_SACK, in);
		assertTrue(g.done());
		assertEquals(350, g.getHave());
		assertEquals("You have enough: off to the shop", g.describe());
	}

	@Test
	public void otherShopsGoalFallsBackAndOffIsNothing()
	{
		Goals.Input in = owning();
		assertEquals("Plank sack", Goals.homes(Goals.Choice.COAL_BAG, in).getTitle());
		assertNull(Goals.homes(Goals.Choice.OFF, in));
		assertNull(Goals.homes(null, in));
	}

	@Test
	public void levelTargetsAndTheTop()
	{
		Goals.Input in = owning();
		in.skill = "Smithing";
		in.xp = Goals.xpFor(60);
		in.targetLevel = 70;
		in.xpPerHour = 100_000;
		Goal g = Goals.furnace(in);
		assertEquals("Smithing level 70", g.getTitle());
		assertEquals(Goals.xpFor(70) - Goals.xpFor(60), g.toGo());
		assertEquals((long) Math.ceil((Goals.xpFor(70) - Goals.xpFor(60)) * 60 / 100_000.0), g.minutesLeft());
		in.xp = Goals.xpFor(99);
		in.targetLevel = 0;
		assertNull(Goals.furnace(in));
	}

	@Test
	public void durations()
	{
		assertEquals("1m", Goal.duration(0));
		assertEquals("45m", Goal.duration(45));
		assertEquals("2h", Goal.duration(120));
		assertEquals("2h 10m", Goal.duration(130));
		assertEquals("120h", Goal.duration(7200));
	}
}
