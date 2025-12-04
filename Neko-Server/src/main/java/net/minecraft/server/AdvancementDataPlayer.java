package net.minecraft.server;

import java.io.File;

// 完整的空壳，实现所有插件需要的方法签名
public class AdvancementDataPlayer {

    public AdvancementDataPlayer() {}

    public AdvancementDataPlayer(MinecraftServer minecraftserver, File file, EntityPlayer entityplayer) {
        // 保持构造器存在即可
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // Citizens 会调用的关键方法（必须存在）
    public void a() {
        // 清空所有进度（空实现）
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // Vanilla 登录事件会调用
    public void a(EntityPlayer entityplayer) {
        // 空
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // 多个插件要调的初始化方法
    public void b() {
        // 空
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    public void b(EntityPlayer player) {
        // 空
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    public void c() {
        // 空
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // 检查成就完成状态（插件会用）
    public boolean b(String s) {
        return false;
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    public boolean grantCriteria(Advancement advancement, String s) {
        return false;
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    public boolean revokeCritera(Advancement advancement, String s) {
        return false;
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // Vanilla & 多插件会查询进度
    public Object getProgress(Advancement advancement) {
        return null;
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }

    // Vanilla 会调用（同步进度）
    public void a(Advancement advancement, Object advancementprogress) {
        // 空
        DedicatedServer.LOGGER.warn("成就系统被删除");
    }
}
