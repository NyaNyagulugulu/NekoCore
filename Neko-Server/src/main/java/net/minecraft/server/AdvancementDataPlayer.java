package net.minecraft.server;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementDataPlayer {

    private static final Logger a = LogManager.getLogger();
    private static final Gson b = (new GsonBuilder()).registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.a()).registerTypeAdapter(MinecraftKey.class, new MinecraftKey.a()).setPrettyPrinting().create();
    private static final TypeToken<Map<MinecraftKey, AdvancementProgress>> c = new TypeToken<Map<MinecraftKey, AdvancementProgress>>() { // CraftBukkit - decompile error
    };
    private final MinecraftServer d;
    private final File e;
    public final Map<Advancement, AdvancementProgress> data = Maps.newLinkedHashMap();
    private final Set<Advancement> g = Sets.newLinkedHashSet();
    private final Set<Advancement> h = Sets.newLinkedHashSet();
    private final Set<Advancement> i = Sets.newLinkedHashSet();
    private EntityPlayer player;
    @Nullable
    private Advancement k;
    private boolean l = true;

    public AdvancementDataPlayer(MinecraftServer minecraftserver, File file, EntityPlayer entityplayer) {
        this.d = minecraftserver;
        this.e = file;
        this.player = entityplayer;
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        // 不执行初始化逻辑
    }

    public AdvancementDataPlayer() {
        //System.out.println("成就系统已被删除");
        this.d = null;
        this.e = null;
        this.player = null;
    }

    public void a(EntityPlayer entityplayer) {
        this.player = entityplayer;
    }

    public void a() {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        // 空实现，不执行任何逻辑
    }

    public void b() {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        // 只清除数据，不执行其他初始化逻辑
        this.data.clear();
        this.g.clear();
        this.h.clear();
        this.i.clear();
        this.l = true;
        this.k = null;
    }

    private void d() {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private void e() {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private void f() {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private void g() {
        if (this.e.isFile()) {
            return;
        }



        this.f();
        this.e();
        this.d();
    }

    public void c() {
        HashMap hashmap = Maps.newHashMap();
        Iterator iterator = this.data.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            AdvancementProgress advancementprogress = (AdvancementProgress) entry.getValue();

            if (advancementprogress.b()) {
                hashmap.put(((Advancement) entry.getKey()).getName(), advancementprogress);
            }
        }

        if (this.e.getParentFile() != null) {
            this.e.getParentFile().mkdirs();
        }

        try {
            Files.write(AdvancementDataPlayer.b.toJson(hashmap), this.e, StandardCharsets.UTF_8);
        } catch (IOException ioexception) {
            AdvancementDataPlayer.a.error("Couldn\'t save player advancements to " + this.e, ioexception);
        }

    }

    public boolean grantCriteria(Advancement advancement, String s) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        return false;
    }

    public boolean revokeCritera(Advancement advancement, String s) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        return false;
    }

    private void c(Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private void d(Advancement advancement) {
        AdvancementProgress advancementprogress = this.getProgress(advancement);
        Iterator iterator = advancement.getCriteria().entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            CriterionProgress criterionprogress = advancementprogress.getCriterionProgress((String) entry.getKey());

            if (criterionprogress != null && (criterionprogress.a() || advancementprogress.isDone())) {
                CriterionInstance criterioninstance = ((Criterion) entry.getValue()).a();

                if (criterioninstance != null) {
                    CriterionTrigger criteriontrigger = CriterionTriggers.a(criterioninstance.a());

                    if (criteriontrigger != null) {
                        criteriontrigger.b(this, new CriterionTrigger.a(criterioninstance, advancement, (String) entry.getKey()));
                    }
                }
            }
        }

    }

    public void b(EntityPlayer entityplayer) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        // 不执行发送进度数据逻辑
        this.l = false;
    }

    public void a(@Nullable Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        Advancement advancement1 = this.k;

        if (advancement != null && advancement.b() == null && advancement.c() != null) {
            this.k = advancement;
        } else {
            this.k = null;
        }

        if (advancement1 != this.k) {
            // 发送null选择包
            this.player.playerConnection.sendPacket(new PacketPlayOutSelectAdvancementTab(null));
        }

    }

    public AdvancementProgress getProgress(Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        // 返回空的进度对象
        AdvancementProgress advancementprogress = new AdvancementProgress();
        advancementprogress.a(advancement.getCriteria(), advancement.i());
        return advancementprogress;
    }

    private void a(Advancement advancement, AdvancementProgress advancementprogress) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private void e(Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
    }

    private boolean f(Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        return false;
    }

    private boolean g(Advancement advancement) {
        //AdvancementDataPlayer.a.warn("成就系统已被删除");
        return false;
    }
}
