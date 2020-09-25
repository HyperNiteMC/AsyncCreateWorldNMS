package com.ericlam.mc.async.create.world.main;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.ericlam.mc.async.create.world.AsyncCreateWorldAPI;
import com.ericlam.mc.async.create.world.xuan.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AsyncCreateWorld extends JavaPlugin implements AsyncCreateWorldAPI {

    private static AsyncCreateWorldAPI api;
    private MinecraftVersion currentVersion;
    private final Map<MinecraftVersion, WorldCreateHandler> handlerMap = new HashMap<>();

    @Override
    public void onEnable() {
        api = this;

        // register
        this.register(MinecraftVersion.NETHER_UPDATE_2, new WorldCreateHandle_v1_16_R2());
        this.register(MinecraftVersion.BEE_UPDATE, new WorldCreateHandle_v1_15());
        this.register(MinecraftVersion.VILLAGE_UPDATE, new WorldCreateHandle_v1_14());
        // register

        this.currentVersion = ProtocolLibrary.getProtocolManager().getMinecraftVersion();
    }

    public static AsyncCreateWorldAPI getApi() {
        return api;
    }

    @Override
    public WorldCreateHandler getWorldCreator() {
        return this.getWorldCreator(currentVersion);
    }

    @Override
    public WorldCreateHandler getWorldCreator(MinecraftVersion version) {
        return Optional.ofNullable(handlerMap.get(version)).orElseGet(UnknownWorldCreateHandle::new);
    }

    @Override
    public void register(MinecraftVersion version, WorldCreateHandler handler) {
        this.handlerMap.put(version, handler);
    }
}
