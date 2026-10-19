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

	/**
	 * Contract: the board exactly as Codex Navigator's GoalBoard.of wrote it (Dragon Slayer II in
	 * focus and the Varrock Hard diary pinned, on an account with Smithing 45), unedited.
	 */
	private static final String WRITTEN = "{\"version\":1,\"focus\":\"quest:Dragon Slayer II\",\"goals\":[{\"key\":\"quest:Dragon Slayer II\",\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.0,\"next\":\"Start Shield of Arrav\",\"hoursLeft\":109.5,\"skills\":{\"Slayer\":18,\"Ranged\":30,\"Construction\":50,\"Firemaking\":49,\"Herblore\":45,\"Defence\":40,\"Prayer\":42,\"Cooking\":53,\"Strength\":50,\"Thieving\":60,\"Crafting\":62,\"Fishing\":53,\"Woodcutting\":55,\"Agility\":60,\"Hitpoints\":50,\"Smithing\":70,\"Mining\":68,\"Magic\":75},\"quests\":[\"Family Crest\",\"Shield of Arrav\",\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\",\"Druidic Ritual\",\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\",\"Waterfall Quest\",\"Legends\\u0027 Quest\",\"The Fremennik Trials\",\"Rune Mysteries\",\"Lunar Diplomacy\",\"Death Plateau\",\"Troll Stronghold\",\"Eadgar\\u0027s Ruse\",\"Dream Mentor\",\"Gertrude\\u0027s Cat\",\"Icthlarin\\u0027s Little Helper\",\"A Tail of Two Cats\",\"The Restless Ghost\",\"Ernest the Chicken\",\"Priest in Peril\",\"Animal Magnetism\",\"Ghosts Ahoy\",\"The Dig Site\",\"Bone Voyage\",\"X Marks the Spot\",\"Client of Kourend\",\"Dragon Slayer II\"]},{\"key\":\"diary:Varrock Hard\",\"type\":\"diary\",\"title\":\"Varrock Hard Diary\",\"complete\":false,\"progress\":0.05,\"next\":\"Start Death Plateau\",\"hoursLeft\":46.5,\"skills\":{\"Fletching\":10,\"Herblore\":10,\"Slayer\":10,\"Construction\":50,\"Ranged\":40,\"Thieving\":53,\"Firemaking\":60,\"Magic\":54,\"Prayer\":52,\"Agility\":51,\"Hunter\":66,\"Woodcutting\":60,\"Farming\":68},\"quests\":[\"The Dig Site\",\"Temple of Ikov\",\"The Tourist Trap\",\"Death Plateau\",\"Troll Stronghold\",\"Priest in Peril\",\"Waterfall Quest\",\"Desert Treasure I\"]}]}";

	@Test
	public void readsTheBoardNavigatorActuallyWrites()
	{
		NavigatorGoals.Board b = new Gson().fromJson(WRITTEN, NavigatorGoals.Board.class);
		assertEquals(1, b.version);
		assertEquals("quest:Dragon Slayer II", b.focus);
		NavigatorGoals.Target t = NavigatorGoals.target(b, net.runelite.api.Skill.SMITHING.getName(), 45);
		assertEquals(70, t.getLevel());
		assertEquals("Dragon Slayer II", t.getGoal());
		assertTrue(t.isFocus());
		// The diary needs no Smithing, so with the focus dropped nothing else asks for it.
		b.focus = null;
		assertEquals(70, NavigatorGoals.target(b, "Smithing", 45).getLevel());
		assertFalse(NavigatorGoals.target(b, "Smithing", 45).isFocus());
		assertEquals(66, NavigatorGoals.target(b, net.runelite.api.Skill.HUNTER.getName(), 1).getLevel());
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
