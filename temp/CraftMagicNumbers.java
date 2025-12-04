package org.bukkit.craftbukkit.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.AdvancementDataWorld;

import net.minecraft.server.Block;
import net.minecraft.server.Blocks;
import net.minecraft.server.ChatDeserializer;
import net.minecraft.server.Item;
import net.minecraft.server.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MojangsonParseException;
import net.minecraft.server.MojangsonParser;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.StatisticList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.UnsafeValues;
import org.bukkit.advancement.Advancement;
import org.bukkit.craftbukkit.CraftStatistic;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

@SuppressWarnings("deprecation")
public final class CraftMagicNumbers implements UnsafeValues {
    public static final UnsafeValues INSTANCE = new CraftMagicNumbers();

    private CraftMagicNumbers() {}

    public static Block getBlock(org.bukkit.block.Block block) {
        return getBlock(block.getType());
    }

    @Deprecated
    // A bad method for bad magic.
    public static Block getBlock(int id) {
        return getBlock(Material.getMaterial(id));
    }

    @Deprecated
    // A bad method for bad magic.
    public static int getId(Block block) {
        return Block.getId(block);
    }

    public static Material getMaterial(Block block) {
        return Material.getMaterial(net.minecraft.server.Block.getId(block));
    }

    public static Material getMaterial(net.minecraft.server.Item item) {
        return Material.getMaterial(Item.getId(item));
    }

    @Override
    public Material getMaterialFromInternalName(String name) {
        MinecraftKey minecraftkey = new MinecraftKey(name);

        if (net.minecraft.server.Item.REGISTRY.d(minecraftkey)) {
            net.minecraft.server.Item item = (net.minecraft.server.Item) net.minecraft.server.Item.REGISTRY.get(minecraftkey);
            if (item != null) {
                return getMaterial(item);
            }
        }

        if (net.minecraft.server.Block.REGISTRY.d(minecraftkey)) {
            Block block = (Block) net.minecraft.server.Block.REGISTRY.get(minecraftkey);
            if (block != null) {
                return getMaterial(block);
            }
        }

        return null;
    }

    @Override
    public List<String> tabCompleteInternalMaterialName(String token, List<String> completions) {
        if (completions == null) {
            completions = new ArrayList<String>();
        }
        Iterator iterator = net.minecraft.server.Item.REGISTRY.iterator();
        while (iterator.hasNext()) {
            net.minecraft.server.Item item = (net.minecraft.server.Item) iterator.next();
            String name = net.minecraft.server.Item.REGISTRY.b(item).toString();
            if (StringUtil.startsWithIgnoreCase(name, token)) {
                completions.add(name);
            }
        }
        iterator = net.minecraft.server.Block.REGISTRY.iterator();
        while (iterator.hasNext()) {
            Block block = (Block) iterator.next();
            String name = net.minecraft.server.Block.REGISTRY.b(block).toString();
            if (StringUtil.startsWithIgnoreCase(name, token)) {
                completions.add(name);
            }
        }
        return completions;
    }

    @Override
    public ItemStack modifyItemStack(ItemStack stack, String arguments) {
        net.minecraft.server.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        try {
            nmsStack.setTag((NBTTagCompound) MojangsonParser.parse(arguments));
        } catch (MojangsonParseException ex) {
            Logger.getLogger(CraftMagicNumbers.class.getName()).log(Level.SEVERE, null, ex);
        }

        stack.setItemMeta(CraftItemStack.getItemMeta(nmsStack));

        return stack;
    }

    @Override
    public Statistic getStatisticFromInternalName(String name) {
        return CraftStatistic.getBukkitStatisticByName(name);
    }

    

    @Override
    public List<String> tabCompleteInternalStatisticOrAchievementName(String token, List<String> completions) {
        List<String> matches = new ArrayList<String>();
        Iterator iterator = StatisticList.stats.iterator();
        while (iterator.hasNext()) {
            String statistic = ((net.minecraft.server.Statistic) iterator.next()).name;
            if (statistic.startsWith(token)) {
                matches.add(statistic);
            }
        }
        return matches;
    }

    @Override
    public Advancement loadAdvancement(NamespacedKey key, String advancement) {
        if (Bukkit.getAdvancement(key) != null) {
            throw new IllegalArgumentException("Advancement " + key + " already exists.");
        }

        net.minecraft.server.Advancement.SerializedAdvancement nms = (net.minecraft.server.Advancement.SerializedAdvancement) ChatDeserializer.a(AdvancementDataWorld.DESERIALIZER, advancement, net.minecraft.server.Advancement.SerializedAdvancement.class);
        if (nms != null) {
            AdvancementDataWorld.REGISTRY.a(Maps.newHashMap(Collections.singletonMap(CraftNamespacedKey.toMinecraft(key), nms)));
            Advancement bukkit = Bukkit.getAdvancement(key);

            if (bukkit != null) {
                File file = new File(MinecraftServer.getServer().getAdvancementData().folder, key.getNamespace() + File.separator + key.getKey() + ".json");
                file.getParentFile().mkdirs();

                try {
                    Files.write(advancement, file, Charsets.UTF_8);
                } catch (IOException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error saving advancement " + key, ex);
                }

                MinecraftServer.getServer().getPlayerList().reload();

                return bukkit;
            }
        }

        return null;
    }

    @Override
    public boolean removeAdvancement(NamespacedKey key) {
        File file = new File(MinecraftServer.getServer().getAdvancementData().folder, key.getNamespace() + File.separator + key.getKey() + ".json");
        return file.delete();
    }

    /**
     * This helper class represents the different NBT Tags.
     * <p>
     * These should match NBTBase#getTypeId
     */
    public static class NBT {

        public static final int TAG_END = 0;
        public static final int TAG_BYTE = 1;
        public static final int TAG_SHORT = 2;
        public static final int TAG_INT = 3;
        public static final int TAG_LONG = 4;
        public static final int TAG_FLOAT = 5;
        public static final int TAG_DOUBLE = 6;
        public static final int TAG_BYTE_ARRAY = 7;
        public static final int TAG_STRING = 8;
        public static final int TAG_LIST = 9;
        public static final int TAG_COMPOUND = 10;
        public static final int TAG_INT_ARRAY = 11;
    }
}