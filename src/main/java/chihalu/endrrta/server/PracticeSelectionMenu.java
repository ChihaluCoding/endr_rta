package chihalu.endrrta.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import chihalu.endrrta.config.EndrRTAConfigManager;

public final class PracticeSelectionMenu extends ChestMenu {
	private static final int DEFAULT_ROWS = 6;
	private static final int DEFAULT_MENU_SIZE = 54;
	private static final Set<Relative> ABSOLUTE_TELEPORT = Set.of();
	private static final float MENU_CLICK_SOUND_PITCH = 2.0F;
	private static final float MENU_CLICK_SOUND_VOLUME = 1.0F;
	private static final String BASTION_SCENARIO_ID = "bastion";
	private static final String BASTION_BACK_ID = "bastion_back";
	private static final String GAME_SELECTION_ID = "game_selection";
	private static final String MAIN_BACK_ID = "main_back";

	private final Container menuContainer;
	private final Map<String, PracticeDestination> destinations;
	private final Map<Integer, String> scenarioIdsBySlot;
	private MenuPage page;

	public PracticeSelectionMenu(int containerId, Inventory playerInventory, Map<String, PracticeDestination> destinations) {
		this(containerId, playerInventory, createMainMenuContainer(destinations), destinations, createMainSlotMap(), MenuPage.MAIN);
	}

	private static PracticeSelectionMenu launcher(int containerId, Inventory playerInventory, Map<String, PracticeDestination> destinations) {
		return new PracticeSelectionMenu(containerId, playerInventory, createLauncherMenuContainer(), destinations, createLauncherSlotMap(), MenuPage.LAUNCHER);
	}

