package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.BarPick;
import java.util.Collections;
import org.junit.Test;

public class FurnacePickTest
{
	@Test
	public void coinsAreShortenedWithTheirSign()
	{
		assertEquals("+9,870", FurnacePilot.compact(9_870));
		assertEquals("+71k", FurnacePilot.compact(70_552));
		assertEquals("-104k", FurnacePilot.compact(-104_276));
		assertEquals("+12.4M", FurnacePilot.compact(12_400_000));
		assertEquals("+1.0M", FurnacePilot.compact(999_700));
		assertEquals("+0", FurnacePilot.compact(0));
	}

	@Test
	public void noPricesNoCard()
	{
		assertNull(FurnacePilot.card(Collections.<BarPick.Row>emptyList(), Bar.STEEL, 0, 50));
	}

	@Test
	public void typicalTripNeedsThreeRounds()
	{
		com.codex.minigamepilot.core.Pace pace = new com.codex.minigamepilot.core.Pace();
		assertEquals(0, Pilots.typicalTicks(pace));
	}
}
