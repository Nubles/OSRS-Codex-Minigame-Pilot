package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.pilot.Flight;
import com.codex.minigamepilot.ui.Theme;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Rings lines of an open interface that a step names: for the Giants' Foundry, the best moulds
 * you own for the commission in the mould jig's list. It only draws; choosing is yours.
 */
public class MarkGuide extends Overlay
{
	private static final int PULSE_MS = 1600;

	private final Client client;
	private final Supplier<Flight> flight;
	private final RoundRectangle2D.Float ring = new RoundRectangle2D.Float();

	public MarkGuide(Client client, Supplier<Flight> flight)
	{
		this.client = client;
		this.flight = flight;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Flight f = flight.get();
		if (f == null || f.marks.isEmpty())
		{
			return null;
		}
		Widget list = client.getWidget(InterfaceID.GiantsFoundryMould.CONTENT);
		if (list == null || list.isHidden())
		{
			return null;
		}
		Rectangle clip = list.getBounds();
		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() % PULSE_MS / (double) PULSE_MS * Math.PI * 2);
		Theme.smooth(g);
		ring(g, list.getDynamicChildren(), f.marks, clip, pulse);
		ring(g, list.getChildren(), f.marks, clip, pulse);
		return null;
	}

	private void ring(Graphics2D g, Widget[] kids, List<String> marks, Rectangle clip, double pulse)
	{
		if (kids == null)
		{
			return;
		}
		for (Widget w : kids)
		{
			if (w == null || w.isHidden() || w.getText() == null)
			{
				continue;
			}
			String text = w.getText().replaceAll("<[^>]*>", "").trim();
			if (!marks.contains(text))
			{
				continue;
			}
			Rectangle b = w.getBounds();
			// Scrolled out of the list: nothing to ring.
			if (b == null || clip != null && !clip.intersects(b))
			{
				continue;
			}
			ring.setRoundRect(b.x - 3f, b.y - 1.5f, b.width + 6, b.height + 3, 8, 8);
			g.setColor(Ink.at(Ink.GOLD_PULSE, pulse));
			g.fill(ring);
			g.setStroke(Ink.THICK);
			g.setColor(Ink.RING_RIM);
			g.draw(ring);
		}
	}
}
