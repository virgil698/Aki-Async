package org.virgil.akiasync.mixin.bridge.sub;

public interface StructureBridge {

    boolean isStructureLocationAsyncEnabled();
    int getStructureLocationThreads();

    boolean isLocateCommandEnabled();
    int getLocateCommandSearchRadius();
    boolean isLocateCommandSkipKnownStructures();

    boolean isVillagerTradeMapsEnabled();
    java.util.Set<String> getVillagerTradeMapTypes();
    int getVillagerMapGenerationTimeoutSeconds();
    int getVillagerTradeMapsSearchRadius();
    boolean isVillagerTradeMapsSkipKnownStructures();

    boolean isDolphinTreasureHuntEnabled();
    int getDolphinTreasureSearchRadius();
    boolean isDolphinTreasureSkipKnownStructures();

    boolean isChestExplorationMapsEnabled();
    java.util.Set<String> getChestExplorationLootTables();

    boolean isStructureLocationDebugEnabled();

    boolean isStructureAlgorithmOptimizationEnabled();
    String getStructureSearchPattern();
    boolean isStructureCachingEnabled();
    boolean isBiomeAwareSearchEnabled();
    int getStructureCacheMaxSize();
    long getStructureCacheExpirationMinutes();

    void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable);

    void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet);

    void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable);

    void handleChestExplorationMapAsyncStart(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures, Object cir);

    void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir);

    void handleVillagerTradeMapAsyncStart(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Object cir);

    void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir);

    boolean isJigsawOptimizationEnabled();
    void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds);
    boolean hasJigsawOctree();
    void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box);
    boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box);
    void clearJigsawOctree();
    String getJigsawOctreeStats();
}
