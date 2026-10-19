package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.ContractBook;
import com.codex.minigamepilot.core.Home;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ContractMeterTest
{
	private final Map<String, String> saved = new HashMap<>();

	private HomesPilot pilot()
	{
		return new HomesPilot(null, c -> { }, saved::get, saved::put);
	}

	private static void handIn(HomesPilot p)
	{
		p.chat("You have completed 12 contracts with a total of 40 points.");
	}

	@Test
	public void aJobSeenFromItsStartIsFiledOnceItsExperienceLands()
	{
		HomesPilot p = pilot();
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		p.load(c);
		int[] fresh = {1, 3, 4, 4, 1, 3, 1, 0};
		p.meter(c, fresh, 10_000);
		// Halfway through the job: the start is kept.
		p.meter(c, new int[]{2, 3, 4, 4, 1, 3, 1, 0}, 10_400);
		handIn(p);
		p.settle(11_000);
		p.settle(11_100);
		assertNull("not before it settles", saved.get(HomesPilot.BOOK_KEY));
		p.settle(11_200);
		ContractBook.Tally t = p.getBook().tally(Contract.Tier.ADEPT);
		assertEquals(1, t.getContracts());
		assertEquals(1200, t.xpEach(), 0);
		assertEquals(c.planksNeeded(fresh), t.planksEach(), 0);
		assertEquals(1, t.steelEach(), 0);
		assertTrue(saved.get(HomesPilot.BOOK_KEY).startsWith("ADEPT=1,1200,"));
	}

	@Test
	public void aJobFirstSeenPartDoneIsNotFiled()
	{
		HomesPilot p = pilot();
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		p.load(c);
		p.meter(c, new int[]{2, 3, 4, 4, 1, 6, 1, 0}, 10_000);
		handIn(p);
		for (int i = 0; i < 5; i++)
		{
			p.settle(12_000);
		}
		assertTrue(p.getBook().isEmpty());
	}

	@Test
	public void theLastJobsFinishedFurnitureDoesNotSpoilTheNext()
	{
		HomesPilot p = pilot();
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		p.load(c);
		// The varbits still show a finished house when the new contract is read.
		p.meter(c, new int[]{2, 6, 6, 7, 2, 6, 2, 0}, 9_000);
		p.meter(c, new int[]{1, 3, 4, 4, 1, 3, 1, 0}, 9_500);
		handIn(p);
		for (int i = 0; i < 3; i++)
		{
			p.settle(10_500);
		}
		assertEquals(1000, p.getBook().tally(Contract.Tier.ADEPT).xpEach(), 0);
	}

	@Test
	public void theCostRankingLightsTheTierYouTakeAndScalesToTheCheapest()
	{
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.ADEPT, 1000, 10, 1, 240);
		book.record(Contract.Tier.EXPERT, 2000, 10, 0, 300);
		java.util.List<ContractBook.Row> rows = book.rank(id -> id == 8780 ? 1500 : id == 8782 ? 2400 : 500, 80);
		com.codex.minigamepilot.ui.View.PickCard card = HomesPilot.costCard(rows, Contract.Tier.ADEPT);
		String sig = new com.codex.minigamepilot.ui.View(true, com.codex.minigamepilot.ui.View.Activity.HOMES, null, null, null, 0)
			.pick(card).signature();
		assertTrue(sig, sig.contains("What it costs"));
		assertTrue(sig, sig.contains("Expert12.0 gp2,000 xp · 24K gp · 24K xp/hfalse"));
		assertTrue(sig, sig.contains("Adept15.5 gp1,000 xp · 16K gp · 15K xp/htrue"));
		assertTrue(sig, sig.contains("NoviceFinish one to learn what it costs"));
	}

	@Test
	public void experienceNotLoadedYetIsNotTakenAsTheStart()
	{
		HomesPilot p = pilot();
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		p.load(c);
		int[] fresh = {1, 3, 4, 4, 1, 3, 1, 0};
		// Just logged in: the furniture is read before the skills arrive.
		p.meter(c, fresh, 0);
		p.meter(c, fresh, 5_000);
		handIn(p);
		for (int i = 0; i < 3; i++)
		{
			p.settle(6_000);
		}
		assertEquals(1000, p.getBook().tally(Contract.Tier.ADEPT).xpEach(), 0);
	}
}
