package com.codex.minigamepilot.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

/**
 * Every key the Pilot saves for itself, and the two it reads from other Codex plugins, damaged
 * every way a save can be: empty, cut short, garbage, wrong separators, huge, negative and
 * not-a-number figures, a newer version. None may throw, and nothing read back may print a
 * figure below zero, "NaN" or "Infinity".
 */
public class SavedKeysFuzzTest
{
	private static final Gson GSON = RuneLiteAPI.GSON;
	private static final long NOW = 1_760_000_000_000L;
	private static final String[] NUMBERS = {
		"", "x", "-1", "0", "-9223372036854775808", "9223372036854775807", "99999999999999999999", "2147483648", "1e30", "NaN", " 7 ",
	};
	private static final Pattern NUMBER = Pattern.compile("-?\\d+");

	/** A valid save and damaged copies of it: cut at every length, each figure replaced, separators doubled. */
	public static List<String> damaged(String good)
	{
		Set<String> out = new LinkedHashSet<>();
		out.add(null);
		out.add("");
		out.add(" ");
		out.add("garbage");
		out.add("\u0000");
		out.add("{\"version\":2}");
		out.add(good);
		for (int cut = 0; cut < good.length(); cut++)
		{
			out.add(good.substring(0, cut));
		}
		Matcher m = NUMBER.matcher(good);
		while (m.find())
		{
			for (String n : NUMBERS)
			{
				out.add(good.substring(0, m.start()) + n + good.substring(m.end()));
			}
		}
		for (String sep : new String[]{"|", ";", ":", ",", "="})
		{
			out.add(good.replace(sep, sep + sep));
			out.add(good.replace(sep, ""));
			out.add(sep + good + sep);
		}
		out.add(good + "|future|fields");
		out.add(good + ";" + good);
		return new ArrayList<>(out);
	}

	/** Runs the reader over every damaged copy, collecting what throws or prints a bad figure. */
	static void survive(String key, String good, Consumer<String> read)
	{
		List<String> failures = new ArrayList<>();
		for (String saved : damaged(good))
		{
			try
			{
				read.accept(saved);
			}
			catch (RuntimeException | AssertionError e)
			{
				failures.add(key + " = " + saved + ": " + e);
			}
		}
		assertTrue(failures.size() + " failures: " + failures, failures.isEmpty());
	}

	/** Text a card prints: no negative figure, no NaN or Infinity. */
	static void sane(String text)
	{
		if (text == null)
		{
			return;
		}
		assertFalse(text, text.contains("NaN") || text.contains("Infinity"));
		assertFalse(text, Pattern.compile("(^|[\\s(:])-\\d").matcher(text).find());
	}

	@Test
	public void visitSummary()
	{
		survive("visit.MOTHERLODE", "1760000000000|72|318 pay-dirt;21,400 Mining xp;ore worth 48k", s ->
		{
			Visit v = Visit.decode(s);
			if (v != null)
			{
				sane(v.line());
				sane(v.status(NOW));
				sane(v.duration());
			}
		});
	}

	@Test
	public void bestVisit()
	{
		survive("best.MOTHERLODE", "1759990000000|1760000000000|72|1620|81000", s ->
		{
			BestVisit b = BestVisit.decode(s);
			if (b != null)
			{
				sane(b.pace("pay-dirt"));
				sane(b.status("pay-dirt", "Mining", null, NOW));
				sane(b.status("pay-dirt", "Mining", Visit.decode("1760000000000|72|318 pay-dirt"), NOW));
			}
		});
	}

	@Test
	public void pointsBook()
	{
		Consumer<String> read = s ->
		{
			Points p = Points.decode(s);
			for (long owned : new long[]{0, 1_250, Long.MAX_VALUE / 2})
			{
				long balance = p.balance(owned);
				// -1 is "not read from a message yet".
				assertTrue(s + " balance " + balance, balance >= -1);
				assertTrue(s + " balance " + balance + " over total " + p.getTotal(), balance <= Math.max(0, p.getTotal()));
			}
			if (p.known())
			{
				sane(Figures.count(p.getTotal()));
			}
			Points.decode(p.encode());
		};
		survive("goal.pointsBook", "470:120:UNKNOWN:350", read);
		survive("goal.pointsBook", "470:120:BALANCE:0", read);
		survive("goal.points", "470", read);
	}

	@Test
	public void ownedRewards()
	{
		survive("goal.owned", "AMYS_SAW,PLANK_SACK,BIGGER_SACK", s ->
		{
			Owned o = Owned.decode(s);
			for (Goal.Reward r : Goal.Reward.values())
			{
				o.has(r);
			}
			Owned.decode(o.encode());
		});
	}

	@Test
	public void contractBook()
	{
		survive("homes.book", "EXPERT=12,24000,120,36,10,9000;ADEPT=4,4000,24,0,4,2400", s ->
		{
			ContractBook book = ContractBook.decode(s);
			for (ContractBook.Row row : book.rank(id -> 1_000, 70))
			{
				for (double v : new double[]{row.getCoinsEach(), row.getPerXp(), row.getXpPerHour()})
				{
					assertTrue(s + " " + v, Double.isFinite(v) && v >= 0);
				}
			}
			for (Contract.Tier tier : Contract.Tier.values())
			{
				ContractBook.Tally t = book.tally(tier);
				if (t != null)
				{
					Supplies.Homes h = Supplies.homes(tier, 1_000, 200, t);
					// -1 is "not learned yet".
					assertTrue(s, h.getContracts() >= -1);
					sane(h.headline());
				}
			}
			ContractBook.decode(book.encode());
		});
	}

