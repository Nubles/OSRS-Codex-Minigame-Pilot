package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import org.junit.Test;

public class BestVisitTest
{
	private static final long T = 1_760_000_000_000L;
	private static final long MIN = 60_000L;

	@Test
	public void aVisitIsMeasuredInUnitsAndExperienceAnHour()
	{
		BestVisit b = BestVisit.of(T, T + 72 * MIN, 72 * MIN, 1_944, 97_200);
		assertEquals(1_620, b.getPerHour());
		assertEquals(81_000, b.getXpPerHour());
		assertEquals("1,620 pay-dirt an hour", b.pace("pay-dirt"));
	}

	@Test
	public void aShortOrIdleVisitIsNoRecord()
	{
		assertNull(BestVisit.of(T, T, BestVisit.MIN_MS - 1, 400, 20_000));
		assertNull(BestVisit.of(T, T, 60 * MIN, 0, 20_000));
		assertNull(BestVisit.of(0, T, 60 * MIN, 400, 20_000));
	}

	@Test
	public void onlyBeatingAnotherSessionIsNews()
	{
		BestVisit held = BestVisit.of(T, T + 60 * MIN, 60 * MIN, 1_500, 0);
		BestVisit slower = BestVisit.of(T + 1, T + 60 * MIN, 60 * MIN, 1_400, 0);
		BestVisit faster = BestVisit.of(T + 1, T + 60 * MIN, 60 * MIN, 1_600, 0);
		BestVisit sameSessionFaster = BestVisit.of(T, T + 90 * MIN, 60 * MIN, 1_600, 0);
		assertEquals(BestVisit.Outcome.QUIET, BestVisit.judge(null, held));
		assertEquals(BestVisit.Outcome.KEPT, BestVisit.judge(held, null));
		assertEquals(BestVisit.Outcome.KEPT, BestVisit.judge(held, slower));
		assertEquals(BestVisit.Outcome.KEPT, BestVisit.judge(held, held));
		assertEquals(BestVisit.Outcome.BEATEN, BestVisit.judge(held, faster));
		// Stepping out of the same session a second time, faster, updates the record quietly.
		assertEquals(BestVisit.Outcome.QUIET, BestVisit.judge(held, sameSessionFaster));
		assertEquals("A new best: 1,600 pay-dirt an hour, up from 1,500.", faster.beat(held, "pay-dirt"));
	}

	@Test
	public void theCardSaysWhenAndHowLongUnlessItWasTheLastVisit()
	{
		long end = T + 72 * MIN;
		BestVisit b = BestVisit.of(T, end, 72 * MIN, 1_944, 97_200);
		assertEquals("Best visit 3 days ago: 1,620 pay-dirt and 81k Mining xp an hour over 1h 12m.",
			b.status("pay-dirt", "Mining", null, end + 72 * 3_600_000L));
		Visit last = Visit.of(end, 72 * MIN, Collections.singletonList("1,944 pay-dirt"));
		assertEquals("Your best visit yet, at 1,620 pay-dirt and 81k Mining xp an hour.",
			b.status("pay-dirt", "Mining", last, end + MIN));
		BestVisit noXp = BestVisit.of(T, end, 20 * MIN, 300, 0);
		assertEquals("Best visit 3 days ago: 900 bars an hour over 20m.", noXp.status("bars", "Smithing", null, end + 72 * 3_600_000L));
	}

	@Test
	public void survivesBeingSavedAndADamagedSave()
	{
		BestVisit b = BestVisit.of(T, T + 72 * MIN, 72 * MIN, 1_944, 97_200);
		BestVisit back = BestVisit.decode(b.encode());
		assertEquals(b.encode(), back.encode());
		assertEquals(BestVisit.Outcome.KEPT, BestVisit.judge(back, b));
		assertNull(BestVisit.decode(null));
		assertNull(BestVisit.decode(""));
		assertNull(BestVisit.decode("1|2|3|4"));
		assertNull(BestVisit.decode("a|b|c|d|e"));
		assertNull(BestVisit.decode(T + "|" + (T - 1) + "|20|300|0"));
		assertNull(BestVisit.decode(T + "|" + T + "|3|300|0"));
		assertNull(BestVisit.decode(T + "|" + T + "|20|0|0"));
	}
}
