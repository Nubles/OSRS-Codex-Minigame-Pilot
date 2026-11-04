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
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Port Piscarilius's six houses drawn as they lie, north up: the house of the task lit gold and
 * ringed, the others in ink, the patrols as embers with the way each faces, Captain Khaled in
 * teal (or an arrow toward him at the edge when he is off the map), and where you stand. Under
 * it, a key.
 */
class DistrictMap extends JComponent
{
	private static final int MAX_SCALE = 6;
	private static final int PAD = 10;
	private static final int KEY = 18;
	/** A house drawn this many tiles across. */
	private static final int HOUSE = 6;
	/** Room around the houses, in tiles. */
	private static final int MARGIN = 4;
	/** Khaled further east than this beyond the houses is shown as an arrow at the edge. */
	private static final int FAR = 14;
	/** Room at the right for the arrow toward Captain Khaled when he is off the map, in pixels. */
	private static final int GUTTER = 40;

	private final View.District d;
	private final int scale;
	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	/** Captain Khaled is off the map: an arrow in the gutter points his way. */
	private final boolean khaledFar;

	DistrictMap(View.District d, int width)
	{
		this.d = d;
		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		for (View.District.Place p : d.houses)
		{
			x0 = Math.min(x0, p.x);
			y0 = Math.min(y0, p.y);
			x1 = Math.max(x1, p.x);
			y1 = Math.max(y1, p.y);
		}
		if (d.houses.isEmpty())
		{
			x0 = y0 = 0;
			x1 = y1 = 1;
		}
		khaledFar = d.khaled != null && far(d.khaled, x1, y0, y1);
		if (d.khaled != null && !khaledFar)
		{
			x1 = Math.max(x1, d.khaled[0]);
			y0 = Math.min(y0, d.khaled[1]);
			y1 = Math.max(y1, d.khaled[1]);
		}
		minX = x0 - MARGIN;
		minY = y0 - MARGIN - 2;
		maxX = x1 + MARGIN;
		maxY = y1 + MARGIN;
		scale = Math.max(3, Math.min(MAX_SCALE, (width - PAD * 2 - (khaledFar ? GUTTER : 0)) / Math.max(1, maxX - minX)));
		setOpaque(false);
		setPreferredSize(new Dimension(width, (maxY - minY) * scale + PAD * 2 + KEY));
	}

	private static boolean far(int[] k, int maxX, int minY, int maxY)
	{
		return k[0] > maxX + FAR || k[1] < minY - FAR || k[1] > maxY + FAR;
	}

	private float px(int left, double x)
	{
		return (float) (left + (x - minX) * scale);
	}

