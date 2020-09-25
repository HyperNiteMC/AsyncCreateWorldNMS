package com.ericlam.mc.async.create.world.xuan;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import net.minecraft.server.v1_16_R2.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldCreateHandle_v1_16_R2 implements WorldCreateHandler {
    @Override
    public World createWorld(WorldCreator creator) {

        // 修補原本函數
        CraftServer     craftServer     = (CraftServer) Bukkit.getServer();
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
        
        
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
                worldSession = Convertable.a(craftServer.getWorldContainer().toPath()).c(name, actualDimension);
            } catch (IOException var21) {
                throw new RuntimeException(var21);
            }

            MinecraftServer.convertWorld(worldSession);
            boolean hardcore = creator.hardcore();
            RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a(DynamicOpsNBT.a, dedicatedServer.dataPackResources.h(), dedicatedServer.customRegistry);
            WorldDataServer worlddata = (WorldDataServer)worldSession.a(registryreadops, dedicatedServer.datapackconfiguration);
            if (worlddata == null) {
                Properties properties = new Properties();
                properties.put("generator-settings", creator.generatorSettings());
                properties.put("level-seed", Objects.toString(creator.seed()));
                properties.put("generate-structures", Objects.toString(creator.generateStructures()));
                properties.put("level-type", creator.type().getName());
                GeneratorSettings generatorsettings = GeneratorSettings.a(dedicatedServer.getCustomRegistry(), properties);
                WorldSettings worldSettings = new WorldSettings(name, EnumGamemode.getById(craftServer.getDefaultGameMode().getValue()), hardcore, EnumDifficulty.EASY, false, new GameRules(), dedicatedServer.datapackconfiguration);
                worlddata = new WorldDataServer(worldSettings, generatorsettings, Lifecycle.stable());
            }

            worlddata.checkName(name);
            worlddata.a(dedicatedServer.getServerModName(), dedicatedServer.getModded().isPresent());
            if (dedicatedServer.options.has("forceUpgrade")) {
                net.minecraft.server.v1_16_R2.Main.convertWorld(worldSession, DataConverterRegistry.a(), dedicatedServer.options.has("eraseCache"), () -> true, worlddata.getGeneratorSettings().d().d().stream().map((entry) -> ResourceKey.a(IRegistry.K, entry.getKey().a())).collect(ImmutableSet.toImmutableSet()));
            }

            long j = BiomeManager.a(creator.seed());
            List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(worlddata));
            RegistryMaterials<WorldDimension> registrymaterials = worlddata.getGeneratorSettings().d();
            WorldDimension worlddimension = registrymaterials.a(actualDimension);
            DimensionManager dimensionmanager;
            Object chunkgenerator;
            if (worlddimension == null) {
                dimensionmanager = dedicatedServer.customRegistry.a().d(DimensionManager.OVERWORLD);
                chunkgenerator = GeneratorSettings.a(dedicatedServer.customRegistry.b(IRegistry.ay), dedicatedServer.customRegistry.b(IRegistry.ar), (new Random()).nextLong());
            } else {
                dimensionmanager = worlddimension.b();
                chunkgenerator = worlddimension.c();
            }

            ResourceKey<net.minecraft.server.v1_16_R2.World> worldKey = ResourceKey.a(IRegistry.L, new MinecraftKey(name.toLowerCase(Locale.ENGLISH)));
            WorldServer internal = new WorldServer(dedicatedServer, dedicatedServer.executorService, worldSession, worlddata, worldKey, dimensionmanager, craftServer.getServer().worldLoadListenerFactory.create(11), (net.minecraft.server.v1_16_R2.ChunkGenerator)chunkgenerator, worlddata.getGeneratorSettings().isDebugWorld(), j, creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of(), true, creator.environment(), generator);

            if (craftServer.getWorld(name) != null) {
            //if (!this.worlds.containsKey(name.toLowerCase(Locale.ENGLISH))) {
                return null;
            } else {
                // 不進行初始化
                //dedicatedServer.initWorld(internal, worlddata, worlddata, worlddata.getGeneratorSettings());
                internal.setSpawnFlags(true, true);
                dedicatedServer.worldServer.put(internal.getDimensionKey(), internal);

                // 不緩存記憶體
                world = internal.getWorld();
                world.setKeepSpawnInMemory(false);

                // 取消事件
                //this.pluginManager.callEvent(new WorldInitEvent(internal.getWorld()));
                // 不進行初始化
                //this.getServer().loadSpawn(internal.getChunkProvider().playerChunkMap.worldLoadListener, internal);
                // 取消事件
                //this.pluginManager.callEvent(new WorldLoadEvent(internal.getWorld()));

                return internal.getWorld();
            }
        }
    }



}
