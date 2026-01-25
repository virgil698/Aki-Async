package org.virgil.akiasync.mixin;

import org.leavesmc.plugin.mixin.condition.ConditionalMixinConfigPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AkiAsyncMixinConfigPlugin extends ConditionalMixinConfigPlugin {

    private static final boolean IS_1_21_10_OR_NEWER;
    private static final boolean IS_1_21_11_OR_NEWER;

    static {
        IS_1_21_10_OR_NEWER = detectServerVersion_1_21_10();
        IS_1_21_11_OR_NEWER = detectServerVersion_1_21_11();
    }

    private static boolean detectServerVersion_1_21_10() {
        try {
            Class<?> serverEntityClass = Class.forName("net.minecraft.server.level.ServerEntity", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            for (Class<?> innerClass : serverEntityClass.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("Synchronizer")) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectServerVersion_1_21_11() {

        ClassLoader cl = AkiAsyncMixinConfigPlugin.class.getClassLoader();

        boolean newPathExists = cl.getResourceAsStream("net/minecraft/world/entity/vehicle/boat/Boat.class") != null;

        boolean oldPathExists = cl.getResourceAsStream("net/minecraft/world/entity/vehicle/Boat.class") != null;

        boolean is1_21_11 = newPathExists && !oldPathExists;
        System.out.println("[AkiAsync-MixinConfig] Boat class detection: newPath=" + newPathExists + ", oldPath=" + oldPathExists + ", IS_1_21_11=" + is1_21_11);
        return is1_21_11;
    }

    public static boolean is1_21_10OrNewer() {
        return IS_1_21_10_OR_NEWER;
    }

    public static boolean is1_21_11OrNewer() {
        return IS_1_21_11_OR_NEWER;
    }

    private static final Set<String> VERSION_SPECIFIC_MIXINS_1_21_8 = Set.of(
        "org.virgil.akiasync.mixin.mixins.entity.tracker.ServerEntityMixin"
    );

    private static final Set<String> VERSION_SPECIFIC_MIXINS_1_21_10 = Set.of(
        "org.virgil.akiasync.mixin.mixins.entity.tracker.ServerEntityMixin_1_21_10"
    );

    private static final Set<String> VERSION_SPECIFIC_MIXINS_PRE_1_21_11 = Set.of(
        "org.virgil.akiasync.mixin.mixins.optimization.math.MthMixin",
        "org.virgil.akiasync.mixin.mixins.entity.movement.EntityMoveZeroVelocityMixin"
    );

    private static final Set<String> VERSION_SPECIFIC_MIXINS_1_21_11 = Set.of(

    );

    private static final Set<String> DISABLED_MIXINS_1_21_11 = Set.of(
        "org.virgil.akiasync.mixin.mixins.optimization.UtilCompletableFutureMixin",
        "org.virgil.akiasync.mixin.mixins.entity.BatSpookySeasonMixin",
        "org.virgil.akiasync.mixin.mixins.optimization.math.MthMixin_1_21_11"
    );

    private static final Set<String> COPPER_GOLEM_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.ai.brain.CopperGolemAiMixin",
        "org.virgil.akiasync.mixin.mixins.ai.brain.CopperGolemMixin",
        "org.virgil.akiasync.mixin.mixins.ai.brain.TransportItemsBetweenContainersMixin"
    );

    private static final Set<String> CONCURRENT_COLLECTIONS_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.ai.brain.GoalSelectorMixin",
        "org.virgil.akiasync.mixin.mixins.ai.brain.GossipContainerMixin"
    );

    private static final Set<String> BEEDATA_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.blockentity.BeehiveBlockEntityBeeMixin"
    );

    private static final Set<String> MOBCOUNTS_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.entity.LocalMobCapCalculatorMixin"
    );

    private static final Set<String> LITHIUM_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.world.LithiumServerChunkCacheMixin",
        "org.virgil.akiasync.mixin.mixins.world.LithiumServerLevel"
    );

    private static final Set<String> GAMEPROFILE_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.world.ServerPlayerMixin"
    );

    private static final Set<String> SLF4J_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.entity.collision.EntityLookupCacheMixin"
    );

    private static final Set<String> JETBRAINS_ANNOTATIONS_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.mixins.chunk.loading.ServerChunkCacheMixin",
        "org.virgil.akiasync.mixin.mixins.entity.movement.PathFinderMixin",
        "org.virgil.akiasync.mixin.mixins.pathfinding.async.PathFinderMixin"
    );

    private static final Set<String> LOG4J_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.util.SynchronisePlugin"
    );

    private static final Set<String> FASTUTIL_HACK_MIXINS = Set.of(
        "org.virgil.akiasync.mixin.util.concurrent.FastUtilHackUtil",
        "org.virgil.akiasync.mixin.util.concurrent.Int2ObjectConcurrentHashMap"
    );

    private static final Set<String> SYNCHRONISE_PLUGIN_DEPENDENTS = Set.of(
        "org.virgil.akiasync.mixin.mixins.optimization.thread.SyncAllMixin",
        "org.virgil.akiasync.mixin.mixins.util.collections.FastUtilsMixin"
    );

    private static final boolean HAS_COPPER_GOLEM;
    private static final boolean HAS_CONCURRENT_COLLECTIONS;
    private static final boolean HAS_BEE_DATA;
    private static final boolean HAS_MOB_COUNTS;
    private static final boolean HAS_LITHIUM;
    private static final boolean HAS_GAME_PROFILE;
    private static final boolean HAS_SLF4J;
    private static final boolean HAS_JETBRAINS_ANNOTATIONS;
    private static final boolean HAS_LOG4J;

    static {
        HAS_COPPER_GOLEM = detectCopperGolem();
        HAS_CONCURRENT_COLLECTIONS = detectConcurrentCollections();
        HAS_BEE_DATA = detectBeeData();
        HAS_MOB_COUNTS = detectMobCounts();
        HAS_LITHIUM = detectLithium();
        HAS_GAME_PROFILE = detectGameProfile();
        HAS_SLF4J = detectSlf4j();
        HAS_JETBRAINS_ANNOTATIONS = detectJetBrainsAnnotations();
        HAS_LOG4J = detectLog4j();
    }

    private static boolean detectCopperGolem() {
        try {
            Class.forName("net.minecraft.world.entity.animal.coppergolem.CopperGolem", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectConcurrentCollections() {
        try {
            Class.forName("org.virgil.akiasync.mixin.util.concurrent.ConcurrentCollections");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectBeeData() {
        try {
            Class<?> beehiveClass = Class.forName("net.minecraft.world.level.block.entity.BeehiveBlockEntity", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            for (Class<?> innerClass : beehiveClass.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("BeeData")) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectMobCounts() {
        try {
            Class<?> localMobCapClass = Class.forName("net.minecraft.server.level.LocalMobCapCalculator", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            for (Class<?> innerClass : localMobCapClass.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("MobCounts")) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectLithium() {
        try {
            Class.forName("net.caffeinemc.mods.lithium.common.entity.NavigatingEntity", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectGameProfile() {
        try {
            Class.forName("com.mojang.authlib.GameProfile", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectSlf4j() {
        try {
            Class.forName("org.slf4j.Logger", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectJetBrainsAnnotations() {
        try {
            Class.forName("org.jetbrains.annotations.Nullable", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean detectLog4j() {
        try {
            Class.forName("org.apache.logging.log4j.Logger", false, AkiAsyncMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

        System.out.println("[AkiAsync-MixinConfig] shouldApplyMixin called: " + mixinClassName + " -> " + targetClassName + " (IS_1_21_11=" + IS_1_21_11_OR_NEWER + ")");

        if (VERSION_SPECIFIC_MIXINS_1_21_8.contains(mixinClassName)) {
            System.out.println("[AkiAsync-MixinConfig] " + mixinClassName + " is 1.21.8 specific, apply=" + !IS_1_21_10_OR_NEWER);
            return !IS_1_21_10_OR_NEWER;
        }

        if (VERSION_SPECIFIC_MIXINS_1_21_10.contains(mixinClassName)) {
            System.out.println("[AkiAsync-MixinConfig] " + mixinClassName + " is 1.21.10+ specific, apply=" + IS_1_21_10_OR_NEWER);
            return IS_1_21_10_OR_NEWER;
        }

        if (VERSION_SPECIFIC_MIXINS_PRE_1_21_11.contains(mixinClassName)) {
            System.out.println("[AkiAsync-MixinConfig] " + mixinClassName + " is PRE-1.21.11 specific, apply=" + !IS_1_21_11_OR_NEWER);
            return !IS_1_21_11_OR_NEWER;
        }

        if (VERSION_SPECIFIC_MIXINS_1_21_11.contains(mixinClassName)) {
            return IS_1_21_11_OR_NEWER;
        }

        if (DISABLED_MIXINS_1_21_11.contains(mixinClassName) && IS_1_21_11_OR_NEWER) {
            return false;
        }

        if (COPPER_GOLEM_MIXINS.contains(mixinClassName) && !HAS_COPPER_GOLEM) {
            return false;
        }

        if (CONCURRENT_COLLECTIONS_MIXINS.contains(mixinClassName) && !HAS_CONCURRENT_COLLECTIONS) {
            return false;
        }

        if (BEEDATA_MIXINS.contains(mixinClassName) && !HAS_BEE_DATA) {
            return false;
        }

        if (MOBCOUNTS_MIXINS.contains(mixinClassName) && !HAS_MOB_COUNTS) {
            return false;
        }

        if (LITHIUM_MIXINS.contains(mixinClassName) && !HAS_LITHIUM) {
            return false;
        }

        if (GAMEPROFILE_MIXINS.contains(mixinClassName) && !HAS_GAME_PROFILE) {
            return false;
        }

        if (SLF4J_MIXINS.contains(mixinClassName) && !HAS_SLF4J) {
            return false;
        }

        if (JETBRAINS_ANNOTATIONS_MIXINS.contains(mixinClassName) && !HAS_JETBRAINS_ANNOTATIONS) {
            return false;
        }

        if (LOG4J_MIXINS.contains(mixinClassName) && !HAS_LOG4J) {
            return false;
        }

        if (FASTUTIL_HACK_MIXINS.contains(mixinClassName) && !HAS_JETBRAINS_ANNOTATIONS) {
            return false;
        }

        if (SYNCHRONISE_PLUGIN_DEPENDENTS.contains(mixinClassName) && !HAS_LOG4J) {
            return false;
        }
        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }
}
