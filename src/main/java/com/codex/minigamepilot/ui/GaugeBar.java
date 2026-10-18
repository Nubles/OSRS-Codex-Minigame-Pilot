package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * A label, a figure and an engraved bar. The solid part is what is there now; a hatched part
 * after it is what is on its way, such as pay-dirt still washing through the wheel.
 */
class GaugeBar extends JComponent
{
	private final View.Gauge gauge;

	GaugeBar(View.Gauge gauge)
	{
		this.gauge = gauge;
		setOpaque(false);
		setPreferredSize(new Dimension(10, gauge.note == null || gauge.note.isEmpty() ? 30 : 42));
	}

	/** 41,000 as 41K, so coins fit beside the label. */
	private static String compact(int n)
	{
		return n >= 10_000 ? (n / 1000) + "K" : String.valueOf(n);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		String figure = gauge.max > 0 ? compact(gauge.value) + " / " + compact(gauge.max) : compact(gauge.value);
		if (gauge.coming > 0)
		{
			figure = "+" + gauge.coming + "  " + figure;
		}
		Ui.shadowed(g, gauge.label.toUpperCase(), 0, 11, Theme.TEXT_MUTED);
		Ui.shadowed(g, figure, w - fm.stringWidth(figure), 11, gauge.tone);

		int by = 16;
		int bh = 9;
		RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, by + 0.5f, w - 1, bh, bh, bh);
		g.setPaint(new GradientPaint(0, by, Theme.INK_DEEP, 0, by + bh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
		g.fill(track);

		double max = Math.max(1, gauge.max > 0 ? gauge.max : gauge.value + gauge.coming);
		int fill = (int) Math.round((w - 2) * Math.min(1, gauge.value / max));
		int coming = (int) Math.round((w - 2) * Math.min(1, (gauge.value + gauge.coming) / max)) - fill;
		Shape old = g.getClip();
		g.clip(track);
		if (fill > 0)
		{
			g.setPaint(new GradientPaint(0, by, Theme.mix(gauge.tone, Theme.TEXT, 0.3), 0, by + bh, Theme.mix(gauge.tone, Theme.INK_DEEP, 0.25)));
			g.fillRect(1, by, fill, bh + 1);
		}
		if (coming > 0)
		{
			g.setColor(Theme.alpha(Theme.TEAL, 70));
			g.fillRect(1 + fill, by, coming, bh + 1);
			Shape bar = g.getClip();
			g.clipRect(1 + fill, by, coming, bh + 1);
			g.setStroke(new BasicStroke(1.5f));
			g.setColor(Theme.alpha(Theme.TEAL, 170));
			for (int x = 1 + fill - bh; x < 1 + fill + coming; x += 4)
			{
				g.drawLine(x, by + bh + 1, x + bh, by);
			}
			g.setClip(bar);
		}
		g.setClip(old);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(track);

		if (gauge.note != null && !gauge.note.isEmpty())
		{
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(gauge.note, fm, w), 0, by + bh + 13);
		}
		g.dispose();
	}
}
