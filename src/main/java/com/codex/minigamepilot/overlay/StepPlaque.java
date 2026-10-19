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

	private static final java.awt.image.BufferedImage MARK = Sigil.icon(16);
	private static final Color GOLD_RIM = Theme.alpha(Theme.GOLD, 170);
	private static final Color GOLD_BAR = Theme.alpha(Theme.GOLD, 200);
	private static final Color TEAL_RIM = Theme.alpha(Theme.TEAL, 170);
	private static final Color TEAL_BAR = Theme.alpha(Theme.TEAL, 200);

	private volatile Step step;
	/** The step's capitalised verb, worked out once per step rather than every frame. */
	private Step verbOf;
	private String verb;
	/** The backdrop, made again only when the plaque's height changes. */
	private GradientPaint backdrop;
	private int backdropHeight;

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
		if (s != verbOf)
		{
			verbOf = s;
			verb = s.getVerb().toUpperCase();
		}
		String verb = this.verb;
		int textX = PAD + icon + 7;
		int line1 = bold.stringWidth(verb) + 8 + small.stringWidth(s.getTitle());
		int line2 = s.getDetail().isEmpty() ? 0 : small.stringWidth(s.getDetail());
		int width = Math.min(MAX_WIDTH, textX + Math.max(line1, line2) + PAD + 2);
		int height = PAD * 2 + bold.getAscent() + (line2 > 0 ? small.getHeight() : 0) + 1;

		boolean teal = s.isWaiting();
		Color tone = teal ? Theme.TEAL : Theme.GOLD;
		if (backdrop == null || backdropHeight != height)
		{
			backdrop = new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 235), 0, height, Theme.alpha(Theme.INK_DEEP, 235));
			backdropHeight = height;
		}
		g.setPaint(backdrop);
		g.fillRoundRect(0, 0, width - 1, height - 1, 10, 10);
		g.setColor(teal ? TEAL_RIM : GOLD_RIM);
		g.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
		g.setColor(teal ? TEAL_BAR : GOLD_BAR);
		g.fillRect(1, 6, 2, height - 12);

		g.drawImage(MARK, PAD, PAD, null);
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
