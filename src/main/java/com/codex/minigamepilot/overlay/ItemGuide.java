package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.pilot.Flight;
import com.codex.minigamepilot.ui.Theme;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Rings the items a step names, in the inventory, the worn equipment and the open bank: the
 * coal to withdraw, the gloves to put on, the teleport to take.
 */
public class ItemGuide extends WidgetItemOverlay
{
	private static final int PULSE_MS = 1600;

	private final Supplier<Flight> flight;
	/** Reused for every ringed item, so a frame allocates nothing. */
	private final RoundRectangle2D.Float ring = new RoundRectangle2D.Float();

	public ItemGuide(Supplier<Flight> flight)
	{
		this.flight = flight;
		showOnInventory();
		showOnBank();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem item)
	{
		Flight f = flight.get();
		if (f == null || f.step == null || !f.step.hasItem(itemId))
		{
			return;
		}
		Rectangle b = item.getCanvasBounds();
		if (b == null)
		{
			return;
		}
		Theme.smooth(g);
		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() % PULSE_MS / (double) PULSE_MS * Math.PI * 2);
		ring.setRoundRect(b.x - 1.5f, b.y - 1.5f, b.width + 3, b.height + 3, 8, 8);
		g.setColor(Ink.at(Ink.GOLD_PULSE, pulse));
		g.fill(ring);
		g.setStroke(Ink.THICK);
		g.setColor(Ink.RING_RIM);
		g.draw(ring);
	}
}
