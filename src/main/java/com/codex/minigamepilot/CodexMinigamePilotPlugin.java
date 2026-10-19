package com.codex.minigamepilot;

import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.NavigatorGoals;
import com.codex.minigamepilot.goal.GoalWatch;
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
import com.codex.minigamepilot.pilot.FieldNotes;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.events.ConfigChanged;
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
	private static final String PACE_KEY = "pace.";
	private static final String VERSION = "1.1";
	/** Where Codex Navigator saves its goal board for companion plugins. */
	static final String NAVIGATOR_GROUP = "codexnavigator";
	static final String NAVIGATOR_BOARD_KEY = "goalBoard";

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

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

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
	private FieldNotes notes;
	private final Map<Integer, Integer> lastInventory = new HashMap<>();
	/** Whether lastInventory holds this login's inventory yet; the first one seen is only a base. */
	private boolean inventoryKnown;
	/**
	 * Ticks to wait after logging in or hopping before reading the minigames: the inventory,
	 * skills and varbits arrive over the first ticks, and a pack read as empty then full would
	 * look like a haul. RuneLite's own XP tracker waits the same.
	 */
	private static final int LOGIN_GRACE = 2;
	private int loginGrace;
	private GoalWatch goals;
	private String navigatorJson;
	private NavigatorGoals.Board navigatorBoard;
	/** Where the Pilot keeps its own look at the bank's supplies. */
	static final String STOCK_KEY = "stock.bank";
	/** The supplies in the bank as the Pilot last saw them, or null. */
	private BankStock ownStock;
	private String stewardJson;
	private BankStock stewardStock;

	@Provides
	CodexMinigamePilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexMinigamePilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		notes = new FieldNotes(new File(RuneLite.RUNELITE_DIR, "codex-minigame-pilot"), gson);
		if (config.fieldNotes())
		{
			notes.open(VERSION);
		}
		pilots = new Pilots(client, loadContract(), this::saveContract, notes, this::loadBests, this::saveBests,
			id -> (int) Math.min(Integer.MAX_VALUE, itemManager.getItemPrice(id)),
			key -> configManager.getRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, key),
			(key, value) -> configManager.setRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, key, value),
			this::bankStock);
		goals = new GoalWatch(key -> configManager.getRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, key),
			(key, value) -> configManager.setRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, key, value),
			this::navigatorBoard);
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
				seedInventory(client.getItemContainer(InventoryID.INV));
				pilots.rescan();
				goals.reload();
				pilots.loadContract(loadContract());
				loadStock();
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
		goals = null;
		panel = null;
		shown = null;
		notes.shutdown();
		notes = null;
		lastInventory.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CodexMinigamePilotConfig.GROUP.equals(event.getGroup()) || !"fieldNotes".equals(event.getKey()) || notes == null)
		{
			return;
		}
		if (config.fieldNotes())
		{
			notes.open(VERSION);
		}
		else
		{
			notes.close();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (pilots != null)
		{
			pilots.varbit(event.getVarbitId(), event.getValue());
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			if (goals != null)
			{
				goals.bank(event.getItemContainer());
			}
			seeBank(event.getItemContainer());
			return;
		}
		if (event.getContainerId() != InventoryID.INV || pilots == null)
		{
			return;
		}
		if (!inventoryKnown)
		{
			// The inventory as it arrives after logging in: a base, not a change.
			seedInventory(event.getItemContainer());
			return;
		}
		Map<Integer, Integer> now = new HashMap<>();
		for (Item item : event.getItemContainer().getItems())
		{
			if (item.getId() > 0)
			{
				now.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		Map<Integer, Integer> changes = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : now.entrySet())
		{
			int d = e.getValue() - lastInventory.getOrDefault(e.getKey(), 0);
			if (d != 0)
			{
				changes.put(e.getKey(), d);
			}
		}
		for (Map.Entry<Integer, Integer> e : lastInventory.entrySet())
		{
			if (!now.containsKey(e.getKey()))
			{
				changes.put(e.getKey(), -e.getValue());
			}
		}
		lastInventory.clear();
		lastInventory.putAll(now);
		pilots.inventory(changes);
	}

	/** Takes an inventory as the base later changes are measured from. */
	private void seedInventory(ItemContainer container)
	{
		lastInventory.clear();
		if (container == null)
		{
			inventoryKnown = false;
			return;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				lastInventory.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		inventoryKnown = true;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				pilots.clearScene();
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				// What was seen before is no base for what comes after: the next login may be
				// another account, and this one may have played elsewhere in between.
				pilots.relogged(System.currentTimeMillis());
				if (goals != null)
				{
					goals.relog();
				}
				lastInventory.clear();
				inventoryKnown = false;
				loginGrace = LOGIN_GRACE;
				// The game drops its hint arrow with the scene; set it again once back.
				arrow = null;
				flight = null;
				plaque.show(null);
				publish();
				break;
			case LOGGING_IN:
				loginGrace = LOGIN_GRACE;
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		pilots.loadContract(loadContract());
		if (goals != null)
		{
			goals.reload();
		}
		loadStock();
	}

	/** This account's saved look at the bank, after logging in or switching account. */
	private void loadStock()
	{
		ownStock = BankStock.decode(configManager.getRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, STOCK_KEY));
		stewardJson = null;
		stewardStock = null;
	}

	/** The bank changed while open: keep its supplies, saved only when they change. */
	private void seeBank(ItemContainer container)
	{
		if (container == null)
		{
			return;
		}
		Map<Integer, Long> counts = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0 && BankStock.isSupply(item.getId()))
			{
				counts.merge(item.getId(), (long) item.getQuantity(), Long::sum);
			}
		}
		BankStock seen = BankStock.seen(counts, System.currentTimeMillis());
		if (!seen.sameItems(ownStock))
		{
			configManager.setRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, STOCK_KEY, seen.encode());
		}
		ownStock = seen;
	}

	/**
	 * The supplies in the bank: the Pilot's own look or Codex Bank Steward's shared snapshot,
	 * whichever is newer, or null when neither has seen the bank. Bank Steward's is parsed again
	 * only when it changes.
	 */
	private BankStock bankStock()
	{
		String json = configManager.getRSProfileConfiguration(BankStock.STEWARD_GROUP, BankStock.STEWARD_KEY);
		if (json == null || json.isEmpty())
		{
			stewardJson = null;
			stewardStock = null;
		}
		else if (!json.equals(stewardJson))
		{
			stewardJson = json;
			stewardStock = BankStock.steward(gson, json);
		}
		return BankStock.newest(ownStock, stewardStock);
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
			pilots.chat(type.name(), Text.removeTags(event.getMessage()));
			if (goals != null)
			{
				goals.chat(Text.removeTags(event.getMessage()));
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		pilots.click(Text.removeTags(event.getMenuOption()), Text.removeTags(event.getMenuTarget()), event.getId(),
			event.getMenuAction() == null ? null : event.getMenuAction().name(), event.getItemId());
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
		if (loginGrace > 0 || client.getItemContainer(InventoryID.INV) == null)
		{
			loginGrace = Math.max(0, loginGrace - 1);
			return;
		}
		Bar bar = config.bar() == CodexMinigamePilotConfig.BarChoice.AUTO ? null : Bar.valueOf(config.bar().name());
		Flight f = pilots.fly(config.motherlode(), config.furnace(), config.homes(), bar, config.cofferLow(), System.currentTimeMillis());
		if (config.showGoals() && goals != null)
		{
			goals.tick(client, f.view, new GoalWatch.Choices(
				config.motherlode() ? config.motherlodeGoal().choice() : Goals.Choice.OFF,
				config.homes() ? config.homesGoal().choice() : Goals.Choice.OFF,
				config.furnace() ? config.furnaceGoal() : -1),
				f.activity == View.Activity.HOMES || f.activity == View.Activity.NONE ? loadContract() : null);
		}
		flight = f;
		String note = pilots.takeNote();
		if (note != null && config.visitChat())
		{
			chat(note);
		}
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

	private void chat(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Codex Pilot: ")
				.append(ChatColorType.NORMAL).append(text)
				.build())
			.build());
	}

	private String loadBests(View.Activity activity)
	{
		return configManager.getRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, PACE_KEY + activity.name());
	}

	private void saveBests(View.Activity activity, String bests)
	{
		configManager.setRSProfileConfiguration(CodexMinigamePilotConfig.GROUP, PACE_KEY + activity.name(), bests);
	}

	/**
	 * Codex Navigator's goal board for this account, read from where Navigator saves it for
	 * companion plugins, or null when Navigator is not installed or has nothing pinned. It is
	 * parsed again only when it changes.
	 */
	private NavigatorGoals.Board navigatorBoard()
	{
		String json = configManager.getRSProfileConfiguration(NAVIGATOR_GROUP, NAVIGATOR_BOARD_KEY);
		if (json == null || json.isEmpty())
		{
			navigatorJson = null;
			navigatorBoard = null;
			return null;
		}
		if (!json.equals(navigatorJson))
		{
			navigatorJson = json;
			try
			{
				navigatorBoard = gson.fromJson(json, NavigatorGoals.Board.class);
			}
			catch (JsonParseException | IllegalStateException e)
			{
				// A board this version cannot read is left alone: goals work as without Navigator.
				navigatorBoard = null;
			}
		}
		return navigatorBoard;
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