	public static void openLauncher(ServerPlayer player, Map<String, PracticeDestination> destinations) {
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, ignoredPlayer) -> PracticeSelectionMenu.launcher(containerId, inventory, destinations),
				titleForPage(MenuPage.LAUNCHER)
		));
	}

	public static void openMain(ServerPlayer player, Map<String, PracticeDestination> destinations) {
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, ignoredPlayer) -> new PracticeSelectionMenu(containerId, inventory, destinations),
				titleForPage(MenuPage.MAIN)
		));
	}

	private PracticeSelectionMenu(
			int containerId,
			Inventory playerInventory,
			Container menuContainer,
			Map<String, PracticeDestination> destinations,
			Map<Integer, String> scenarioIdsBySlot,
			MenuPage page
	) {
		super(MenuType.GENERIC_9x6, containerId, playerInventory, menuContainer, DEFAULT_ROWS);
		this.menuContainer = menuContainer;
		this.destinations = destinations;
		this.scenarioIdsBySlot = new HashMap<>(scenarioIdsBySlot);
		this.page = page;
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (player instanceof ServerPlayer serverPlayer && handleDisplaySlotClick(serverPlayer, slotId)) {
			return;
		}
		if (slotId >= 0 && slotId < menuContainer.getContainerSize()) {
			return;
		}
		super.clicked(slotId, button, input, player);
	}

	public boolean handleDisplaySlotClick(ServerPlayer player, int slotId) {
		String scenarioId = scenarioIdsBySlot.get(slotId);
		if (scenarioId == null) {
			return false;
		}
		playMenuClickSound(player);
		if (page == MenuPage.LAUNCHER && GAME_SELECTION_ID.equals(scenarioId)) {
			updateToMainMenu();
			return true;
		}
		if (page == MenuPage.MAIN && BASTION_SCENARIO_ID.equals(scenarioId)) {
			updateToBastionMenu();
			return true;
		}
		if (page == MenuPage.MAIN && MAIN_BACK_ID.equals(scenarioId)) {
			updateToLauncherMenu();
			return true;
		}
		if (page == MenuPage.BASTION && BASTION_BACK_ID.equals(scenarioId)) {
			updateToMainMenu();
			return true;
		}
		if ("bastion_random".equals(scenarioId)) {
			handleRandomBastion(player);
			return true;
		}
		PracticeStartPointManager.selectScenario(player, scenarioId);
		PracticeDestination destination = destinations.get(scenarioId);
		if (destination == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] " + PracticeStartPointManager.label(scenarioId)
					+ " のTP先が未設定です。/setstartpoint x y z ow/nether/end で設定してください。"));
			return true;
		}
		teleportToPractice(player, destination);
		return true;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.index >= menuContainer.getContainerSize() && super.canTakeItemForPickAll(stack, slot);
	}

	public boolean isDisplaySlot(int slotIndex) {
		return slotIndex >= 0 && slotIndex < menuContainer.getContainerSize();
	}

	public boolean isBastionPage() {
		return page == MenuPage.BASTION;
	}

	public Component pageTitle() {
		return titleForPage(resolvePageFromContents());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	private void teleportToPractice(ServerPlayer player, PracticeDestination destination) {
		ServerLevel targetLevel = player.level().getServer().getLevel(destination.dimension());
		if (targetLevel == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 練習場のディメンションが見つかりません: " + destination.dimension().identifier()));
			return;
		}

		EndrRTAConfigManager.get().practiceScenario = destination.scenarioId();
		PracticeStartPointManager.selectScenario(player, destination.scenarioId());
		EndrRTAConfigManager.save();
		player.closeContainer();
		player.teleportTo(
				targetLevel,
				destination.pos().getX() + 0.5D,
				destination.pos().getY(),
				destination.pos().getZ() + 0.5D,
				ABSOLUTE_TELEPORT,
				player.getYRot(),
				player.getXRot(),
				false
		);
		player.sendSystemMessage(Component.literal("[EnderRTA] " + destination.label() + " 練習場へ移動しました。"));
	}

	private void updateToMainMenu() {
		if (page == MenuPage.MAIN) {
			return;
		}
		replaceMenuContent(createMainMenuContainer(destinations), createMainSlotMap(), MenuPage.MAIN);
	}

	private void updateToLauncherMenu() {
		if (page == MenuPage.LAUNCHER) {
			return;
		}
		replaceMenuContent(createLauncherMenuContainer(), createLauncherSlotMap(), MenuPage.LAUNCHER);
	}

	private void updateToBastionMenu() {
		if (page == MenuPage.BASTION) {
			return;
		}
		replaceMenuContent(createBastionMenuContainer(destinations), createBastionSlotMap(), MenuPage.BASTION);
	}

	private void replaceMenuContent(Container newContainer, Map<Integer, String> newScenarioIdsBySlot, MenuPage newPage) {
		for (int i = 0; i < menuContainer.getContainerSize(); i++) {
			ItemStack stack = i < newContainer.getContainerSize() ? newContainer.getItem(i) : ItemStack.EMPTY;
			menuContainer.setItem(i, stack);
		}
		scenarioIdsBySlot.clear();
		scenarioIdsBySlot.putAll(newScenarioIdsBySlot);
		page = newPage;
		broadcastChanges();
	}

	private void handleRandomBastion(ServerPlayer player) {
		PracticeDestination destination = PracticeStartPointManager.randomBastionDestination();
		if (destination == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] ランダム候補が未設定です。住居、橋、宝箱部屋、ホグリンの部屋のいずれかを /setstartpoint で設定してください。"));
			return;
		}
		PracticeStartPointManager.selectScenario(player, destination.scenarioId());
		teleportToPractice(player, destination);
	}

	private static void playMenuClickSound(ServerPlayer player) {
		player.level().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				SoundEvents.NOTE_BLOCK_HARP,
				SoundSource.BLOCKS,
				MENU_CLICK_SOUND_VOLUME,
				MENU_CLICK_SOUND_PITCH
		);
	}

	private static Container createMainMenuContainer(Map<String, PracticeDestination> destinations) {
		SimpleContainer container = new SimpleContainer(DEFAULT_MENU_SIZE);
		addScenario(container, destinations, 10, "nether_fortress", "ネザー要塞", Items.BLAZE_ROD);
		addScenario(container, destinations, 12, "bastion", "ピグリン要塞", Items.PIGLIN_HEAD);
		addScenario(container, destinations, 14, "warped_forest_pearls", "歪んだ森エンパ", Items.ENDER_PEARL);
		addScenario(container, destinations, 28, "lava_pool_portal", "マグマ溜まりゲート", Items.LAVA_BUCKET);
		addScenario(container, destinations, 16, "ender_dragon", "エンドラ討伐", Items.DRAGON_HEAD);
		addMenuButton(container, 53, "前の画面に戻る", Items.BARRIER);
		return container;
	}

	private static Container createLauncherMenuContainer() {
		SimpleContainer container = new SimpleContainer(DEFAULT_MENU_SIZE);
		ItemStack gameSelection = new ItemStack(Items.DIAMOND_SWORD);
		gameSelection.set(DataComponents.CUSTOM_NAME, Component.literal("ゲーム選択"));
		container.setItem(10, gameSelection);
		return container;
	}

	private static Container createBastionMenuContainer(Map<String, PracticeDestination> destinations) {
		SimpleContainer container = new SimpleContainer(DEFAULT_MENU_SIZE);
		addScenario(container, destinations, 10, "bastion_housing", "住居", Items.CRIMSON_DOOR);
		addScenario(container, destinations, 12, "bastion_bridge", "橋", Items.LADDER);
		addScenario(container, destinations, 14, "bastion_treasure", "宝箱部屋", Items.CHEST);
		addScenario(container, destinations, 16, "bastion_hoglin_stables", "ホグリンの部屋", Items.HOGLIN_SPAWN_EGG);
		addScenario(container, destinations, 40, "bastion_random", "ランダム", Items.COMPASS, true);
		addMenuButton(container, 53, "前の画面に戻る", Items.BARRIER);
		return container;
	}

	private static Map<Integer, String> createMainSlotMap() {
		Map<Integer, String> slots = new HashMap<>();
		slots.put(10, "nether_fortress");
		slots.put(12, "bastion");
		slots.put(14, "warped_forest_pearls");
		slots.put(28, "lava_pool_portal");
		slots.put(16, "ender_dragon");
		slots.put(53, MAIN_BACK_ID);
		return Map.copyOf(slots);
	}

	private static Map<Integer, String> createLauncherSlotMap() {
		Map<Integer, String> slots = new HashMap<>();
		slots.put(10, GAME_SELECTION_ID);
		return Map.copyOf(slots);
	}

	private static Map<Integer, String> createBastionSlotMap() {
		Map<Integer, String> slots = new HashMap<>();
		slots.put(10, "bastion_housing");
		slots.put(12, "bastion_bridge");
		slots.put(14, "bastion_treasure");
		slots.put(16, "bastion_hoglin_stables");
		slots.put(40, "bastion_random");
		slots.put(53, BASTION_BACK_ID);
		return Map.copyOf(slots);
	}

	private static void addScenario(SimpleContainer container, Map<String, PracticeDestination> destinations, int slot, String scenarioId, String label, Item item) {
		addScenario(container, destinations, slot, scenarioId, label, item, false);
	}

	private static void addScenario(SimpleContainer container, Map<String, PracticeDestination> destinations, int slot, String scenarioId, String label, Item item, boolean alwaysAvailable) {
		ItemStack stack = new ItemStack(item);
		PracticeDestination destination = destinations.get(scenarioId);
		String suffix = !alwaysAvailable && destination == null ? "（未設定）" : "";
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(label + suffix));
		container.setItem(slot, stack);
	}

	private static void addMenuButton(SimpleContainer container, int slot, String label, Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
		container.setItem(slot, stack);
	}

	private static Component titleForPage(MenuPage page) {
		return switch (page) {
			case LAUNCHER -> Component.literal("メニュー");
			case MAIN -> Component.literal("ゲーム選択");
			case BASTION -> Component.literal("ピグリン要塞");
		};
	}

	private MenuPage resolvePageFromContents() {
		ItemStack firstScenario = menuContainer.getItem(10);
		if (firstScenario.is(Items.DIAMOND_SWORD)) {
			return MenuPage.LAUNCHER;
		}
		if (firstScenario.is(Items.CRIMSON_DOOR) || menuContainer.getItem(40).is(Items.COMPASS)) {
			return MenuPage.BASTION;
		}
		return MenuPage.MAIN;
	}

	private enum MenuPage {
		LAUNCHER,
		MAIN,
		BASTION
	}

}