	private float py(double y)
	{
		return (float) (PAD + (maxY - y) * scale);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int mapW = (maxX - minX) * scale;
		int left = Math.max(PAD, (w - mapW - (khaledFar ? GUTTER : 0)) / 2);
		int mapH = (maxY - minY) * scale;
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();

		// The ground: a faint grid of streets.
		g.setColor(Theme.alpha(Theme.STROKE_SOFT, 110));
		g.setStroke(new BasicStroke(1f));
		for (int x = minX - minX % 8 + 8; x < maxX; x += 8)
		{
			g.draw(new Line2D.Float(px(left, x), PAD, px(left, x), PAD + mapH));
		}
		for (int y = minY - minY % 8 + 8; y < maxY; y += 8)
		{
			g.draw(new Line2D.Float(left, py(y), left + mapW, py(y)));
		}

		View.District.Place target = null;
		for (View.District.Place p : d.houses)
		{
			if (p.target)
			{
				target = p;
				continue;
			}
			house(g, left, p, false);
		}

		// The way: from you to the house, or with an artefact to Captain Khaled, as the crow flies.
		if (d.you != null)
		{
			float[] to = null;
			if (d.hot && d.khaled != null)
			{
				to = khaledFar ? new float[]{left + mapW + GUTTER / 2f - 6, clampY(py(d.khaled[1]), mapH)} : new float[]{px(left, d.khaled[0] + 0.5), py(d.khaled[1] + 0.5)};
			}
			else if (!d.hot && target != null)
			{
				to = new float[]{px(left, target.x + 0.5), py(target.y + 0.5)};
			}
			if (to != null)
			{
				g.setColor(Theme.alpha(d.hot ? Theme.TEAL : Theme.GOLD, 150));
				g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 4f}, 0f));
				g.draw(new Line2D.Float(clampX(px(left, d.you[0] + 0.5), left, mapW), clampY(py(d.you[1] + 0.5), mapH), to[0], to[1]));
			}
		}
		if (target != null)
		{
			house(g, left, target, true);
		}

		for (View.District.Patrol p : d.patrols)
		{
			if (p.x < minX || p.x >= maxX || p.y <= minY || p.y > maxY)
			{
				continue;
			}
			float cx = px(left, p.x + 0.5);
			float cy = py(p.y + 0.5);
			double a = p.orientation * Math.PI * 2 / 2048.0;
			double dx = -Math.sin(a);
			double dy = -Math.cos(a);
			// The way it faces: a short wedge.
			double len = 3.2 * scale;
			double spread = 0.5;
			Path2D.Float cone = new Path2D.Float();
			cone.moveTo(cx, cy);
			cone.lineTo(cx + len * (dx * Math.cos(spread) - dy * Math.sin(spread)), cy - len * (dy * Math.cos(spread) + dx * Math.sin(spread)));
			cone.lineTo(cx + len * (dx * Math.cos(-spread) - dy * Math.sin(-spread)), cy - len * (dy * Math.cos(-spread) + dx * Math.sin(-spread)));
			cone.closePath();
			g.setColor(Theme.alpha(Theme.EMBER, d.hot ? 110 : 55));
			g.fill(cone);
			Ellipse2D.Float dot = new Ellipse2D.Float(cx - 3f, cy - 3f, 6, 6);
			g.setColor(d.hot ? Theme.EMBER : Theme.mix(Theme.EMBER, Theme.INK_DEEP, 0.35));
			g.fill(dot);
			g.setColor(Theme.INK_DEEP);
			g.setStroke(new BasicStroke(1f));
			g.draw(dot);
		}

		if (d.khaled != null)
		{
			if (khaledFar)
			{
				// Off the map: an arrow in the gutter the way he is, his name under it.
				float y = clampY(py(d.khaled[1]), mapH);
				float x = left + mapW + GUTTER / 2f + 4;
				Path2D.Float arrow = new Path2D.Float();
				arrow.moveTo(x + 5, y);
				arrow.lineTo(x - 5, y - 6);
				arrow.lineTo(x - 5, y + 6);
				arrow.closePath();
				g.setColor(Theme.TEAL);
				g.fill(arrow);
				String k = d.keeperName;
				g.drawString(k, x - fm.stringWidth(k) / 2f, y + 8 + fm.getAscent());
			}
			else
			{
				float cx = px(left, d.khaled[0] + 0.5);
				float cy = py(d.khaled[1] + 0.5);
				Ellipse2D.Float dot = new Ellipse2D.Float(cx - 4f, cy - 4f, 8, 8);
				g.setColor(Theme.TEAL);
				g.fill(dot);
				g.setColor(Theme.INK_DEEP);
				g.draw(dot);
			}
		}

		if (d.you != null)
		{
			float x = clampX(px(left, d.you[0] + 0.5), left, mapW);
			float y = clampY(py(d.you[1] + 0.5), mapH);
			Ellipse2D.Float dot = new Ellipse2D.Float(x - 3.5f, y - 3.5f, 7, 7);
			g.setColor(Theme.TEXT);
			g.fill(dot);
			g.setColor(Theme.GOLD);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(dot);
		}

		// The key.
		java.util.List<String> names = new java.util.ArrayList<>();
		java.util.List<Color> tones = new java.util.ArrayList<>();
		if (target != null)
		{
			names.add(d.targetName);
			tones.add(Theme.GOLD_BRIGHT);
		}
		if (d.keyPatrols || !d.patrols.isEmpty())
		{
			names.add(d.patrolName);
			tones.add(Theme.EMBER);
		}
		if (d.keyPatrols || d.khaled != null)
		{
			names.add(d.keeperName);
			tones.add(Theme.TEAL);
		}
		names.add("You");
		tones.add(Theme.TEXT);
		int row = 0;
		for (String n : names)
		{
			row += 10 + fm.stringWidth(n) + 10;
		}
		row -= 10;
		int kx = Math.max(2, (w - row) / 2);
		int ky = PAD + mapH + 8;
		for (int i = 0; i < names.size(); i++)
		{
			g.setColor(tones.get(i));
			g.fill(new Ellipse2D.Float(kx, ky, 7, 7));
			g.setColor(Theme.TEXT_MUTED);
			g.drawString(names.get(i), kx + 10, ky + 7);
			kx += 10 + fm.stringWidth(names.get(i)) + 10;
		}
		g.dispose();
	}

	private float clampX(float x, int left, int mapW)
	{
		return Math.max(left + 3, Math.min(left + mapW - 3, x));
	}

	private float clampY(float y, int mapH)
	{
		return Math.max(PAD + 3, Math.min(PAD + mapH - 3, y));
	}

	/** A house: a small block with a pitched roof, its label under it; the task's gilt and ringed. */
	private void house(Graphics2D g, int left, View.District.Place p, boolean target)
	{
		float s = HOUSE * scale;
		float x = px(left, p.x + 0.5) - s / 2;
		float y = py(p.y + 0.5) - s / 2;
		float roof = s * 0.38f;
		Path2D.Float shape = new Path2D.Float();
		shape.moveTo(x, y + roof);
		shape.lineTo(x + s / 2, y);
		shape.lineTo(x + s, y + roof);
		shape.lineTo(x + s, y + s);
		shape.lineTo(x, y + s);
		shape.closePath();
		if (target)
		{
			g.setColor(Theme.alpha(Theme.GOLD, 60));
			g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(shape);
			g.setPaint(new GradientPaint(x, y, Theme.mix(Theme.GOLD_BRIGHT, Color.WHITE, 0.12), x, y + s, Theme.mix(Theme.GOLD, Theme.INK_DEEP, 0.3)));
			g.fill(shape);
			g.setColor(Theme.GOLD_BRIGHT);
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(shape);
		}
		else
		{
			g.setColor(Theme.CARD_TOP);
			g.fill(shape);
			g.setColor(p.seen ? Theme.STROKE : Theme.alpha(Theme.STROKE, 200));
			g.setStroke(p.seen ? new BasicStroke(1f) : new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 2f}, 0f));
			g.draw(shape);
		}
		// The door.
		g.setColor(target ? Theme.mix(Theme.GOLD, Theme.INK_DEEP, 0.55) : Theme.STROKE_SOFT);
		g.fill(new RoundRectangle2D.Float(x + s / 2 - s * 0.1f, y + s * 0.68f, s * 0.2f, s * 0.32f, 2, 2));
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		String label = p.label;
		g.setColor(target ? Theme.GOLD_BRIGHT : Theme.TEXT_FAINT);
		g.drawString(label, x + s / 2 - fm.stringWidth(label) / 2f, y + s + fm.getAscent() + 1);
	}
}
