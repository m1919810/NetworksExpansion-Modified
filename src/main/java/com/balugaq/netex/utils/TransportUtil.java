package com.balugaq.netex.utils;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkAsyncUtil;
import io.github.sefiraat.networks.managers.ExperimentalFeatureManager;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.N;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class TransportUtil {
    /**
     * grabbing utils
     * @param root
     * @param item
     * @param limit
     * @param loc
     * @return
     */
    public static int sendLimitedItemToRoot(NetworkRoot root, ItemStack item, int limit,Location loc) {
        return NetworkAsyncUtil.getInstance().ensureLocation(loc,()->{
            int itemAmount = item.getAmount();
            if(itemAmount <= limit) {
                root.addItemStack(item);
                int left=item.getAmount();
                return limit-itemAmount+left;
            }else{
                ItemStack sample = StackUtils.getLimitedRequest(item, limit);
                item.setAmount(itemAmount-limit);
                root.addItemStack(sample);
                int left=sample.getAmount();
                if(left>0){
                    item.setAmount(item.getAmount()+left);
                }
                return left;
            }
        });
    }

    /**
     * fetch item using request , then push into menu
     * @param blockMenu
     * @param itemRequest
     * @param matchAmount
     * @param limit
     * @param breakAfterFirstMatch
     * @param breakWhenNoMatch
     * @param itemRequestor
     * @param slots
     */
    public static void fetchItemAndPush(BlockMenu blockMenu, ItemRequest itemRequest, ToIntFunction<ItemStack> matchAmount, int limit, boolean breakAfterFirstMatch,boolean breakWhenNoMatch,Function<ItemRequest, ItemStack> itemRequestor, int... slots){

        var re=calFetchItem(blockMenu::getItemInSlot, itemRequest, matchAmount, limit, breakAfterFirstMatch, breakWhenNoMatch, slots);
        if(re==null){
            return;
        }
        int space=re.getFirstValue();
        itemRequest.setAmount(space);

        final ItemStack retrieved = itemRequestor.apply(itemRequest);
        if (retrieved != null && retrieved.getType() != Material.AIR) {
            //BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
            NetworkAsyncUtil.getInstance().ensureLocation(blockMenu.getLocation(),()->{
                BlockMenuUtil.pushItemAlreadyMatched(blockMenu, retrieved, re.getSecondValue());
            });
        }
    }

    /**
     * calculate item fetch info: amount , with matching slots
     * @param indexer
     * @param itemRequest
     * @param matchAmount
     * @param limit
     * @param breakAfterFirstMatch
     * @param breakWhenNoMatch
     * @param slots
     * @return
     * @param <T>
     */
    public static <T extends Object> Pair<Integer,List<Integer>> calFetchItem(IntFunction<T> indexer, ItemRequest itemRequest, ToIntFunction<T> matchAmount, int limit, boolean breakAfterFirstMatch, boolean breakWhenNoMatch, int... slots){
        int freeSpace = 0;
        int maxStackSize=itemRequest.getMaxStackSize();
        if(maxStackSize<=0)return null;
        limit=Math.min(limit,breakAfterFirstMatch?maxStackSize:(slots.length*maxStackSize));
        List<Integer> matchedSlots=new ArrayList<>(slots.length);
        for (int slot : slots) {
            int match=matchAmount.applyAsInt(indexer.apply(slot));
            if (match > 0) {
                freeSpace += match;
                matchedSlots.add(slot);
                if(breakAfterFirstMatch) {
                    //first stop mode
                    break;
                }
            }else if(breakWhenNoMatch) {
                //if match Amount<=0 in this slot,then next slots probably no match
                break;
            }
            if(freeSpace >= limit){
                break;
            }
        }
        if (freeSpace <= 0||matchedSlots.isEmpty()) {
            return null;
        }
        return new Pair<>(Math.min(freeSpace, limit), matchedSlots);
    }

    public static void fetchItemAndPush(BlockMenu blockMenu, ItemRequest itemRequest, ToIntFunction<ItemStack> matchAmount, int limit, boolean breakAfterFirstMatch, Function<ItemRequest,ItemStack> itemRequestor, int... slots) {
        fetchItemAndPush( blockMenu, itemRequest, matchAmount, limit, breakAfterFirstMatch, false, itemRequestor, slots);
    }




    public static int commonMatch(ItemStack itemStack,ItemRequest itemRequest){
        int maxStackSize=itemRequest.getMaxStackSize();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return maxStackSize;
        } else {
            if (itemStack.getAmount() >= maxStackSize) {
                return 0;
            }
            if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                return maxStackSize - itemStack.getAmount();
            }
            return 0;
        }
    }
//    public static int commonMatchCache(@Nonnull ItemStackCache itemStack, ItemRequest itemRequest){
//        int maxStackSize=itemRequest.getMaxStackSize();
//        if (itemStack.getItemStack() == null || itemStack.getItemType() == Material.AIR) {
//            return maxStackSize;
//        } else {
//            if (itemStack.getItemAmount() >= maxStackSize) {
//                return 0;
//            }
//            if (StackUtils.itemsMatch(itemRequest, itemStack)) {
//                return maxStackSize - itemStack.getItemAmount();
//            }
//            return 0;
//        }
//    }
    public static void outPower(@Nonnull Location targetLocation, @Nonnull NetworkRoot root, int rate) {
        SlimefunBlockData data=StorageCacheUtils.getBlock(targetLocation);
        if(data==null){
            return;
        }
        if(!data.isDataLoaded()){
            StorageCacheUtils.requestLoad(data);
            return;
        }
        final SlimefunItem slimefunItem = SlimefunItem.getById(data.getSfId());
        if (!(slimefunItem instanceof EnergyNetComponent component) || slimefunItem instanceof NetworkObject) {
            return;
        }
        if(!component.isChargeable()){
            return;
        }
        final int capacity = component.getCapacity();
        final int chargeInt = component.getCharge(targetLocation);
        final int space = capacity - chargeInt;
        if (space <= 0) {
            return;
        }
        final int possibleGeneration = Math.min(rate, space);

        final long power = root.getRootPower();
        if (power <= 0) {
            return;
        }
        final int gen = power < possibleGeneration ? (int) power : possibleGeneration;
        component.addCharge(targetLocation, gen);
        root.removeRootPower(gen);
    }
}
