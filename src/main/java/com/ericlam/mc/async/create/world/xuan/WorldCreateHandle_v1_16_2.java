package main.java.com.ericlam.mc.async.create.world.xuan;

import com.destroystokyo.paper.PaperConfig;
import com.ericlam.mc.async.create.world.xuan.WorldCreateHandler;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.Validate;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.Locale;

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
        WorldType type = WorldType.getType(creator.type().getName());
        boolean generateStructures = creator.generateStructures();

        org.bukkit.World world = craftServer.getWorld(name);
        if (world != null) {
            return world;
        } else if (folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        } else {
            if (generator == null) {
                generator = craftServer.getGenerator(name);
            }

            dedicatedServer.convertWorld(name);
            int dimension = 10 + dedicatedServer.worldServer.size();
            boolean used = false;

            do {

                for (WorldServer server : dedicatedServer.getWorlds()) {
                    used = server.getWorldProvider().getDimensionManager().getDimensionID() + 1 == dimension;
                    if (used) {
                        ++dimension;
                        break;
                    }
                }
            } while (used);

            boolean hardcore = false;
            WorldNBTStorage sdm = new WorldNBTStorage(craftServer.getWorldContainer(), name, craftServer.getServer(), dedicatedServer.dataConverterManager);
            WorldData worlddata = sdm.getWorldData();
            WorldSettings worldSettings;
            if (worlddata == null) {
                worldSettings = new WorldSettings((Long) PaperConfig.seedOverride.getOrDefault(name, creator.seed()), EnumGamemode.getById(craftServer.getDefaultGameMode().getValue()), generateStructures, hardcore, type);
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
            }, actualDimension.hasSkyLight(), null ,actualDimension));
            WorldServer internal = new WorldServer(dedicatedServer, dedicatedServer.executorService, sdm, worlddata, internalDimension, dedicatedServer.getMethodProfiler(), craftServer.getServer().worldLoadListenerFactory.create(11), creator.environment(), generator);

            //dedicatedServer.initWorld(internal, worlddata, worldSettings);
            //internal.worldData.setDifficulty(EnumDifficulty.EASY);
            //internal.setSpawnFlags(true, true);
            // this.getServer().loadSpawn(internal.getChunkProvider().playerChunkMap.worldLoadListener, internal); // 不進行初始化
            //this.pluginManager.callEvent(new WorldLoadEvent(internal.getWorld())); // 不進行初始化


            internal.keepSpawnInMemory = false; // 不緩存記憶體
            world = internal.getWorld();
            world.setKeepSpawnInMemory(false); // 不緩存記憶體

            // 新增到清單中
            dedicatedServer.worldServer.put(internal.getWorldProvider().getDimensionManager(), internal);

            //Bukkit.getPluginManager().callEvent(new WorldInitEvent(world)); // 事件

            return world;
    }
}
