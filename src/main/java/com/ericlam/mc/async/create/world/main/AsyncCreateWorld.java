package com.ericlam.mc.async.create.world.main;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.ericlam.mc.async.create.world.AsyncCreateWorldAPI;
import com.ericlam.mc.async.create.world.xuan.*;
import main.java.com.ericlam.mc.async.create.world.xuan.WorldCreateHandle_v1_16_2;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AsyncCreateWorld extends JavaPlugin implements AsyncCreateWorldAPI {

    private static AsyncCreateWorldAPI api;
    private WorldCreateHandler worldCreateHandler;
    private final Map<MinecraftVersion, WorldCreateHandler> handlerMap = new HashMap<>();

    @Override
    public void onEnable() {
        api = this;

        // register
        this.register(MinecraftVersion.NETHER_UPDATE_2, new WorldCreateHandle_v1_16_2());
        this.register(MinecraftVersion.BEE_UPDATE, new WorldCreateHandle_v1_15());
        this.register(MinecraftVersion.VILLAGE_UPDATE, new WorldCreateHandle_v1_14());
        // register

        MinecraftVersion version = ProtocolLibrary.getProtocolManager().getMinecraftVersion();
        worldCreateHandler = Optional.ofNullable(handlerMap.get(version)).orElseGet(UnknownWorldCreateHandle::new);
    }

    public static AsyncCreateWorldAPI getApi(){
        return api;
    }

    @Override
    public World createWorld(WorldCreator creator) {
        return worldCreateHandler.createWorld(creator);
    }

    @Override
    public void register(MinecraftVersion version, WorldCreateHandler handler) {
        this.handlerMap.put(version, handler);
    }
}
