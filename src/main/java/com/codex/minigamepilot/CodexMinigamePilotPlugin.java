package com.codex.minigamepilot;

import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Home;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.overlay.ItemGuide;
import com.codex.minigamepilot.overlay.MinimapGuide;
import com.codex.minigamepilot.overlay.SceneGuide;
import com.codex.minigamepilot.overlay.StepPlaque;
import com.codex.minigamepilot.pilot.Flight;
import com.codex.minigamepilot.pilot.FurnacePilot;
import com.codex.minigamepilot.pilot.Pilots;
import com.codex.minigamepilot.ui.PilotPanel;
import com.codex.minigamepilot.ui.Sigil;
import com.codex.minigamepilot.ui.View;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Codex Minigame Pilot",
	description = "One clear next step for Motherlode Mine, Blast Furnace and Mahogany Homes: what to click, the path there, and why",
	tags = {"motherlode", "mlm", "blast furnace", "mahogany homes", "construction", "mining", "smithing", "guide", "codex"}
)
public class CodexMinigamePilotPlugin extends Plugin
{
	private static final String CONTRACT_KEY = "contract";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private CodexMinigamePilotConfig config;

	private final StepPlaque plaque = new StepPlaque();
	private volatile Flight flight;
	private Pilots pilots;
	private PilotPanel panel;
	private NavigationButton navButton;
	private SceneGuide sceneGuide;
	private MinimapGuide minimapGuide;
	private ItemGuide itemGuide;
	private String shown;
	private WorldPoint arrow;
	private WorldMapPoint mapPoint;

	@Provides
	CodexMinigamePilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexMinigamePilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		pilots = new Pilots(client, loadContract(), this::saveContract);
		panel = new PilotPanel();
		navButton = NavigationButton.builder()
			.tooltip("Codex Minigame Pilot")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		sceneGuide = new SceneGuide(client, () -> flight,
			() -> new SceneGuide.Settings(config.route(), config.labels(), config.others()));
		minimapGuide = new MinimapGuide(client, () -> flight, config::minimap);
		itemGuide = new ItemGuide(() -> flight);
		overlayManager.add(sceneGuide);
		overlayManager.add(minimapGuide);
		overlayManager.add(itemGuide);
		overlayManager.add(plaque);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				pilots.rescan();
			}
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(sceneGuide);
		overlayManager.remove(minimapGuide);
		overlayManager.remove(itemGuide);
		overlayManager.remove(plaque);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			clearArrow();
			clearMapPoint();
		});
		flight = null;
		pilots = null;
		panel = null;
		shown = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			pilots.clearScene();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			flight = null;
			plaque.show(null);
			publish();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		pilots.loadContract(loadContract());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		pilots.scene().add(e.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		pilots.scene().remove(e.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned e)
	{
		pilots.scene().add(e.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned e)
	{
		pilots.scene().remove(e.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned e)
	{
		pilots.scene().add(e.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned e)
	{
		pilots.scene().remove(e.getDecorativeObject());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type == ChatMessageType.GAMEMESSAGE || type == ChatMessageType.SPAM || type == ChatMessageType.MESBOX)
		{
			pilots.chat(Text.removeTags(event.getMessage()));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int id = event.getItemId();
		if (id == ItemIds.COAL_BAG || id == ItemIds.COAL_BAG_OPEN)
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INV);
			ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			boolean bankOpen = FurnacePilot.bankOpen(client);
			pilots.coalBagOption(Text.removeTags(event.getMenuOption()),
				inv == null ? 0 : inv.count(Bar.COAL), bankOpen && bank != null ? bank.count(Bar.COAL) : -1);
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() != GameState.LOGGED_IN || pilots == null)
		{
			return;
		}
		Bar bar = config.bar() == CodexMinigamePilotConfig.BarChoice.AUTO ? null : Bar.valueOf(config.bar().name());
		Flight f = pilots.fly(config.motherlode(), config.furnace(), config.homes(), bar, config.cofferLow(), System.currentTimeMillis());
		flight = f;
		plaque.show(config.plaque() && f.step != null ? f.step : null);
		updateArrow(f);
		updateMapPoint(f);
		publish();
	}

	private void updateArrow(Flight f)
	{
		WorldPoint want = config.hintArrow() && f.activity == View.Activity.HOMES ? f.destination : null;
		if (want != null && !want.equals(arrow))
		{
			client.setHintArrow(want);
			arrow = want;
		}
		else if (want == null)
		{
			clearArrow();
		}
	}

	/** Clears the hint arrow only if it is still the one the Pilot set. */
	private void clearArrow()
	{
		if (arrow != null && client.hasHintArrow() && arrow.equals(client.getHintArrowPoint()))
		{
			client.clearHintArrow();
		}
		arrow = null;
	}

	private void updateMapPoint(Flight f)
	{
		WorldPoint want = config.worldMap() && f.activity == View.Activity.HOMES ? f.destination : null;
		if (want == null)
		{
			clearMapPoint();
			return;
		}
		if (mapPoint != null && want.equals(mapPoint.getWorldPoint()))
		{
			return;
		}
		clearMapPoint();
		BufferedImage icon = Sigil.icon(20);
		mapPoint = WorldMapPoint.builder()
			.worldPoint(want)
			.image(icon)
			.name("Mahogany Homes client")
			.tooltip("Mahogany Homes client")
			.jumpOnClick(true)
			.snapToEdge(true)
			.build();
		worldMapPointManager.add(mapPoint);
	}

	private void clearMapPoint()
	{
		if (mapPoint != null)
		{
			worldMapPointManager.remove(mapPoint);
			mapPoint = null;
		}
	}

	/** Hands the panel a new view when anything on it has changed. */
	private void publish()
	{
		Flight f = flight;
		View view;
		if (client.getGameState() != GameState.LOGGED_IN || pilots == null)
		{
			view = View.loggedOut();
		}
		else if (f == null)
		{
			view = pilots.idleView();
		}
		else
		{
			view = f.view;
		}
		String signature = view.signature();
		if (signature.equals(shown))
		{
			return;
		}
		shown = signature;
		PilotPanel p = panel;
		SwingUtilities.invokeLater(() ->
		{
			if (p != null)
			{
				p.update(view);
			}
		});
	}

	private Contract loadContract()
	{
		String saved = configManager.getRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, CONTRACT_KEY);
		if (saved == null || saved.isEmpty())
		{
			return null;
		}
		String[] parts = saved.split(":");
		Home home = Home.byName(parts[0]);
		if (home == null)
		{
			return null;
		}
		Contract.Tier tier = parts.length > 1 ? Contract.Tier.byName(parts[1]) : null;
		return new Contract(home, tier);
	}

	private void saveContract(Contract contract)
	{
		if (contract == null)
		{
			configManager.unsetRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, CONTRACT_KEY);
			return;
		}
		String value = contract.getHome().getName() + (contract.getTier() == null ? "" : ":" + contract.getTier().getName());
		configManager.setRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, CONTRACT_KEY, value);
	}
}
