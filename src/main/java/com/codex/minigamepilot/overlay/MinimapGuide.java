package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.pilot.Flight;
import com.codex.minigamepilot.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** The same path on the minimap, ending in a gold ring where the step is. */
public class MinimapGuide extends Overlay
{
	private final Client client;
	private final Supplier<Flight> flight;
	private final Supplier<Boolean> enabled;

	public MinimapGuide(Client client, Supplier<Flight> flight, Supplier<Boolean> enabled)
	{
		this.client = client;
		this.flight = flight;
		this.enabled = enabled;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Flight f = flight.get();
		if (f == null || f.step == null || !enabled.get())
		{
			return null;
		}
		Theme.smooth(g);
		List<WorldPoint> route = f.route;
		Point end = null;
		for (int i = 0; i < route.size(); i++)
		{
			Point p = minimap(route.get(i));
			if (p == null)
			{
				continue;
			}
			end = p;
			if (i % 2 == 1 && i != route.size() - 1)
			{
				continue;
			}
			g.setColor(Theme.alpha(Theme.TEAL, 200));
			g.fill(new Ellipse2D.Float(p.getX() - 1.5f, p.getY() - 1.5f, 3, 3));
		}
		Point target = null;
		if (f.object != null)
		{
			target = minimap(f.object.getWorldLocation());
		}
		else if (f.npc != null)
		{
			target = minimap(f.npc.getWorldLocation());
		}
		if (target == null)
		{
			target = end;
		}
		if (target != null)
		{
			g.setStroke(new BasicStroke(2f));
			g.setColor(Theme.alpha(Color.BLACK, 140));
			g.draw(new Ellipse2D.Float(target.getX() - 3.5f, target.getY() - 3.5f, 9, 9));
			g.setColor(Theme.GOLD_BRIGHT);
			g.draw(new Ellipse2D.Float(target.getX() - 4.5f, target.getY() - 4.5f, 9, 9));
		}
		return null;
	}

	private Point minimap(WorldPoint wp)
	{
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
		return lp == null ? null : Perspective.localToMinimap(client, lp);
	}
}
