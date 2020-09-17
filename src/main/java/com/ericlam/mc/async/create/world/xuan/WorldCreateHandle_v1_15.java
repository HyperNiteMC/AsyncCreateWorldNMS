package com.ericlam.mc.async.create.world.xuan;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;

public class WorldCreateHandle_v1_15 implements WorldCreateHandler {

    @Override
    public World createWorld(WorldCreator creator) {

        CraftServer craftServer = ((CraftServer) Bukkit.getServer());
        DedicatedServer console = ((CraftServer) Bukkit.getServer()).getServer();

        Preconditions.checkState(!console.worldServer.isEmpty(), "Cannot create additional worlds on STARTUP");
        org.apache.commons.lang.Validate.notNull(creator, "Creator may not be null");
        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(craftServer.getWorldContainer(), name);
        World world = craftServer.getWorld(name);
        WorldType type = WorldType.getType(creator.type().getName());
        boolean generateStructures = creator.generateStructures();
        if (world != null) {
            return world;
        } else if (folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        } else {
            if (generator == null) {
                generator = craftServer.getGenerator(name);
            }

            console.convertWorld(name);
            int dimension = 10 + console.worldServer.size();
            boolean used = false;

            do {
                Iterator var10 = console.getWorlds().iterator();

                while (var10.hasNext()) {
                    WorldServer server = (WorldServer) var10.next();
                    used = server.getWorldProvider().getDimensionManager().getDimensionID() + 1 == dimension;
                    if (used) {
                        ++dimension;
                        break;
                    }
                }
            } while (used);

            boolean hardcore = false;
            WorldNBTStorage sdm = new WorldNBTStorage(craftServer.getWorldContainer(), name, craftServer.getServer(), craftServer.getHandle().getServer().dataConverterManager);
            WorldData worlddata = sdm.getWorldData();
            WorldSettings worldSettings;
            if (worlddata == null) {
                worldSettings = new WorldSettings(/*(Long)PaperConfig.seedOverride.getOrDefault(name, creator.seed())*/ creator.seed(), EnumGamemode.getById(craftServer.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
                JsonElement parsedSettings = (new JsonParser()).parse(creator.generatorSettings());
                if (parsedSettings.isJsonObject()) {
                    worldSettings.setGeneratorSettings(parsedSettings.getAsJsonObject());
                }

                worlddata = new WorldData(worldSettings, name);
            } else {
                worlddata.setName(name);
                worldSettings = new WorldSettings(worlddata);
            }

            DimensionManager actualDimension = DimensionManager.a(creator.environment().getId());
            DimensionManager internalDimension = DimensionManager.register(name.toLowerCase(Locale.ENGLISH), new DimensionManager(dimension, actualDimension.getSuffix(), actualDimension.folder, (w, manager) -> {
                return (WorldProvider) actualDimension.providerFactory.apply(w, manager);
            }, actualDimension.hasSkyLight(), actualDimension.getGenLayerZoomer(), actualDimension));
            WorldServer internal = new WorldServer(console, console.executorService, sdm, worlddata, internalDimension, console.getMethodProfiler(), console.worldLoadListenerFactory.create(11), creator.environment(), generator);
            if (craftServer.getWorld(name) == null) {
                return null;
            } else {
                //console.initWorld(internal, worlddata, worldSettings); // 不要初始化
                internal.worldData.setDifficulty(EnumDifficulty.EASY);
                internal.setSpawnFlags(true, true);
                console.worldServer.put(internal.getWorldProvider().getDimensionManager(), internal);

                internal.keepSpawnInMemory = false; // 不緩存記憶體
                world = internal.getWorld();
                world.setKeepSpawnInMemory(false);  // 不緩存記憶體

                //this.pluginManager.callEvent(new WorldInitEvent(internal.getWorld()));
                //this.getServer().loadSpawn(internal.getChunkProvider().playerChunkMap.worldLoadListener, internal);
                //this.pluginManager.callEvent(new WorldLoadEvent(internal.getWorld()));

                return world;
            }
        }
    }

}
