package com.codex.minigamepilot.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LineRowTest
{
	private static int heightAt(View.Line line, int width)
	{
		LineRow row = new LineRow(line);
		row.setBounds(0, 0, width, row.getPreferredSize().height);
		return row.getPreferredSize().height;
	}

	@Test
	public void aRowNarrowerThanItsGuessGivesTheCostALineOfItsOwn()
	{
		// Guessed at the panel's width it fits on one line; in a narrower card the name used to
		// be cut to "Carpenter's trou..." and, narrower still, the cost ran over it.
		View.Line line = new View.Line("Carpenter's trousers", "600", View.Line.Mark.TODO);
		assertEquals(18, new LineRow(line).getPreferredSize().height);
		assertEquals(31, heightAt(line, 120));
	}

	@Test
	public void aRowWiderThanItsGuessKeepsOneLine()
	{
		View.Line line = new View.Line("Mahogany drawers", "Build, 2 planks", View.Line.Mark.TODO);
		assertEquals(31, new LineRow(line).getPreferredSize().height);
		assertEquals(18, heightAt(line, 400));
	}
}
