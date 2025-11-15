package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.entity.CraftSentientNPC;
import net.minecraft.server.EntityWaterAnimal;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WaterMob;

public class CraftWaterMob extends CraftLivingEntity implements WaterMob, CraftSentientNPC { // Paper

    public CraftWaterMob(CraftServer server, EntityWaterAnimal entity) {
        super(server, entity);
    }

    @Override
    public EntityWaterAnimal getHandle() {
        return (EntityWaterAnimal) entity;
    }

    @Override
    public String toString() {
        return "CraftWaterMob";
    }
}
