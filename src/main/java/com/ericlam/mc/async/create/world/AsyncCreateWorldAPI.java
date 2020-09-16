package com.ericlam.mc.async.create.world;

import com.comphenix.protocol.utility.MinecraftVersion;
import com.ericlam.mc.async.create.world.xuan.WorldCreateHandler;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public interface AsyncCreateWorldAPI {

    World createWorld(WorldCreator creator);

    void register(MinecraftVersion version, WorldCreateHandler handler);

}
