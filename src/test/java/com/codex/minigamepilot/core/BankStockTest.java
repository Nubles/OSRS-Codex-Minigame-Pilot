package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BankStockTest
{
	private static final Gson GSON = new Gson();
	/**
	 * Codex Bank Steward's {@code bankV1} exactly as its own BankSnapshotTest proves its writer
	 * produces it (bank-steward-bankV1.json), with a teak plank and coal entry added in the same shape.
	 */
	private static final String STEWARD = "{\"version\":1,\"savedAt\":1760000000000,\"items\":{\"385\":240,\"453\":1200,"
		+ "\"995\":1234567,\"2434\":30,\"8780\":212,\"8839\":1,\"11666\":1,\"26950\":1},\"truncated\":false}";

	@Test
	public void groupAndKeyMatchBankSteward()
	{
		// CodexBankStewardConfig.GROUP and BankSnapshot.KEY.
		assertEquals("codexbanksteward", BankStock.STEWARD_GROUP);
		assertEquals("bankV1", BankStock.STEWARD_KEY);
	}

	@Test
	public void readsBankStewardsSnapshotKeepingOnlySupplies()
	{
		BankStock s = BankStock.steward(GSON, STEWARD);
		assertSame(BankStock.Source.BANK_STEWARD, s.getSource());
		assertEquals(1_760_000_000_000L, s.getSavedAt());
		assertEquals(212, s.count(8780));
		assertEquals(1200, s.count(Bar.COAL));
		assertEquals("coins are not a supply", 0, s.count(995));
		assertNull(BankStock.steward(GSON, null));
		assertNull(BankStock.steward(GSON, "not json {"));
		assertNull(BankStock.steward(GSON, "{\"version\":1}"));
		BankStock odd = BankStock.steward(GSON, "{\"items\":{\"x\":1,\"453\":\"many\",\"451\":-2,\"8782\":3}}");
		assertEquals(3, odd.count(8782));
		assertEquals(0, odd.getSavedAt());
	}

	@Test
	public void theNewerLookWins()
	{
		Map<Integer, Long> seen = new HashMap<>();
		seen.put(8780, 40L);
		seen.put(995, 10L);
		BankStock own = BankStock.seen(seen, 1_760_000_000_000L);
		BankStock steward = BankStock.steward(GSON, STEWARD);
		assertSame("a tie goes to the Pilot", own, BankStock.newest(own, steward));
		BankStock older = BankStock.seen(seen, 1_759_000_000_000L);
		assertSame(steward, BankStock.newest(older, steward));
		assertSame(own, BankStock.newest(own, null));
		assertSame(steward, BankStock.newest(null, steward));
		assertNull(BankStock.newest(null, null));
	}

	@Test
	public void savesItsOwnLookAndReadsItBack()
	{
		Map<Integer, Long> seen = new HashMap<>();
		seen.put(8780, 40L);
		seen.put(Bar.COAL, 900L);
		seen.put(995, 10L);
		BankStock own = BankStock.seen(seen, 123L);
		assertEquals("123|453=900;8780=40", own.encode());
		BankStock read = BankStock.decode(own.encode());
		assertTrue(read.sameItems(own));
		assertEquals(123L, read.getSavedAt());
		assertEquals(0, BankStock.decode("5|").count(Bar.COAL));
		assertNull(BankStock.decode("garbage"));
		assertNull(BankStock.decode(null));
		BankStock damaged = BankStock.decode("9|453=x;8780=4;995=7");
		assertEquals(4, damaged.count(8780));
		assertFalse(damaged.sameItems(own));
	}
}
