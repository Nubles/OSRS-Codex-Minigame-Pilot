package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class NavigatorGoalsTest
{
	/** A board as Codex Navigator writes it (GoalBoard, version 1), extra fields and all. */
	private static final String BOARD = "{\"version\":1,\"focus\":\"quest:Dragon Slayer II\",\"goals\":["
		+ "{\"key\":\"diary:Varrock Hard\",\"type\":\"diary\",\"title\":\"Varrock Hard\",\"complete\":false,\"progress\":0.4,"
		+ "\"next\":\"Train Smithing to 60\",\"hoursLeft\":6.5,\"skills\":{\"Smithing\":60,\"Agility\":51},\"quests\":[]},"
		+ "{\"key\":\"quest:Dragon Slayer II\",\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.2,"
		+ "\"next\":\"Train Smithing to 70\",\"hoursLeft\":30.0,\"skills\":{\"Smithing\":70,\"Mining\":68,\"Construction\":50},"
		+ "\"quests\":[\"Bone Voyage\",\"Dragon Slayer II\"]},"
		+ "{\"key\":\"quest:Done Deal\",\"type\":\"quest\",\"title\":\"Done Deal\",\"complete\":true,\"progress\":1.0,"
		+ "\"hoursLeft\":0.0,\"skills\":{},\"quests\":[],\"somethingNew\":[1,2]}]}";

	private static NavigatorGoals.Board board()
	{
		return new Gson().fromJson(BOARD, NavigatorGoals.Board.class);
	}

	@Test
	public void readsNavigatorsBoardAsItIsWritten()
	{
		NavigatorGoals.Board b = board();
		assertEquals(1, b.version);
		assertEquals(3, b.goals.size());
		assertEquals(Integer.valueOf(70), b.goals.get(1).skills.get("Smithing"));
		assertTrue(b.goals.get(2).complete);
	}

	@Test
	public void theFocusGoalComesFirst()
	{
		NavigatorGoals.Target t = NavigatorGoals.target(board(), "Smithing", 45);
		assertEquals(70, t.getLevel());
		assertEquals("Dragon Slayer II", t.getGoal());
		assertTrue(t.isFocus());
	}

	@Test
	public void otherwiseTheNearestLevelAboveYours()
	{
		NavigatorGoals.Board b = board();
		b.focus = null;
		NavigatorGoals.Target t = NavigatorGoals.target(b, "smithing", 45);
		assertEquals(60, t.getLevel());
		assertEquals("Varrock Hard", t.getGoal());
		assertFalse(t.isFocus());
		// Past 60 the next goal up is the focus-free 70.
		assertEquals(70, NavigatorGoals.target(b, "Smithing", 60).getLevel());
	}

	@Test
	public void nothingWhenNoGoalNeedsTheSkill()
	{
		assertNull(NavigatorGoals.target(board(), "Smithing", 70));
		assertNull(NavigatorGoals.target(board(), "Fishing", 1));
		assertNull(NavigatorGoals.target(null, "Smithing", 1));
		assertNull(NavigatorGoals.target(new Gson().fromJson("{}", NavigatorGoals.Board.class), "Smithing", 1));
	}
}
