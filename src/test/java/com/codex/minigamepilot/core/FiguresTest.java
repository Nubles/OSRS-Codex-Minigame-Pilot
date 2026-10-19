package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FiguresTest
{
	@Test
	public void countsAreExactUntilTenThousand()
	{
		assertEquals("0", Figures.count(0));
		assertEquals("9,999", Figures.count(9_999));
		assertEquals("68k", Figures.count(68_040));
		assertEquals("142k", Figures.count(142_400));
	}

	@Test
	public void millionsAreRoundedNotCutAndNeverWrittenAsThousands()
	{
		// The old panel wrote 1,234,567 xp as "1234k" and 12,900,000 as "12M".
		assertEquals("1.2M", Figures.count(1_234_567));
		assertEquals("12.9M", Figures.count(12_900_000));
		assertEquals("1.0M", Figures.count(999_960));
		assertEquals("999k", Figures.count(999_400));
		assertEquals("1.00B", Figures.count(999_960_000));
		assertEquals("2.15B", Figures.count(2_147_483_647L));
	}

	@Test
	public void briefFiguresShortenFromTenThousand()
	{
		assertEquals("9,870", Figures.brief(9_870));
		assertEquals("41k", Figures.brief(41_400));
		assertEquals("-104k", Figures.brief(-104_276));
		assertEquals("+9,870", Figures.signed(9_870));
		assertEquals("-104k", Figures.signed(-104_276));
		assertEquals("+0", Figures.signed(0));
	}

	@Test
	public void aPairSharesTheLargerOnesUnit()
	{
		assertEquals("612 / 1,800", Figures.pair(612, 1_800));
		assertEquals("41k / 993k", Figures.pair(41_000, 993_000));
		// Not "9,870 / 41k" nor "0k / 41k".
		assertEquals("10k / 41k", Figures.pair(9_870, 41_000));
		assertEquals("300 / 41k", Figures.pair(300, 41_000));
		assertEquals("1.2M / 13.0M", Figures.pair(1_234_567, 13_034_431));
		assertEquals("12k / 13.0M", Figures.pair(12_000, 13_034_431));
		assertEquals("0 / 26", Figures.pair(0, 26));
	}

	@Test
	public void aPairShortOfItsGoalNeverReadsFull()
	{
		// 9,600 nuggets of 10,000 read "10k / 10k", and 12,980,000 xp of 99 read "13.0M / 13.0M".
		assertEquals("9k / 10k", Figures.pair(9_600, 10_000));
		assertEquals("12.9M / 13.0M", Figures.pair(12_980_000, 13_034_431));
		assertEquals("13.0M / 13.0M", Figures.pair(13_034_431, 13_034_431));
		assertEquals("10k / 10k", Figures.pair(10_400, 10_000));
		assertEquals("0.9M / 1.0M", Figures.pair(999_600, 1_000_000));
		assertEquals("1.99B / 2.00B", Figures.pair(1_999_999_999L, 2_000_000_000L));
		for (long need : new long[]{10_000, 41_000, 999_000, 1_000_000, 13_034_431, 2_000_000_000L})
		{
			for (long have = Math.max(0, need - 60_000); have < need; have += Math.max(1, need / 997))
			{
				String[] two = Figures.pair(have, need).split(" / ");
				org.junit.Assert.assertNotEquals(have + " of " + need, two[1], two[0]);
			}
			String[] two = Figures.pair(need - 1, need).split(" / ");
			org.junit.Assert.assertNotEquals((need - 1) + " of " + need, two[1], two[0]);
		}
	}
}
