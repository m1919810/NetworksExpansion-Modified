package com.ytdd9527.networks.expansion.core.item.machine.cargo.advanced;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networks.expansion.util.DisplayGroupGenerators;

import dev.sefiraat.sefilib.entity.display.DisplayGroup;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.Theme;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class AdvancedLineTransferPusher extends AdvancedDirectional implements RecipeDisplayItem {

    private static final String KEY_UUID = "display-uuid";
    private boolean useSpecialModel;
    private Function<Location, DisplayGroup> displayGroupGenerator;
    private static final ItemStack AIR = new CustomItemStack(Material.AIR);
    private static final int TRANSPORT_LIMIT = 64;

    private static final int TRANSPORT_MODE_SLOT = 27;
    private static final int MINUS_SLOT = 36;
    private static final int SHOW_SLOT = 37;
    private static final int ADD_SLOT = 38;

    private int pushItemTick;
    private int maxDistance;
    private int limit;
    private static final int[] BACKGROUND_SLOTS = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 17, 18, 20, 22, 23, 27, 28, 30, 31, 33, 34, 35, 39, 40, 41, 42, 43, 44
    };
    private static final int[] TEMPLATE_BACKGROUND = new int[]{16};
    private static final int[] TEMPLATE_SLOTS = new int[]{24, 25, 26};
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;

    public static final CustomItemStack TEMPLATE_BACKGROUND_STACK = new CustomItemStack(
        Material.BLUE_STAINED_GLASS_PANE, Theme.PASSIVE + "指定需要推送的物品"
    );
    private static final String TICK_COUNTER_KEY = "chain_PusherPlus_tick_counter";

    public AdvancedLineTransferPusher(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, String configKey) {
        super(itemGroup, item, recipeType, recipe, NodeType.LINE_TRANSMITTER_PUSHER, TRANSPORT_LIMIT);
        for (int slot : TEMPLATE_SLOTS) {
            this.getSlotsToDrop().add(slot);
        }
        loadConfigurations(configKey);
    }

    private void loadConfigurations(String configKey) {
        int defaultMaxDistance = 32;
        int defaultPushItemTick = 1;
        boolean defaultUseSpecialModel = false;

        FileConfiguration config = Networks.getInstance().getConfig();

        this.maxDistance = config.getInt("items." + configKey + ".max-distance", defaultMaxDistance);
        this.pushItemTick = config.getInt("items." + configKey + ".pushitem-tick", defaultPushItemTick);
        this.useSpecialModel = config.getBoolean("items." + configKey + ".use-special-model.enable", defaultUseSpecialModel);


        Map<String, Function<Location, DisplayGroup>> generatorMap = new HashMap<>();
        generatorMap.put("cloche", DisplayGroupGenerators::generateCloche);
        generatorMap.put("cell", DisplayGroupGenerators::generateCell);

        this.displayGroupGenerator = null;

        if (this.useSpecialModel) {
            String generatorKey = config.getString("items." + configKey + ".use-special-model.type");
            this.displayGroupGenerator = generatorMap.get(generatorKey);
            if (this.displayGroupGenerator == null) {
                Networks.getInstance().getLogger().warning("未知的展示组类型 '" + generatorKey + "', 特殊模型已禁用。");
                this.useSpecialModel = false;
            }
        }
    }
    private void performPushItemOperationAsync(@Nonnull NetworkRoot root, @Nullable BlockMenu blockMenu) {
        if (blockMenu != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    tryPushItem(root, blockMenu);
                }
            }.runTaskAsynchronously(Networks.getInstance());
        }
    }
    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        super.onTick(blockMenu, block);
        int tickCounter = getTickCounter(block);
        tickCounter = (tickCounter + 1) % pushItemTick;
        if (tickCounter == 0) {
            final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());
            if (definition == null || definition.getNode() == null) {
                return;
            }
            NetworkRoot root = definition.getNode().getRoot();
            performPushItemOperationAsync(root, blockMenu);
        }
        updateTickCounter(block, tickCounter);
    }
    private int getTickCounter(Block block) {

        String tickCounterValue = BlockStorage.getLocationInfo(block.getLocation(), TICK_COUNTER_KEY);
        try {

            return (tickCounterValue != null) ? Integer.parseInt(tickCounterValue) : 0;
        } catch (NumberFormatException e) {

            return 0;
        }
    }
    private void updateTickCounter(Block block, int tickCounter) {

        BlockStorage.addBlockInfo(block.getLocation(), TICK_COUNTER_KEY, Integer.toString(tickCounter));
    }

    private void tryPushItem(@Nonnull NetworkRoot root, @Nonnull BlockMenu blockMenu) {
        final BlockFace direction = this.getCurrentDirection(blockMenu);

        Block targetBlock = blockMenu.getBlock().getRelative(direction);

        for (int i = 0; i <= maxDistance; i++) {

            final BlockMenu targetMenu = StorageCacheUtils.getMenu(targetBlock.getLocation());

            if (targetMenu == null || targetBlock == null || targetBlock.getType() == Material.AIR) {
                return;
            }

            int currentLimit = getCurrentNumber(blockMenu.getLocation());
            String currentTransportMode = getCurrentTransportMode(blockMenu.getLocation());

            for (int itemSlot : this.getItemSlots()) {
                final ItemStack testItem = blockMenu.getItemInSlot(itemSlot);

                if (testItem == null || testItem.getType() == Material.AIR) {
                    continue;
                }

                final ItemStack clone = testItem.clone();
                clone.setAmount(1);

                int[] slots = targetMenu.getPreset().getSlotsAccessedByItemTransport(targetMenu, ItemTransportFlow.INSERT, clone);

                int freeAmount = 0;
                int retrievedAmount = 0;
                // 读取模式
                switch (currentTransportMode) {
                    // 无限制模式
                    case TRANSPORT_MODE_NONE -> {
                        // 计算总共需要推送的数量
                        for (int slot : slots) {
                            final ItemStack itemStack = targetMenu.getItemInSlot(slot);
                            if (itemStack == null || itemStack.getType() == Material.AIR) {
                                freeAmount += clone.getMaxStackSize();
                            } else {
                                if (StackUtils.itemsMatch(itemStack, clone)) {
                                    freeAmount += itemStack.getMaxStackSize() - itemStack.getAmount();
                                }
                            }

                            if (freeAmount > currentLimit) {
                                freeAmount = currentLimit;
                                break;
                            }
                        }

                        // 直接推送物品
                        final ItemRequest itemRequest = new ItemRequest(clone, freeAmount);
                        ItemStack retrieved = root.getItemStack(itemRequest);
                        if (retrieved != null) {
                            targetMenu.pushItem(retrieved, slots);
                        }
                    }
                    // 仅空模式
                    case TRANSPORT_MODE_NULL_ONLY -> {
                        for (int slot : slots) {
                            // 读取每个槽的物品
                            final ItemStack itemStack = targetMenu.getItemInSlot(slot);

                            // 仅空槽会被运输
                            if (itemStack == null || itemStack.getType() == Material.AIR) {
                                // 计算需要推送的数量
                                int amount = clone.getMaxStackSize();
                                if (retrievedAmount + amount > currentLimit) {
                                    amount = currentLimit - retrievedAmount;
                                }

                                // 推送物品
                                final ItemRequest itemRequest = new ItemRequest(clone, amount);
                                ItemStack retrieved = root.getItemStack(itemRequest);

                                // 只推送到指定的格
                                if (retrieved != null) {
                                    targetMenu.pushItem(retrieved, slot);
                                    // 增加数量
                                    retrievedAmount += retrieved.getAmount();
                                }
                            }
                            if (retrievedAmount >= currentLimit) {
                                break;
                            }
                        }
                    }

                    // 仅非空模式
                    case TRANSPORT_MODE_NONNULL_ONLY -> {
                        for (int slot : slots) {
                            if (retrievedAmount >= currentLimit) {
                                break;
                            }
                            // 读取每个槽的物品
                            final ItemStack itemStack = targetMenu.getItemInSlot(slot);

                            // 仅非空模式本质上就是只运输到有相同物品的格子
                            if (StackUtils.itemsMatch(clone, itemStack)) {

                                // 计算需要推送的数量
                                int amount = itemStack.getMaxStackSize() - itemStack.getAmount();
                                if (retrievedAmount + amount > currentLimit) {
                                    amount = currentLimit - retrievedAmount;
                                }

                                if (amount <= 0) {
                                    continue;
                                }

                                // 推送物品
                                final ItemRequest itemRequest = new ItemRequest(clone, amount);
                                ItemStack retrieved = root.getItemStack(itemRequest);

                                // 只推送到指定的格
                                if (retrieved != null) {
                                    // 增加数量
                                    targetMenu.pushItem(retrieved, slot);
                                    retrievedAmount += amount - retrieved.getAmount();
                                }
                            }
                        }
                    }
                }

            }
            targetBlock = targetBlock.getRelative(direction);
        }
    }
    @Nonnull
    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }
    @Nullable
    @Override
    protected int[] getOtherBackgroundSlots() {
        return TEMPLATE_BACKGROUND;
    }
    @Nullable
    @Override
    protected CustomItemStack getOtherBackgroundStack() {
        return TEMPLATE_BACKGROUND_STACK;
    }
    @Override
    public int getNorthSlot() {
        return NORTH_SLOT;
    }
    @Override
    public int getSouthSlot() {
        return SOUTH_SLOT;
    }
    @Override
    public int getEastSlot() {
        return EAST_SLOT;
    }
    @Override
    public int getWestSlot() {
        return WEST_SLOT;
    }
    @Override
    public int getUpSlot() {
        return UP_SLOT;
    }
    @Override
    public int getDownSlot() {
        return DOWN_SLOT;
    }
    @Override
    public int[] getItemSlots() {
        return TEMPLATE_SLOTS;
    }
    @Override
    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.BLUE, 2);
    }
    @Override
    public void onPlace(BlockPlaceEvent e) {
        super.onPlace(e);
        if (useSpecialModel) {
            e.getBlock().setType(Material.BARRIER);
            setupDisplay(e.getBlock().getLocation());
        }
    }

    @Override
    public void postBreak(BlockBreakEvent e) {
        super.postBreak(e);
        Location location = e.getBlock().getLocation();
        removeDisplay(location);
        e.getBlock().setType(Material.AIR);
    }

    private void setupDisplay(@Nonnull Location location) {
        if (this.displayGroupGenerator != null) {
            DisplayGroup displayGroup = this.displayGroupGenerator.apply(location.clone().add(0.5, 0, 0.5));
            StorageCacheUtils.setData(location, KEY_UUID, displayGroup.getParentUUID().toString());
        }
    }
    private void removeDisplay(@Nonnull Location location) {
        DisplayGroup group = getDisplayGroup(location);
        if (group != null) {
            group.remove();
        }
    }
    @Nullable
    private UUID getDisplayGroupUUID(@Nonnull Location location) {
        String uuid = StorageCacheUtils.getData(location, KEY_UUID);
        if (uuid == null) {
            return null;
        }
        return UUID.fromString(uuid);
    }
    @Nullable
    private DisplayGroup getDisplayGroup(@Nonnull Location location) {
        UUID uuid = getDisplayGroupUUID(location);
        if (uuid == null) {
            return null;
        }
        return DisplayGroup.fromUUID(uuid);
    }

    protected int getMinusSlot() {
        return MINUS_SLOT;
    }

    protected int getShowSlot() {
        return SHOW_SLOT;
    }

    protected int getAddSlot() {
        return ADD_SLOT;
    }
    @Override
    public void postRegister() {
        super.postRegister();
        setLimit(3456);
    }
    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> displayRecipes  = new ArrayList<>(6);
        displayRecipes.add(new CustomItemStack(Material.BOOK,
                "&a⇩运行频率⇩",
                "",
                "&e执行频率&f:",
                "&f-&7[&a推送频率&7]&f:&7 每 &6" + pushItemTick + " SfTick &7推送一次",
                "&f-&7[&a1 SfTick=0.5s]",
                "",
                "&f-&7 简而言之，链式推送器不会频繁操作，从而保持服务器流畅"
        ));
        displayRecipes.add(AIR);
        displayRecipes.add(new CustomItemStack(Material.BOOK,
                "&a⇩功能⇩",
                "",
                "&e最大距离&7: &6" + maxDistance + "格",
                "",
                "&e运行流程&f:",
                "&f-&7 打开界面设置你所需的方向",
                "&f-&7 网络链式推送器当前方块开始，沿着设定方向搜索",
                "",
                "&e推送条件&f:",
                "&f-&7[&a推送物品&7]&f:&7遇到可输入槽位，且物品不是空气时",
                "&f-&7[&a停止条件①&7]&f:&7达到最大推送距离[&6" + maxDistance + "格&7]"
        ));
        displayRecipes.add(AIR);
        displayRecipes.add(new CustomItemStack(Material.BOOK,
                "&a⇩使用指南⇩",
                "",
                "&7网络链式推送器效率最大化建议：",
                "",
                "&f-&7 如果你使用网络链式推送器就没必要给机器继续使用推送器了",
                "&f-&7 不要双管齐下多此一举",
                "",
                "&f-&7 充分利用网络链式推送器范围: 每次推送物品可以覆盖长达&7[&6"+maxDistance+"格&7]的距离",
                "&f-&7 确保您的布局设计能够覆盖多个机器，以实现最大效率",
                "",
                "&f-&7 避免单个机器配置: 不要仅在一个机器上使用网络链式推送器",
                "&f-&7 这样做会限制您的自动化系统的潜力和扩展性",
                "",
                "&f-&7请遵循这些建议，您将能够最大化每个链式推送器的工作效能，",
                "&f-&7同时保持也可以服务器流畅运行"
        ));
        return displayRecipes ;
    }

    @Override
    protected int getTransportModeSlot() {
        return TRANSPORT_MODE_SLOT;
    }
}