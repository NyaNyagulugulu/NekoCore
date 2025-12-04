package net.minecraft.server;

import java.io.File;

// 删不得，删了直接炸
public class AdvancementDataPlayer {
    public AdvancementDataPlayer() {
        // Empty constructor
    }

    public AdvancementDataPlayer(MinecraftServer minecraftserver, File file, EntityPlayer entityplayer) {
        // Constructor for compatibility
    }

    public void a(EntityPlayer entityplayer) {
        // Do nothing
    }

    public void b() {
        // Do nothing
    }

    public void b(EntityPlayer player) {
        // Do nothing
    }

    public void c() {
        // Do nothing
    }

    public boolean b(String s) {
        return false;
    }

    public boolean grantCriteria(Advancement advancement, String s) {
        return false;
    }

    public boolean revokeCritera(Advancement advancement, String s) {
        return false;
    }

    public Object getProgress(Advancement advancement) {
        return null;
    }

    public void a(Advancement advancement, Object advancementprogress) {
        // Do nothing
    }
}
