package net.minecraft.server;

public interface KeyedObject {
    MinecraftKey getMinecraftKey();
    default String getMinecraftKeyString() {
        return getMinecraftKey().toString();
    }
}
