package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class VisitTest
{
	private static final long NOW = 1_760_000_000_000L;

	@Test
	public void saysWhatASessionCameTo()
	{
		Visit v = Visit.of(NOW, 72 * 60_000L, Arrays.asList("318 pay-dirt", "21,400 Mining xp", "ore worth 48K"));
		assertEquals("1h 12m: 318 pay-dirt, 21,400 Mining xp and ore worth 48K.", v.line());
		assertEquals("Last visit 3 hours ago: 1h 12m for 318 pay-dirt, 21,400 Mining xp and ore worth 48K.",
			v.status(NOW + 3 * 3_600_000L));
		assertEquals("52m: 400 bars.", Visit.of(NOW, 52 * 60_000L, Collections.singletonList("400 bars")).line());
	}

	@Test
	public void aShortOrIdleSessionHasNoSummary()
	{
		assertNull(Visit.of(NOW, Visit.MIN_MS - 1, Collections.singletonList("3 pay-dirt")));
		assertNull(Visit.of(NOW, 60 * 60_000L, Collections.emptyList()));
		assertNull(Visit.of(NOW, 60 * 60_000L, Arrays.asList("", null, " ")));
	}

	@Test
	public void survivesBeingSavedAndADamagedSave()
	{
		Visit v = Visit.of(NOW, 30 * 60_000L, Arrays.asList("12 bars", "a|b;c"));
		Visit back = Visit.decode(v.encode());
		assertEquals(v.line(), back.line());
		assertEquals(NOW, back.getEndedAt());
		assertEquals("30m: 12 bars and a b,c.", back.line());
		assertNull(Visit.decode(null));
		assertNull(Visit.decode(""));
		assertNull(Visit.decode("x|3|bars"));
		assertNull(Visit.decode("100|3"));
		assertNull(Visit.decode("100|3|"));
	}
}
