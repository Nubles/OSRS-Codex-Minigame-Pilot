package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FiguresTest
{
	@Test
	public void countsAreExactUntilAHundredThousand()
	{
		assertEquals("0", Figures.count(0));
		assertEquals("68,040", Figures.count(68_040));
		assertEquals("99,999", Figures.count(99_999));
		assertEquals("142K", Figures.count(142_400));
	}

	@Test
	public void millionsAreRoundedNotCutAndNeverWrittenAsThousands()
	{
		// The old panel wrote 1,234,567 xp as "1234K" and 12,900,000 as "12M".
		assertEquals("1.2M", Figures.count(1_234_567));
		assertEquals("12.9M", Figures.count(12_900_000));
		assertEquals("1.0M", Figures.count(999_960));
		assertEquals("999K", Figures.count(999_400));
		assertEquals("1.00B", Figures.count(999_960_000));
		assertEquals("2.15B", Figures.count(2_147_483_647L));
	}

	@Test
	public void briefFiguresShortenFromTenThousand()
	{
		assertEquals("9,870", Figures.brief(9_870));
		assertEquals("41K", Figures.brief(41_400));
		assertEquals("-104K", Figures.brief(-104_276));
		assertEquals("+9,870", Figures.signed(9_870));
		assertEquals("-104K", Figures.signed(-104_276));
		assertEquals("+0", Figures.signed(0));
	}

	@Test
	public void aPairSharesTheLargerOnesUnit()
	{
		assertEquals("612 / 1,800", Figures.pair(612, 1_800));
		assertEquals("41K / 993K", Figures.pair(41_000, 993_000));
		// Not "9,870 / 41K" nor "0K / 41K".
		assertEquals("10K / 41K", Figures.pair(9_870, 41_000));
		assertEquals("300 / 41K", Figures.pair(300, 41_000));
		assertEquals("1.2M / 13.0M", Figures.pair(1_234_567, 13_034_431));
		assertEquals("12K / 13.0M", Figures.pair(12_000, 13_034_431));
		assertEquals("0 / 26", Figures.pair(0, 26));
	}
}
