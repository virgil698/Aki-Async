package com.akiasync.mixin.mixins.datapack;

import com.akiasync.mixin.datapack.ZipDataPackCache;
import com.akiasync.mixin.datapack.ZipDataPackCache.ArchiveFingerprint;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin {
    @Shadow
    @Final
    private FilePackResources.SharedZipFileAccess zipFileAccess;

    @Shadow
    @Final
    private String prefix;

    @Unique
    private volatile ArchiveFingerprint akiAsync$archiveFingerprint;

    @ModifyReturnValue(
            method = "getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/server/packs/resources/IoSupplier;",
            at = @At("RETURN")
    )
    private IoSupplier<InputStream> akiAsync$cacheDirectResource(
            IoSupplier<InputStream> original,
            PackType packType,
            Identifier location
    ) {
        if (original == null || packType != PackType.SERVER_DATA) {
            return original;
        }
        return ZipDataPackCache.wrap(
                akiAsync$fingerprint(), prefix, packType.getDirectory(), location.toString(), original
        );
    }

    @WrapMethod(method = "listResources")
    private void akiAsync$cacheResourceIndex(
            PackType packType,
            String namespace,
            String path,
            PackResources.ResourceOutput output,
            Operation<Void> original
    ) {
        if (packType != PackType.SERVER_DATA) {
            original.call(packType, namespace, path, output);
            return;
        }

        ArchiveFingerprint archive = akiAsync$fingerprint();
        List<Identifier> cached = ZipDataPackCache.findIndex(
                archive, prefix, packType.getDirectory(), namespace, path
        );
        if (cached != null) {
            List<IoSupplier<InputStream>> suppliers = new ArrayList<>(cached.size());
            FilePackResources self = (FilePackResources) (Object) this;
            for (Identifier location : cached) {
                IoSupplier<InputStream> supplier = self.getResource(packType, location);
                if (supplier == null) {
                    ZipDataPackCache.invalidateIndex(
                            archive, prefix, packType.getDirectory(), namespace, path
                    );
                    original.call(packType, namespace, path, output);
                    return;
                }
                suppliers.add(supplier);
            }
            for (int index = 0; index < cached.size(); index++) {
                output.accept(cached.get(index), suppliers.get(index));
            }
            return;
        }

        List<Identifier> discovered = new ArrayList<>();
        PackResources.ResourceOutput cachingOutput = (location, supplier) -> {
            discovered.add(location);
            output.accept(location, ZipDataPackCache.wrap(
                    archive, prefix, packType.getDirectory(), location.toString(), supplier
            ));
        };
        original.call(packType, namespace, path, cachingOutput);
        ZipDataPackCache.storeIndex(
                archive, prefix, packType.getDirectory(), namespace, path, discovered
        );
    }

    @Unique
    private ArchiveFingerprint akiAsync$fingerprint() {
        ArchiveFingerprint next = ZipDataPackCache.fingerprint(zipFileAccess.file);
        ArchiveFingerprint current = akiAsync$archiveFingerprint;
        if (!next.equals(current)) {
            akiAsync$archiveFingerprint = next;
        }
        return next;
    }
}