	@Test
	public void ownBankStock()
	{
		survive("stock.bank", "1760000000000|453=5000;440=3000;2351=200;8780=400;8782=100", s ->
		{
			BankStock b = BankStock.decode(s);
			if (b != null)
			{
				for (int id : new int[]{453, 440, 2351, 8780, 8782})
				{
					assertTrue(s, b.count(id) >= 0 && b.count(id) <= Integer.MAX_VALUE);
				}
				assertTrue(s, b.getSavedAt() >= 0);
				BankStock.decode(b.encode());
				Supplies.Furnace f = Supplies.furnace(Bar.STEEL, b.count(440), b.count(453), 26, 27, 60);
				assertTrue(s, f.getBars() >= 0);
				sane(f.headline());
				sane(f.limit());
			}
		});
	}

	@Test
	public void bankStewardSnapshot()
	{
		Consumer<String> read = s ->
		{
			BankStock b = BankStock.steward(GSON, s);
			if (b != null)
			{
				for (int id : new int[]{453, 440, 8780})
				{
					assertTrue(s + " " + b.count(id), b.count(id) >= 0 && b.count(id) <= Integer.MAX_VALUE);
				}
				assertTrue(s, b.getSavedAt() >= 0);
			}
		};
		survive("codexbanksteward/bankV1", "{\"version\":1,\"savedAt\":1760000000000,\"items\":{\"453\":5000,\"440\":3000},\"truncated\":false}", read);
		for (String bad : new String[]{"\"x\"", "[]", "{}", "null", "true", "1e30", "-5", "NaN", "\"12\""})
		{
			read.accept("{\"version\":1,\"savedAt\":" + bad + ",\"items\":{\"453\":" + bad + "},\"truncated\":" + bad + "}");
			read.accept("{\"version\":" + bad + ",\"items\":" + bad + "}");
		}
	}

	@Test
	public void paceBests()
	{
		survive("pace.FURNACE", "3/27=15;3/27#0=4;3/27#1=6;7/27=120", s ->
		{
			Pace p = new Pace();
			p.loadBests(s);
			assertTrue(s, p.best("3/27") >= 0);
			p.loadBests(p.saveBests());
		});
	}

	@Test
	public void navigatorGoalBoard()
	{
		String board = "{\"version\":1,\"focus\":\"quest:Dragon Slayer II\",\"goals\":[{\"key\":\"quest:Dragon Slayer II\",\"type\":\"quest\","
			+ "\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.4,\"next\":\"x\",\"hoursLeft\":12.5,"
			+ "\"skills\":{\"Smithing\":70,\"Mining\":68},\"quests\":[]}]}";
		Consumer<String> read = s ->
		{
			NavigatorGoals.Board b = NavigatorGoals.read(GSON, s);
			NavigatorGoals.Target t = NavigatorGoals.target(b, "Smithing", 50);
			if (t != null)
			{
				assertTrue(s, t.getLevel() > 50 && t.getLevel() <= 99);
			}
		};
		survive("codexnavigator/goalBoard", board, read);
		for (String bad : new String[]{"\"\"", "\"x\"", "[]", "{}", "null", "true", "1e400", "NaN", "-1", "99999999999", "[1,\"x\"]"})
		{
			for (String field : new String[]{"version", "focus", "goals", "key", "title", "complete", "hoursLeft", "skills", "quests"})
			{
				read.accept(board.replaceFirst("\"" + field + "\":(\\{[^}]*\\}|\\[[^\\]]*\\]|\"[^\"]*\"|[^,}]+)",
					"\"" + field + "\":" + Matcher.quoteReplacement(bad)));
			}
			read.accept(board.replace("\"Smithing\":70", "\"Smithing\":" + bad));
		}
	}

	@Test
	public void aSnapshotFromTheFutureDoesNotOutrankYourOwnLook()
	{
		// Bank Steward saved with a clock set years ahead: its snapshot must not win over the
		// Pilot's own look from a minute ago for ever more.
		BankStock own = BankStock.seen(java.util.Collections.singletonMap(453, 100L), NOW - 60_000L);
		BankStock ahead = BankStock.steward(GSON, "{\"version\":1,\"savedAt\":" + (NOW + 10 * 365 * 86_400_000L) + ",\"items\":{\"453\":9}}");
		org.junit.Assert.assertSame(own, BankStock.newest(own, ahead, NOW));
	}

	@Test
	public void aThousandItemBankReadsInAFewMilliseconds()
	{
		StringBuilder items = new StringBuilder("{");
		for (int id = 30_000; id < 31_500; id++)
		{
			items.append(id == 30_000 ? "" : ",").append('"').append(id).append("\":").append(id * 7);
		}
		items.append(",\"453\":5000}");
		String json = "{\"version\":1,\"savedAt\":1760000000000,\"items\":" + items + ",\"truncated\":false}";
		for (int i = 0; i < 50; i++)
		{
			BankStock.steward(GSON, json);
		}
		long t = System.nanoTime();
		for (int i = 0; i < 100; i++)
		{
			BankStock.steward(GSON, json);
		}
		double ms = (System.nanoTime() - t) / 1e6 / 100;
		assertTrue(ms + " ms", ms < 3);
		org.junit.Assert.assertEquals(5000, BankStock.steward(GSON, json).count(453));
	}

	@Test
	public void savedContract()
	{
		// ":" (a home name lost) split to nothing and threw on every login.
		for (String good : new String[]{"Jess:Expert", "Jess"})
		{
			survive("contract", good, s ->
			{
				Contract c = Contract.decode(s);
				if (c != null)
				{
					assertTrue(s, c.getHome() != null);
				}
			});
		}
		survive("contract", ":", Contract::decode);
	}
}
