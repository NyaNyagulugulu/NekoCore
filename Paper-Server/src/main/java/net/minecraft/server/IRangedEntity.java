package net.minecraft.server;

public interface IRangedEntity {

    void a(EntityLiving entityliving, float f); default void rangedAttack(EntityLiving entityliving, float f) { a(entityliving, f); } // Paper OBF HELPER


    void p(boolean flag); default void setChargingAttack(boolean flag) { p(flag); } // Paper OBF HELPER
}
