package com.ericlam.mc.async.create.world.xuan;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R1.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldCreateHandle_v1_16_2 implements WorldCreateHandler {
    @Override
    public World createWorld(WorldCreator creator) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer(); // 修補原本函數
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer(); // 修補原本函數
        Preconditions.checkState(!dedicatedServer.worldServer.isEmpty(), "Cannot create additional worlds on STARTUP");
        Validate.notNull(creator, "Creator may not be null");
        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(craftServer.getWorldContainer(), name);
        World world = craftServer.getWorld(name);
        if (world != null) {
            return world;
        } else if (folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        } else {
            if (generator == null) {
                generator = craftServer.getGenerator(name);
            }

            ResourceKey actualDimension;
            switch(creator.environment()) {
                case NORMAL:
                    actualDimension = WorldDimension.OVERWORLD;
                    break;
                case NETHER:
                    actualDimension = WorldDimension.THE_NETHER;
                    break;
                case THE_END:
                    actualDimension = WorldDimension.THE_END;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal dimension");
            }

            Convertable.ConversionSession worldSession;
            try {
                worldSession = Convertable.a(dedicatedServer.convertable.a(net.minecraft.server.v1_16_R1.World.OVERWORLD).getParentFile().toPath()).c(name, actualDimension);
            } catch (IOException var23) {
                throw new RuntimeException(var23);
            }

            MinecraftServer.convertWorld(worldSession);
            boolean hardcore = creator.hardcore();
            IRegistryCustom.Dimension iregistrycustom_dimension = IRegistryCustom.b();
            RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, dedicatedServer.dataPackResources.h(), iregistrycustom_dimension);
            WorldDataServer worlddata = (WorldDataServer)worldSession.a(registryreadops, dedicatedServer.datapackconfiguration);
            if (worlddata == null) {
                Properties properties = new Properties();
                properties.put("generator-settings", Objects.toString(creator.generatorSettings()));
                properties.put("level-seed", Objects.toString(creator.seed()));
                properties.put("generate-structures", Objects.toString(creator.generateStructures()));
                properties.put("level-type", Objects.toString(creator.type().getName()));
                GeneratorSettings generatorsettings = GeneratorSettings.a(properties);
                WorldSettings worldSettings = new WorldSettings(name, EnumGamemode.getById(GameMode.getByValue(dedicatedServer.getWorldServer(net.minecraft.server.v1_16_R1.World.OVERWORLD).worldDataServer.getGameType().getId()).getValue()), hardcore, EnumDifficulty.EASY, false, new GameRules(), dedicatedServer.datapackconfiguration);
                worlddata = new WorldDataServer(worldSettings, generatorsettings, Lifecycle.stable());
            }

            worlddata.checkName(name);
            worlddata.a(dedicatedServer.getServerModName(), dedicatedServer.getModded().isPresent());
            if (dedicatedServer.options.has("forceUpgrade")) {
                net.minecraft.server.v1_16_R1.Main.convertWorld(worldSession, DataConverterRegistry.a(), dedicatedServer.options.has("eraseCache"), () -> {
                    return true;
                }, (ImmutableSet)worlddata.getGeneratorSettings().e().c().stream().map((entry) -> {
                    return ResourceKey.a(IRegistry.ad, ((ResourceKey)entry.getKey()).a());
                }).collect(ImmutableSet.toImmutableSet()));
            }

            long j = BiomeManager.a(creator.seed());
            List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));
            RegistryMaterials<WorldDimension> registrymaterials = worlddata.getGeneratorSettings().e();
            WorldDimension worlddimension = (WorldDimension)registrymaterials.a(actualDimension);
            DimensionManager dimensionmanager;
            Object chunkgenerator;
            if (worlddimension == null) {
                dimensionmanager = DimensionManager.a();
                chunkgenerator = GeneratorSettings.a((new Random()).nextLong());
            } else {
                dimensionmanager = worlddimension.b();
                chunkgenerator = worlddimension.c();
            }

            ResourceKey<DimensionManager> typeKey = (ResourceKey)dedicatedServer.f.a().c(dimensionmanager).orElseThrow(() -> {
                return new IllegalStateException("Unregistered dimension type: " + dimensionmanager);
            });
            ResourceKey<net.minecraft.server.v1_16_R1.World> worldKey = ResourceKey.a(IRegistry.ae, new MinecraftKey(name.toLowerCase(Locale.ENGLISH)));
            WorldServer internal = new WorldServer(dedicatedServer, dedicatedServer.executorService, worldSession, worlddata, worldKey, typeKey, dimensionmanager, dedicatedServer.worldLoadListenerFactory.create(11), (net.minecraft.server.v1_16_R1.ChunkGenerator)chunkgenerator, worlddata.getGeneratorSettings().isDebugWorld(), j, creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of(), true, creator.environment(), generator);

            if (Bukkit.getWorld(name.toLowerCase(Locale.ENGLISH)) != null) {
                return null;
            } else {
                //this.console.initWorld(internal, worlddata, worlddata, worlddata.getGeneratorSettings());
                internal.setSpawnFlags(true, true);
                dedicatedServer.worldServer.put(internal.getDimensionKey(), internal);
                //this.pluginManager.callEvent(new WorldInitEvent(internal.getWorld()));
                dedicatedServer.loadSpawn(internal.getChunkProvider().playerChunkMap.worldLoadListener, internal);
                //this.pluginManager.callEvent(new WorldLoadEvent(internal.getWorld()));

                internal.keepSpawnInMemory = false; // 不緩存記憶體
                world = internal.getWorld();
                world.setKeepSpawnInMemory(false);  // 不緩存記憶體

                return internal.getWorld();
            }
        }
    }



}
