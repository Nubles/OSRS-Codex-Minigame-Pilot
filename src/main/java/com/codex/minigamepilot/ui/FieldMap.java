package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * A minigame's patches drawn as they lie on the ground, north up: each one coloured by what it
 * needs (ember for water, blood when it is about to wilt, teal once watered, gold when grown),
 * numbered in your round's order, the next one ringed in gold, and where you stand. Under it, a
 * key to the colours.
 */
class FieldMap extends JComponent
{
	/** Pixels a tile at most, so a small field is not drawn huge. */
	private static final int MAX_SCALE = 10;
	private static final int PAD = 8;
	private static final int KEY = 30;

	private final View.Field field;
	private int scale = MAX_SCALE;
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;

	FieldMap(View.Field field, int width)
	{
		this.field = field;
		setOpaque(false);
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		for (View.Field.Cell c : field.cells)
		{
			minX = Math.min(minX, c.x);
			minY = Math.min(minY, c.y);
			maxX = Math.max(maxX, c.x + c.size);
			maxY = Math.max(maxY, c.y + c.size);
		}
		if (field.cells.isEmpty())
		{
			minX = minY = 0;
			maxX = maxY = 1;
		}
		int span = Math.max(1, maxX - minX);
		scale = Math.max(3, Math.min(MAX_SCALE, (width - PAD * 2) / span));
		setPreferredSize(new Dimension(width, (maxY - minY) * scale + PAD * 2 + KEY));
	}

	private static Color fill(View.Field.Need need)
	{
		switch (need)
		{
			case DRY:
				return Theme.EMBER;
			case URGENT:
				return Theme.BLOOD;
			case WET:
				return Theme.TEAL;
			case GROWN:
				return Theme.GOLD_BRIGHT;
			case DEAD:
				return Theme.BLOOD_DEEP;
			default:
				return Theme.INK_DEEP;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int fieldW = (maxX - minX) * scale;
		int left = Math.max(PAD, (w - fieldW) / 2);
		int top = PAD;
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();

		// Faint furrows behind the patches.
		g.setColor(Theme.alpha(Theme.STROKE_SOFT, 120));
		for (int y = minY; y <= maxY; y += 3)
		{
			int py = top + (maxY - y) * scale;
			g.drawLine(left - 2, py, left + fieldW + 2, py);
		}

		View.Field.Cell next = null;
		for (View.Field.Cell c : field.cells)
		{
			float x = left + (c.x - minX) * scale + 1;
			float y = top + (maxY - c.y - c.size) * scale + 1;
			float s = c.size * scale - 2;
			RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, s, s, 6, 6);
			Color tone = fill(c.need);
			if (c.need == View.Field.Need.EMPTY)
			{
				g.setColor(Theme.alpha(Theme.CARD_TOP, 255));
				g.fill(box);
				g.setColor(c.planned ? Theme.alpha(Theme.GOLD, 200) : Theme.STROKE);
				g.setStroke(c.planned ? new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 2f}, 0f)
					: new BasicStroke(1f));
				g.draw(box);
			}
			else
			{
				g.setPaint(new GradientPaint(x, y, Theme.mix(tone, Color.WHITE, 0.18), x, y + s, Theme.mix(tone, Theme.INK_DEEP, 0.25)));
				g.fill(box);
				g.setStroke(new BasicStroke(1f));
				g.setColor(Theme.mix(tone, Theme.INK_DEEP, 0.5));
				g.draw(box);
				if (c.need == View.Field.Need.DEAD)
				{
					g.setColor(Theme.alpha(Theme.TEXT, 170));
					g.setStroke(new BasicStroke(1.4f));
					g.draw(new Line2D.Float(x + 4, y + 4, x + s - 4, y + s - 4));
					g.draw(new Line2D.Float(x + s - 4, y + 4, x + 4, y + s - 4));
				}
			}
			if (c.order > 0)
			{
				String n = String.valueOf(c.order);
				int tx = Math.round(x + (s - fm.stringWidth(n)) / 2f);
				int ty = Math.round(y + (s + fm.getAscent() - fm.getDescent()) / 2f);
				boolean light = c.need == View.Field.Need.GROWN || c.need == View.Field.Need.WET || c.need == View.Field.Need.DRY;
				g.setColor(light ? Theme.INK_DEEP : Theme.TEXT);
				g.drawString(n, tx, ty);
			}
			if (c.next)
			{
				next = c;
			}
		}
		if (next != null)
		{
			float x = left + (next.x - minX) * scale - 1;
			float y = top + (maxY - next.y - next.size) * scale - 1;
			float s = next.size * scale + 2;
			g.setColor(Theme.alpha(Theme.GOLD, 70));
			g.setStroke(new BasicStroke(5f));
			g.draw(new RoundRectangle2D.Float(x - 1, y - 1, s + 2, s + 2, 9, 9));
			g.setColor(Theme.GOLD_BRIGHT);
			g.setStroke(new BasicStroke(2f));
			g.draw(new RoundRectangle2D.Float(x, y, s, s, 8, 8));
		}
		if (field.you != null)
		{
			float x = left + (field.you[0] - minX) * scale + scale / 2f;
			float y = top + (maxY - field.you[1]) * scale - scale / 2f;
			x = Math.max(2, Math.min(w - 2, x));
			y = Math.max(2, Math.min(top + (maxY - minY) * scale + PAD - 2, y));
			Ellipse2D.Float dot = new Ellipse2D.Float(x - 3.5f, y - 3.5f, 7, 7);
			g.setColor(Theme.TEXT);
			g.fill(dot);
			g.setColor(Theme.GOLD);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(dot);
		}

		// The key, in as many rows as it needs.
		String[] names = {"Needs water", "Wilting", "Watered", "Grown"};
		Color[] tones = {Theme.EMBER, Theme.BLOOD, Theme.TEAL, Theme.GOLD_BRIGHT};
		int[] widths = new int[names.length];
		for (int i = 0; i < names.length; i++)
		{
			widths[i] = 10 + fm.stringWidth(names[i]);
		}
		int ky = top + (maxY - minY) * scale + PAD + 4;
		int i = 0;
		while (i < names.length)
		{
			int j = i;
			int row = 0;
			while (j < names.length && (j == i || row + 10 + widths[j] <= w - 4))
			{
				row += (j == i ? 0 : 10) + widths[j];
				j++;
			}
			int kx = Math.max(2, (w - row) / 2);
			for (int k = i; k < j; k++)
			{
				g.setColor(tones[k]);
				g.fill(new RoundRectangle2D.Float(kx, ky, 7, 7, 3, 3));
				g.setColor(Theme.TEXT_MUTED);
				g.drawString(names[k], kx + 10, ky + 7);
				kx += widths[k] + 10;
			}
			ky += 12;
			i = j;
		}
		g.dispose();
	}
}
