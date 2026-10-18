package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** The Pilot's mark: a gilt compass ring with a teal needle pointing the way. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float cx = x + 8 * s;
		float cy = y + 8 * s;
		float stroke = Math.max(1f, 1.3f * s);

		// The ring, with its four cardinal ticks.
		g.setStroke(new BasicStroke(stroke));
		g.setPaint(new GradientPaint(x, y, Theme.GOLD_BRIGHT, x, y + size, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(x + 1.4f * s, y + 1.4f * s, 13.2f * s, 13.2f * s));
		g.setStroke(new BasicStroke(Math.max(0.8f, 0.9f * s)));
		for (int i = 0; i < 4; i++)
		{
			double a = Math.PI / 2 * i;
			float ox = (float) Math.cos(a);
			float oy = (float) Math.sin(a);
			g.drawLine(Math.round(cx + ox * 5.2f * s), Math.round(cy + oy * 5.2f * s),
				Math.round(cx + ox * 6.6f * s), Math.round(cy + oy * 6.6f * s));
		}

		// The needle: a teal point to the north-east and a dark tail behind it.
		double angle = -Math.PI / 4;
		float len = 5.4f * s;
		float wid = 1.7f * s;
		float tx = (float) Math.cos(angle);
		float ty = (float) Math.sin(angle);
		float nx = -ty;
		float ny = tx;
		Path2D head = new Path2D.Float();
		head.moveTo(cx + tx * len, cy + ty * len);
		head.lineTo(cx + nx * wid, cy + ny * wid);
		head.lineTo(cx - nx * wid, cy - ny * wid);
		head.closePath();
		Path2D tail = new Path2D.Float();
		tail.moveTo(cx - tx * len * 0.8f, cy - ty * len * 0.8f);
		tail.lineTo(cx + nx * wid, cy + ny * wid);
		tail.lineTo(cx - nx * wid, cy - ny * wid);
		tail.closePath();
		g.setColor(Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.3));
		g.fill(tail);
		g.setPaint(new GradientPaint(cx, cy, Theme.TEAL, cx + tx * len, cy + ty * len, Theme.mix(Theme.TEAL, Theme.TEXT, 0.5)));
		g.fill(head);
		g.setColor(Theme.GOLD_BRIGHT);
		float hub = Math.max(1.6f, 1.6f * s);
		g.fill(new Ellipse2D.Float(cx - hub / 2, cy - hub / 2, hub, hub));
	}
}
