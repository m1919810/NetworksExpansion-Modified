package io.github.sefiraat.networks.utils;

import com.balugaq.netex.api.enums.MinecraftVersion;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.managers.ExperimentalFeatureManager;
import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import lombok.experimental.UtilityClass;
import me.matl114.matlib.nmsMirror.impl.CraftBukkit;
import me.matl114.matlib.nmsMirror.impl.NMSCore;
import me.matl114.matlib.nmsMirror.impl.NMSItem;
import me.matl114.matlib.nmsMirror.inventory.v1_20_R4.ItemStackHelper_1_20_R4;
import me.matl114.matlib.nmsUtils.ItemUtils;
import me.matl114.matlib.utils.CraftUtils;
import me.matl114.matlib.utils.version.Version;
import me.matl114.matlib.utils.version.VersionAtMost;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.ShieldMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@UtilityClass
@SuppressWarnings("deprecation")
public class StackUtils {
    private static final MinecraftVersion MC_VERSION = Networks.getInstance().getMCVersion();
    private static final boolean IS_1_20_5 = MC_VERSION.isAtLeast(MinecraftVersion.MC1_20_5);
    private static final boolean IS_1_21 = MC_VERSION.isAtLeast(MinecraftVersion.MC1_21);

    @Nonnull
    public static ItemStack getAsQuantity(@Nullable ItemStack itemStack, int amount) {
        return getAsQuantity(itemStack,amount,true);
    }
    public static ItemStack getLimitedRequest(@Nullable ItemStack itemStack, int amount) {
        if(itemStack == null) {
            return new ItemStack(Material.AIR);
        }
        if(itemStack.getAmount() <= amount) {
            return itemStack;
        }else {
            ItemStack newItem = itemStack.clone();
            newItem.setAmount(amount);
            return newItem;
        }
    }
    public static ItemStack getAsQuantity(@Nullable ItemStack itemStack,int amount,boolean forceCopy) {
        if (itemStack == null) {
            return ItemUtils.newStack(Material.AIR, 0);//  new ItemStack(Material.AIR);
        }
        if(!forceCopy&&itemStack.getAmount()<=amount){
            return itemStack;
        }
        ItemStack clone = itemStack.clone();
        clone.setAmount(amount);
        return clone;
    }
    public static ItemStack getAsQuantity(@Nonnull ItemStackCache cache,int amount){
        return getAsQuantity(cache.getItemStack(),amount,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2, boolean checkLore, boolean checkAmount, boolean checkCustomModelId) {
        return itemsMatchCore(itemStack1, itemStack2, checkLore, checkAmount, checkCustomModelId);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2, boolean checkLore, boolean checkAmount) {
        return itemsMatchCore(itemStack1, itemStack2, checkLore, checkAmount,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2, boolean checkLore) {
        return itemsMatchCore(itemStack1, itemStack2, checkLore, false,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2) {
        return itemsMatchCore(itemStack1, itemStack2, false, false,true);
    }

    public static boolean itemsMatch(@Nonnull ItemStackCache cache, @Nullable ItemStack itemStack, boolean checkLore) {
        return itemsMatchCore(cache.getItemStack(), itemStack, checkLore, false,true);
    }

    public static boolean itemsMatch(@Nonnull ItemStackCache cache, @Nullable ItemStack itemStack) {
        return itemsMatchCore(cache.getItemStack(), itemStack, false, false,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack, @Nonnull ItemStackCache cache, boolean checkLore, boolean checkAmount) {
        return itemsMatchCore(cache.getItemStack(), itemStack, checkLore, checkAmount,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack, @Nonnull ItemStackCache cache, boolean checkLore) {
        return itemsMatchCore(cache.getItemStack(), itemStack, checkLore, false,true);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack, @Nonnull ItemStackCache cache) {
        return itemsMatchCore(cache.getItemStack(), itemStack, false, false,true);
    }
    public static boolean itemsMatch(@Nonnull ItemStackCache itemStack, @Nonnull ItemStackCache cache) {
        //hashcode已经算出来了 而且都不匹配 那显然他不是匹配的
        if( itemStack.hashCodeNoLore!= null && cache.hashCodeNoLore != null && !itemStack.hashCodeNoLore.equals(cache.hashCodeNoLore) ){
            return false;
        }
        return itemsMatchCore(itemStack.getItemStack(), cache.getItemStack(), false);
        //return itemsMatchCore(cache, itemStack, false, false,true);
    }

    /**
     * Checks if items match each other, checks go in order from lightest to heaviest
     *
     * @para     The cached {@link ItemStack} to compare against
     * @return True if items match
     */

    public static String getOptionalId(ItemStack item){
        PersistentDataContainer pdcView = ItemUtils.getPersistentDataContainerView(item, false);
        return pdcView == null? null: pdcView.get(Slimefun.getItemDataService().getKey(), PersistentDataType.STRING);
    }
    private static final ReferenceSet<Material> LORE_SENSITIVE_MATERIAL = new ReferenceOpenHashSet<>(List.of(Material.PLAYER_HEAD,Material.SUGAR));
    private static final ReferenceSet<?> LORE_SENSITIVE_MATERIAL_NMS = new ReferenceOpenHashSet<>(
        LORE_SENSITIVE_MATERIAL.stream()
        .map(CraftBukkit.MAGIC_NUMBERS::getItem)
        .collect(Collectors.toUnmodifiableSet())
    );


    public static boolean shouldNotEscapeLore(ItemStack stack){
        var mat = stack.getType();
        return LORE_SENSITIVE_MATERIAL.contains(mat);
    }

    public static SlimefunItem getByItem(ItemStack item){
        String id = getOptionalId(item);
        return id == null ?null: SlimefunItem.getById(id);
    }
    public static boolean itemsMatchCore(@Nullable ItemStack cache, @Nullable ItemStack cache2, boolean checkLore){
        return ItemUtils.matchItemStack(cache, cache2, checkLore);
    }
    private static final boolean VERSION_AT_1_20_R4 = NMSItem.ITEMSTACK instanceof ItemStackHelper_1_20_R4;
    public static boolean itemsMatchCore(@Nullable ItemStack cache, @Nullable ItemStack cache2, boolean checkLore, boolean checkAmount, boolean checkCustomModelId) {
        // Null check
//        if (cache.getItemStack() == null || cache2.getItemStack()== null) {
//            return cache2.getItemStack() == null && cache.getItemStack() == null;
//        }
        if(cache == null || cache2 == null){
            return cache == cache2;
        }
        Object handle1 = CraftBukkit.ITEMSTACK.unwrapToNMS(cache);
        Object handle2 = CraftBukkit.ITEMSTACK.unwrapToNMS(cache2);
        if(checkLore){
            return NMSItem.ITEMSTACK.isSameItemSameTags(handle1, handle2);// .matchItem(handle1, handle2, true, true);
        }else {
            Object item1 = NMSItem.ITEMSTACK.getItem(handle1);
            Object item2 = NMSItem.ITEMSTACK.getItem(handle2);
            //material do not match
            if(item1 != item2){
                return false;
            }
            //check lore depends on material
            //before Component version, lore is shit, we can not depend on it
            return VERSION_AT_1_20_R4 ? NMSItem.ITEMSTACK.matchNbt(handle1, handle2,  LORE_SENSITIVE_MATERIAL_NMS.contains(item1), true) : (LORE_SENSITIVE_MATERIAL_NMS.contains(item1)?(NMSItem.ITEMSTACK.matchNbt(handle1, handle2, false, false) && distinctItemCheck(handle1, handle2)): NMSItem.ITEMSTACK.matchNbt(handle1, handle2,  false, true));
        }

//        Material type=cache2.getItemType();
//        if (type != cache.getItemType()) {
//            return false;
//        }
////        //todo remove
//
//        // If amounts do not match, then the items cannot possibly match
//        if (checkAmount && cache2.getItemAmount() > cache.getItemAmount()) {
//            return false;
//        }
//
//        //no use
//        //precheck sfid
//
//        // Now we need to compare meta's directly - cache is already out, but let's fetch the 2nd meta also
//
//        final ItemMeta itemMeta = cache2.getItemMeta();
//        final ItemMeta cachedMeta = cache.getItemMeta();
//        return metaMatchCore(type,itemMeta,cachedMeta,checkLore,checkCustomModelId);
    }
    private static final String SF_ID_KEY = Slimefun.getItemDataService().getKey().toString();
    @VersionAtMost(Version.v1_20_R3)
    private static boolean distinctItemCheck(Object val1, Object val2){

        Object customNBT = NMSItem.ITEMSTACK.getPersistentDataCompound(val1, false);
        if(customNBT == null){
            return true;
        }

        String id = NMSCore.COMPOUND_TAG.getString(customNBT, SF_ID_KEY);
        if(id == null){
            return true;
        }
        //few item can be distinctive so most of them will return true;
        if(SlimefunItem.getById(id) instanceof DistinctiveItem distinctiveItem){
            ItemMeta meta1 = CraftBukkit.ITEMSTACK.getItemMeta(val1);
            ItemMeta meta2 = CraftBukkit.ITEMSTACK.getItemMeta(val2);
            return distinctiveItem.canStack(meta1, meta2);
        }
        return true;
    }

    @Deprecated(forRemoval = true)
    public static boolean metaMatchCore(Material itemType,ItemMeta itemMeta,ItemMeta cachedMeta,boolean checkLore, boolean checkCustomModelId) {
        //ExperimentalFeatureManager.getInstance().startGlobalProfiler();
        if (itemMeta == null || cachedMeta == null) {
            return itemMeta == cachedMeta;
        }

        // ItemMetas are different types and cannot match
        if (!itemMeta.getClass().equals(cachedMeta.getClass())) {
            return false;
        }
        // Check the display name
        // if sf item is different ,their name is probably different
        if (!CraftUtils.matchDisplayNameField(itemMeta,cachedMeta)) {
            return false;
        }
        if (checkCustomModelId) {
            // Custom model data is different, no match
            final boolean hasCustomOne = itemMeta.hasCustomModelData();
            final boolean hasCustomTwo = cachedMeta.hasCustomModelData();
            if (hasCustomOne) {
                if (!hasCustomTwo || itemMeta.getCustomModelData() != cachedMeta.getCustomModelData()) {
                    return false;
                }
            } else if (hasCustomTwo) {
                return false;
            }
        }
        // PDCs don't match
        if (!itemMeta.getPersistentDataContainer().equals(cachedMeta.getPersistentDataContainer())) {
            return false;
        }

        //this means slimefun id matchs
        // 99.999% of items can be judged to not match

        // Quick meta-extension escapes
        if(CraftUtils.canQuickEscapeMetaVariant(itemMeta,cachedMeta)){
            return false;
        }

//        if(EnvironmentManager.getManager().getVersioned().differentSpecialMeta(itemMeta,cachedMeta)){
//            return false;
//        }
        // Make sure enchantments match

        if(!CraftUtils.matchEnchantmentsFields(itemMeta,cachedMeta)){
            return false;
        }
        // Check the attribute modifiers
        final boolean hasAttributeOne = itemMeta.hasAttributeModifiers();
        final boolean hasAttributeTwo = cachedMeta.hasAttributeModifiers();
        if (hasAttributeOne) {
            if (!hasAttributeTwo || !Objects.equals(itemMeta.getAttributeModifiers(), cachedMeta.getAttributeModifiers())) {
                return false;
            }
        } else if (hasAttributeTwo) {
            return false;
        }
        // Check the lore
        if (checkLore
                //these shits should be compared in Distinctive Items,but we do these comparasion for them,
                //should put data in pdc
                ||
                (!ExperimentalFeatureManager.getInstance().isEnableMatchDistinctiveItem() &&
                ( itemType == Material.PLAYER_HEAD // Fix Soul jars in SoulJars & Number Components in MomoTech
                || itemType == Material.SPAWNER // Fix Reinforced Spawner in Slimefun4
                || itemType == Material.SUGAR )// Fix Symbols in MomoTech
                )
        ) {//do not use reflection ,as some item will fail exam
            final boolean hasLore1= itemMeta.hasLore();
            final boolean hasLore2 = cachedMeta.hasLore();
            if (hasLore1&&hasLore2) {
                if (!Objects.equals(itemMeta.getLore(), cachedMeta.getLore())) {
                    return false;
                }
            } else if (hasLore1!=hasLore2) {
                return false;
            }
        }
        // Slimefun ID check no need to worry about distinction, covered in PDC + lore
        final Optional<String> optionalStackId1 = Slimefun.getItemDataService().getItemData(itemMeta);
        final Optional<String> optionalStackId2 = Slimefun.getItemDataService().getItemData(cachedMeta);
        if (optionalStackId1.isPresent() != optionalStackId2.isPresent()) {
            return false;
        }
        if (optionalStackId1.isPresent()) {
            if(ExperimentalFeatureManager.getInstance().isEnableMatchDistinctiveItem()){
                final String stackId1 = optionalStackId1.get();
                //when pdc equals, id value should equals
                SlimefunItem item=SlimefunItem.getById(stackId1);
                //compare distinctives
                if(item instanceof DistinctiveItem distinctiveItem){
                    if(!distinctiveItem.canStack(itemMeta, cachedMeta)){
                        return false;
                    }
                }
            }
            //ExperimentalFeatureManager.getInstance().endGlobalProfiler(()->"meta All matched sf %s");
            //SlimefunItem matched
            return true;
//            }
        }

        // Everything should match if we've managed to get here
        return true;
    }
    @Deprecated(forRemoval = true)
    public static boolean canQuickEscapeMetaVariant(@Nonnull ItemMeta metaOne, @Nonnull ItemMeta metaTwo) {

        // Damageable (first as everything can be damageable apparently)
        if (metaOne instanceof Damageable instanceOne && metaTwo instanceof Damageable instanceTwo) {
            if (instanceOne.hasDamage() != instanceTwo.hasDamage()) {
                return true;
            }

            if (instanceOne.getDamage() != instanceTwo.getDamage()) {
                return true;
            }
        }

        if (metaOne instanceof Repairable instanceOne && metaTwo instanceof Repairable instanceTwo) {
            if (instanceOne.hasRepairCost() != instanceTwo.hasRepairCost()) {
                return true;
            }

            if (instanceOne.getRepairCost() != instanceTwo.getRepairCost()) {
                return true;
            }
        }

        // Axolotl
        if (metaOne instanceof AxolotlBucketMeta instanceOne && metaTwo instanceof AxolotlBucketMeta instanceTwo) {
            if (instanceOne.hasVariant() != instanceTwo.hasVariant()) {
                return true;
            }

            if (!instanceOne.hasVariant() || !instanceTwo.hasVariant()) {
                return true;
            }

            if (instanceOne.getVariant() != instanceTwo.getVariant()) {
                return true;
            }
        }

        // Banner
        if (metaOne instanceof BannerMeta instanceOne && metaTwo instanceof BannerMeta instanceTwo) {
            if (instanceOne.numberOfPatterns() != instanceTwo.numberOfPatterns()) {
                return true;
            }

            if (!instanceOne.getPatterns().equals(instanceTwo.getPatterns())) {
                return true;
            }
        }

        // BlockData
        if (metaOne instanceof BlockDataMeta instanceOne && metaTwo instanceof BlockDataMeta instanceTwo) {
            if (instanceOne.hasBlockData() != instanceTwo.hasBlockData()) {
                return true;
            }
        }

        // BlockState
        if (metaOne instanceof BlockStateMeta instanceOne && metaTwo instanceof BlockStateMeta instanceTwo) {
            if (instanceOne.hasBlockState() != instanceTwo.hasBlockState()) {
                return true;
            }

//            if (!matchBlockStateMeta(instanceOne,instanceTwo)) {
//                return true;
//            }
        }

        // Books
        if (metaOne instanceof BookMeta instanceOne && metaTwo instanceof BookMeta instanceTwo) {
            if (instanceOne.getPageCount() != instanceTwo.getPageCount()) {
                return true;
            }
            if (!Objects.equals(instanceOne.getAuthor(), instanceTwo.getAuthor())) {
                return true;
            }
            if (!Objects.equals(instanceOne.getTitle(), instanceTwo.getTitle())) {
                return true;
            }
            if (!Objects.equals(instanceOne.getGeneration(), instanceTwo.getGeneration())) {
                return true;
            }
        }

        // Bundle
        if (metaOne instanceof BundleMeta instanceOne && metaTwo instanceof BundleMeta instanceTwo) {
            if (instanceOne.hasItems() != instanceTwo.hasItems()) {
                return true;
            }
            if (!instanceOne.getItems().equals(instanceTwo.getItems())) {
                return true;
            }
        }

        // Compass
        if (metaOne instanceof CompassMeta instanceOne && metaTwo instanceof CompassMeta instanceTwo) {
            if (instanceOne.isLodestoneTracked() != instanceTwo.isLodestoneTracked()) {
                return true;
            }
            if (!Objects.equals(instanceOne.getLodestone(), instanceTwo.getLodestone())) {
                return true;
            }
        }

        // Crossbow
        if (metaOne instanceof CrossbowMeta instanceOne && metaTwo instanceof CrossbowMeta instanceTwo) {
            if (instanceOne.hasChargedProjectiles() != instanceTwo.hasChargedProjectiles()) {
                return true;
            }
            if (!instanceOne.getChargedProjectiles().equals(instanceTwo.getChargedProjectiles())) {
                return true;
            }
        }

        // Enchantment Storage
        if (metaOne instanceof EnchantmentStorageMeta instanceOne && metaTwo instanceof EnchantmentStorageMeta instanceTwo) {
            if (instanceOne.hasStoredEnchants() != instanceTwo.hasStoredEnchants()) {
                return true;
            }
            if (!instanceOne.getStoredEnchants().equals(instanceTwo.getStoredEnchants())) {
                return true;
            }
        }

        // Firework Star
        if (metaOne instanceof FireworkEffectMeta instanceOne && metaTwo instanceof FireworkEffectMeta instanceTwo) {
            if (!Objects.equals(instanceOne.getEffect(), instanceTwo.getEffect())) {
                return true;
            }
        }

        // Firework
        if (metaOne instanceof FireworkMeta instanceOne && metaTwo instanceof FireworkMeta instanceTwo) {
            if (instanceOne.getPower() != instanceTwo.getPower()) {
                return true;
            }
            if (!instanceOne.getEffects().equals(instanceTwo.getEffects())) {
                return true;
            }
        }

        // Leather Armor
        if (metaOne instanceof LeatherArmorMeta instanceOne && metaTwo instanceof LeatherArmorMeta instanceTwo) {
            if (!instanceOne.getColor().equals(instanceTwo.getColor())) {
                return true;
            }
        }

        // Maps
        if (metaOne instanceof MapMeta instanceOne && metaTwo instanceof MapMeta instanceTwo) {
            if (instanceOne.hasMapView() != instanceTwo.hasMapView()) {
                return true;
            }
            if (instanceOne.hasLocationName() != instanceTwo.hasLocationName()) {
                return true;
            }
            if (instanceOne.hasColor() != instanceTwo.hasColor()) {
                return true;
            }
            if (!Objects.equals(instanceOne.getMapView(), instanceTwo.getMapView())) {
                return true;
            }
            if (!Objects.equals(instanceOne.getLocationName(), instanceTwo.getLocationName())) {
                return true;
            }
            if (!Objects.equals(instanceOne.getColor(), instanceTwo.getColor())) {
                return true;
            }
        }

        // Potion
        if (metaOne instanceof PotionMeta instanceOne && metaTwo instanceof PotionMeta instanceTwo) {
            if (IS_1_20_5) {
                if (instanceOne.getBasePotionType() != instanceTwo.getBasePotionType()) {
                    return true;
                }
            } else {
                if (!Objects.equals(instanceOne.getBasePotionData(), instanceTwo.getBasePotionData())) {
                    return true;
                }
            }
            if (instanceOne.hasCustomEffects() != instanceTwo.hasCustomEffects()) {
                return true;
            }
            if (instanceOne.hasColor() != instanceTwo.hasColor()) {
                return true;
            }
            if (!Objects.equals(instanceOne.getColor(), instanceTwo.getColor())) {
                return true;
            }
            if (!instanceOne.getCustomEffects().equals(instanceTwo.getCustomEffects())) {
                return true;
            }
        }

        // Skull
        //too many skull items in slimefun
        //only head-cutter heads can be stack in this way
//        if (metaOne instanceof SkullMeta instanceOne && metaTwo instanceof SkullMeta instanceTwo) {
//            if (instanceOne.hasOwner() != instanceTwo.hasOwner()) {
//                return true;
//            }
//            if (!Objects.equals(instanceOne.getOwningPlayer(), instanceTwo.getOwningPlayer())) {
//                return true;
//            }
//        }

        // Stew
        if (metaOne instanceof SuspiciousStewMeta instanceOne && metaTwo instanceof SuspiciousStewMeta instanceTwo) {
            if (instanceOne.hasCustomEffects() != instanceTwo.hasCustomEffects()) {
                return true;
            }

            if (!Objects.equals(instanceOne.getCustomEffects(), instanceTwo.getCustomEffects())) {
                return true;
            }
        }

        // Fish Bucket
        if (metaOne instanceof TropicalFishBucketMeta instanceOne && metaTwo instanceof TropicalFishBucketMeta instanceTwo) {
            if (instanceOne.hasVariant() != instanceTwo.hasVariant()) {
                return true;
            }
            if (!instanceOne.getPattern().equals(instanceTwo.getPattern())) {
                return true;
            }
            if (!instanceOne.getBodyColor().equals(instanceTwo.getBodyColor())) {
                return true;
            }
            if (!instanceOne.getPatternColor().equals(instanceTwo.getPatternColor())) {
                return true;
            }
        }

        // Knowledge Book
        if (metaOne instanceof KnowledgeBookMeta instanceOne && metaTwo instanceof KnowledgeBookMeta instanceTwo) {
            if (instanceOne.hasRecipes() != instanceTwo.hasRecipes()) {
                return true;
            }

            if (!Objects.equals(instanceOne.getRecipes(), instanceTwo.getRecipes())) {
                return true;
            }
        }

        // Music Instrument
        if (metaOne instanceof MusicInstrumentMeta instanceOne && metaTwo instanceof MusicInstrumentMeta instanceTwo) {
            if (!Objects.equals(instanceOne.getInstrument(), instanceTwo.getInstrument())) {
                return true;
            }
        }

        // Armor
        if (metaOne instanceof ArmorMeta instanceOne && metaTwo instanceof ArmorMeta instanceTwo) {
            if (!Objects.equals(instanceOne.getTrim(), instanceTwo.getTrim())) {
                return true;
            }
        }

        if (IS_1_20_5) {
            // Writable Book
            if (metaOne instanceof WritableBookMeta instanceOne && metaTwo instanceof WritableBookMeta instanceTwo) {
                if (instanceOne.getPageCount() != instanceTwo.getPageCount()) {
                    return true;
                }
                if (!Objects.equals(instanceOne.getPages(), instanceTwo.getPages())) {
                    return true;
                }
            }
            if (IS_1_21) {
                // Ominous Bottle
                if (metaOne instanceof OminousBottleMeta instanceOne && metaTwo instanceof OminousBottleMeta instanceTwo) {
                    if (instanceOne.hasAmplifier() != instanceTwo.hasAmplifier()) {
                        return true;
                    }

                    if (instanceOne.getAmplifier() != instanceTwo.getAmplifier()) {
                        return true;
                    }
                }
                // Shield
                if (metaOne instanceof ShieldMeta instanceOne && metaTwo instanceof ShieldMeta instanceTwo) {
                    if (Objects.equals(instanceOne.getBaseColor(), instanceTwo.getBaseColor())) {
                        return true;
                    }
                }
            }
        }

        // Cannot escape via any meta extension check
        return false;
    }
    public static boolean checkVersionedFeatures(ItemMeta itemMeta,ItemMeta cachedMeta){
        if (IS_1_20_5) {
            // Check if fire-resistant
            if (itemMeta.isFireResistant() != cachedMeta.isFireResistant()) {
                return false;
            }

            // Check if unbreakable
            if (itemMeta.isUnbreakable() != cachedMeta.isUnbreakable()) {
                return false;
            }

            // Check if hide tooltip
            if (itemMeta.isHideTooltip() != cachedMeta.isHideTooltip()) {
                return false;
            }

            // Check rarity
            final boolean hasRarityOne = itemMeta.hasRarity();
            final boolean hasRarityTwo = cachedMeta.hasRarity();
            if (hasRarityOne) {
                if (!hasRarityTwo || itemMeta.getRarity() != cachedMeta.getRarity()) {
                    return false;
                }
            } else if (hasRarityTwo) {
                return false;
            }

            // Check food components
            if (itemMeta.hasFood() && cachedMeta.hasFood()) {
                if (!Objects.equals(itemMeta.getFood(), cachedMeta.getFood())) {
                    return false;
                }
            } else if (itemMeta.hasFood() != cachedMeta.hasFood()) {
                return false;
            }

            // Check tool components
            if (itemMeta.hasTool() && cachedMeta.hasTool()) {
                if (!Objects.equals(itemMeta.getTool(), cachedMeta.getTool())) {
                    return false;
                }
            } else if (itemMeta.hasTool() != cachedMeta.hasTool()) {
                return false;
            }

            if (IS_1_21) {
                // Check jukebox playable
                if (itemMeta.hasJukeboxPlayable() && cachedMeta.hasJukeboxPlayable()) {
                    if (!Objects.equals(itemMeta.getJukeboxPlayable(), cachedMeta.getJukeboxPlayable())) {
                        return false;
                    }
                } else if (itemMeta.hasJukeboxPlayable() != cachedMeta.hasJukeboxPlayable()) {
                    return false;
                }
            }
        }
        return true;
    }

//    public static boolean matchBlockStateMeta(BlockStateMeta meta1, BlockStateMeta meta2){
//        return EnvironmentManager.getManager().getVersioned().matchBlockStateMeta(meta1, meta2);
//    }

    /**
     * Heal the entity by the provided amount
     *
     * @param itemStack         The {@link LivingEntity} to heal
     * @param durationInSeconds The amount to heal by
     */
    @ParametersAreNonnullByDefault
    public static void putOnCooldown(ItemStack itemStack, int durationInSeconds) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            PersistentDataAPI.setLong(itemMeta, Keys.ON_COOLDOWN, System.currentTimeMillis() + (durationInSeconds * 1000L));
            itemStack.setItemMeta(itemMeta);
        }
    }

    /**
     * Heal the entity by the provided amount
     *
     * @param itemStack The {@link LivingEntity} to heal
     */
    @ParametersAreNonnullByDefault
    public static boolean isOnCooldown(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            long cooldownUntil = PersistentDataAPI.getLong(itemMeta, Keys.ON_COOLDOWN, -1);
            if (cooldownUntil == -1) {
                cooldownUntil = PersistentDataAPI.getLong(itemMeta, Keys.ON_COOLDOWN2, -1);
            }
            if (cooldownUntil == -1) {
                cooldownUntil = PersistentDataAPI.getLong(itemMeta, Keys.ON_COOLDOWN3, 0);
            }
            return System.currentTimeMillis() < cooldownUntil;
        }
        return false;
    }
}