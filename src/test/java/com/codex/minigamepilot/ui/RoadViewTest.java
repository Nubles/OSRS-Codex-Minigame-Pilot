package com.codex.minigamepilot.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.RewardRoad;
import java.awt.Canvas;
import java.awt.FontMetrics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RoadViewTest
{
	private static List<RewardRoad.Stop> road(long balance, double perHour, Integer... owned)
	{
		Set<Integer> set = new HashSet<>(Arrays.asList(owned));
		Goals.Input in = new Goals.Input();
		in.have = set::contains;
		in.balance = balance;
		in.currencyPerHour = perHour;
		in.skill = "Mining";
		in.xp = Goals.xpFor(70);
		in.xpPerHour = 41_000;
		return RewardRoad.withLevel(RewardRoad.motherlode(in), "Mining", 72, "Dragon Slayer II", in);
	}

	@Test
	public void eachStopSaysWhereItStands()
	{
		List<RewardRoad.Stop> r = road(64, 21, 12013, 12014, 12015, 12016);
		assertEquals("Owned", RoadView.status(r.get(0), false));
		assertEquals(1, RoadView.next(r));
		assertEquals("36 to go, 1h 43m", RoadView.status(r.get(1), true));
		assertEquals("100 nuggets, in 6h 29m", RoadView.status(r.get(2), false));
		assertEquals("In 6h 29m", RoadView.status(r.get(2), false, true));
		assertEquals("162K xp, in 3h 57m", RoadView.status(r.get(4), false));
		assertEquals("Mining 72 is for Dragon Slayer II, a Codex Navigator goal.", RoadView.footnote(r));
		assertEquals("Mining 72: 162K xp, in 3h 57m. Dragon Slayer II needs it", RoadView.tip(r.get(4)));
		assertEquals("Gem bag: 100 nuggets, in 6h 29m, counting the stops above it first", RoadView.tip(r.get(2)));
	}

	@Test
	public void withNoPaceOrBalanceOnlyTheCostIsSaid()
	{
		List<RewardRoad.Stop> r = road(-1, 0);
		assertEquals("180 nuggets", RoadView.status(r.get(0), true));
		assertEquals("100 nuggets", RoadView.status(r.get(1), false));
		List<RewardRoad.Stop> rich = road(1_000, 0, 12013, 12014, 12015, 12016);
		assertEquals("Yours to buy now, 100 nuggets", RoadView.status(rich.get(1), true));
		assertEquals("Yours to buy now", RoadView.status(rich.get(1), true, true));
		assertEquals(-1, RoadView.next(Arrays.asList(rich.get(0))));
	}

	@Test
	public void bigExperienceReadsInMillions()
	{
		Goals.Input in = new Goals.Input();
		in.xp = 0;
		RewardRoad.Stop s = RewardRoad.withLevel(RewardRoad.motherlode(in), "Mining", 99, "x", in).get(4);
		assertEquals("13.0M xp to go", RoadView.status(s, true));
		assertNull(RoadView.footnote(RewardRoad.motherlode(in)));
	}

	@Test
	public void theRoadIsPartOfWhatTheViewSignsSoItRedraws()
	{
		View a = new View(true, View.Activity.MOTHERLODE, null, null, null, -1);
		View b = new View(true, View.Activity.MOTHERLODE, null, null, null, -1);
		assertEquals(a.signature(), b.signature());
		b.road(road(64, 21));
		assertNotEquals(a.signature(), b.signature());
	}

	@Test
	public void aLongChecklistLineDropsItsNoteToASecondLine()
	{
		FontMetrics fm = new Canvas().getFontMetrics(Theme.small());
		assertTrue(LineRow.fits(fm, new View.Line("Clock", "Done", View.Line.Mark.DONE), LineRow.ROOM));
		assertFalse(LineRow.fits(fm, new View.Line("Large oak bookshelf", "Remove, 3 planks", View.Line.Mark.TODO), LineRow.ROOM));
	}
}
