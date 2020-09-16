package com.ericlam.mc.async.create.world.xuan;

import com.ericlam.mc.async.create.world.main.AsyncCreateWorld;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class UnknownWorldCreateHandle implements WorldCreateHandler{

    @Override
    public World createWorld(WorldCreator creator) {
        AsyncCreateWorld.getProvidingPlugin(AsyncCreateWorld.class).getLogger().warning("Unknown MineCraft Version, async handle failed, use back Bukkit world creator");
        return creator.createWorld();
    }
}
