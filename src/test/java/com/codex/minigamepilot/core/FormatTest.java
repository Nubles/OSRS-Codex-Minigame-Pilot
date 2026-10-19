package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** The suite's one number format, as every Codex plugin with coin or xp figures pins it. */
public class FormatTest
{
	@Test
	public void amountsFollowTheSuiteRule()
	{
		// In full below 10,000; whole k below a million; M to one decimal; B to two.
		assertEquals("8,240", Figures.count(8_240));
		assertEquals("10k", Figures.count(10_000));
		assertEquals("41k", Figures.count(41_200));
		assertEquals("96k", Figures.count(95_700));
		assertEquals("412k", Figures.count(412_000));
		assertEquals("1.2M", Figures.count(1_240_000));
		assertEquals("26.0M", Figures.count(26_000_000));
		assertEquals("812.4M", Figures.count(812_400_000));
		assertEquals("2.10B", Figures.count(2_100_000_000L));
		// Rounding that reaches the next unit is written in it.
		assertEquals("100k", Figures.count(99_960));
		assertEquals("1.0M", Figures.count(999_600));
		assertEquals("999k", Figures.count(999_499));
		assertEquals("1.00B", Figures.count(999_960_000));
		assertEquals("-45k", Figures.brief(-45_200));
		assertEquals("+880k", Figures.signed(880_000));
		// Tiles, lines and gauges all write a count the same way.
		assertEquals(Figures.brief(68_040), Figures.count(68_040));
		assertEquals("41k / 993k", Figures.pair(41_000, 993_000));
	}
}
