package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.entity.CraftSentientNPC;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.EntityLiving;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.ComplexLivingEntity;

public abstract class CraftComplexLivingEntity extends CraftLivingEntity implements ComplexLivingEntity, CraftSentientNPC { // Paper
    public CraftComplexLivingEntity(CraftServer server, EntityLiving entity) {
        super(server, entity);
    }

    @Override
    public EntityInsentient getHandle() { // Paper
        return (EntityInsentient) entity; // Paper
    }

    @Override
    public String toString() {
        return "CraftComplexLivingEntity";
    }
}
