package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Sigil;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A slim gilt plaque in the game view with the next step: the action in gold, the instruction,
 * and its line of why underneath. It draws what the plugin hands it.
 */
public class StepPlaque extends Overlay
{
	private static final int PAD = 7;
	private static final int MAX_WIDTH = 300;

	private volatile Step step;

	public StepPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** @param step the step to show, or null to hide the plaque */
	public void show(Step step)
	{
		this.step = step;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Step s = step;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int icon = 16;
		String verb = s.getVerb().toUpperCase();
		int textX = PAD + icon + 7;
		int line1 = bold.stringWidth(verb) + 8 + small.stringWidth(s.getTitle());
		int line2 = s.getDetail().isEmpty() ? 0 : small.stringWidth(s.getDetail());
		int width = Math.min(MAX_WIDTH, textX + Math.max(line1, line2) + PAD + 2);
		int height = PAD * 2 + bold.getAscent() + (line2 > 0 ? small.getHeight() : 0) + 1;

		Color tone = s.isWaiting() ? Theme.TEAL : Theme.GOLD;
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 235), 0, height, Theme.alpha(Theme.INK_DEEP, 235)));
		g.fill(shape);
		g.setColor(Theme.alpha(tone, 170));
		g.draw(shape);
		g.setColor(Theme.alpha(tone, 200));
		g.fillRect(1, 6, 2, height - 12);

		Sigil.paint(g, PAD, PAD, icon);
		int base = PAD + bold.getAscent();
		g.setFont(Theme.title());
		Ui.shadowed(g, verb, textX, base, tone);
		int x = textX + bold.stringWidth(verb) + 8;
		g.setFont(Theme.small());
		Ui.shadowed(g, Ui.clip(s.getTitle(), small, width - x - PAD), x, base, Theme.TEXT);
		if (line2 > 0)
		{
			Ui.shadowed(g, Ui.clip(s.getDetail(), small, width - textX - PAD), textX, base + small.getHeight(), Theme.TEXT_MUTED);
		}
		return new Dimension(width, height);
	}
}
