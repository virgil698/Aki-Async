package org.virgil.akiasync.mixin.async.explosion;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.ExceptionHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnifiedExplosionEngine {

    private static volatile boolean vectorApiChecked = false;
    private static volatile boolean vectorApiAvailable = false;
    private static volatile VectorApiHelper vectorHelper = null;

    private static boolean isVectorApiAvailable() {
        if (!vectorApiChecked) {
            synchronized (UnifiedExplosionEngine.class) {
                if (!vectorApiChecked) {
                    vectorApiAvailable = checkVectorApiAvailability();
                    vectorApiChecked = true;
                }
            }
        }
        return vectorApiAvailable;
    }

    private static boolean checkVectorApiAvailability() {
        try {

            Class.forName("jdk.incubator.vector.ByteVector");
            Class.forName("jdk.incubator.vector.VectorSpecies");
            Class.forName("jdk.incubator.vector.VectorMask");

            vectorHelper = new VectorApiHelper();
            return vectorHelper.isInitialized();
        } catch (ClassNotFoundException | NoClassDefFoundError | UnsupportedClassVersionError e) {

            return false;
        } catch (Exception e) {
            ExceptionHandler.handleExpected("UnifiedExplosionEngine", "checkVectorApi", e);
            return false;
        }
    }

    private static class VectorApiHelper {
        private final Object species;
        private final int step;
        private final MethodHandle constructor;
        private final boolean initialized;

        VectorApiHelper() {
            Object tmpSpecies = null;
            MethodHandle tmpConstructor = null;
            int tmpStep = 0;
            boolean tmpInitialized = false;

            try {

                Class<?> byteVectorClass = Class.forName("jdk.incubator.vector.ByteVector");
                java.lang.reflect.Field speciesField = byteVectorClass.getField("SPECIES_PREFERRED");
                tmpSpecies = speciesField.get(null);

                java.lang.reflect.Method lengthMethod = tmpSpecies.getClass().getMethod("length");
                tmpStep = (int) lengthMethod.invoke(tmpSpecies);

                java.lang.reflect.Method vectorTypeMethod = tmpSpecies.getClass().getMethod("vectorType");
                Class<?> targetClass = (Class<?>) vectorTypeMethod.invoke(tmpSpecies);

                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
                MethodType ctorType = MethodType.methodType(void.class, byte[].class);
                tmpConstructor = lookup.findConstructor(targetClass, ctorType);

                tmpInitialized = true;
            } catch (Exception e) {

            }

            this.species = tmpSpecies;
            this.step = tmpStep;
            this.constructor = tmpConstructor;
            this.initialized = tmpInitialized && tmpSpecies != null && tmpConstructor != null;
        }

        boolean isInitialized() {
            return initialized;
        }

        int getStep() {
            return step;
        }

        MethodHandle getConstructor() {
            return constructor;
        }

        Object getSpecies() {
            return species;
        }
    }

    private static final int VECTOR_STEP_FALLBACK = 16;

    private static final double[][] PRECOMPUTED_RAYS;
    private static final int RAY_COUNT;

    static {
        List<double[]> rays = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    double dx = (x / 15.0 * 2.0 - 1.0);
                    double dy = (y / 15.0 * 2.0 - 1.0);
                    double dz = (z / 15.0 * 2.0 - 1.0);
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length > 0) {
                        rays.add(new double[]{dx / length * 0.3, dy / length * 0.3, dz / length * 0.3});
                    }
                }
            }
        }
        PRECOMPUTED_RAYS = rays.toArray(new double[0][]);
        RAY_COUNT = PRECOMPUTED_RAYS.length;
    }

    private static final Map<ServerLevel, UnifiedExplosionEngine> ENGINES = new ConcurrentHashMap<>();

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<BlockState> blockStateCache = new Long2ObjectOpenHashMap<>(512);
    private final Long2FloatOpenHashMap resistanceCache = new Long2FloatOpenHashMap(512);
    private final Long2ObjectOpenHashMap<FluidState> fluidStateCache = new Long2ObjectOpenHashMap<>(256);

    private double centerX, centerY, centerZ;

    private final List<BlockPos> destroyedBlocks = new ArrayList<>(1000);
    private final Map<UUID, Vec3> hurtEntities = new HashMap<>(100);
    private final Set<Long> processedBlocks = new HashSet<>(1000);

    private long lastCleanupTime = 0;

    private UnifiedExplosionEngine(ServerLevel level) {
        this.level = level;
        this.resistanceCache.defaultReturnValue(-1.0f);
    }

    public static UnifiedExplosionEngine getOrCreate(ServerLevel level) {
        return ENGINES.computeIfAbsent(level, UnifiedExplosionEngine::new);
    }

    public static void clearLevelCache(ServerLevel level) {
        UnifiedExplosionEngine engine = ENGINES.remove(level);
        if (engine != null) {
            engine.clear();
        }
    }

    public static void clearAllCaches() {
        for (UnifiedExplosionEngine engine : ENGINES.values()) {
            engine.clear();
        }
        ENGINES.clear();
    }

    public ExplosionResult calculate(Vec3 center, float power, boolean fire) {
        Bridge bridge = BridgeManager.getBridge();

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[UnifiedEngine] Starting explosion at %s, power=%.1f", center, power);
        }

        destroyedBlocks.clear();
        hurtEntities.clear();
        processedBlocks.clear();

        BlockPos centerPos = BlockPos.containing(center);

        BlockState centerState = getBlockState(centerPos);
        if (!centerState.getFluidState().isEmpty()) {
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[UnifiedEngine] Explosion in fluid, skipping block destruction");
            }

            calculateEntityDamageInline(center, power, bridge);
            return new ExplosionResult(destroyedBlocks, hurtEntities, fire);
        }

        calculateAffectedBlocksInline(center, power, bridge);

        calculateEntityDamageInline(center, power, bridge);

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[UnifiedEngine] Completed: %d blocks, %d entities",
                destroyedBlocks.size(), hurtEntities.size());
        }

        cleanupIfNeeded();

        return new ExplosionResult(destroyedBlocks, hurtEntities, fire);
    }

    private void calculateAffectedBlocksInline(Vec3 center, float power, Bridge bridge) {
        double cx = center.x;
        double cy = center.y;
        double cz = center.z;
        net.minecraft.util.RandomSource random = level.getRandom();

        for (int rayIndex = 0; rayIndex < RAY_COUNT; rayIndex++) {
            double[] ray = PRECOMPUTED_RAYS[rayIndex];
            double dirX = ray[0];
            double dirY = ray[1];
            double dirZ = ray[2];

            float rayPower = power * (0.7f + random.nextFloat() * 0.6f);
            double x = cx;
            double y = cy;
            double z = cz;

            while (rayPower > 0.0f) {
                int bx = (int) Math.floor(x);
                int by = (int) Math.floor(y);
                int bz = (int) Math.floor(z);
                long posKey = BlockPos.asLong(bx, by, bz);

                if (processedBlocks.contains(posKey)) {
                    x += dirX;
                    y += dirY;
                    z += dirZ;
                    rayPower -= 0.22500001f;
                    continue;
                }

                BlockPos pos = new BlockPos(bx, by, bz);
                BlockState state = getBlockState(pos);

                if (!state.isAir()) {
                    float resistance = getResistance(pos, state);
                    FluidState fluidState = getFluidState(pos, state);

                    if (!fluidState.isEmpty()) {
                        rayPower -= (resistance + 0.3f) * 0.3f;
                        x += dirX;
                        y += dirY;
                        z += dirZ;
                        continue;
                    }

                    rayPower -= (resistance + 0.3f) * 0.3f;

                    if (rayPower > 0.0f) {

                        if (state.canBeReplaced() && (state.isAir() ||
                            state.is(Blocks.WATER) || state.is(Blocks.LAVA) ||
                            state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))) {
                            x += dirX;
                            y += dirY;
                            z += dirZ;
                            continue;
                        }

                        if (processedBlocks.add(posKey)) {
                            destroyedBlocks.add(pos);
                        }
                    }
                }

                x += dirX;
                y += dirY;
                z += dirZ;
                rayPower -= 0.22500001f;
            }
        }
    }

    private void calculateEntityDamageInline(Vec3 center, float power, Bridge bridge) {
        double radius = Math.min(power * 2.0, 8.0);
        this.centerX = center.x;
        this.centerY = center.y;
        this.centerZ = center.z;

        AABB searchBox = new AABB(
            centerX - radius, centerY - radius, centerZ - radius,
            centerX + radius, centerY + radius, centerZ + radius
        );

        List<Entity> entities;
        if (org.virgil.akiasync.mixin.util.DirectEntityQuery.isAvailable()) {
            entities = org.virgil.akiasync.mixin.util.DirectEntityQuery.getEntitiesInRange(level, searchBox);
        } else {
            entities = level.getEntities(null, searchBox);
        }

        if (entities.isEmpty()) {
            return;
        }

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[UnifiedEngine] Processing %d entities", entities.size());
        }

        int vectorStep = isVectorApiAvailable() && vectorHelper != null ? vectorHelper.getStep() : VECTOR_STEP_FALLBACK;
        if (isVectorApiAvailable() && entities.size() >= vectorStep) {
            try {
                calculateEntityDamageVectorized(entities, center, radius, bridge);
            } catch (NoClassDefFoundError | UnsupportedClassVersionError e) {

                markVectorApiUnavailable();
                calculateEntityDamageScalar(entities, center, radius, bridge);
            }
        } else {
            calculateEntityDamageScalar(entities, center, radius, bridge);
        }
    }

    private static synchronized void markVectorApiUnavailable() {
        vectorApiAvailable = false;
    }

    private void calculateEntityDamageVectorized(List<Entity> entities, Vec3 center, double radius, Bridge bridge) {
        if (vectorHelper == null || !vectorHelper.isInitialized()) {
            calculateEntityDamageScalar(entities, center, radius, bridge);
            return;
        }

        int count = entities.size();
        int step = vectorHelper.getStep();
        int globalNum = (count + step - 1) / step;

        byte[][] xm = new byte[globalNum][step];
        byte[][] ym = new byte[globalNum][step];
        byte[][] zm = new byte[globalNum][step];
        byte[][] xa = new byte[globalNum][step];
        byte[][] ya = new byte[globalNum][step];
        byte[][] za = new byte[globalNum][step];
        Entity[] storage = new Entity[count];

        int idx = 0;
        int fullRows = count / step;

        for (int row = 0; row < fullRows; row++) {
            for (int j = 0; j < step; j++) {
                Entity entity = entities.get(idx);
                storage[idx] = entity;
                AABB box = entity.getBoundingBox();
                xm[row][j] = quantize(box.minX - centerX);
                ym[row][j] = quantize(box.minY - centerY);
                zm[row][j] = quantize(box.minZ - centerZ);
                xa[row][j] = quantize(box.maxX - centerX);
                ya[row][j] = quantize(box.maxY - centerY);
                za[row][j] = quantize(box.maxZ - centerZ);
                idx++;
            }
        }

        int tailCount = count % step;
        if (tailCount > 0) {
            int endIndex = globalNum - 1;
            for (int j = 0; j < tailCount; j++) {
                Entity entity = entities.get(idx);
                storage[idx] = entity;
                AABB box = entity.getBoundingBox();
                xm[endIndex][j] = quantize(box.minX - centerX);
                ym[endIndex][j] = quantize(box.minY - centerY);
                zm[endIndex][j] = quantize(box.minZ - centerZ);
                xa[endIndex][j] = quantize(box.maxX - centerX);
                ya[endIndex][j] = quantize(box.maxY - centerY);
                za[endIndex][j] = quantize(box.maxZ - centerZ);
                idx++;
            }

            for (int j = tailCount; j < step; j++) {
                xm[endIndex][j] = Byte.MAX_VALUE;
                ym[endIndex][j] = Byte.MAX_VALUE;
                zm[endIndex][j] = Byte.MAX_VALUE;
                xa[endIndex][j] = Byte.MIN_VALUE;
                ya[endIndex][j] = Byte.MIN_VALUE;
                za[endIndex][j] = Byte.MIN_VALUE;
            }
        }

        byte qMinX = quantize(-radius);
        byte qMinY = quantize(-radius);
        byte qMinZ = quantize(-radius);
        byte qMaxX = quantize(radius);
        byte qMaxY = quantize(radius);
        byte qMaxZ = quantize(radius);

        try {

            performVectorizedDamageCalculation(
                xm, ym, zm, xa, ya, za, storage, count, globalNum, step,
                qMinX, qMinY, qMinZ, qMaxX, qMaxY, qMaxZ,
                center, radius, bridge
            );
        } catch (Throwable e) {
            ExceptionHandler.handleExpected("UnifiedExplosionEngine", "vectorizedDamage",
                e instanceof Exception ? (Exception) e : new RuntimeException(e));

            calculateEntityDamageScalar(entities, center, radius, bridge);
        }
    }

    private void performVectorizedDamageCalculation(
            byte[][] xm, byte[][] ym, byte[][] zm,
            byte[][] xa, byte[][] ya, byte[][] za,
            Entity[] storage, int count, int globalNum, int step,
            byte qMinX, byte qMinY, byte qMinZ,
            byte qMaxX, byte qMaxY, byte qMaxZ,
            Vec3 center, double radius, Bridge bridge) throws Throwable {

        Class<?> byteVectorClass = Class.forName("jdk.incubator.vector.ByteVector");
        Object species = vectorHelper.getSpecies();
        MethodHandle constructor = vectorHelper.getConstructor();

        java.lang.reflect.Method broadcastMethod = byteVectorClass.getMethod("broadcast",
            Class.forName("jdk.incubator.vector.VectorSpecies"), byte.class);

        Object qMinXv = broadcastMethod.invoke(null, species, qMinX);
        Object qMinYv = broadcastMethod.invoke(null, species, qMinY);
        Object qMinZv = broadcastMethod.invoke(null, species, qMinZ);
        Object qMaxXv = broadcastMethod.invoke(null, species, qMaxX);
        Object qMaxYv = broadcastMethod.invoke(null, species, qMaxY);
        Object qMaxZv = broadcastMethod.invoke(null, species, qMaxZ);

        java.lang.reflect.Method ltMethod = byteVectorClass.getMethod("lt", byteVectorClass);
        Class<?> maskClass = Class.forName("jdk.incubator.vector.VectorMask");
        java.lang.reflect.Method andMethod = maskClass.getMethod("and", maskClass);
        java.lang.reflect.Method toLongMethod = maskClass.getMethod("toLong");

        outerLoop:
        for (int footprint = 0; footprint < globalNum; footprint++) {
            int realprint = footprint * step;

            Object minX = constructor.invoke(xm[footprint]);
            Object minY = constructor.invoke(ym[footprint]);
            Object minZ = constructor.invoke(zm[footprint]);
            Object maxX = constructor.invoke(xa[footprint]);
            Object maxY = constructor.invoke(ya[footprint]);
            Object maxZ = constructor.invoke(za[footprint]);

            Object xCond1 = ltMethod.invoke(minX, qMaxXv);
            Object xCond2 = ltMethod.invoke(qMinXv, maxX);
            Object xCond = andMethod.invoke(xCond1, xCond2);

            Object yCond1 = ltMethod.invoke(minY, qMaxYv);
            Object yCond2 = ltMethod.invoke(qMinYv, maxY);
            Object yCond = andMethod.invoke(yCond1, yCond2);

            Object zCond1 = ltMethod.invoke(minZ, qMaxZv);
            Object zCond2 = ltMethod.invoke(qMinZv, maxZ);
            Object zCond = andMethod.invoke(zCond1, zCond2);

            Object mask = andMethod.invoke(andMethod.invoke(xCond, yCond), zCond);

            long bits = (long) toLongMethod.invoke(mask);
            while (bits != 0) {
                int j = Long.numberOfTrailingZeros(bits);
                int entityIdx = realprint + j;

                if (entityIdx >= count) {
                    break outerLoop;
                }

                Entity entity = storage[entityIdx];
                if (entity != null) {
                    processEntityDamageInline(entity, center, radius, bridge);
                }

                bits &= (bits - 1);
            }
        }
    }

    private void calculateEntityDamageScalar(List<Entity> entities, Vec3 center, double radius, Bridge bridge) {
        for (Entity entity : entities) {
            processEntityDamageInline(entity, center, radius, bridge);
        }
    }

    private void processEntityDamageInline(Entity entity, Vec3 center, double radius, Bridge bridge) {
        Vec3 entityPos = entity.position();
        double dx = entityPos.x - center.x;
        double dy = entityPos.y - center.y;
        double dz = entityPos.z - center.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= radius) {
            return;
        }

        double exposure = calculateExposureInline(center, entity.getBoundingBox());
        if (exposure <= 0) {
            return;
        }

        double impact = (1.0 - dist / radius) * exposure;
        if (impact <= 0.0) {
            return;
        }

        double knockbackX = dx / dist * impact;
        double knockbackY = Math.max(dy / dist * impact, impact * 0.3);
        double knockbackZ = dz / dist * impact;

        double maxKnockback = 2.0;
        double knockbackLength = Math.sqrt(knockbackX * knockbackX + knockbackY * knockbackY + knockbackZ * knockbackZ);
        if (knockbackLength > maxKnockback) {
            double scale = maxKnockback / knockbackLength;
            knockbackX *= scale;
            knockbackY *= scale;
            knockbackZ *= scale;
        }

        hurtEntities.put(entity.getUUID(), new Vec3(knockbackX, knockbackY, knockbackZ));

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[UnifiedEngine] Entity %s: dist=%.2f, exposure=%.3f, impact=%.3f",
                entity.getUUID(), dist, exposure, impact);
        }
    }

    private double calculateExposureInline(Vec3 explosionCenter, AABB aabb) {
        Bridge bridge = BridgeManager.getBridge();
        boolean useFullRaycast = bridge != null && bridge.isTNTUseFullRaycast();

        double sizeX = aabb.maxX - aabb.minX;
        double sizeY = aabb.maxY - aabb.minY;
        double sizeZ = aabb.maxZ - aabb.minZ;

        double stepMultiplier = 2.0;
        double stepX = 1.0 / (sizeX * stepMultiplier + 1.0);
        double stepY = 1.0 / (sizeY * stepMultiplier + 1.0);
        double stepZ = 1.0 / (sizeZ * stepMultiplier + 1.0);

        if (stepX < 0 || stepY < 0 || stepZ < 0) {
            return 0;
        }

        int visibleRays = 0;
        int totalRays = 0;

        double offsetX = useFullRaycast ? (1.0 - Math.floor(1.0 / stepX) * stepX) * 0.5 : 0;
        double offsetZ = useFullRaycast ? (1.0 - Math.floor(1.0 / stepZ) * stepZ) * 0.5 : 0;

        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    double targetX = Mth.lerp(x, aabb.minX, aabb.maxX) + offsetX;
                    double targetY = Mth.lerp(y, aabb.minY, aabb.maxY);
                    double targetZ = Mth.lerp(z, aabb.minZ, aabb.maxZ) + offsetZ;

                    if (!hasBlockCollisionDDA(explosionCenter.x, explosionCenter.y, explosionCenter.z,
                                              targetX, targetY, targetZ)) {
                        visibleRays++;
                    }
                    totalRays++;
                }
            }
        }

        return totalRays > 0 ? (double) visibleRays / totalRays : 0;
    }

    private boolean hasBlockCollisionDDA(double startX, double startY, double startZ,
                                          double endX, double endY, double endZ) {
        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.01) {
            return false;
        }

        dx /= distance;
        dy /= distance;
        dz /= distance;

        int blockX = (int) Math.floor(startX);
        int blockY = (int) Math.floor(startY);
        int blockZ = (int) Math.floor(startZ);

        int endBlockX = (int) Math.floor(endX);
        int endBlockY = (int) Math.floor(endY);
        int endBlockZ = (int) Math.floor(endZ);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tMaxX = computeTMax(startX, dx, stepX);
        double tMaxY = computeTMax(startY, dy, stepY);
        double tMaxZ = computeTMax(startZ, dz, stepZ);

        double tDeltaX = stepX != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
        double tDeltaZ = stepZ != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

        for (int step = 0; step < 200; step++) {
            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
            BlockState state = getBlockState(pos);

            if (!state.isAir()) {
                float resistance = getResistance(pos, state);
                FluidState fluidState = getFluidState(pos, state);

                if (!fluidState.isEmpty()) {
                    if (resistance > 0.5f) return true;
                } else if (resistance > 0.5f) {
                    return true;
                }
            }

            if (blockX == endBlockX && blockY == endBlockY && blockZ == endBlockZ) {
                break;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    blockX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    blockZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    blockY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    blockZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return false;
    }

    private double computeTMax(double current, double direction, int step) {
        if (step == 0) {
            return Double.MAX_VALUE;
        }
        double blockCoord = Math.floor(current);
        double boundary = step > 0 ? blockCoord + 1.0 : blockCoord;
        return (boundary - current) / direction;
    }

    private BlockState getBlockState(BlockPos pos) {
        long key = pos.asLong();
        BlockState cached = blockStateCache.get(key);
        if (cached != null) {
            return cached;
        }
        BlockState state = level.getBlockState(pos);
        blockStateCache.put(key, state);
        return state;
    }

    private float getResistance(BlockPos pos, BlockState state) {
        long key = pos.asLong();
        float cached = resistanceCache.get(key);
        if (cached >= 0) {
            return cached;
        }
        float resistance = state.getBlock().getExplosionResistance();
        resistanceCache.put(key, resistance);
        return resistance;
    }

    private FluidState getFluidState(BlockPos pos, BlockState state) {
        long key = pos.asLong();
        FluidState cached = fluidStateCache.get(key);
        if (cached != null) {
            return cached;
        }
        FluidState fluidState = state.getFluidState();
        fluidStateCache.put(key, fluidState);
        return fluidState;
    }

    private static byte quantize(double value) {
        return (byte) Math.clamp(Math.round(value * 16), Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    private int getCacheExpiryTicks() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null ? bridge.getTNTCacheExpiryTicks() : 600;
    }

    private void cleanupIfNeeded() {
        long currentTime = level.getGameTime();
        if (currentTime - lastCleanupTime < getCacheExpiryTicks()) {
            return;
        }

        if (blockStateCache.size() > 2000) {
            blockStateCache.clear();
        }
        if (resistanceCache.size() > 2000) {
            resistanceCache.clear();
        }
        if (fluidStateCache.size() > 1000) {
            fluidStateCache.clear();
        }

        lastCleanupTime = currentTime;
    }

    private void clear() {
        blockStateCache.clear();
        resistanceCache.clear();
        fluidStateCache.clear();
        destroyedBlocks.clear();
        hurtEntities.clear();
        processedBlocks.clear();
    }

    public String getStats() {
        boolean vectorAvailable = isVectorApiAvailable();
        int vectorStep = vectorAvailable && vectorHelper != null ? vectorHelper.getStep() : 0;
        return String.format("UnifiedEngine[BlockState=%d, Resistance=%d, Fluid=%d, Vector=%s]",
            blockStateCache.size(), resistanceCache.size(), fluidStateCache.size(),
            vectorAvailable ? "ByteVector[" + vectorStep + "]" : "Scalar");
    }

    public static String getGlobalStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("UnifiedExplosionEngine Global Stats:\n");
        sb.append("  Levels: ").append(ENGINES.size()).append("\n");

        boolean vectorAvailable = isVectorApiAvailable();
        int vectorStep = vectorAvailable && vectorHelper != null ? vectorHelper.getStep() : 0;
        sb.append("  Vector Support: ").append(vectorAvailable ? "Yes (ByteVector[" + vectorStep + "])" : "No (Scalar fallback)").append("\n");
        sb.append("  Precomputed Rays: ").append(RAY_COUNT).append("\n");

        for (Map.Entry<ServerLevel, UnifiedExplosionEngine> entry : ENGINES.entrySet()) {
            sb.append("  ").append(entry.getKey().dimension().location()).append(": ");
            sb.append(entry.getValue().getStats()).append("\n");
        }

        return sb.toString();
    }

    public static boolean isVectorSupported() {
        return isVectorApiAvailable();
    }

    public static int getVectorWidth() {
        if (isVectorApiAvailable() && vectorHelper != null) {
            return vectorHelper.getStep();
        }
        return 0;
    }
}
