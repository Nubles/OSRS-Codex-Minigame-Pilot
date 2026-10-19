package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.pilot.Flight;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the step in the world: the thing to click lit in gold with a small plate naming the
 * action, the path to it as a trail of teal beads that brighten toward the end, and anything
 * else still to do marked quietly. It only draws; it never clicks or changes menus.
 */
public class SceneGuide extends Overlay
{
	/** Beads drawn at most, so a long walk does not paint the whole screen. */
	private static final int MAX_BEADS = 40;
	private static final Color PLATE_RIM_GOLD = Theme.alpha(Theme.GOLD, 200);
	private static final Color PLATE_RIM_TEAL = Theme.alpha(Theme.TEAL, 200);
	private static final Color PLATE_TEXT_GOLD = Theme.mix(Theme.GOLD, Color.WHITE, 0.3);
	private static final Color PLATE_TEXT_TEAL = Theme.mix(Theme.TEAL, Color.WHITE, 0.3);
	private static final int PULSE_MS = 1600;

	private final Client client;
	private final Supplier<Flight> flight;
	private final Supplier<Settings> settings;
	/** Reused for every bead, so the trail allocates nothing per frame. */
	private final Ellipse2D.Float bead = new Ellipse2D.Float();

	/** What to draw, from the plugin's config. */
	public static final class Settings
	{
		public final boolean route;
		public final boolean labels;
		public final boolean others;

		public Settings(boolean route, boolean labels, boolean others)
		{
			this.route = route;
			this.labels = labels;
			this.others = others;
		}
	}

	public SceneGuide(Client client, Supplier<Flight> flight, Supplier<Settings> settings)
	{
		this.client = client;
		this.flight = flight;
		this.settings = settings;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Flight f = flight.get();
		if (f == null || f.step == null)
		{
			return null;
		}
		Settings set = settings.get();
		Theme.smooth(g);
		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() % PULSE_MS / (double) PULSE_MS * Math.PI * 2);

		if (set.others)
		{
			for (TileObject o : f.others)
			{
				Shape box = o.getClickbox();
				if (box != null)
				{
					g.setStroke(Ink.THIN);
					g.setColor(Ink.OTHER_RIM);
					g.draw(box);
				}
			}
		}
		if (set.route)
		{
			drawRoute(g, f.route, f.step.isWaiting());
		}
		if (f.object != null)
		{
			drawObject(g, f.object, f, pulse, set.labels);
		}
		if (f.npc != null)
		{
			drawNpc(g, f.npc, f, pulse, set.labels);
		}
		return null;
	}

	private void drawRoute(Graphics2D g, List<WorldPoint> route, boolean waiting)
	{
		int n = Math.min(route.size(), MAX_BEADS);
		for (int i = 0; i < n; i++)
		{
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), route.get(i));
			if (lp == null)
			{
				continue;
			}
			Point p = Perspective.localToCanvas(client, lp, client.getTopLevelWorldView().getPlane());
			if (p == null)
			{
				continue;
			}
			boolean last = i == route.size() - 1;
			if (last)
			{
				Polygon tile = Perspective.getCanvasTilePoly(client, lp);
				if (tile != null)
				{
					g.setColor(Ink.END_FILL);
					g.fill(tile);
					g.setStroke(Ink.MEDIUM);
					g.setColor(Ink.END_RIM);
					g.draw(tile);
				}
				continue;
			}
			double t = n <= 1 ? 1 : i / (double) (n - 1);
			float r = (float) (2.2 + 1.4 * t);
			g.setColor(waiting ? Ink.WAIT_SHADOW : Ink.at(Ink.BEAD_SHADOW, t));
			bead.setFrame(p.getX() - r + 1, p.getY() - r + 1, r * 2, r * 2);
			g.fill(bead);
			g.setColor(waiting ? Ink.WAIT_BEAD : Ink.at(Ink.BEAD, t));
			bead.setFrame(p.getX() - r, p.getY() - r, r * 2, r * 2);
			g.fill(bead);
		}
	}

	private void drawObject(Graphics2D g, TileObject o, Flight f, double pulse, boolean label)
	{
		Shape box = o.getClickbox();
		boolean teal = f.step.isWaiting();
		Color tone = teal ? Theme.TEAL : Theme.GOLD;
		if (box != null)
		{
			Point mouse = client.getMouseCanvasPosition();
			boolean hover = mouse != null && box.contains(mouse.getX(), mouse.getY());
			g.setColor(hover ? (teal ? Ink.TEAL_HOVER : Ink.GOLD_HOVER) : Ink.at(teal ? Ink.TEAL_PULSE : Ink.GOLD_PULSE, pulse));
			g.fill(box);
			g.setStroke(Ink.THICK);
			g.setColor(teal ? (hover ? Ink.TEAL_RIM_HOVER : Ink.TEAL_RIM) : (hover ? Ink.GOLD_RIM_HOVER : Ink.GOLD_RIM));
			g.draw(box);
		}
		if (label)
		{
			Point at = o.getCanvasTextLocation(g, f.step.getVerb(), 160);
			if (at != null)
			{
				plate(g, f.step.getVerb(), at.getX(), at.getY(), tone);
			}
		}
	}

	private void drawNpc(Graphics2D g, NPC npc, Flight f, double pulse, boolean label)
	{
		Shape hull = npc.getConvexHull();
		if (hull != null)
		{
			g.setColor(Ink.at(Ink.HULL_PULSE, pulse));
			g.fill(hull);
			g.setStroke(Ink.THICK);
			g.setColor(Ink.HULL_RIM);
			g.draw(hull);
		}
		if (label)
		{
			Point at = npc.getCanvasTextLocation(g, f.step.getVerb(), npc.getLogicalHeight() + 40);
			if (at != null)
			{
				plate(g, f.step.getVerb(), at.getX(), at.getY(), Theme.GOLD);
			}
		}
	}

	/** A small ink plate with a gilt rim and the action on it, centred over a point. */
	private static void plate(Graphics2D g, String text, int x, int y, Color tone)
	{
		g.setFont(Theme.title());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text) + 14;
		int h = fm.getAscent() + 8;
		int left = x + fm.stringWidth(text) / 2 - w / 2;
		int top = y - fm.getAscent() - 4;
		g.setColor(Ink.PLATE_FILL);
		g.fillRoundRect(left, top, w, h, 8, 8);
		g.setStroke(Ink.THIN);
		boolean teal = tone == Theme.TEAL;
		g.setColor(teal ? PLATE_RIM_TEAL : PLATE_RIM_GOLD);
		g.drawRoundRect(left, top, w, h, 8, 8);
		Ui.shadowed(g, text, left + 7, top + fm.getAscent() + 3, teal ? PLATE_TEXT_TEAL : PLATE_TEXT_GOLD);
	}
}
