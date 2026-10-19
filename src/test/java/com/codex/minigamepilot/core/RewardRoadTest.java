package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RewardRoadTest
{
	private static Goals.Input owning(long balance, Integer... ids)
	{
		Set<Integer> set = new HashSet<>(Arrays.asList(ids));
		Goals.Input in = new Goals.Input();
		in.have = set::contains;
		in.balance = balance;
		return in;
	}

	@Test
	public void theMotherlodeRoadRunsInTheUsualOrderAndAddsUpAsItGoes()
	{
		// The whole kit owned, 64 nuggets held, 21 an hour.
		Goals.Input in = owning(64, 12013, 12014, 12015, 12016);
		in.currencyPerHour = 21;
		List<RewardRoad.Stop> road = RewardRoad.motherlode(in);
		assertEquals(4, road.size());
		assertEquals("Prospector kit", road.get(0).getName());
		assertTrue(road.get(0).isOwned());
		assertEquals(12013, road.get(0).getIcon());

		RewardRoad.Stop coal = road.get(1);
		assertEquals("Coal bag", coal.getName());
		assertEquals(100, coal.getCost());
		assertEquals(36, coal.getLeft());
		assertFalse(coal.isAffordable());
		assertEquals((long) Math.ceil(36 * 60.0 / 21), coal.getMinutesAway());

		// The gem bag comes after the coal bag: 200 in all, less the 64 held.
		assertEquals(136, road.get(2).getLeft());
		assertEquals(100, road.get(2).getCost());
		// The bigger sack has no item to show and is counted after both bags.
		assertEquals(0, road.get(3).getIcon());
		assertEquals(336, road.get(3).getLeft());
		assertTrue(RewardRoad.open(road));
	}

	@Test
	public void whatYouHoldLightsEveryStopItCoversInOrder()
	{
		Goals.Input in = owning(250, 12013, 12014, 12015, 12016);
		List<RewardRoad.Stop> road = RewardRoad.motherlode(in);
		assertTrue(road.get(1).isAffordable());
		assertTrue(road.get(2).isAffordable());
		assertEquals(0, road.get(2).getLeft());
		assertEquals(0, road.get(2).getMinutesAway());
		// 250 covers both bags but not the sack after them; with no pace there is no time.
		assertFalse(road.get(3).isAffordable());
		assertEquals(150, road.get(3).getLeft());
		assertEquals(-1, road.get(3).getMinutesAway());
	}

	@Test
	public void aPartlyOwnedKitCostsOnlyWhatIsMissing()
	{
		List<RewardRoad.Stop> road = RewardRoad.motherlode(owning(0, 12013, 12015));
		assertFalse(road.get(0).isOwned());
		assertEquals(60 + 30, road.get(0).getCost());
	}

	@Test
	public void theBiggerSackIsOwnedByItsVarbitAndAnUnknownBalanceLightsNothing()
	{
		Goals.Input in = owning(-1);
		in.ownedOtherwise = EnumSet.of(Goal.Reward.BIGGER_SACK);
		List<RewardRoad.Stop> road = RewardRoad.motherlode(in);
		assertTrue(road.get(3).isOwned());
		for (RewardRoad.Stop s : road.subList(0, 3))
		{
			assertFalse(s.isKnown());
			assertFalse(s.isAffordable());
			assertEquals(-1, s.getMinutesAway());
			assertEquals(s.getLeft(), s.getLeft());
		}
		assertEquals(180, road.get(0).getLeft());
	}

	@Test
	public void homesCountsContractsWhenThereIsNoHourlyPaceYet()
	{
		Goals.Input in = owning(612, 24882);
		in.currencyPerRound = 4;
		in.roundName = "adept contract";
		List<RewardRoad.Stop> road = RewardRoad.homes(in);
		assertEquals("Plank sack", road.get(0).getName());
		assertTrue(road.get(0).isOwned());
		assertEquals("Carpenter's outfit", road.get(1).getName());
		assertEquals(2000, road.get(1).getCost());
		assertEquals(1388, road.get(1).getLeft());
		assertEquals(347, road.get(1).getRoundsAway());
		assertEquals(-1, road.get(1).getMinutesAway());
		assertEquals(Goal.Currency.POINTS, road.get(1).getCurrency());
		assertEquals(1888, road.get(2).getLeft());
	}

	@Test
	public void aNavigatorLevelClosesTheRoadOnlyWhileItIsAhead()
	{
		Goals.Input in = owning(0);
		in.xp = Goals.xpFor(70);
		in.xpPerHour = 60_000;
		List<RewardRoad.Stop> road = RewardRoad.withLevel(RewardRoad.motherlode(in), "Mining", 72, "Dragon Slayer II", in);
		RewardRoad.Stop level = road.get(road.size() - 1);
		assertEquals("Mining 72", level.getName());
		assertEquals(72, level.getLevel());
		assertEquals(Goals.xpFor(72) - Goals.xpFor(70), level.getLeft());
		assertEquals("Dragon Slayer II", level.getReason());
		assertEquals((long) Math.ceil(level.getLeft() * 60.0 / 60_000), level.getMinutesAway());
		// Already there, or nonsense levels: nothing added.
		assertEquals(4, RewardRoad.withLevel(RewardRoad.motherlode(in), "Mining", 70, "x", in).size());
		assertEquals(4, RewardRoad.withLevel(RewardRoad.motherlode(in), "Mining", 120, "x", in).size());
		assertEquals(4, RewardRoad.withLevel(RewardRoad.motherlode(in), null, 80, "x", in).size());
	}

	@Test
	public void theFurnaceRoadIsTheLevelsThatOpenItsBarsFromTheLastReached()
	{
		Goals.Input in = owning(-1);
		in.xp = Goals.xpFor(52);
		in.xpPerHour = 80_000;
		List<RewardRoad.Stop> road = RewardRoad.furnace(in);
		assertEquals(Arrays.asList("Mithril bars", "No foreman fee", "Adamantite bars", "Runite bars"), names(road));
		assertTrue(road.get(0).isOwned());
		assertEquals(2359, road.get(0).getIcon());
		assertEquals(0, road.get(1).getIcon());
		assertEquals(60, road.get(1).getLevel());
		assertEquals(Goals.xpFor(70) - Goals.xpFor(52), road.get(2).getLeft());
		assertEquals((long) Math.ceil(road.get(2).getLeft() * 60.0 / 80_000), road.get(2).getMinutesAway());
		// A Navigator level slots in by level, and one already on the road takes the goal's name.
		List<RewardRoad.Stop> withGoal = RewardRoad.withLevel(road, "Smithing", 68, "Monkey Madness II", in);
		assertEquals(Arrays.asList("Mithril bars", "No foreman fee", "Smithing 68", "Adamantite bars", "Runite bars"), names(withGoal));
		List<RewardRoad.Stop> same = RewardRoad.withLevel(road, "Smithing", 70, "Dragon Slayer II", in);
		assertEquals(4, same.size());
		assertEquals("Dragon Slayer II", same.get(2).getReason());
		assertEquals("Adamantite bars", same.get(2).getName());
		assertEquals("Smithing 70", same.get(2).getLevelName());
		assertEquals("Smithing 68", withGoal.get(2).getLevelName());
		// Past runite only the last level is left, a road of one.
		in.xp = Goals.xpFor(90);
		assertEquals(Arrays.asList("Runite bars"), names(RewardRoad.furnace(in)));
		in.xp = 0;
		assertEquals("Iron bars", RewardRoad.furnace(in).get(0).getName());
		assertFalse(RewardRoad.furnace(in).get(0).isOwned());
	}

	private static List<String> names(List<RewardRoad.Stop> road)
	{
		List<String> out = new java.util.ArrayList<>();
		for (RewardRoad.Stop s : road)
		{
			out.add(s.getName());
		}
		return out;
	}

	@Test
	public void everythingOwnedLeavesNothingOpen()
	{
		Goals.Input in = owning(5, 24882, 24872, 24874, 24876, 24878, 24880, 24885);
		assertFalse(RewardRoad.open(RewardRoad.homes(in)));
		assertTrue(RewardRoad.of(Goals.HOMES_ORDER, null).isEmpty());
	}
}
