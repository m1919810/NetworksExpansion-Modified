package com.balugaq.netex.utils;

import com.balugaq.netex.api.enums.TransportMode;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.sefiraat.networks.NetworkAsyncUtil;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.managers.ExperimentalFeatureManager;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.AdminDebuggable;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import lombok.experimental.UtilityClass;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static com.balugaq.netex.utils.TransportUtil.*;

@UtilityClass
public class LineOperationUtil {
    public static void doOperation(Location startLocation, BlockFace direction, int limit, Consumer<BlockMenu> consumer) {
        doOperation(startLocation, direction, limit, false, true, consumer);
    }

    public static void doOperation(Location startLocation, BlockFace direction, int limit, boolean skipNoMenu, Consumer<BlockMenu> consumer) {
        doOperation(startLocation, direction, limit, skipNoMenu, true, consumer);
    }

    public static void doOperation(Location startLocation, BlockFace direction, int limit, boolean skipNoMenu, boolean optimizeExperience, Consumer<BlockMenu> consumer) {
        doOperation(startLocation, direction, limit, skipNoMenu, optimizeExperience, false, consumer);
    }
    public static void doOperation(Location startLocation, BlockFace direction, int limit, boolean skipNoMenu, boolean optimizeExperience,boolean parallel, Consumer<BlockMenu> consumer) {
        if(parallel){
            doOperationParallel(startLocation, direction, limit, skipNoMenu, optimizeExperience, consumer);
        }else {
            doOperationOrdinal(startLocation, direction, limit, skipNoMenu, optimizeExperience, consumer);
        }
    }
    public static void doOperation(Location startLocation, BlockFace direction, int limit, boolean skipNoMenu,Predicate<Location> stopLocation, Consumer<Location> consumer) {
        doOperation(startLocation, direction, limit, skipNoMenu, true,stopLocation, consumer);
    }
    public static void doOperation(Location startLocation, BlockFace direction, int limit, boolean skipNoMenu, boolean optimizeExperience, Predicate<Location> stopLocation, Consumer<Location> consumer) {
        doOperation(startLocation, direction, limit, skipNoMenu, optimizeExperience, false,stopLocation, consumer);
    }
    public static void doOperation(Location startLocation, BlockFace direction, int limit,boolean skipNotPass, boolean optimizeExperience,boolean parallel,Predicate<Location> stopLocation, Consumer<Location> consumer) {
//        if(false&&parallel&&ExperimentalFeatureManager.getInstance().isEnableParallelLineOperation()){
//            //doOperationParallel(startLocation, direction, limit, skipNoMenu, optimizeExperience, consumer);
//        }else {
            doOperationOrdinal(startLocation, direction, limit,skipNotPass,optimizeExperience,stopLocation, consumer);
        //}
    }
    //menu operation
    public static void doOperationOrdinal(Location startLocation,BlockFace direction,int limit,boolean skipNoMenu,boolean optimizeExperience, Consumer<BlockMenu> consumer){
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        Vector directionVec = direction.getDirection();
        for (int i = 0; i < finalLimit; i++) {
            location.add(directionVec);
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
            if (blockMenu == null) {
                if (skipNoMenu) {
                    continue;
                } else {
                    return;
                }
            }
            consumer.accept(blockMenu);
        }
    }
    //just location
    public static void doOperationOrdinal(Location startLocation,BlockFace direction,int limit,boolean skipNotPass,boolean optimizeExperience,Predicate<Location> stopLocation, Consumer<Location> consumer){
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        Vector directionVec = direction.getDirection();
        for (int i = 0; i < finalLimit; i++) {
            location.add(directionVec);
            //final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
            if (!stopLocation.test(location)) {
                if (skipNotPass) {
                    continue;
                } else {
                    return;
                }
            }
            consumer.accept(location);
        }
    }
    public static void doOperationParallel(Location startLocation,BlockFace direction,int limit,boolean skipNoMenu,boolean optimizeExperience, Consumer<BlockMenu> consumer) {
        Semaphore lock = NetworkAsyncUtil.getInstance().getParallelTaskLock(startLocation);
        List<Future<Void>> futures = new ArrayList<>();
        try{
            lock.acquire();
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
        try{
            Location location = startLocation.clone();
            int finalLimit = limit;
            if (optimizeExperience) {
                finalLimit += 1;
            }
            Vector directionVec = direction.getDirection();
            for (int i = 0; i < finalLimit; i++) {
                location.add(directionVec);
                final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                if (blockMenu == null) {
                    if (skipNoMenu) {
                        continue;
                    }else {
                        break;
                    }
                }
                futures.add(NetworkAsyncUtil.getInstance().submitParallelFuture(() -> {
                        try{
                            consumer.accept(blockMenu);
                        }catch (Throwable e) {
                            e.printStackTrace();
                        }
                }));//  CompletableFuture.runAsync(,NetworkAsyncUtil.getInstance().getParallelExecutor()));
            }
        } finally {
            if(futures.isEmpty()){
                lock.release();
            }else {
                if(NetworkAsyncUtil.getInstance().isUseAsync()){
                    var task =  CompletableFuture.runAsync(()->{
                        try{
                            for (int i = 0, size = futures.size(); i < size; i++) {
                                Future<Void> f = futures.get(i);
                                if (!f.isDone()) {
                                    try { f.get(); }
                                    catch (CancellationException | ExecutionException ignore) {}
                                }
                            }
                        }catch (InterruptedException i){
                            throw new RuntimeException(i);
                        }
                    });
                    task.whenComplete((vd,t)->{
                        lock.release();
                        if(t instanceof TimeoutException timeout){
                            Networks.getInstance().getLogger().severe("Time out while waiting for Line Operation done!");
                            timeout.printStackTrace();
                        }
                    });
                    task.orTimeout(2,TimeUnit.SECONDS);
                }else {
                    timeout:
                    try{
                        for (int i = 0, size = futures.size(); i < size; i++) {
                            Future<Void> f = futures.get(i);
                            if (!f.isDone()) {
                                try { f.get(2, TimeUnit.SECONDS); }
                                catch (CancellationException | ExecutionException ignore) {}
                                catch (TimeoutException timeout){
                                    Networks.getInstance().getLogger().severe("Time out while waiting for Line Operation done!");
                                    break timeout;
                                }
                            }
                        }
                    }catch (InterruptedException i){
                        throw new RuntimeException(i);
                    }
                    finally {
                        lock.release();
                    }
                }

            }
        }

        //when can we remove this shit
    }

    public static void grabItem(
            @Nonnull NetworkRoot root,
            @Nonnull BlockMenu blockMenu,
            @Nonnull TransportMode transportMode,
            int limitQuantity
    ) {
        final int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.WITHDRAW, null);
        Location location = blockMenu.getLocation();
        int limit = limitQuantity;
        switch (transportMode) {
            case NONE, NONNULL_ONLY -> {
                /*
                 * Grab all the items.
                 */
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        //final int exceptedReceive = Math.min(item.getAmount(), limit);
                        limit=sendLimitedItemToRoot(root, item, limit, location);
                        if (limit <= 0) {
                            break;
                        }
                    }
                }
            }
            case NULL_ONLY -> {
                /*
                 * Nothing to do.
                 */
            }
            case FIRST_ONLY -> {
                /*
                 * Grab the first item only.
                 */
                if (slots.length > 0) {
                    final ItemStack item = blockMenu.getItemInSlot(slots[0]);
                    if (item != null && item.getType() != Material.AIR) {
                        limit=sendLimitedItemToRoot(root, item, limit, location);
                        if (limit <= 0) {
                            break;
                        }
                    }
                }
            }
            case LAST_ONLY -> {
                /*
                 * Grab the last item only.
                 */
                if (slots.length > 0) {
                    final ItemStack item = blockMenu.getItemInSlot(slots[slots.length - 1]);
                    if (item != null && item.getType() != Material.AIR) {
                        limit=sendLimitedItemToRoot(root, item, limit, location);
                        if (limit <= 0) {
                            break;
                        }
                    }
                }
            }
            case FIRST_STOP -> {
                /*
                 * Grab the first non-null item only.
                 */
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        limit=sendLimitedItemToRoot(root, item, limit, location);
                        break;
                    }
                }
            }
            case LAZY -> {
                /*
                 * When it's first item is non-null, we will grab all the items.
                 */
                if (slots.length > 0) {
                    final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                    if (delta != null && delta.getType() != Material.AIR) {
                        for (int slot : slots) {
                            ItemStack item = blockMenu.getItemInSlot(slot);
                            if (item != null && item.getType() != Material.AIR) {
                                limit=sendLimitedItemToRoot(root, item, limit, location);
                                if (limit <= 0) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            case GREEDY -> {
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        //final int exceptedReceive = Math.min(item.getAmount(), limit);
                        int oldLimit = limit;
                        limit=sendLimitedItemToRoot(root, item, limit, location);
                        if(limit==oldLimit){
                            //this slot pushes nothing, then next slot probably pushes nothing
                            break;
                        }
                        if (limit <= 0) {
                            break;
                        }
                    }else{
                        //this slot pushes nothing, then next slot probably pushes nothing
                        break;
                    }
                }
            }
        }
        return;
    }

    public static void pushItem(
            @Nonnull NetworkRoot root,
            @Nonnull BlockMenu blockMenu,
            @Nonnull List<ItemStack> clones,
            @Nonnull TransportMode transportMode,
            int limitQuantity
    ) {
        for (ItemStack clone : clones) {
            pushItem(root, blockMenu, clone, transportMode, limitQuantity);
        }
    }
    public static Pair<Integer,List<Integer>> calPush( BlockMenu blockMenu, ItemRequest quest, TransportMode transportMode, int limitPerMenu,int... slots){
        int maxStackSize=quest.getMaxStackSize();
        return switch (transportMode) {
            case NONE -> {
                yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return maxStackSize;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,false,false, slots);
            }

            case NULL_ONLY -> {
                // int free = limitQuantity;
                yield calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->
                        itemStack==null?maxStackSize:0,limitPerMenu,false,false,slots
                );
            }

            case NONNULL_ONLY -> {
                yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return 0;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,false,false,slots);
            }
            case FIRST_ONLY -> {
                //int free = limitQuantity;
                if (slots.length == 0) {
                    yield null;
                }
                final int slot = slots[0];
                yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return maxStackSize;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,true,false,slot);
            }
            case LAST_ONLY -> {
                if (slots.length == 0) {
                    yield  null;
                }
                final int slot = slots[slots.length-1];
                yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return maxStackSize;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,true,false,slot);
            }
            case FIRST_STOP -> {
                yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return maxStackSize;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,true,false,slots);
            }
            case LAZY -> {
                if (slots.length > 0) {
                    final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                    if (delta == null || delta.getType() == Material.AIR) {
                        yield  calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                            if (itemStack == null || itemStack.getType() == Material.AIR) {
                                return maxStackSize;
                            } else {
                                if (itemStack.getAmount() >= maxStackSize) {
                                    return 0;
                                }
                                if (StackUtils.itemsMatch(quest, itemStack)) {
                                    return maxStackSize - itemStack.getAmount();
                                }
                                return 0;
                            }
                        },limitPerMenu,false,false,slots);
                    }
                }
                yield null;
            }
            case GREEDY -> {
                yield calFetchItem(blockMenu::getItemInSlot,quest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return maxStackSize;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(quest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitPerMenu,false,true,slots);
            }
        };
    }
    public static void linePushItemOperationParallel(NetworkRoot root,Location startLocation,BlockFace face,int limit,boolean parallel,boolean skipNoMenu,boolean optimizeExperience,List<ItemStack> clones, List<NetworkRoot.PusherPrefetcherInfo> prefetcherInfos,int limitPerMenu,TransportMode transportMode){
        int size = clones.size();
        for(int i = 0; i< size; ++i){
            linePushItemOperationParallel(root,startLocation,face,limit,parallel,skipNoMenu,optimizeExperience,clones.get(i), prefetcherInfos.get(i),limitPerMenu,transportMode);
        }
    }
    public static void linePushItemOperationParallel(NetworkRoot root, Location startLocation, BlockFace face, int limit, boolean parallel, boolean skipNoMenu, boolean optimizeExperience, ItemStack clone, NetworkRoot.PusherPrefetcherInfo info, int limitPerMenu, TransportMode transportMode) {
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        BlockMenu[] menus = new BlockMenu[finalLimit];
        int[] consumes = new int[finalLimit];
        List<Integer>[] pushSlots=new List[finalLimit];
        Vector directionVec = face.getDirection();
        final int maxStackSize=clone.getMaxStackSize();
        final ItemRequest quest=new ItemRequest(clone,maxStackSize);
        int sum = 0;
        for (int i = 0; i < finalLimit; i++) {
            final int index=i;
            location.add(directionVec);
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
            if (blockMenu == null) {
                if (skipNoMenu) {
                    continue;
                }else {
                    break;
                }
            }
            int[] slots=blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu,ItemTransportFlow.INSERT,clone);
            var re=calPush(blockMenu,quest,transportMode,limitPerMenu,slots);
            if(re==null||re.getFirstValue()==null||re.getSecondValue()==null){
                continue;
            }
            menus[index]=blockMenu;
            consumes[index]=re.getFirstValue();
            pushSlots[index]=re.getSecondValue();
            sum += consumes[index];
        }

        quest.setAmount(sum);
        final ItemStack retrieved = info.getItemStackWithPrefetch(root, quest); //root.getItemStack(quest);
        if (retrieved != null && retrieved.getType() != Material.AIR) {
            int requested=retrieved.getAmount();
            for(int i=0;i<finalLimit;++i){
                final int index=i;
                if(menus[index]!=null&&consumes[index]>0){
                    int request=Math.min(consumes[index],requested);
                    requested-=request;
                    if(request>0){
                        NetworkAsyncUtil.getInstance().ensureLocation(menus[index].getLocation(), ()->{
                            ItemStack pushed=StackUtils.getAsQuantity(retrieved,request);
                            BlockMenuUtil.pushItemAlreadyMatched(menus[index],pushed, pushSlots[index]);
                        });
                    }
                    if(requested<=0){
                        break;
                    }
                }
            }
        }



    }

    public static void pushItem(
            @Nonnull NetworkRoot root,
            @Nonnull BlockMenu blockMenu,
            @Nonnull ItemStack clone,
            @Nonnull TransportMode transportMode,
            int limitQuantity
    ) {
        final int maxStackSize=clone.getMaxStackSize();
        final ItemRequest itemRequest = new ItemRequest(clone,maxStackSize);
        final int[] slots = blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.INSERT, clone);
        switch (transportMode) {
            case NONE -> {
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                },limitQuantity,false,root::getItemStack,slots);
            }

            case NULL_ONLY -> {
               // int free = limitQuantity;
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->
                    itemStack==null?maxStackSize:0,limitQuantity,false,root::getItemStack,slots
                );
            }

            case NONNULL_ONLY -> {
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return 0;
                    } else {
                        if (itemStack.getAmount() >= maxStackSize) {
                            return 0;
                        }
                        if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                            return maxStackSize - itemStack.getAmount();
                        }
                        return 0;
                    }
                },limitQuantity,false, root::getItemStack,slots);
            }
            case FIRST_ONLY -> {
                //int free = limitQuantity;
                if (slots.length == 0) {
                    break;
                }
                final int slot = slots[0];
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                },limitQuantity,true,root::getItemStack,slot);
            }
            case LAST_ONLY -> {
                if (slots.length == 0) {
                    break;
                }
                final int slot = slots[slots.length-1];
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                },limitQuantity,true,root::getItemStack,slot);
            }
            case FIRST_STOP -> {
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                },limitQuantity,true, root::getItemStack,slots);
            }
            case LAZY -> {
                if (slots.length > 0) {
                    final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                    if (delta == null || delta.getType() == Material.AIR) {
                        fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                        },limitQuantity,false, root::getItemStack,slots);
                    }
                }
            }
            case GREEDY -> {
                fetchItemAndPush(blockMenu,itemRequest,(itemStack)->{
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
                },limitQuantity,false,true, root::getItemStack,slots);
            }
        }
    }


}
