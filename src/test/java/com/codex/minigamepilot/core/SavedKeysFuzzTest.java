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
	public static void survive(String key, String good, Consumer<String> read)
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
		// The quickest of five batches: one batch on a busy machine (a shared CI runner, or this
		// test run beside others) read 3.6 ms and failed it.
		double ms = Double.MAX_VALUE;
		for (int batch = 0; batch < 5; batch++)
		{
			long t = System.nanoTime();
			for (int i = 0; i < 20; i++)
			{
				BankStock.steward(GSON, json);
			}
			ms = Math.min(ms, (System.nanoTime() - t) / 1e6 / 20);
		}
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

	@Test
	public void aRewardANewerVersionAddedIsNotForgottenOnTheNextSave()
	{
		// A newer version saved a reward this one does not know. Decoding dropped it, and the
		// next save (any new reward seen) wrote the list back without it: back on the newer
		// version, the goal card asked you to buy what you already owned.
		Owned o = Owned.decode("PLANK_SACK,GOLDEN_NUGGET_HAT");
		org.junit.Assert.assertTrue(o.has(Goal.Reward.PLANK_SACK));
		String again = o.encode();
		org.junit.Assert.assertTrue(again, again.contains("GOLDEN_NUGGET_HAT"));
		org.junit.Assert.assertTrue(again, again.contains("PLANK_SACK"));
		// Damage is still dropped, and cannot make the value grow.
		org.junit.Assert.assertEquals("PLANK_SACK", Owned.decode("PLANK_SACK,garbage,{},, ,\"x\"").encode());
		StringBuilder many = new StringBuilder();
		for (int i = 0; i < 500; i++)
		{
			many.append("NEW_").append(i).append(',');
		}
		org.junit.Assert.assertTrue(Owned.decode(many.toString()).encode().split(",").length <= 16);
	}

	@Test
	public void foundrySwords()
	{
		survive("foundry.swords", "1;1760000000000,300,135,13560;1760000100000,250,130,12780", s ->
		{
			Swords g = Swords.decode(s);
			sane(String.valueOf(g.repPerHour() < 0 ? 0 : Math.round(g.repPerHour())));
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			for (Swords.Sword w : g.all())
			{
				assertTrue(w.quality >= 0 && w.xp >= 0 && w.ticks >= 0);
			}
			Swords.decode(g.encode());
		});
	}

	@Test
	public void mixologyRounds()
	{
		survive("mixology.rounds", "1;1760000000000,120,40,30,20,2000,3;1760000100000,0,20,0,0,190,1", s ->
		{
			MixRounds g = MixRounds.decode(s);
			sane(String.valueOf(g.resinPerHour() < 0 ? 0 : Math.round(g.resinPerHour())));
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			double h = g.hoursTo(Mixology.Reward.GOGGLES, new int[3]);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			for (MixRounds.Round r : g.all())
			{
				assertTrue(r.resin() >= 0 && r.xp >= 0 && r.ticks >= 0 && r.orders >= 0);
			}
			MixRounds.decode(g.encode());
		});
	}

	@Test
	public void titheKept()
	{
		survive("tithe.kept", "1;1760000000000,1000,LOGAVANO,100,35,37030;1760000100000,0,-,30,10,900", s ->
		{
			TitheLog g = TitheLog.decode(s);
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			sane(String.valueOf(g.pointsPerHour() < 0 ? 0 : Math.round(g.pointsPerHour())));
			double h = g.hoursFor(400);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			sane(String.valueOf(g.sacksFor(400)));
			for (TitheLog.Run r : g.all())
			{
				assertTrue(r.xp >= 0 && r.ticks >= 0 && r.points >= 0 && r.fruit >= 0);
			}
			TitheLog.decode(g.encode());
		});
	}

	@Test
	public void roguesDenKept()
	{
		survive("roguesden.kept", "1;1760000000000,210,1,SAFE,C,1000,50;1760000100000,90,0,GRILLS,-,300,10;1760000200000,0,0,-,-,0,0", s ->
		{
			MazeLog g = MazeLog.decode(s);
			sane(String.valueOf(g.perHour() < 0 ? 0 : Math.round(g.perHour())));
			sane(String.valueOf(g.agilityXpPerHour() < 0 ? 0 : Math.round(g.agilityXpPerHour())));
			double h = g.hoursFor(12);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			sane(String.valueOf(Math.max(0, g.typical())));
			for (MazeLog.Run r : g.all())
			{
				assertTrue(r.ticks >= 0 && r.ticks <= MazeLog.LONGEST && r.agilityXp >= 0 && r.thievingXp >= 0);
				assertTrue(r.finished || r.reward == MazeLog.Reward.NONE);
			}
			for (int n : g.sentBack().values())
			{
				assertTrue(n > 0);
			}
			MazeLog.decode(g.encode());
		});
	}

	@Test
	public void assaultKept()
	{
		survive("assault.kept", "1;1760000000000,1500,5,170,5,5,1C93d/2C120d/3C80d/4C90d/5C99d/6C88d/7C91d/8C95d/9C97d/10C210d;1760000100000,0,12,0,0,0,1A60x/1A70d/2-0x",
			s ->
			{
				AssaultLog g = AssaultLog.decode(s);
				sane(String.valueOf(g.pointsPerHour() < 0 ? 0 : Math.round(g.pointsPerHour())));
				sane(String.valueOf(g.typicalGame()));
				for (int n = 1; n <= Assault.WAVES; n++)
				{
					sane(String.valueOf(g.quickestWave(n)));
				}
				for (AssaultLog.Game x : g.all())
				{
					assertTrue(x.ticks >= 0 && x.ticks <= AssaultLog.LONGEST_GAME && !x.waves.isEmpty());
					for (Assault.Role r : Assault.Role.values())
					{
						assertTrue(x.points(r) >= 0 && x.points(r) <= Assault.CAP);
					}
					for (AssaultLog.Wave w : x.waves)
					{
						assertTrue(w.number >= 1 && w.number <= Assault.WAVES && w.ticks >= 0 && w.ticks <= AssaultLog.LONGEST_WAVE);
					}
				}
				for (AssaultLog.Rate r : g.rates().values())
				{
					assertTrue(r.games > 0 && r.own >= 0 && r.side >= 0 && r.ticks >= 0 && !Double.isNaN(r.own));
				}
				java.util.Map<Assault.Role, Integer> need = new java.util.EnumMap<>(Assault.Role.class);
				for (Assault.Role r : Assault.Role.values())
				{
					need.put(r, 375);
				}
				AssaultPlan.Result p = AssaultPlan.plan(need, new java.util.EnumMap<>(Assault.Role.class), g.rates(), g.typicalGame());
				assertTrue(p.total >= 0 && p.total <= AssaultPlan.MOST && !Double.isNaN(p.hours));
				AssaultLog.decode(g.encode());
			});
	}

	@Test
	public void wintertodtKept()
	{
		survive("wintertodt.kept", "1;1760000000000,420,100,1240,s,3;1760000100000,0,0,0,l,1;1760000200000,0,0,560,s,0", s ->
		{
			WintertodtLog g = WintertodtLog.decode(s);
			sane(String.valueOf(g.pointsPerHour() < 0 ? 0 : Math.round(g.pointsPerHour())));
			sane(String.valueOf(g.rewardsPerHour() < 0 ? 0 : Math.round(g.rewardsPerHour())));
			sane(String.valueOf(g.typicalRound()));
			assertTrue(!Double.isNaN(g.rewardsPerRound()) && !Double.isNaN(g.rewards()));
			for (WintertodtLog.Game x : g.all())
			{
				assertTrue(x.ticks >= 0 && x.ticks <= WintertodtLog.LONGEST_ROUND && x.wait >= 0 && x.wait <= WintertodtLog.LONGEST_WAIT);
				assertTrue(x.points >= 0 && x.points <= WintertodtLog.POINTS_MAX && x.hits >= 0);
			}
			WintertodtLog.decode(g.encode());
		});
	}

	@Test
	public void gnomeKept()
	{
		survive("gnome.kept", "1;1767225617400,E,CHEESE_TOM_BATTA,ROMETTI,16200,0,o,158,0,;1767226267200,H,DRUNK_DRAGON,CAPTAIN_NINTO,400000,2400,o,160,12000,257:2+9472:1", s ->
		{
			GnomeLog g = GnomeLog.decode(s);
			assertTrue(!Double.isNaN(g.xpPerHour()) && !Double.isNaN(g.gpPerHour()) && !Double.isNaN(g.perHour()));
			for (GnomeLog.Delivery d : g.all())
			{
				assertTrue(d.ms >= 0 && d.ms <= GnomeLog.LONGEST && d.wait >= 0 && d.wait <= GnomeLog.LONGEST_WAIT);
				assertTrue(d.xp >= 0 && d.gp >= 0 && d.tip.size() <= 8);
			}
			g.byDish();
			g.tips();
			GnomeLog.decode(g.encode());
		});
	}

	@Test
	public void wintertodtOwned()
	{
		survive("wintertodt.owned", "1;GHWT", s ->
		{
			String o = Wintertodt.decodeOwned(s);
			assertTrue(o.length() <= Wintertodt.OWNED_CODES.length());
			assertTrue(Wintertodt.decodeOwned("1;" + o).equals(o));
		});
	}

	@Test
	public void roguesDenOwned()
	{
		survive("roguesden.owned", "MASK,TOP,BOOTS", s ->
		{
			java.util.Set<Maze.Piece> p = Maze.decodePieces(s);
			assertTrue(p.size() <= 5);
			assertTrue(Maze.decodePieces(Maze.encodePieces(p)).equals(p));
		});
	}

	@Test
	public void valuablesKept()
	{
		survive("valuables.kept", "1;1760000000000,LAVINIA,250,400,120,120,5400,6300,900;1760000100000,-,0,0,100,10,4500,4500,0", s ->
		{
			HouseLog g = HouseLog.decode(s);
			for (int rate : new int[]{55, 65})
			{
				sane(String.valueOf(g.coinsPerHour(rate) < 0 ? 0 : Math.round(g.coinsPerHour(rate))));
			}
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			sane(String.valueOf(g.housesPerHour() < 0 ? 0 : Math.round(g.housesPerHour())));
			double h = g.hoursFor(400_000);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			for (Houses.House house : Houses.House.values())
			{
				sane(String.valueOf(Math.max(0, g.typical(house))));
			}
			for (HouseLog.Haul r : g.all())
			{
				assertTrue(r.ticks >= 0 && r.round >= r.ticks && r.kept >= 0 && r.kept <= r.taken && r.items >= 0 && r.value(65) >= 0);
			}
			sane(String.valueOf(g.kept()));
			HouseLog.decode(g.encode());
		});
	}

	@Test
	public void artefactsKept()
	{
		survive("artefacts.kept", "1;1760000000000,100,NORTH,1,4710,800;1760000100000,0,-,0,750,0", s ->
		{
			ArtefactLog g = ArtefactLog.decode(s);
			sane(String.valueOf(g.perHour() < 0 ? 0 : Math.round(g.perHour())));
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			sane(String.valueOf(g.coinsPerHour() < 0 ? 0 : Math.round(g.coinsPerHour())));
			double h = g.hoursFor(400_000);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			for (Artefacts.House house : Artefacts.House.values())
			{
				sane(String.valueOf(Math.max(0, g.typical(house))));
			}
			for (ArtefactLog.Run r : g.all())
			{
				assertTrue(r.xp >= 0 && r.ticks >= 0 && r.coins >= 0 && r.coins <= Artefacts.COINS_MAX);
			}
			ArtefactLog.decode(g.encode());
		});
	}

	@Test
	public void totemsKept()
	{
		survive("totems.kept", "1;1760000000000,200,MAGIC,5000,1000;1760000100000,0,-,190,0", s ->
		{
			TotemLog g = TotemLog.decode(s);
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			sane(String.valueOf(g.offeringsPerHour() < 0 ? 0 : Math.round(g.offeringsPerHour())));
			double h = g.hoursTo(Totems.Reward.GREENMAN_MASK, 0, 0);
			sane(String.valueOf(h < 0 ? 0 : Math.round(h)));
			for (TotemLog.Totem t : g.all())
			{
				assertTrue(t.xp >= 0 && t.ticks >= 0 && t.offerings >= 0);
			}
			TotemLog.decode(g.encode());
		});
	}

	@Test
	public void contractLog()
	{
		survive("homes.log", "1;1760000000000,LARRY,EXPERT,2950,190;1760000200000,BOB,ADEPT,0,0", s ->
		{
			ContractLog g = ContractLog.decode(s);
			sane(String.valueOf(g.pointsPerHour() < 0 ? 0 : Math.round(g.pointsPerHour())));
			sane(String.valueOf(g.xpPerHour() < 0 ? 0 : Math.round(g.xpPerHour())));
			for (ContractLog.Entry e : g.all())
			{
				assertTrue(e.xp >= 0 && e.seconds >= 0 && e.home != null && e.tier != null);
				sane(HomeWay.clock(e.seconds));
			}
			ContractLog.decode(g.encode());
		});
	}

	@Test
	public void wayfinderAtlas()
	{
		String good = "{\"version\":1,\"savedAt\":1760000000000,\"walkSecondsPerTile\":0.3,\"teleports\":[{\"name\":\"Falador Teleport\","
			+ "\"source\":\"Falador teleport\",\"kind\":\"SPELL\",\"x\":2966,\"y\":3379,\"plane\":0,\"seconds\":4,\"carried\":true,"
			+ "\"timed\":false}],\"networks\":{\"FAIRY_RING\":true},\"timedTrips\":0}";
		survive("codexwayfinder/atlasV1", good, s ->
		{
			WayfinderAtlas a = WayfinderAtlas.parse(GSON, s);
			HomeWay.Plan p = HomeWay.plan(Home.LARRY, 3200, 3200, 0, a);
			if (p != null)
			{
				sane(p.line());
				assertTrue(a.getWalkSecondsPerTile() > 0);
			}
		});
	}
}
