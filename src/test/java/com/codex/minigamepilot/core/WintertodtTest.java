package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Wintertodt.Unique;
import org.junit.Test;

/** The Wintertodt's figures against the OSRS wiki, and its HUD as the game's own client scripts write it. */
public class WintertodtTest
{
	@Test
	public void wikiFigures()
	{
		// "50 Firemaking is required to fight the Wintertodt"
		assertEquals(50, Wintertodt.FIREMAKING);
		// The points table: lighting 25, roots 10, kindling 25, repairing 25, healing a pyromancer 75.
		assertEquals(25, Wintertodt.LIGHT_POINTS);
		assertEquals(10, Wintertodt.ROOT_POINTS);
		assertEquals(25, Wintertodt.KINDLING_POINTS);
		assertEquals(25, Wintertodt.FIX_POINTS);
		// "The number of points awarded for healing pyromancers has been increased from 30 to 75."
		assertEquals(75, Wintertodt.HEAL_POINTS);
		// "Players must collect a minimum of 500 points to be rewarded anything for their efforts."
		assertEquals(500, Wintertodt.MIN_POINTS);
		// Food "which will restore 35% warmth"; the potion "restore 30% of your own warmth".
		assertEquals(35, Wintertodt.FOOD_WARMTH);
		assertEquals(30, Wintertodt.POTION_WARMTH);
		// "now always respawns in one minute": 100 ticks of 0.6 seconds.
		assertEquals(100, Wintertodt.RESPAWN_TICKS);
		// "run to the brazier when the number of logs is two less than the remaining energy."
		assertEquals(2, Wintertodt.FEED_MARGIN);
		// Big-search: "batches of 5 per click".
		assertEquals(5, Wintertodt.BIG_SEARCH);
	}

	@Test
	public void rollsAsTheRewardCartCountsThem()
	{
		// Under 500: "You did not earn enough points to be worthy of a gift".
		assertEquals(0, Wintertodt.sureRolls(499));
		assertEquals(0, Wintertodt.rolls(499), 0);
		// "500 points = 2 rolls."
		assertEquals(2, Wintertodt.sureRolls(500));
		assertEquals(0, Wintertodt.extraChance(500), 0);
		// "505 points = 2 rolls + 1% chance of an extra roll."
		assertEquals(2, Wintertodt.sureRolls(505));
		assertEquals(0.01, Wintertodt.extraChance(505), 1e-9);
		// "750 points = 2 rolls + 50% chance of an extra roll."
		assertEquals(0.5, Wintertodt.extraChance(750), 1e-9);
		// "1000 points = 3 rolls."; "1500 points = 4 rolls."
		assertEquals(3, Wintertodt.sureRolls(1000));
		assertEquals(4, Wintertodt.sureRolls(1500));
		// The Wintertodt page: "1200 points would guarantee 3 rewards plus a 200/500 (40%) chance of getting a fourth".
		assertEquals(3, Wintertodt.sureRolls(1200));
		assertEquals(0.4, Wintertodt.extraChance(1200), 1e-9);
		assertEquals(3.4, Wintertodt.rolls(1200), 1e-9);
		assertEquals(500, Wintertodt.toNextRoll(0));
		assertEquals(180, Wintertodt.toNextRoll(320));
		assertEquals(500, Wintertodt.toNextRoll(500));
		assertEquals(300, Wintertodt.toNextRoll(1200));
	}

	@Test
	public void uniquesInTheCartsOrder()
	{
		// The cart's order: Phoenix 1/5,000, dragon axe 1/10,000, tome of fire 1/1,000, warm gloves 1/150,
		// bruma torch 1/150, pyromancer outfit 1/150, burnt page 1/45.
		assertEquals(5000, Unique.PHOENIX.oneIn());
		assertEquals(10000, Unique.DRAGON_AXE.oneIn());
		assertEquals(1000, Unique.TOME_OF_FIRE.oneIn());
		assertEquals(150, Unique.WARM_GLOVES.oneIn());
		assertEquals(150, Unique.BRUMA_TORCH.oneIn());
		assertEquals(150, Unique.PYROMANCER.oneIn());
		assertEquals(45, Unique.BURNT_PAGE.oneIn());
		assertEquals(0, Unique.PHOENIX.ordinal());
		assertEquals(Unique.BURNT_PAGE.ordinal(), Unique.values().length - 1);
		// The wiki's effective rates: phoenix 1/5,000; bruma torch "about 1/151"; the outfit "about 1/152".
		assertEquals(5000, Math.round(1 / Unique.PHOENIX.perReward(false)));
		assertEquals(150, Math.round(1 / Unique.WARM_GLOVES.perReward(false)));
		assertEquals(151, Math.round(1 / Unique.BRUMA_TORCH.perReward(false)));
		assertEquals(152, Math.round(1 / Unique.PYROMANCER.perReward(false)));
		// "fifteen times more common" at 200 million Firemaking experience.
		assertEquals(15.0 / 5000, Unique.PHOENIX.perReward(true), 1e-12);
	}

	@Test
	public void oddsMaths()
	{
		double p = 1 / 150.0;
		assertEquals(150, Wintertodt.expectedRewards(p, 1), 1e-9);
		assertEquals(600, Wintertodt.expectedRewards(p, 4), 1e-9);
		assertEquals(104, Math.round(Wintertodt.evenOdds(p)));
		assertEquals(0.5, Wintertodt.chanceIn(p, Wintertodt.evenOdds(p)), 1e-9);
		assertEquals(0, Wintertodt.chanceIn(p, 0), 0);
		assertTrue(Double.isInfinite(Wintertodt.expectedRewards(0, 1)));
	}

	@Test
	public void hudAsTheGamesScriptWritesIt()
	{
		// wint_update: "Wintertodt's Energy: <n>%", "Your Warmth: <n>%", "Points<br><n>".
		assertEquals(45, Wintertodt.energy("Wintertodt's Energy: 45%"));
		assertEquals(0, Wintertodt.energy("Wintertodt's Energy: 0%"));
		assertEquals(-1, Wintertodt.energy("The Wintertodt returns in: 0:42"));
		assertEquals(72, Wintertodt.warmth("Your Warmth: 72%"));
		assertEquals(320, Wintertodt.points("Points<br>320"));
		assertEquals(1240, Wintertodt.points("Points 1240"));
		assertEquals(-1, Wintertodt.points(null));
		// wint_timer_countdown: "The Wintertodt returns in: <m>:<ss>".
		assertEquals(42, Wintertodt.returnsIn("The Wintertodt returns in: 0:42"));
		assertEquals(65, Wintertodt.returnsIn("The Wintertodt returns in: 1:05"));
		assertEquals(-1, Wintertodt.returnsIn("Wintertodt's Energy: 45%"));
		// The warmth varbit runs 0 to 1000; RuneLite's status bars divide it by 10.
		assertEquals(72, Wintertodt.warmthOf(725));
		assertEquals(100, Wintertodt.warmthOf(1000));
	}

	@Test
	public void ownedCodes()
	{
		assertEquals("", Wintertodt.decodeOwned(null));
		assertEquals("", Wintertodt.decodeOwned("GHW"));
		assertEquals("GHW", Wintertodt.decodeOwned("1;GHWGzz"));
	}
}
