package com.destroystokyo.paper.entity;

import net.minecraft.server.EntityInsentient;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

public interface CraftSentientNPC <T extends EntityInsentient> extends SentientNPC {
    T getHandle();

    default public void setTarget(LivingEntity target) {
        T entity = getHandle();
        if (target == null) {
            entity.setGoalTarget(null, null, false);
        } else if (target instanceof CraftLivingEntity) {
            entity.setGoalTarget(((CraftLivingEntity) target).getHandle(), null, false);
        }
    }

    default public LivingEntity getTarget() {
        if (getHandle().getGoalTarget() == null) return null;

        return (CraftLivingEntity) getHandle().getGoalTarget().getBukkitEntity();
    }

}
