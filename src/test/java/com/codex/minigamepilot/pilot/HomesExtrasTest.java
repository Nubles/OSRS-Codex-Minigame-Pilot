package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.ContractBook;
import com.codex.minigamepilot.core.ContractLog;
import com.codex.minigamepilot.core.Home;
import com.codex.minigamepilot.ui.View;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class HomesExtrasTest
{
	private static BankStock bank(int id, long n)
	{
		Map<Integer, Long> m = new HashMap<>();
		m.put(id, n);
		return BankStock.seen(m, 1);
	}

	@Test
	public void lowPlanksSaySoAfterAHandIn()
	{
		assertNull("no bank seen", HomesPilot.lowLine(Contract.Tier.EXPERT, null, 5, null));
		assertNull("17 a contract, 40 is two", HomesPilot.lowLine(Contract.Tier.EXPERT, bank(8782, 30), 4, null));
		assertEquals("Mahogany Homes: your mahogany planks cover at least 1 more contract (20 left)",
			HomesPilot.lowLine(Contract.Tier.EXPERT, bank(8782, 18), 2, null));
		assertEquals("Mahogany Homes: not enough teak planks for another adept contract (9 left)",
			HomesPilot.lowLine(Contract.Tier.ADEPT, bank(8780, 9), 0, null));
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.ADEPT, 2400, 10, 0, 200);
		assertEquals("Mahogany Homes: your teak planks cover about 1 more contract (12 left)",
			HomesPilot.lowLine(Contract.Tier.ADEPT, bank(8780, 12), 0, book));
	}

	@Test
	public void plankSackGauge()
	{
		View.Gauge g = HomesPilot.sackGauge(new int[]{0, 2, 12, 0, 0, 0, 0}, 14);
		Map<String, String> r = new View(true, View.Activity.HOMES, null, null, null, 0).gauge(g).readings();
		assertEquals("14/28", r.get("gauge.Plank_sack"));
		View.Gauge empty = HomesPilot.sackGauge(new int[7], 0);
		assertEquals("0/28", new View(true, View.Activity.HOMES, null, null, null, 0).gauge(empty).readings().get("gauge.Plank_sack"));
	}

	@Test
	public void historyNewestFirstWithTheQuickestTicked()
	{
		ContractLog log = new ContractLog();
		log.add(new ContractLog.Entry(1, Home.LARRY, Contract.Tier.EXPERT, 2950, 190));
		log.add(new ContractLog.Entry(2, Home.BOB, Contract.Tier.EXPERT, 0, 250));
		List<View.Line> lines = HomesPilot.history(log);
		assertEquals(2, lines.size());
		Map<String, String> r = new View(true, View.Activity.HOMES, null, null, null, 0).lines("Your contracts", "2 kept", lines).readings();
		assertEquals("Bob · Expert · 4m 10s IDLE, Larry · Expert · 3m 10s DONE", r.get("lines"));
	}

	@Test
	public void historyHeaderGivesPointsAnHourOnceTimed()
	{
		ContractLog log = new ContractLog();
		log.add(new ContractLog.Entry(1, Home.LARRY, Contract.Tier.EXPERT, 2950, 0));
		assertEquals("1 kept", HomesPilot.historyNote(log));
		log.add(new ContractLog.Entry(2, Home.TAU, Contract.Tier.EXPERT, 0, 180));
		assertEquals("100 points an hour", HomesPilot.historyNote(log));
	}
}
