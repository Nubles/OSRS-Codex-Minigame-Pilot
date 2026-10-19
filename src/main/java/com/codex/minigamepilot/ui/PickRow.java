package com.codex.minigamepilot.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * One bar in the ranking of what pays: its name and what it makes on the first line, the coins a
 * bar and the coal trips it needs on the second, and a thin gilt rule as long as its share of the
 * best bar's earnings. The bar being made is lit; bars your level does not reach are dimmed.
 */
class PickRow extends JComponent
{
	private final View.Pick pick;

	PickRow(View.Pick pick)
	{
		this.pick = pick;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 32));
		setToolTipText(pick.name + ": " + pick.detail);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		if (pick.current)
		{
			g.setColor(Theme.alpha(Theme.GOLD, 34));
			g.fill(new RoundRectangle2D.Float(-2, 0, w + 4, h - 1, 8, 8));
		}
		Color name = !pick.unlocked ? Theme.TEXT_FAINT : pick.current ? Theme.GOLD_BRIGHT : Theme.TEXT;
		Color figure = !pick.unlocked ? Theme.TEXT_FAINT : pick.loss ? Theme.BLOOD : pick.current ? Theme.GOLD_BRIGHT : Theme.GOLD;

		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		int fw = body.stringWidth(pick.figure);
		Ui.shadowed(g, pick.figure, w - fw - 2, 13, figure);
		Ui.shadowed(g, Ui.clip(pick.name, body, w - fw - 10), 2, 13, name);

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(pick.unlocked ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
		g.drawString(Ui.clip(pick.detail, small, w - 4), 2, 25);

		if (pick.unlocked && pick.share > 0)
		{
			int len = Math.max(3, (int) Math.round((w - 4) * Math.min(1, pick.share)));
			g.setPaint(new GradientPaint(2, 0, Theme.alpha(pick.current ? Theme.GOLD_BRIGHT : Theme.GOLD, 200), 2 + len, 0, Theme.alpha(Theme.GOLD_DIM, 120)));
			g.fill(new RoundRectangle2D.Float(2, h - 4, len, 2, 2, 2));
		}
		g.dispose();
	}
}
